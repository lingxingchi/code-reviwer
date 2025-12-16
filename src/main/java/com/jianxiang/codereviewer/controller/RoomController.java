package com.jianxiang.codereviewer.controller;

import com.jianxiang.codereviewer.common.util.ApiResponse;
import com.jianxiang.codereviewer.dto.room.CreateRoomRequest;
import com.jianxiang.codereviewer.dto.room.RoomResponse;
import com.jianxiang.codereviewer.dto.room.UpdateRoomRequest;
import com.jianxiang.codereviewer.service.room.RoomReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @className: RoomController
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/28 15:15
 */
@RestController
@Slf4j
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomReviewService roomReviewService;

    @PostMapping
    public Mono<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody CreateRoomRequest request,
                                                      Authentication authentication) {
        Long details = (Long) authentication.getDetails();
        log.info("用户[{}]创建房间：{}", details, request.getName());

        return roomReviewService.createRoom(request, details)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("房间创建成功: {}", response.getData().getRoomCode()));
    }

    @GetMapping("/{roomCode}")
    public Mono<ApiResponse<RoomResponse>> getRoomByCode(@PathVariable String roomCode) {
        return roomReviewService.getRoomByCode(roomCode)
                .map(ApiResponse::success);
    }

    @PutMapping("/{roomCode}")
    public Mono<ApiResponse<RoomResponse>> updateRoom(@PathVariable String roomCode,
                                                      @Valid @RequestBody UpdateRoomRequest request,
                                                      Authentication authentication) {
        Long userId = (Long) authentication.getDetails();

        return roomReviewService.updateRoom(roomCode, request, userId)
                .map(ApiResponse::success);
    }

    /**
     * 获取房间列表（支持筛选）
     */
    @GetMapping
    public Mono<ApiResponse<List<RoomResponse>>> getRoomList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name) {

        log.info("查询房间列表 - status: {}, name: {}", status, name);

        return roomReviewService.getRoomList(status, name)
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 获取我的房间列表
     */
    @GetMapping("/my")
    public Mono<ApiResponse<List<RoomResponse>>> getMyRooms(
            @RequestParam(required = false) String status,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("查询用户[{}]的房间列表", userId);

        return roomReviewService.getUserRoom(userId, status)
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 开始评审（WAITING → IN_PROGRESS）
     */
    @PutMapping("/{roomCode}/start")
    public Mono<ApiResponse<RoomResponse>> startRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("用户[{}]开始房间[{}]的评审", userId, roomCode);

        return roomReviewService.startRoom(roomCode, userId)
                .map(ApiResponse::success);
    }

    /**
     * 完成评审（IN_PROGRESS → COMPLETED）
     */
    @PutMapping("/{roomCode}/complete")
    public Mono<ApiResponse<RoomResponse>> completeRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("用户[{}]完成房间[{}]的评审", userId, roomCode);

        return roomReviewService.completeRoom(roomCode, userId)
                .map(ApiResponse::success);
    }

    /**
     * 取消评审
     */
    @PutMapping("/{roomCode}/cancel")
    public Mono<ApiResponse<RoomResponse>> cancelRoom(
            @PathVariable String roomCode,
            Authentication authentication) {

        Long userId = (Long) authentication.getDetails();
        log.info("用户[{}]取消房间[{}]的评审", userId, roomCode);

        return roomReviewService.cancelRoom(roomCode, userId)
                .map(ApiResponse::success);
    }
}
