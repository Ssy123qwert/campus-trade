package com.campustrade.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.campustrade.dto.R;
import com.campustrade.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${campustrade.upload.path:${user.dir}/uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @PostMapping("/upload")
    public R<List<String>> upload(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return R.fail("请选择文件");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String ext = FileUtil.extName(file.getOriginalFilename()).toLowerCase();
            if (!ALLOWED_TYPES.contains(ext)) {
                return R.fail("不支持的文件类型: " + ext);
            }
            if (file.getSize() > 5 * 1024 * 1024) {
                return R.fail("单张图片不能超过5MB");
            }
            try {
                String dir = uploadPath + File.separator + "images";
                FileUtil.mkdir(dir);
                String filename = IdUtil.fastSimpleUUID() + "." + ext;
                File dest = new File(dir, filename);
                file.transferTo(dest);
                urls.add("/api/file/image/" + filename);
            } catch (IOException e) {
                return R.fail("上传失败: " + e.getMessage());
            }
        }
        return R.ok(urls);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        // 路径穿越防护：文件名不能包含路径穿越字符
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw BusinessException.badRequest("非法文件名");
        }
        // 仅允许白名单中的图片扩展名
        String ext = FileUtil.extName(filename).toLowerCase();
        if (!ALLOWED_TYPES.contains(ext)) {
            throw BusinessException.badRequest("不支持的文件类型");
        }
        File file = new File(uploadPath + File.separator + "images", filename);
        // 二次确认：规范化路径必须在 uploadPath/images 目录之下，防止符号链接等绕过
        try {
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadPath, "images").getCanonicalPath();
            if (!canonicalPath.startsWith(basePath + File.separator) && !canonicalPath.equals(basePath)) {
                throw BusinessException.badRequest("非法文件路径");
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("文件路径解析失败");
        }
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType = switch (ext) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new FileSystemResource(file));
    }
}
