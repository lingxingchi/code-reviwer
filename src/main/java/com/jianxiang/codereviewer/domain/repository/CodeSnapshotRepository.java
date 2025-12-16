package com.jianxiang.codereviewer.domain.repository;

import com.jianxiang.codereviewer.domain.entity.CodeSnapshot;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @className: CodeSnapshotRepository
 * @author: jianXiang
 * @description: 代码快照Repository
 * @date: 2026/1/29
 */
@Repository
public interface CodeSnapshotRepository extends ReactiveCrudRepository<CodeSnapshot, Long> {

    /**
     * 根据房间ID查询所有快照（按版本号倒序）
     */
    Flux<CodeSnapshot> findByRoomIdOrderByVersionDesc(Long roomId);

    /**
     * 根据房间ID和版本号查询快照
     */
    Mono<CodeSnapshot> findByRoomIdAndVersion(Long roomId, Integer version);

    /**
     * 查询房间的最大版本号
     */
    @Query("SELECT MAX(version) FROM code_snapshot WHERE room_id = :roomId")
    Mono<Integer> findMaxVersionByRoomId(Long roomId);

    /**
     * 统计房间的快照数量
     */
    Mono<Long> countByRoomId(Long roomId);

    /**
     * 根据房间ID删除所有快照
     */
    Mono<Void> deleteByRoomId(Long roomId);
}
