package com.campustrade.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * JWT 令牌工具类
 * - Access Token：短时效（2h），含 userId + role，用于接口鉴权
 * - Refresh Token：长时效（7d），仅含 userId，用于换取新 Access Token
 * - 黑名单机制：登出时将 Token 加入 Redis 黑名单，使其立即失效
 */
@Component
public class JwtUtil {

    private final SecretKey secretKey;          // 签名密钥
    private final long accessTokenExpire;        // Access Token 有效期（秒）
    private final long refreshTokenExpire;       // Refresh Token 有效期（秒）
    private final StringRedisTemplate redisTemplate;

    // Redis 黑名单前缀
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    // Redis Token 存储前缀
    private static final String TOKEN_PREFIX = "token:refresh:";

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expire}") long accessTokenExpire,
            @Value("${jwt.refresh-token-expire}") long refreshTokenExpire,
            StringRedisTemplate redisTemplate) {
        // 用 UTF-8 字节构造足够长的密钥（至少 256 位 = 32 字节）
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpire = accessTokenExpire;
        this.refreshTokenExpire = refreshTokenExpire;
        this.redisTemplate = redisTemplate;
    }

    // ======================== 生成 Token ========================

    /**
     * 生成 Access Token
     * @param userId 用户 ID
     * @param role   用户角色（0=普通用户 1=管理员）
     * @return JWT 字符串
     */
    public String generateAccessToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpire * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId))           // 主题：用户 ID
                .claim("role", role)                        // 自定义声明：角色
                .issuedAt(now)                              // 签发时间
                .expiration(expiry)                         // 过期时间
                .signWith(secretKey)                        // 签名
                .compact();
    }

    /**
     * 生成 Refresh Token
     * @param userId 用户 ID
     * @param role   用户角色（0=普通用户 1=管理员），刷新后保持角色
     * @return JWT 字符串
     */
    public String generateRefreshToken(Long userId, Integer role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenExpire * 1000);

        String token = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("type", "refresh")                    // 标记为 Refresh Token
                .claim("role", role)                         // 保存角色，刷新时使用
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();

        // Refresh Token 存入 Redis，支持服务端主动失效
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + userId,
                token,
                refreshTokenExpire,
                TimeUnit.SECONDS
        );

        return token;
    }

    // ======================== 验证 Token ========================

    /**
     * 验证 Token 是否有效
     * @param token JWT 字符串
     * @return true=有效, false=无效（过期/签名错误/在黑名单中）
     */
    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) return false;
            // 检查是否在黑名单中（登出后加入）
            if (isBlacklisted(token)) return false;
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 Token 中提取用户 ID
     * @param token JWT 字符串
     * @return 用户 ID，解析失败返回 null
     */
    public Long getUserIdFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 Token 中提取角色
     * @param token JWT 字符串
     * @return 角色（0=普通用户 1=管理员），解析失败返回 null
     */
    public Integer getRoleFromToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.get("role", Integer.class);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断 Token 是否已过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    // ======================== 黑名单管理 ========================

    /**
     * 将 Token 加入黑名单（登出时调用）
     * 黑名单的 TTL = Token 剩余有效期，过期自动清理
     * @param token 要失效的 Token
     */
    public void blacklistToken(String token) {
        try {
            Claims claims = parseToken(token);
            long remainingTtl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTtl > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + token,
                        "1",
                        remainingTtl,
                        TimeUnit.MILLISECONDS
                );
            }
        } catch (Exception ignored) {
            // Token 已过期，无需加入黑名单
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     */
    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    // ======================== 刷新 Access Token ========================

    /**
     * 用 Refresh Token 换取新 Access Token
     * @param refreshToken  Refresh Token
     * @return 新的 Token 对（accessToken + refreshToken），刷新失败返回 null
     */
    public TokenPair refreshAccessToken(String refreshToken) {
        try {
            // 验证 Refresh Token
            Claims claims = parseToken(refreshToken);
            // 确认是 Refresh Token
            if (!"refresh".equals(claims.get("type"))) return null;

            Long userId = Long.parseLong(claims.getSubject());

            // 检查 Redis 中的 Refresh Token 是否匹配（防止旧 Token 重放）
            String storedToken = redisTemplate.opsForValue().get(TOKEN_PREFIX + userId);
            if (storedToken == null || !storedToken.equals(refreshToken)) return null;

            // 从 Refresh Token 中提取角色（已在生成时写入）
            Integer role = getRoleFromToken(refreshToken);
            if (role == null) role = 0; // 兼容旧 refresh token

            // 生成新 Token 对
            String newAccessToken = generateAccessToken(userId, role);
            String newRefreshToken = generateRefreshToken(userId, role);

            return new TokenPair(newAccessToken, newRefreshToken);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 清除用户的 Refresh Token（登出时调用）
     */
    public void removeRefreshToken(Long userId) {
        redisTemplate.delete(TOKEN_PREFIX + userId);
    }

    // ======================== 内部方法 ========================

    /**
     * 解析 JWT，返回 Claims
     */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)                       // 验证签名
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ======================== 内部类 ========================

    /**
     * Token 对：同时返回 Access Token 和 Refresh Token
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
    }
}
