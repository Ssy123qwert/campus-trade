# 🛒 校园二手交易平台

> 基于 Spring Boot 3 + Vue 3 的校园二手交易平台，功能完整、安全可靠、易于部署。

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.4-4FC08D)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1)
![Redis](https://img.shields.io/badge/Redis-7.x-DC382D)
![License](https://img.shields.io/badge/License-MIT-yellow)

---

## 📋 功能概览

| 模块 | 功能 |
|------|------|
| 🔐 **用户认证** | 注册登录、JWT 双 Token、BCrypt 密码、Spring Security 权限控制 |
| 📦 **商品管理** | 发布/编辑/下架/重新上架、图片上传、分类筛选、价格排序 |
| 🔍 **商品搜索** | ES 全文检索（预留）、MySQL LIKE 降级、Redis 热点缓存 |
| 🛒 **订单交易** | 下单（乐观锁防超卖）、模拟支付、30 分钟未支付自动取消 |
| ⭐ **收藏评价** | Redis Set 收藏（O(1) 查询）、买卖双方双向评价、好评率统计 |
| 💬 **实时聊天** | WebSocket + STOMP 协议、JWT 鉴权、在线推送、离线消息 |
| 🔔 **消息通知** | 系统通知（新消息/订单状态变更）、未读红点计数 |
| 💰 **议价系统** | 买家出价、卖家接受/拒绝、重复出价检查 |
| 🚩 **举报系统** | 违规举报、管理员审核处理 |
| 📊 **管理后台** | 用户/商品/订单管理、ECharts 数据看板（趋势图、分类占比、热门排行） |
| 📖 **API 文档** | Knife4j / Swagger 在线接口文档 |
| 🛡️ **接口限流** | Redis 滑动窗口计数器、注册/登录防刷 |
| 📝 **操作日志** | AOP 切面记录管理员关键操作、审计追溯 |

---

## 🛠️ 技术栈

### 后端
| 技术 | 用途 |
|------|------|
| Spring Boot 3.2.5 | 核心框架 |
| MyBatis-Plus 3.5.6 | ORM |
| Spring Security | 认证授权 |
| JWT (jjwt 0.12.5) | 令牌认证 |
| MySQL 8.0 | 持久化存储 |
| Redis 7.x | 缓存/计数器/限流 |
| WebSocket + STOMP | 实时通讯 |
| Elasticsearch 8.x | 全文检索（预留） |
| RabbitMQ | 消息队列（预留） |
| Knife4j | API 文档 |

### 前端
| 技术 | 用途 |
|------|------|
| Vue 3 | 前端框架 |
| Vue Router | 路由管理 |
| Vite | 构建工具 |
| ECharts | 数据可视化 |
| STOMP.js | WebSocket 客户端 |

---

## 🚀 快速开始

### 环境要求
- **Java 22**（或 21+）
- **Maven 3.9+**
- **MySQL 8.0+**
- **Redis 7.x**
- **Node.js 18+**（仅前端开发时需要）

### 1. 克隆项目
```bash
git clone https://github.com/Ssy123qwert/campus-trade.git
cd campus-trade
```

### 2. 初始化数据库
```bash
mysql -u root -p < init.sql
mysql -u root -p campus_trade < campus-trade-server/src/main/resources/db/migration/V1__upgrade.sql
mysql -u root -p campus_trade < campus-trade-server/src/main/resources/db/migration/V2__notifications.sql
mysql -u root -p campus_trade < campus-trade-server/src/main/resources/db/migration/V3__offer_report.sql
```

### 3. 配置 application.yml
```bash
cp campus-trade-server/src/main/resources/application.yml.example \
   campus-trade-server/src/main/resources/application.yml
```
修改 `application.yml` 中的 MySQL 密码为你自己的密码。

### 4. 启动 Redis
```bash
redis-server
```

### 5. 启动项目
```bash
cd campus-trade-server
mvn spring-boot:run
```

### 6. 打开浏览器
访问 **http://localhost:8080**

### 📦 一键启动（Windows）
双击项目根目录的 `start.bat`，自动检查 Redis/MySQL 并启动后端。

---

## 🔑 测试账号

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 👑 管理员 | `root` | `123456` | 可访问管理后台和数据看板 |
| 👤 普通用户 | `test` | `123456` | 可发布/购买商品 |

---

## 🗂️ 项目结构

```
campus-trade/
├── campus-trade-server/          # 后端 (Spring Boot)
│   ├── src/main/java/
│   │   └── com/campustrade/
│   │       ├── config/           # 配置类 (WebSocket/Knife4j/MyBatisPlus)
│   │       ├── security/         # JWT认证 + Spring Security
│   │       ├── controller/       # API控制器 (14个)
│   │       ├── service/          # 业务逻辑层
│   │       ├── mapper/           # MyBatis-Plus Mapper
│   │       ├── entity/           # 实体类
│   │       ├── dto/              # 数据传输对象
│   │       ├── annotation/       # 自定义注解 (RateLimit/OperationLog)
│   │       ├── aop/              # AOP切面 (限流/操作日志)
│   │       ├── exception/        # 全局异常处理
│   │       └── document/         # ES索引文档
│   └── src/main/resources/
│       ├── application.yml.example  # 配置文件模板
│       └── db/migration/            # 数据库迁移脚本
├── campus-trade-ui/              # 前端 (Vue 3)
│   └── src/
│       ├── views/                # 页面组件 (17个)
│       ├── api/                  # API客户端
│       └── components/           # 公共组件
├── Dockerfile                    # Docker构建
├── docker-compose.yml            # Docker一键部署
├── start.bat                     # Windows一键启动
└── init.sql                      # 数据库初始化
```

---

## 🌐 接口文档

启动项目后访问：
- **Swagger UI**: http://localhost:8080/doc.html

---

## 🐳 Docker 部署

```bash
docker compose up -d
```

会自动构建并启动 MySQL + Redis + App 三个容器。

---

## 📊 数据看板

管理员登录后，在「管理后台」→「📊 数据看板」可查看：
- 概览卡片（用户/商品/订单/交易额）
- 增长趋势图（可切换 7天/30天）
- 分类占比环形图
- 热门商品 TOP10
- 操作日志

---

## 📜 开源协议

本项目仅供学习交流使用，MIT License。
