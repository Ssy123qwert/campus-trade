package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Announcement;
import com.campustrade.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公告管理 Controller
 *
 * 安全说明：
 * - GET 接口公开（已通过 SecurityConfig /api/announcement/** GET 放行）
 * - POST/PUT/DELETE 需要 ADMIN 权限
 */
@RestController
@RequestMapping("/api/announcement")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/latest")
    public R<Announcement> latest() {
        List<Announcement> list = announcementService.lambdaQuery()
                .orderByDesc(Announcement::getCreateTime)
                .last("LIMIT 1")
                .list();
        return R.ok(list.isEmpty() ? null : list.get(0));
    }

    @GetMapping("/list")
    public R<List<Announcement>> list() {
        return R.ok(announcementService.lambdaQuery()
                .orderByDesc(Announcement::getCreateTime)
                .list());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/save")
    public R<Announcement> save(@RequestParam String content) {
        if (content == null || content.isBlank()) {
            return R.fail("公告内容不能为空");
        }
        if (content.length() > 2000) {
            return R.fail("公告内容不超过2000字");
        }
        Announcement announcement = new Announcement();
        announcement.setContent(content);
        announcementService.save(announcement);
        return R.ok(announcement);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/update")
    public R<Announcement> update(@RequestParam Long id, @RequestParam String content) {
        if (content == null || content.isBlank()) {
            return R.fail("公告内容不能为空");
        }
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) return R.fail("公告不存在");
        announcement.setContent(content);
        announcementService.updateById(announcement);
        return R.ok(announcement);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam Long id) {
        announcementService.removeById(id);
        return R.ok("删除成功");
    }
}
