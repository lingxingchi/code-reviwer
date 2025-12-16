package com.jianxiang.codereviewer.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评审房间状态枚举（会议模式）
 *
 * 状态流转规则：
 * WAITING → IN_PROGRESS → COMPLETED
 *    ↓            ↓
 * CANCELLED ← CANCELLED
 *
 * 终态：COMPLETED、CANCELLED
 */
@Getter
@AllArgsConstructor
public enum RoomStatus {

    /**
     * 等待开始：房间已创建，准备阶段（可上传代码、邀请成员）
     */
    WAITING("WAITING", "等待开始"),

    /**
     * 进行中：评审会议正在进行
     */
    IN_PROGRESS("IN_PROGRESS", "进行中"),

    /**
     * 已完成：评审正常完成（终态）
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已取消：评审被取消（终态）
     */
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    /**
     * 根据状态码获取房间状态
     *
     * @param code 状态码
     * @return 房间状态枚举
     * @throws IllegalArgumentException 如果状态码无效
     */
    public static RoomStatus getRoomStatus(String code) {
        if (code == null) {
            return null;
        }
        for (RoomStatus status : RoomStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的房间状态码: " + code);
    }

    /**
     * 判断是否为等待开始状态
     */
    public boolean isWaiting() {
        return this == WAITING;
    }

    /**
     * 判断是否为进行中状态
     */
    public boolean isInProgress() {
        return this == IN_PROGRESS;
    }

    /**
     * 判断是否为已完成状态
     */
    public boolean isCompleted() {
        return this == COMPLETED;
    }

    /**
     * 判断是否为已取消状态
     */
    public boolean isCancelled() {
        return this == CANCELLED;
    }

    /**
     * 判断是否为终态（不可再变更）
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 判断是否可以评审（进行中时才能评论和编辑代码）
     */
    public boolean canReview() {
        return this == IN_PROGRESS;
    }

    /**
     * 判断是否可以编辑房间信息（等待开始时可编辑）
     */
    public boolean canEdit() {
        return this == WAITING;
    }

    /**
     * 判断是否可以邀请成员（等待开始和进行中都可以邀请）
     */
    public boolean canInvite() {
        return this == WAITING || this == IN_PROGRESS;
    }

    /**
     * 验证状态转换是否合法
     *
     * @param target 目标状态
     * @return 是否可以转换
     */
    public boolean canTransitionTo(RoomStatus target) {
        if (target == null) {
            return false;
        }

        switch (this) {
            case WAITING:
                // 等待开始 → 进行中/已取消
                return target == IN_PROGRESS || target == CANCELLED;

            case IN_PROGRESS:
                // 进行中 → 已完成/已取消
                return target == COMPLETED || target == CANCELLED;

            case COMPLETED:
            case CANCELLED:
                // 终态不可转换
                return false;

            default:
                return false;
        }
    }

    /**
     * 获取允许转换到的状态列表（用于前端展示可用操作）
     *
     * @return 允许转换的状态数组
     */
    public RoomStatus[] getAllowedTransitions() {
        switch (this) {
            case WAITING:
                return new RoomStatus[]{IN_PROGRESS, CANCELLED};
            case IN_PROGRESS:
                return new RoomStatus[]{COMPLETED, CANCELLED};
            case COMPLETED:
            case CANCELLED:
            default:
                return new RoomStatus[0];
        }
    }

    /**
     * 获取状态转换的操作名称（用于日志和前端按钮文案）
     *
     * @param target 目标状态
     * @return 操作名称
     */
    public String getTransitionAction(RoomStatus target) {
        if (!canTransitionTo(target)) {
            return null;
        }

        if (this == WAITING && target == IN_PROGRESS) {
            return "开始评审";
        }
        if (this == IN_PROGRESS && target == COMPLETED) {
            return "完成评审";
        }
        if (target == CANCELLED) {
            return "取消评审";
        }

        return "变更状态";
    }
}
