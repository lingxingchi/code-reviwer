package com.jianxiang.codereviewer.controller;

import com.jianxiang.codereviewer.common.util.ApiResponse;
import com.jianxiang.codereviewer.dto.auth.LoginRequest;
import com.jianxiang.codereviewer.dto.auth.LoginResponse;
import com.jianxiang.codereviewer.dto.auth.RegisterRequest;
import com.jianxiang.codereviewer.dto.user.UserDTO;
import com.jianxiang.codereviewer.service.user.AuthService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @className: AuthController
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 16:18
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Mono<ApiResponse<UserDTO>> register(@RequestBody RegisterRequest registerRequest) {
        return authService.register(registerRequest)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("用户注册成功:{}", registerRequest.getUsername()))
                .doOnError(error -> log.error("用户注册失败：{}", error.getMessage()));
    }

    @PostMapping("/login")
    public Mono<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        return authService.login(loginRequest)
                .map(ApiResponse::success)
                .doOnSuccess(response -> log.info("用户登录成功：{}", loginRequest.getUsername()))
                .doOnError(error -> log.error("用户登录失败：{}", error.getMessage()));
    }

    @GetMapping("/health")
    public Mono<ApiResponse<String>> health() {
        return Mono.just(ApiResponse.success("Auth service is running"));
    }
}
