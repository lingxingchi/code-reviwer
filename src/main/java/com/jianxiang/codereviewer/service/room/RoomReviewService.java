package com.jianxiang.codereviewer.service.room;

import com.jianxiang.codereviewer.common.exception.BusinessException;
import com.jianxiang.codereviewer.domain.entity.ReviewRoom;
import com.jianxiang.codereviewer.domain.entity.User;
import com.jianxiang.codereviewer.domain.enums.RoomStatus;
import com.jianxiang.codereviewer.domain.repository.ReviewRoomRepository;
import com.jianxiang.codereviewer.domain.repository.RoomMemberRepository;
import com.jianxiang.codereviewer.domain.repository.UserRepository;
import com.jianxiang.codereviewer.dto.room.CreateRoomRequest;
import com.jianxiang.codereviewer.dto.room.RoomResponse;
import com.jianxiang.codereviewer.dto.room.UpdateRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * @className: RoomReviewService
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/27 19:04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomReviewService {

    private final ReviewRoomRepository reviewRoomRepository;

    private final UserRepository userRepository;

    private final RoomMemberService roomMemberService;

    public Mono<RoomResponse> createRoom(CreateRoomRequest createRoomRequest, Long userId) {
        return generateRoomCodeUnique()
                .flatMap(roomCode -> {
                    ReviewRoom reviewRoom = new ReviewRoom();
                    reviewRoom.setName(createRoomRequest.getName());
                    reviewRoom.setRoomCode(roomCode);
                    reviewRoom.setDescription(createRoomRequest.getDescription());
                    reviewRoom.setOwnerId(userId);
                    reviewRoom.setRepoInfo(createRoomRequest.getRepoInfo());
                    reviewRoom.setCodeScope(createRoomRequest.getCodeScope());

                    // 初始化状态和时间
                    reviewRoom.setStatus(RoomStatus.WAITING.getCode());
                    LocalDateTime now = LocalDateTime.now();
                    reviewRoom.setCreateTime(now);
                    reviewRoom.setUpdateTime(now);

                    return reviewRoomRepository.save(reviewRoom);
                })
                .flatMap(savedRoom -> roomMemberService.addOwner(savedRoom.getId(), userId)
                        .thenReturn(savedRoom))
                .flatMap(this::convertToResponse);
    }

    public Mono<RoomResponse> getRoomByCode(String roomCode) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(this::convertToResponse);
    }

    public Mono<RoomResponse> updateRoom(String roomCode, UpdateRoomRequest updateRoomRequest, Long userId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(room -> {
                    // 检查权限
                    if (!room.getOwnerId().equals(userId)) {
                        return Mono.error(new BusinessException("无权限操作"));
                    }

                    // 检查状态：只有等待开始状态才能编辑房间信息
                    RoomStatus roomStatus = RoomStatus.getRoomStatus(room.getStatus());
                    if (!roomStatus.canEdit()) {
                        return Mono.error(new BusinessException("当前状态不允许编辑房间信息"));
                    }

                    // 更新字段
                    if (updateRoomRequest.getName() != null) {
                        room.setName(updateRoomRequest.getName());
                    }
                    if (updateRoomRequest.getDescription() != null) {
                        room.setDescription(updateRoomRequest.getDescription());
                    }
                    if (updateRoomRequest.getRepoInfo() != null) {
                        room.setRepoInfo(updateRoomRequest.getRepoInfo());
                    }
                    if (updateRoomRequest.getCodeScope() != null) {
                        room.setCodeScope(updateRoomRequest.getCodeScope());
                    }
                    room.setUpdateTime(LocalDateTime.now());

                    return reviewRoomRepository.save(room);
                })
                .flatMap(this::convertToResponse);
    }

    /**
     * 获取房间列表（支持动态条件查询）
     *
     * @param status 房间状态（可选）
     * @param name 房间名称（可选）
     * @return 房间列表
     */
    public Flux<RoomResponse> getRoomList(String status, String name) {
        // ✅ 使用动态查询，自动处理参数为空的情况
        return reviewRoomRepository.findByDynamicConditions(status, name)
                .flatMap(this::convertToResponse);
    }

    public Flux<RoomResponse> getUserRoom(Long userId, String status) {
        Flux<ReviewRoom> roomFlux;
        if (status != null && !status.isEmpty()) {
            roomFlux = reviewRoomRepository.findByOwnerIdAndStatus(userId, status);
        } else {
            roomFlux = reviewRoomRepository.findByOwnerId(userId);
        }
        return roomFlux.flatMap(this::convertToResponse);
    }

    /**
     * 取消评审房间
     *
     * @param roomCode 房间代码
     * @param userId 用户ID
     * @return 房间响应
     */
    public Mono<RoomResponse> cancelRoom(String roomCode, Long userId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(room -> {
                    // 检查权限
                    if (!room.getOwnerId().equals(userId)) {
                        return Mono.error(new BusinessException("无权限操作"));
                    }

                    // 检查状态是否可以取消
                    RoomStatus currentStatus = RoomStatus.getRoomStatus(room.getStatus());
                    if (!currentStatus.canTransitionTo(RoomStatus.CANCELLED)) {
                        return Mono.error(new BusinessException(
                                String.format("房间当前状态[%s]不允许取消", currentStatus.getDesc())));
                    }

                    // 更新状态为已取消
                    room.setStatus(RoomStatus.CANCELLED.getCode());
                    room.setCloseTime(LocalDateTime.now());
                    room.setUpdateTime(LocalDateTime.now());

                    return reviewRoomRepository.save(room);
                })
                .flatMap(this::convertToResponse);
    }

    /**
     * 开始评审（WAITING → IN_PROGRESS）
     *
     * @param roomCode 房间代码
     * @param userId 用户ID
     * @return 房间响应
     */
    public Mono<RoomResponse> startRoom(String roomCode, Long userId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(room -> {
                    // 检查权限
                    if (!room.getOwnerId().equals(userId)) {
                        return Mono.error(new BusinessException("无权限操作"));
                    }

                    // 检查状态转换是否合法
                    RoomStatus currentStatus = RoomStatus.getRoomStatus(room.getStatus());
                    if (!currentStatus.canTransitionTo(RoomStatus.IN_PROGRESS)) {
                        return Mono.error(new BusinessException(
                                String.format("房间当前状态[%s]不允许开始评审", currentStatus.getDesc())));
                    }

                    // 更新状态为进行中
                    room.setStatus(RoomStatus.IN_PROGRESS.getCode());
                    room.setUpdateTime(LocalDateTime.now());

                    return reviewRoomRepository.save(room);
                })
                .flatMap(this::convertToResponse);
    }

    /**
     * 完成评审（IN_PROGRESS → COMPLETED）
     *
     * @param roomCode 房间代码
     * @param userId 用户ID
     * @return 房间响应
     */
    public Mono<RoomResponse> completeRoom(String roomCode, Long userId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(room -> {
                    // 检查权限
                    if (!room.getOwnerId().equals(userId)) {
                        return Mono.error(new BusinessException("无权限操作"));
                    }

                    // 检查状态转换是否合法
                    RoomStatus currentStatus = RoomStatus.getRoomStatus(room.getStatus());
                    if (!currentStatus.canTransitionTo(RoomStatus.COMPLETED)) {
                        return Mono.error(new BusinessException(
                                String.format("房间当前状态[%s]不允许完成评审", currentStatus.getDesc())));
                    }

                    // 更新状态为已完成
                    room.setStatus(RoomStatus.COMPLETED.getCode());
                    room.setCloseTime(LocalDateTime.now());
                    room.setUpdateTime(LocalDateTime.now());

                    return reviewRoomRepository.save(room);
                })
                .flatMap(this::convertToResponse);
    }

    /**
     * 生成唯一房间码（带重试次数限制）
     *
     * @return 唯一房间码
     */
    private Mono<String> generateRoomCodeUnique() {
        return generateRoomCodeWithRetry(5);
    }

    /**
     * 生成唯一房间码（递归重试）
     *
     * @param maxRetries 最大重试次数
     * @return 唯一房间码
     */
    private Mono<String> generateRoomCodeWithRetry(int maxRetries) {
        if (maxRetries <= 0) {
            return Mono.error(new BusinessException("生成房间码失败，请重试"));
        }

        return Mono.defer(() -> {
            String roomCode = generateRoomCode();
            return reviewRoomRepository.existsReviewRoomByRoomCode(roomCode)
                    .flatMap(exist -> {
                        if (exist) {
                            log.debug("房间码[{}]已存在，重试生成，剩余次数: {}", roomCode, maxRetries - 1);
                            return generateRoomCodeWithRetry(maxRetries - 1);
                        }
                        return Mono.just(roomCode);
                    });
        });
    }

    private String generateRoomCode() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private Mono<RoomResponse> convertToResponse(ReviewRoom reviewRoom) {
        return userRepository.findById(reviewRoom.getOwnerId())
                .map(User::getUsername)
                .defaultIfEmpty("未知用户")
                .map(username -> {
                    RoomResponse roomResponse = new RoomResponse();
                    BeanUtils.copyProperties(reviewRoom, roomResponse);
                    roomResponse.setOwnerUsername(username);
                    return roomResponse;
                });
    }
}
