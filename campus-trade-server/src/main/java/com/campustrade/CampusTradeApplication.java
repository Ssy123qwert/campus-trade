package com.campustrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 校园二手交易平台 - 启动入口
 *
 * 排除 ES 和 RabbitMQ 的自动配置（中间件未安装时也能启动）
 * 等中间件就绪后，去掉 exclude 即可启用对应功能
 */
@SpringBootApplication(exclude = {
        ElasticsearchDataAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
@EnableScheduling
public class CampusTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusTradeApplication.class, args);
    }
}
