package com.campustrade.controller;

import com.campustrade.dto.R;
import com.campustrade.entity.Announcement;
import com.campustrade.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcement")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

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

    @PostMapping("/save")
    public R<Announcement> save(@RequestParam String content,
                                @RequestHeader(value = "Authorization", required = false) String token) {
        Announcement announcement = new Announcement();
        announcement.setContent(content);
        announcementService.save(announcement);
        return R.ok(announcement);
    }

    @PutMapping("/update")
    public R<Announcement> update(@RequestParam Long id, @RequestParam String content,
                                   @RequestHeader(value = "Authorization", required = false) String token) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) return R.fail("公告不存在");
        announcement.setContent(content);
        announcementService.updateById(announcement);
        return R.ok(announcement);
    }

    @DeleteMapping("/delete")
    public R<String> delete(@RequestParam Long id,
                            @RequestHeader(value = "Authorization", required = false) String token) {
        announcementService.removeById(id);
        return R.ok("删除成功");
    }
}
