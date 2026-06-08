package com.campustrade.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 只映射 /uploads/ 路径，不覆盖 Spring Boot 默认的静态资源配置
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/opt/campus-trade/uploads/");
    }
}
