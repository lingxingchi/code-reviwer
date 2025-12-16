package com.jianxiang.codereviewer.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 房间成员角色枚举
 *
 * @author jianXiang
 * @date 2026/1/28
 */
@Getter
@AllArgsConstructor
public enum RoomMemberRole {

    /**
     * 房主：创建房间的用户，拥有最高权限
     */
    OWNER("OWNER", "房主"),

    /**
     * 成员：被邀请或主动加入的普通成员
     */
    MEMBER("MEMBER", "成员");

    private final String code;
    private final String desc;

    /**
     * 根据角色码获取枚举
     *
     * @param code 角色码
     * @return 角色枚举
     * @throws IllegalArgumentException 如果角色码无效
     */
    public static RoomMemberRole getRoomMemberRole(String code) {
        if (code == null) {
            return null;
        }
        for (RoomMemberRole role : RoomMemberRole.values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("无效的房间成员角色码: " + code);
    }

    /**
     * 判断是否为房主
     */
    public boolean isOwner() {
        return this == OWNER;
    }

    /**
     * 判断是否为普通成员
     */
    public boolean isMember() {
        return this == MEMBER;
    }
}
