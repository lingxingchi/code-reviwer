package com.jianxiang.codereviewer.domain.repository;

import com.jianxiang.codereviewer.domain.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

/**
 * @className: UserRepository
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 15:26
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {

    /**
     * @param username:
     * @return Mono<User>
     * @author jianxiang
     * @description TODO
     * @date 2026/1/26 15:30
     */
    Mono<User> findByUsername(String username);

    /**
     * @param username:
     * @return Mono<Boolean>
     * @author jianxiang
     * @description TODO
     * @date 2026/1/26 15:30
     */
    Mono<Boolean> existsByUsername(String username);
}
