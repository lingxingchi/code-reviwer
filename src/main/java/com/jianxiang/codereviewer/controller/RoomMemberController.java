package com.jianxiang.codereviewer.controller;

import com.jianxiang.codereviewer.common.util.ApiResponse;
import com.jianxiang.codereviewer.dto.member.InviteMemberRequest;
import com.jianxiang.codereviewer.dto.member.RoomMemberResponse;
import com.jianxiang.codereviewer.service.room.RoomMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @className: RoomMemberController
 * @author: jianXiang
 * @description: 房间成员管理控制器
 * @date: 2026/1/29 19:00
 */
@RestController
@Slf4j
@RequestMapping("/api/rooms/{roomCode}/members")
@RequiredArgsConstructor
public class RoomMemberController {

    private final RoomMemberService roomMemberService;

    /**
     * 邀请成员加入房间
     */
    @PostMapping
    public Mono<ApiResponse<RoomMemberResponse>> inviteMember(
            @PathVariable String roomCode,
            @Valid @RequestBody InviteMemberRequest request,
            Authentication authentication) {

        Long inviterId = (Long) authentication.getDetails();
        log.info("用户[{}]邀请用户[{}]加入房间[{}]", inviterId, request.getUserId(), roomCode);

        return roomMemberService.inviteMember(roomCode, request.getUserId(), inviterId)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("成员邀请成功"));
    }

    /**
     * 获取房间成员列表
     */
    @GetMapping
    public Mono<ApiResponse<List<RoomMemberResponse>>> getRoomMembers(
            @PathVariable String roomCode) {

        log.info("查询房间[{}]的成员列表", roomCode);

        return roomMemberService.getRoomMembers(roomCode)
                .collectList()
                .map(ApiResponse::success);
    }

    /**
     * 移除房间成员
     */
    @DeleteMapping("/{userId}")
    public Mono<ApiResponse<Void>> removeMember(
            @PathVariable String roomCode,
            @PathVariable Long userId,
            Authentication authentication) {

        Long operatorId = (Long) authentication.getDetails();
        log.info("用户[{}]从���间[{}]移除成员[{}]", operatorId, roomCode, userId);

        return roomMemberService.removeMember(roomCode, userId, operatorId)
                .then(Mono.just(ApiResponse.<Void>success()))
                .doOnSuccess(response -> log.info("成员移除成功"));
    }
}
