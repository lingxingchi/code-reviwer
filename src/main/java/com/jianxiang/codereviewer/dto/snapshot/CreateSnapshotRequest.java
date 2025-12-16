package com.jianxiang.codereviewer.dto.snapshot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建代码快照请求 DTO
 *
 * @author jianXiang
 * @date 2026/1/29
 */
@Data
public class CreateSnapshotRequest {

    /**
     * 代码内容
     */
    @NotBlank(message = "代码内容不能为空")
    private String content;

    /**
     * 编程语言
     */
    @NotBlank(message = "编程语言不能为空")
    @Size(max = 50, message = "编程语言长度不能超过50个字符")
    private String language;

    /**
     * 文件路径
     */
    @Size(max = 500, message = "文件路径长度不能超过500个字符")
    private String filePath;

    /**
     * 版本说明
     */
    @Size(max = 500, message = "版本说明长度不能超过500个字符")
    private String description;
}
