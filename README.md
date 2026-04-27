# ✈ 航班信息跟踪平台

> 软件项目管理课程设计 · Spring Boot + Vue 3 · 多人协作项目

---

## 目录

- [项目简介](#项目简介)
- [已完成功能](#已完成功能)
- [技术栈](#技术栈)
- [项目结构](#项目结构)
- [快速启动](#快速启动)
- [后端接口文档](#后端接口文档)
- [后续开发计划](#后续开发计划)
- [多人协作规范](#多人协作规范)

---

## 项目简介

航班信息跟踪平台是一个可视化的国内航班实时追踪系统。用户可以在地图上查看所有在途航班的位置、航向、高度、速度等信息，支持点击查看单条航班详情，并可调整仿真时间流速观察航班动态。前端采用 Vue 3 + Leaflet.js 实现地图可视化，后端采用 Spring Boot 3 + MySQL 提供 RESTful 数据接口，前后端完全分离。

---

## 已完成功能

### 前端（`frontend/`）
- [x] Vue 3 + Vite 工程初始化，MVC 分层目录结构（views / components / api / store）
- [x] Leaflet.js 彩色地图（CartoDB Voyager），地图范围限定中国境内
- [x] 覆盖国内 20 个主要机场的航班仿真数据
- [x] 俯视角 SVG 飞机图标，随航向实时旋转，选中高亮红色
- [x] 飞机从出发地沿直线严格飞往目的地（progress 插值，非随机漂移）
- [x] 到达目的地后飞机消失约 3 秒，随机重生新航班继续飞行
- [x] 每条航线全程展示虚线（出发地 → 目的地），落地后虚线同步消失
- [x] 点击飞机弹出详情面板（航班号 / 出发地 / 目的地 / 高度 / 速度 / 坐标 / 状态）
- [x] 底部仿真速率控制栏（⏸ 暂停 / 1× / 2× / 5× / 10×）
- [x] 顶部导航栏实时显示在线航班数量、飞行中数量与系统时钟
- [x] 后端接口层（`src/api/flightApi.js`），`useMock` 一键切换 Mock ↔ 真实后端

### 后端（`backend/`）
- [x] Spring Boot 3.2 + Maven 工程初始化
- [x] MVC 分层架构：`entity / repository / service / controller / dto`
- [x] MySQL 8.0 持久化存储，JPA 自动建表（`ddl-auto: update`）
- [x] 启动时自动执行 `data.sql` 插入 5 条初始测试航班数据
- [x] 全局跨域配置（`WebMvcConfigurer`），允许前端 `localhost:7175` 访问
- [x] 航班实体（`Flight`）+ 轨迹点实体（`FlightTrackPoint`）
- [x] RESTful 接口：获取所有飞行中航班、单个航班详情、航班历史轨迹
- [x] 前后端联调验证通过，`/api/flights` 接口正常返回数据

---

## 技术栈

| 层次 | 技术 | 版本 |
|---|---|---|
| 前端框架 | Vue 3 + Vite | Vue 3.x / Vite 6.x |
| 地图库 | Leaflet.js | 1.9.x |
| HTTP 客户端 | Axios | 1.x |
| 后端框架 | Spring Boot | 3.2.5 |
| 持久层 | Spring Data JPA + Hibernate | 6.4.x |
| 数据库 | MySQL | 8.0+ |
| 构建工具 | Maven | 3.9+ |
| 运行环境 | JDK | 21 |
| 运行环境 | Node.js / npm | 18+ / 9+ |
| 版本管理 | Git + GitHub | — |

---

## 项目结构

```
project/
├── frontend/                        # Vue 3 前端
│   ├── src/
│   │   ├── api/
│   │   │   └── flightApi.js         # 后端接口封装（axios）
│   │   ├── components/
│   │   │   └── map/
│   │   │       ├── FlightMap.vue    # 地图主组件（核心渲染逻辑）
│   │   │       └── FlightPopup.vue  # 航班详情弹窗
│   │   ├── store/
│   │   │   └── flightStore.js       # 全局状态 + 飞行仿真引擎
│   │   ├── views/
│   │   │   └── MapView.vue          # 地图页面（顶部栏 + 地图容器）
│   │   ├── App.vue
│   │   └── main.js
│   ├── .env.development             # 开发环境变量（API 地址、端口）
│   └── vite.config.js               # Vite 配置（端口 7175）
│
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/flighttrack/
│   │   ├── FlightTrackApplication.java      # 启动入口
│   │   ├── controller/
│   │   │   ├── FlightController.java        # 航班 REST 接口
│   │   │   └── GlobalCorsConfig.java        # 全局跨域配置
│   │   ├── service/
│   │   │   └── FlightService.java           # 业务逻辑层
│   │   ├── repository/
│   │   │   ├── FlightRepository.java
│   │   │   └── FlightTrackPointRepository.java
│   │   ├── entity/
│   │   │   ├── Flight.java                  # 航班实体
│   │   │   └── FlightTrackPoint.java        # 轨迹历史点实体
│   │   └── dto/
│   │       └── FlightDTO.java               # 接口响应 DTO
│   └── src/main/resources/
│       ├── application.yml                  # 数据库 / 端口配置
│       └── data.sql                         # 初始测试数据
│
└── README.md
```

---

## 快速启动

### 环境要求

| 工具 | 最低版本 |
|---|---|
| JDK | 21 |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |
| MySQL | 8.0+ |

### 第一步：准备数据库

在 MySQL 中执行（只需一次）：

```sql
CREATE DATABASE flight CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

确认 `backend/src/main/resources/application.yml` 中的数据库密码与本机一致：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/flight?...
    username: root
    password: 你的MySQL密码
```

### 第二步：启动后端

```bash
cd backend
mvn spring-boot:run
```

> 后端运行在 [http://localhost:7176](http://localhost:7176)
>
> 首次启动自动建表并插入 5 条测试航班。
> 验证接口：[http://localhost:7176/api/flights](http://localhost:7176/api/flights)

### 第三步：启动前端

```bash
cd frontend
npm install   # 首次需要安装依赖
npm run dev
```

> 前端运行在 [http://localhost:7175](http://localhost:7175)

### Mock 模式 vs 真实后端

在 `frontend/src/store/flightStore.js` 中：

```js
useMock: true,   // Mock 模式：40 架模拟航班，无需后端
useMock: false,  // 真实模式：调用 localhost:7176/api/flights
```

---

## 后端接口文档

### Base URL

```
http://localhost:7176
```

### 接口列表

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/flights` | 获取所有飞行中的航班 |
| GET | `/api/flights/{id}` | 获取单个航班详情 |
| GET | `/api/flights/{id}/track` | 获取航班历史轨迹点 |

### GET /api/flights 响应示例

```json
[
  {
    "id": 1,
    "flightNo": "CA1001",
    "origin": "北京首都",
    "destination": "上海浦东",
    "latitude": 35.5,
    "longitude": 118.0,
    "altitude": 10000,
    "speed": 850,
    "heading": 135,
    "status": "IN_FLIGHT",
    "updatedAt": "2026-04-27T17:18:59"
  }
]
```

### GET /api/flights/{id}/track 响应示例

```json
[
  { "latitude": 40.08, "longitude": 116.58 },
  { "latitude": 37.20, "longitude": 117.80 },
  { "latitude": 35.50, "longitude": 118.00 }
]
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `flightNo` | String | 航班号，如 `CA1001` |
| `origin` | String | 出发机场名称 |
| `destination` | String | 目的机场名称 |
| `latitude` | Double | 当前纬度（°N） |
| `longitude` | Double | 当前经度（°E） |
| `altitude` | Integer | 飞行高度（米） |
| `speed` | Integer | 飞行速度（km/h） |
| `heading` | Integer | 航向角（0–359°，0 为正北） |
| `status` | String | `IN_FLIGHT` / `LANDED` / `DELAYED` / `CANCELLED` |

---

## 后续开发计划

### 高优先级
- [ ] 对接真实航班数据 API（推荐：[OpenSky Network](https://opensky-network.org) 免费 / [AviationStack](https://aviationstack.com)）
- [ ] WebSocket 实时推送：后端定时推送航班位置，替代前端轮询
- [ ] 用户登录与权限系统（Spring Security + JWT）

### 功能扩展
- [ ] 航班搜索与筛选（按航班号 / 出发地 / 目的地 / 状态）
- [ ] 延误 / 取消告警提醒（弹窗 + 地图颜色标注）
- [ ] 机场详情页（各机场出发 / 到达航班列表）
- [ ] 航班统计报表（折线图、柱状图）
- [ ] 天气图层叠加（云层、降水、风场）

### 工程优化
- [ ] 后端定时任务采集航班数据（Spring `@Scheduled`）
- [ ] 统一响应格式封装（`Result<T>`）
- [ ] 全局异常处理（`@RestControllerAdvice`）
- [ ] 前端多页面路由（Vue Router）
- [ ] 单元测试 / 集成测试

---

## 多人协作规范

### 分支策略

```
main          ← 稳定版本，只接受 PR 合并
dev           ← 日常开发集成分支
feature/xxx   ← 个人功能开发分支（从 dev 切出）
```

### 工作流程

```bash
# 1. 从 dev 新建自己的功能分支
git checkout dev
git pull origin dev
git checkout -b feature/你的功能名

# 2. 开发完成后提交
git add .
git commit -m "feat: 简要描述"

# 3. 推送并在 GitHub 发起 Pull Request → dev
git push -u origin feature/你的功能名
```

### Commit 规范

| 前缀 | 含义 |
|---|---|
| `feat:` | 新功能 |
| `fix:` | Bug 修复 |
| `refactor:` | 代码重构 |
| `docs:` | 文档更新 |
| `style:` | 样式调整 |
| `chore:` | 构建 / 依赖调整 |

---

> 仓库地址：[https://github.com/FananQAQ/Software-Project-Management](https://github.com/FananQAQ/Software-Project-Management)
