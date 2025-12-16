package com.jianxiang.codereviewer.domain.repository;

import com.jianxiang.codereviewer.domain.entity.ReviewRoom;
import reactor.core.publisher.Flux;

public interface ReviewRoomRepositoryCustom {

      /**
       * 动态条件查询房间列表
       *
       * @param status 状态（可选）
       * @param name 名称（可选）
       * @return 房间列表
       */
      Flux<ReviewRoom> findByDynamicConditions(String status, String name);
  }