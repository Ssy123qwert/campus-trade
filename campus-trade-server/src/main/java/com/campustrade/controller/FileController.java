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

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("mp4", "mov", "avi", "mkv", "webm");
    private static final Set<String> ALLOWED_TYPES;

    static {
        ALLOWED_TYPES = new HashSet<>();
        ALLOWED_TYPES.addAll(ALLOWED_IMAGE_TYPES);
        ALLOWED_TYPES.addAll(ALLOWED_VIDEO_TYPES);
    }

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
            boolean isVideo = ALLOWED_VIDEO_TYPES.contains(ext);
            long maxSize = isVideo ? 50L * 1024 * 1024 : 5L * 1024 * 1024;
            if (file.getSize() > maxSize) {
                return R.fail(isVideo ? "单个视频不能超过50MB" : "单张图片不能超过5MB");
            }
            try {
                String subDir = isVideo ? "videos" : "images";
                String dir = uploadPath + File.separator + subDir;
                FileUtil.mkdir(dir);
                String filename = IdUtil.fastSimpleUUID() + "." + ext;
                File dest = new File(dir, filename);
                file.transferTo(dest);
                String urlPrefix = isVideo ? "/api/file/videos/" : "/api/file/image/";
                urls.add(urlPrefix + filename);
            } catch (IOException e) {
                return R.fail("上传失败: " + e.getMessage());
            }
        }
        return R.ok(urls);
    }

    @GetMapping("/image/{filename}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        return serveFile(filename, "images", ALLOWED_IMAGE_TYPES, true);
    }

    @GetMapping("/videos/{filename}")
    public ResponseEntity<Resource> video(@PathVariable String filename) {
        return serveFile(filename, "videos", ALLOWED_VIDEO_TYPES, false);
    }

    private ResponseEntity<Resource> serveFile(String filename, String subDir, Set<String> allowedExts, boolean isImage) {
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw BusinessException.badRequest("非法文件名");
        }
        String ext = FileUtil.extName(filename).toLowerCase();
        if (!allowedExts.contains(ext)) {
            throw BusinessException.badRequest("不支持的文件类型");
        }
        File file = new File(uploadPath + File.separator + subDir, filename);
        try {
            String canonicalPath = file.getCanonicalPath();
            String basePath = new File(uploadPath, subDir).getCanonicalPath();
            if (!canonicalPath.startsWith(basePath + File.separator) && !canonicalPath.equals(basePath)) {
                throw BusinessException.badRequest("非法文件路径");
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("文件路径解析失败");
        }
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType contentType;
        if (isImage) {
            contentType = switch (ext) {
                case "png" -> MediaType.IMAGE_PNG;
                case "gif" -> MediaType.IMAGE_GIF;
                case "webp" -> MediaType.parseMediaType("image/webp");
                default -> MediaType.IMAGE_JPEG;
            };
        } else {
            contentType = switch (ext) {
                case "mp4" -> MediaType.parseMediaType("video/mp4");
                case "webm" -> MediaType.parseMediaType("video/webm");
                case "mov" -> MediaType.parseMediaType("video/quicktime");
                case "avi" -> MediaType.parseMediaType("video/x-msvideo");
                case "mkv" -> MediaType.parseMediaType("video/x-matroska");
                default -> MediaType.APPLICATION_OCTET_STREAM;
            };
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .body(new FileSystemResource(file));
    }
}
