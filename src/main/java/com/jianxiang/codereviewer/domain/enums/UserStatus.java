package com.jianxiang.codereviewer.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatus {

    DISABLED(0, "禁用"),

    ENABLED(1, "启用");

    private final Integer code;

    private final String desc;

    public static UserStatus getUserStatus(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus userStatus : UserStatus.values()) {
            if (userStatus.getCode().equals(code)) {
                return userStatus;
            }
        }
        throw new IllegalArgumentException("无效状态码:" + code);
    }

    public Boolean isEnabled() {
        return this == ENABLED;
    }
}
