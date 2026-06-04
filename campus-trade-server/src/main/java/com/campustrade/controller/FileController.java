package com.campustrade.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.campustrade.dto.R;
import org.springframework.beans.factory.annotation.Value;
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
    public byte[] image(@PathVariable String filename) throws IOException {
        File file = new File(uploadPath + File.separator + "images", filename);
        if (!file.exists()) {
            throw new RuntimeException("文件不存在");
        }
        return FileUtil.readBytes(file);
    }
}
