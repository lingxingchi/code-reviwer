package com.jianxiang.codereviewer.domain.repository;

import com.jianxiang.codereviewer.domain.entity.ReviewRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @className: ReviewRoomRepository
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/27 18:38
 */
@Repository
public interface ReviewRoomRepository
    extends ReactiveCrudRepository<ReviewRoom, Long>,
            ReviewRoomRepositoryCustom {  // ⭐ 继承自定义接口，获得动态查询能力

    Mono<ReviewRoom> findByRoomCode(String roomCode);

    Mono<Boolean> existsReviewRoomByRoomCode(String roomCode);

    Flux<ReviewRoom> findByName(String name);

    Flux<ReviewRoom> findByNameAndStatus(String name, String status);

    Flux<ReviewRoom> findByOwnerId(Long ownerId);

    Flux<ReviewRoom> findByOwnerIdAndStatus(Long ownerId, String status);

    Mono<Boolean> existsByIdAndOwnerId(Long id, Long ownerId);

    Flux<ReviewRoom> findByStatus(String status);

}
