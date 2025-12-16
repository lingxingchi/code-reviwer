package com.jianxiang.codereviewer.controller;

import com.jianxiang.codereviewer.common.util.ApiResponse;
import com.jianxiang.codereviewer.dto.snapshot.CreateSnapshotRequest;
import com.jianxiang.codereviewer.dto.snapshot.SnapshotResponse;
import com.jianxiang.codereviewer.service.snapshot.CodeSnapshotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @className: CodeSnapshotController
 * @author: jianXiang
 * @description: 代码快照控制器
 * @date: 2026/1/29
 */
@RestController
@Slf4j
@RequestMapping("/api/rooms/{roomCode}/snapshots")
@RequiredArgsConstructor
public class CodeSnapshotController {

    private final CodeSnapshotService codeSnapshotService;

    /**
     * 上传代码快照
     */
    @PostMapping
    public Mono<ApiResponse<SnapshotResponse>> createSnapshot(
            @PathVariable String roomCode,
            @Valid @RequestBody CreateSnapshotRequest request,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("用户[{}]在房间[{}]上传代码快照", userId, roomCode);

        return codeSnapshotService.createSnapshot(roomCode, request, userId)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("快照创建成功，版本号: {}", response.getData().getVersion()));
    }

    /**
     * 获取房间的快照列表
     */
    @GetMapping
    public Mono<ApiResponse<List<SnapshotResponse>>> getRoomSnapshots(
            @PathVariable String roomCode) {

        log.info("查询房间[{}]的快照列表", roomCode);

        return codeSnapshotService.getRoomSnapshots(roomCode)
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 根据版本号获取快照详情
     */
    @GetMapping("/{version}")
    public Mono<ApiResponse<SnapshotResponse>> getSnapshotByVersion(
            @PathVariable String roomCode,
            @PathVariable Integer version) {

        log.info("查询房间[{}]的版本[{}]快照", roomCode, version);

        return codeSnapshotService.getSnapshotByVersion(roomCode, version)
                .map(ApiResponse::success);
    }

    /**
     * 删除快照
     */
    @DeleteMapping("/{snapshotId}")
    public Mono<ApiResponse<Void>> deleteSnapshot(
            @PathVariable String roomCode,
            @PathVariable Long snapshotId,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("用户[{}]删除快照[{}]", userId, snapshotId);

        return codeSnapshotService.deleteSnapshot(snapshotId, userId)
                .then(Mono.just(ApiResponse.<Void>success()))
                .doOnSuccess(response -> log.info("快照删除成功"));
    }
}
