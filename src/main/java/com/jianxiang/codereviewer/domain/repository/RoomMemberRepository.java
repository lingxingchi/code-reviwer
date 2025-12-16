package com.jianxiang.codereviewer.domain.repository;

import com.jianxiang.codereviewer.domain.entity.RoomMember;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 房间成员仓库接口
 *
 * @author jianXiang
 * @date 2026/1/28
 */
@Repository
public interface RoomMemberRepository extends ReactiveCrudRepository<RoomMember, Long> {

    /**
     * 查询房间所有成员
     *
     * @param roomId 房间ID
     * @return 成员列表
     */
    Flux<RoomMember> findByRoomId(Long roomId);

    /**
     * 查询用户参与的所有房间成员记录
     *
     * @param userId 用户ID
     * @return 成员列表
     */
    Flux<RoomMember> findByUserId(Long userId);

    /**
     * 检查用户是否是房间成员
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 是否是成员
     */
    Mono<Boolean> existsByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 查询特定用户在房间的成员记录
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 成员记录
     */
    Mono<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 删除房间成员
     *
     * @param roomId 房间ID
     * @param userId 用户ID
     * @return 删除结果
     */
    Mono<Void> deleteByRoomIdAndUserId(Long roomId, Long userId);

    /**
     * 查询特定角色的房间成员
     *
     * @param roomId 房间ID
     * @param role 角色
     * @return 成员记录
     */
    Mono<RoomMember> findByRoomIdAndRole(Long roomId, String role);

    /**
     * 统计房间成员数量
     *
     * @param roomId 房间ID
     * @return 成员数量
     */
    Mono<Long> countByRoomId(Long roomId);
}
