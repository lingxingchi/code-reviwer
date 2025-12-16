package com.jianxiang.codereviewer.service.snapshot;

import com.jianxiang.codereviewer.common.exception.BusinessException;
import com.jianxiang.codereviewer.domain.entity.CodeSnapshot;
import com.jianxiang.codereviewer.domain.entity.User;
import com.jianxiang.codereviewer.domain.repository.CodeSnapshotRepository;
import com.jianxiang.codereviewer.domain.repository.ReviewRoomRepository;
import com.jianxiang.codereviewer.domain.repository.RoomMemberRepository;
import com.jianxiang.codereviewer.domain.repository.UserRepository;
import com.jianxiang.codereviewer.dto.snapshot.CreateSnapshotRequest;
import com.jianxiang.codereviewer.dto.snapshot.SnapshotResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @className: CodeSnapshotService
 * @author: jianXiang
 * @description: 代码快照服务
 * @date: 2026/1/29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeSnapshotService {

    private final CodeSnapshotRepository codeSnapshotRepository;
    private final ReviewRoomRepository reviewRoomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserRepository userRepository;

    /**
     * 创建代码快照
     */
    public Mono<SnapshotResponse> createSnapshot(String roomCode, CreateSnapshotRequest request, Long userId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                // 检查用户是否是房间成员
                .flatMap(room -> roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)
                        .filter(exists -> exists)
                        .switchIfEmpty(Mono.error(new BusinessException("您不是该房间的成员")))
                        .thenReturn(room))
                // 获取下一个版本号
                .flatMap(room -> codeSnapshotRepository.findMaxVersionByRoomId(room.getId())
                        .defaultIfEmpty(0)
                        .map(maxVersion -> maxVersion + 1)
                        .map(nextVersion -> {
                            CodeSnapshot snapshot = new CodeSnapshot();
                            snapshot.setRoomId(room.getId());
                            snapshot.setVersion(nextVersion);
                            snapshot.setContent(request.getContent());
                            snapshot.setLanguage(request.getLanguage());
                            snapshot.setFilePath(request.getFilePath());
                            snapshot.setDescription(request.getDescription());
                            snapshot.setCreatedBy(userId);
                            snapshot.setCreateTime(LocalDateTime.now());
                            return snapshot;
                        }))
                .flatMap(codeSnapshotRepository::save)
                .flatMap(this::convertToResponse);
    }

    /**
     * 根据ID获取快照详情
     */
    public Mono<SnapshotResponse> getSnapshotById(Long snapshotId) {
        return codeSnapshotRepository.findById(snapshotId)
                .switchIfEmpty(Mono.error(new BusinessException("快照不存在")))
                .flatMap(this::convertToResponse);
    }

    /**
     * 根据房间和版本号获取快照
     */
    public Mono<SnapshotResponse> getSnapshotByVersion(String roomCode, Integer version) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMap(room -> codeSnapshotRepository.findByRoomIdAndVersion(room.getId(), version))
                .switchIfEmpty(Mono.error(new BusinessException("快照版本不存在")))
                .flatMap(this::convertToResponse);
    }

    /**
     * 获取房间的快照列表
     */
    public Flux<SnapshotResponse> getRoomSnapshots(String roomCode) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                .flatMapMany(room -> codeSnapshotRepository.findByRoomIdOrderByVersionDesc(room.getId()))
                .flatMap(this::convertToResponse);
    }

    /**
     * 删除快照
     */
    public Mono<Void> deleteSnapshot(Long snapshotId, Long userId) {
        return codeSnapshotRepository.findById(snapshotId)
                .switchIfEmpty(Mono.error(new BusinessException("快照不存在")))
                .flatMap(snapshot -> reviewRoomRepository.findById(snapshot.getRoomId())
                        .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                        // 检查权限：只有创建者或房主可以删除
                        .flatMap(room -> {
                            if (snapshot.getCreatedBy().equals(userId) || room.getOwnerId().equals(userId)) {
                                return Mono.just(snapshot);
                            }
                            return Mono.error(new BusinessException("无权限删除该快照"));
                        }))
                .flatMap(snapshot -> codeSnapshotRepository.deleteById(snapshot.getId()));
    }

    /**
     * 转换为响应对象
     */
    private Mono<SnapshotResponse> convertToResponse(CodeSnapshot snapshot) {
        return userRepository.findById(snapshot.getCreatedBy())
                .map(User::getUsername)
                .defaultIfEmpty("未知用户")
                .map(username -> SnapshotResponse.builder()
                        .id(snapshot.getId())
                        .roomId(snapshot.getRoomId())
                        .version(snapshot.getVersion())
                        .content(snapshot.getContent())
                        .language(snapshot.getLanguage())
                        .filePath(snapshot.getFilePath())
                        .description(snapshot.getDescription())
                        .createdBy(snapshot.getCreatedBy())
                        .createdByUsername(username)
                        .createTime(snapshot.getCreateTime())
                        .build());
    }
}
