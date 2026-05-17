# AgentScope Java 教程示例工程

> 每个章节对应一个独立可运行的 Spring Boot 项目

## 技术栈标准

- **Java**: 21
- **Spring Boot**: 3.x
- **依赖库**: Lombok, Apache Commons Lang3, Jackson
- **构建工具**: Maven
- **数据库**: MySQL 8 (Docker), H2 (开发模式)
- **缓存**: Redis (Docker)
- **向量数据库**: Qdrant (Docker, RAG 专用)
- **消息队列**: RocketMQ (Docker, A2A 章节可选)

## 目录结构

```
tutorial/
├── docker-compose.yml          # 公共 Docker 服务
├── chapter01-overview/          # 第一章：概述
├── chapter02-quickstart/        # 第二章：快速开始
├── chapter03-message-session/   # 第三章：消息与会话管理
├── chapter04-react-agent/      # 第四章：ReAct Agent 详解
├── chapter05-tools/            # 第五章：工具系统
├── chapter06-model-integration/# 第六章：模型集成
├── chapter07-memory/           # 第七章：记忆系统
├── chapter08-multi-agent/      # 第八章：多代理架构模式
├── chapter09-a2a-protocol/     # 第九章：代理间通信协议
├── chapter10-rag/              # 第十章：RAG 检索增强生成
├── chapter11-skills/           # 第十一章：技能系统
├── chapter12-plan/             # 第十二章：计划与任务分解
├── chapter13-hook/             # 第十三章：Hook 钩子系统
├── chapter14-hitl/             # 第十四章：人类在环
├── chapter15-harness/          # 第十五章：Harness 安全执行环境
├── chapter16-springboot/       # 第十六章：Spring Boot 集成
├── chapter17-deployment/       # 第十七章：部署与生产环境
├── chapter18-other-frameworks/ # 第十八章：其他框架集成
└── chapter19-comprehensive/    # 第十九章：综合实战案例
```

## 第十七章：部署与生产环境 (chapter17-deployment)

生产环境完整部署示例，包含：

```bash
# 进入章节目录
cd chapter17-deployment

# Docker 部署
docker build -t agentscope/chapter17-deployment:1.0.0 .
docker-compose up -d

# Kubernetes 部署
kubectl apply -f k8s-common.yaml
kubectl apply -f k8s-deployment.yaml
kubectl apply -f k8s-hpa.yaml

# Helm 部署
helm install agentscope ./helm/agentscope -n agentscope --create-namespace

# 访问服务
curl http://localhost:8080/actuator/health
curl http://localhost:3000/grafana  # Grafana 监控
curl http://localhost:9090/prometheus # Prometheus
```

**包含内容：**
- 多阶段构建 Dockerfile (JRE 优化)
- docker-compose.yml (MySQL + Redis + Prometheus + Grafana + OTel Collector + Jaeger)
- Kubernetes 完整配置 (Deployment, Service, ConfigMap, Secret, HPA, PDB, Ingress, NetworkPolicy)
- Helm Chart 完整模板
- 生产级配置 (优雅关闭、健康检查、监控指标)

---

## Docker 服务规划

| 服务 | 端口 | 用途 | 使用章节 |
|------|------|------|----------|
| MySQL | 3306 | 关系型数据库 | 大部分章节 |
| Redis | 6379 | 缓存/Session | 7, 11, 16 |
| Qdrant | 6333 | 向量数据库 | 10 |
| Nacos | 8848 | 配置中心 | 9, 16 |
| RocketMQ | 9876/10911 | 消息队列 | 9 |

## 运行方式

每个章节示例工程均支持：

```bash
# 开发模式 (使用 H2)
./mvnw spring-boot:run

# 生产模式 (使用 MySQL)
docker-compose up -d  # 启动数据库
./mvnw spring-boot:run -Dspring-profiles=prod
```