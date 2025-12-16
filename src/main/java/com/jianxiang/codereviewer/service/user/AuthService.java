package com.jianxiang.codereviewer.service.user;

import com.jianxiang.codereviewer.common.exception.BusinessException;
import com.jianxiang.codereviewer.common.util.JwtUtil;
import com.jianxiang.codereviewer.domain.entity.User;
import com.jianxiang.codereviewer.domain.enums.UserStatus;
import com.jianxiang.codereviewer.domain.repository.UserRepository;
import com.jianxiang.codereviewer.dto.auth.LoginRequest;
import com.jianxiang.codereviewer.dto.auth.LoginResponse;
import com.jianxiang.codereviewer.dto.auth.RegisterRequest;
import com.jianxiang.codereviewer.dto.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * @className: AuthService
 * @author: jianXiang
 * @description: TODO
 * @date: 2026/1/26 15:40
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    public Mono<LoginResponse> login(LoginRequest loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
                .switchIfEmpty(Mono.error(new BusinessException("用户名密码错误")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new BusinessException("用户名密码错误"));
                    }

                    if (!UserStatus.getUserStatus(user.getStatus()).isEnabled()) {
                        return Mono.error(new BusinessException("用户已被禁用"));
                    }

                    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
                    String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());
                    UserDTO userDTO = new UserDTO();
                    BeanUtils.copyProperties(user, userDTO);

                    LoginResponse loginResponse = new LoginResponse().builder()
                            .token(token)
                            .refreshToken(refreshToken)
                            .user(userDTO)
                            .build();

                    return Mono.just(loginResponse);
                });
    }

    public Mono<UserDTO> register(RegisterRequest registerRequest) {
        return userRepository.existsByUsername(registerRequest.getUsername())
                .flatMap(exist -> {
                   if (exist) {
                       return Mono.error(new BusinessException("用户已存在"));
                   }

                    User user = new User();
                   user.setUsername(registerRequest.getUsername());
                   user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
                   user.setEmail(registerRequest.getEmail());
                   user.setNickname(registerRequest.getNickname());
                   user.setStatus(UserStatus.ENABLED.getCode());
                   user.setCreateTime(LocalDateTime.now());
                   user.setUpdateTime(LocalDateTime.now());

                   return userRepository.save(user);
                })
                .map(saveUser ->{
                    UserDTO userDTO = new UserDTO();
                    BeanUtils.copyProperties(saveUser, userDTO);
                    return userDTO;
                });
    }

}
