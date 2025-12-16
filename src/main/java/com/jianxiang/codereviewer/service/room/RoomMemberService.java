package com.jianxiang.codereviewer.service.room;

import com.jianxiang.codereviewer.common.exception.BusinessException;
import com.jianxiang.codereviewer.domain.entity.RoomMember;
import com.jianxiang.codereviewer.domain.entity.User;
import com.jianxiang.codereviewer.domain.enums.RoomMemberRole;
import com.jianxiang.codereviewer.domain.repository.ReviewRoomRepository;
import com.jianxiang.codereviewer.domain.repository.RoomMemberRepository;
import com.jianxiang.codereviewer.domain.repository.UserRepository;
import com.jianxiang.codereviewer.dto.member.RoomMemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @className: RoomMemberService
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/28 17:34
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomMemberService {

    private final RoomMemberRepository roomMemberRepository;

    private final ReviewRoomRepository reviewRoomRepository;

    private final UserRepository userRepository;

    public Mono<RoomMemberResponse> inviteMember(String roomCode, Long userId, Long inviterId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("房间不存在")))
                // 检查是否是房主（优化：直接用 ownerId 判断）
                .flatMap(room -> {
                    if (!room.getOwnerId().equals(inviterId)) {
                        return Mono.error(new BusinessException("只有房主可以邀请成员"));
                    }
                    return Mono.just(room);
                })
                // 检查被邀请用户是否存在
                .flatMap(room -> userRepository.findById(userId)
                        .switchIfEmpty(Mono.error(new BusinessException("邀请的用户不存在")))
                        .thenReturn(room))
                // 检查用户是否已是房间成员
                .flatMap(room -> roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)
                        .filter(exists -> !exists)
                        .switchIfEmpty(Mono.error(new BusinessException("用户已是房间成员")))
                        .thenReturn(room))
                // 创建成员记录
                .flatMap(room -> {
                    RoomMember roomMember = new RoomMember();
                    roomMember.setRoomId(room.getId());
                    roomMember.setUserId(userId);
                    roomMember.setRole(RoomMemberRole.MEMBER.getCode());
                    roomMember.setJoinTime(LocalDateTime.now());
                    roomMember.setInvitedBy(inviterId);
                    return roomMemberRepository.save(roomMember);
                })
                .flatMap(this::convertToResponse);
    }

    public Flux<RoomMemberResponse> getRoomMembers(String roomCode) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("输入的房间号错误")))
                .flatMapMany(room -> roomMemberRepository.findByRoomId(room.getId()))
                .flatMap(this::convertToResponse);
    }

    public Mono<Void> removeMember(String roomCode, Long userId, Long operatorId) {
        return reviewRoomRepository.findByRoomCode(roomCode)
                .switchIfEmpty(Mono.error(new BusinessException("非法房间号")))
                // 1. 检查操作者是否是房主（优化：直接用 ownerId 判断）
                .flatMap(room -> {
                    if (!room.getOwnerId().equals(operatorId)) {
                        return Mono.error(new BusinessException("只有房主可以移除成员"));
                    }
                    return Mono.just(room);
                })
                // 2. 检查不能移除房主自己
                .flatMap(room -> {
                    if (userId.equals(room.getOwnerId())) {
                        return Mono.error(new BusinessException("不能移除房主"));
                    }
                    return Mono.just(room);
                })
                // 3. 检查被移除用户是否是房间成员
                .flatMap(room -> roomMemberRepository.existsByRoomIdAndUserId(room.getId(), userId)
                        .filter(exists -> exists)
                        .switchIfEmpty(Mono.error(new BusinessException("该用户不是房间成员")))
                        .thenReturn(room))
                // 4. 执行删除
                .flatMap(room -> roomMemberRepository.deleteByRoomIdAndUserId(room.getId(), userId))
                .then();
    }

    private Mono<RoomMemberResponse> convertToResponse(RoomMember roomMember) {
        Mono<String> userMono = userRepository.findById(roomMember.getUserId())
                .map(User::getUsername)
                .defaultIfEmpty("未知用户");

        Mono<String> inviterMono = Mono.justOrEmpty(roomMember.getInvitedBy())
                .flatMap(inviterId -> userRepository.findById(inviterId)
                        .map(User::getUsername))
                .defaultIfEmpty("无");

        return Mono.zip(userMono, inviterMono)
                .map(tuple -> {
                    String userName = tuple.getT1();
                    String inviterName = tuple.getT2();

                    RoomMemberRole roomMemberRole = RoomMemberRole.getRoomMemberRole(roomMember.getRole());

                    return RoomMemberResponse.builder()
                            .id(roomMember.getId())
                            .roomId(roomMember.getRoomId())
                            .userId(roomMember.getUserId())
                            .role(roomMemberRole.getCode())
                            .invitedBy(roomMember.getInvitedBy())
                            .invitedByUsername(inviterName)
                            .roleDesc(roomMemberRole != null ? roomMemberRole.getDesc() : "未知角色")
                            .username(userName)
                            .build();
                });
    }

    public Mono<RoomMember> addOwner(Long roomId, Long userId) {
        RoomMember roomMember = new RoomMember();
        roomMember.setRoomId(roomId);
        roomMember.setUserId(userId);
        roomMember.setRole(RoomMemberRole.OWNER.getCode());
        roomMember.setJoinTime(LocalDateTime.now());
        roomMember.setInvitedBy(null);
        return roomMemberRepository.save(roomMember);
    }
}
