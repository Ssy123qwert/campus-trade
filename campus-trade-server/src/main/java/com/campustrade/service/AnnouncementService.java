package com.campustrade.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campustrade.entity.Announcement;
import com.campustrade.mapper.AnnouncementMapper;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService extends ServiceImpl<AnnouncementMapper, Announcement> {
}
