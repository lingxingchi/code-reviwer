package com.jianxiang.codereviewer.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建评审房间请求 DTO
 */
@Data
public class CreateRoomRequest {

    /**
     * 房间名称
     */
    @NotBlank(message = "房间名不得为空")
    @Size(max = 50, message = "房间明不得超50字符")
    private String name;

    /**
     * 房间描述
     */
    @Size(max = 500, message = "房间描述不得超过500字符")
    private String description;

    /**
     * 仓库信息（Git URL等）
     */
    @Size(max = 500, message = "仓库信息不能超过500字符")
    private String repoInfo;

    /**
     * 代码范围说明
     */
    @Size(max = 500, message = "代码范围不得超过500字符")
    private String codeScope;
}
