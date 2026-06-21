package com.pinfan.controller;

import com.pinfan.common.exception.BusinessException;
import com.pinfan.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Tag(name = "04-文件上传")
@Slf4j
public class FileController {

    // 允许的图片扩展名白名单（小写），防御伪造 Content-Type 上传可执行脚本
    private static final Set<String> ALLOWED_EXTS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");

    @Value("${pinfan.upload.dir}")
    private String uploadDir;

    @Value("${pinfan.upload.url-prefix}")
    private String uploadUrlPrefix;

    @PostMapping("/upload")
    @Operation(summary = "上传图片（multipart/form-data，参数名 file）")
    public R<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        // 校验
        if (file.isEmpty()) {
            throw new BusinessException(400, "文件为空");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException(400, "只支持图片格式");
        }

        // 提取扩展名（防 null 和无 . 文件名）
        String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new BusinessException(400, "文件名为空");
        }
        int dotIdx = originalName.lastIndexOf(".");
        String ext = dotIdx >= 0 ? originalName.substring(dotIdx).toLowerCase() : "";

        // 扩展名白名单校验（防御伪造 Content-Type 上传 .jsp/.sh 等）
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new BusinessException(400, "只支持 png/jpg/jpeg/gif/webp");
        }

        // 生成新文件名 = UUID + 扩展名
        String newName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 确保目录存在
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 保存到磁盘
        File dest = new File(uploadDir, newName);
        file.transferTo(dest);
        
        String url = uploadUrlPrefix + newName;
        log.info("上传成功: 文件名={}, URL={}", originalName, url);
        return R.ok("上传成功", url);
    }
}
