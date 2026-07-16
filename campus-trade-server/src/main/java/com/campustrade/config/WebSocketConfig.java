package com.campustrade.config;

import com.campustrade.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket + STOMP 配置
 *
 * 端点：/ws（支持 SockJS 降级）
 * 代理：/topic（广播）、/queue（点对点）
 * 鉴权：握手时通过 URL 参数或 Sec-WebSocket-Protocol 传递 JWT
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 客户端订阅前缀：点对点 /user，广播 /topic
        config.enableSimpleBroker("/topic", "/queue");
        // 客户端发送前缀
        config.setApplicationDestinationPrefixes("/app");
        // 点对点前缀
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 握手端点（前端连接地址）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();  // 浏览器兼容降级
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // JWT 鉴权拦截器
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                // 连接时验证 JWT
                if (accessor.getCommand() != null
                        && "CONNECT".equals(accessor.getCommand().name())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token == null || !token.startsWith("Bearer ")) {
                        throw new IllegalArgumentException("未提供有效的 JWT Token");
                    }
                    token = token.substring(7);
                    if (!jwtUtil.validateToken(token)) {
                        throw new IllegalArgumentException("JWT 验证失败");
                    }
                }
                return message;
            }
        });
    }
}
