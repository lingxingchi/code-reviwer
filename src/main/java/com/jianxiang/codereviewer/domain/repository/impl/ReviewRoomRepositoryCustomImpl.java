package com.jianxiang.codereviewer.domain.repository.impl;

import com.jianxiang.codereviewer.domain.entity.ReviewRoom;
import com.jianxiang.codereviewer.domain.repository.ReviewRoomRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import static org.springframework.data.relational.core.query.Criteria.where;

/**
 * @className: ReviewRoomRepositoryCustomImpl
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/28 16:04
 */
@Repository
@RequiredArgsConstructor
public class ReviewRoomRepositoryCustomImpl implements ReviewRoomRepositoryCustom {

    private final R2dbcEntityTemplate template;

    @Override
    public Flux<ReviewRoom> findByDynamicConditions(String status, String name) {
        // 构建动态查询条件
        Criteria criteria = Criteria.empty();

        // 动态添加状态条件
        if (status != null && !status.isEmpty()) {
            criteria = criteria.and(where("status").is(status));
        }

        // 动态添加名称条件（模糊查询）
        if (name != null && !name.isEmpty()) {
            criteria = criteria.and(where("name").like("%" + name + "%"));
        }

        // 执行查询
        Query query = Query.query(criteria);
        return template.select(query, ReviewRoom.class);
    }
}