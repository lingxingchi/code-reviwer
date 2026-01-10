package com.jianxiang.codereviewer.websocket.message;

/**
 * WebSocket消息类型枚举
 */
public enum MessageType {
    // 房间管理
    JOIN_ROOM,           // 加入房间
    LEAVE_ROOM,          // 离开房间
    ROOM_MEMBER_UPDATE,  // 房间成员更新

    // 代码同步
    CODE_UPDATE,         // 代码更新
    CODE_CURSOR,         // 光标位置
    CODE_SELECTION,      // 代码选择

    // 评论
    COMMENT_ADD,         // 添加评论
    COMMENT_UPDATE,      // 更新评论
    COMMENT_DELETE,      // 删除评论

    // 系统
    SYSTEM_NOTIFICATION, // 系统通知
    ERROR                // 错误消息
}
