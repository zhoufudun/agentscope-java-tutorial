# AgentScope Java 教程 - 第十一章：技能系统

本章节演示 AgentScope Java 的完整技能系统实现。

## 功能特性

- **技能定义和注册**: 使用 `AgentSkill` 定义技能，支持 YAML frontmatter
- **技能仓库**: MySQL/H2 数据库存储技能定义和资源
- **动态技能加载**: 从文件系统或 classpath 自动加载技能
- **SkillBox 工具包**: 集成 AgentScope 核心技能管理能力
- **技能版本管理**: 支持技能更新和历史版本追踪
- **技能激活控制**: 动态激活/停用技能
- **技能执行上下文**: 管理技能执行的输入输出

## 项目结构

```
chapter11-skills/
├── pom.xml                          # Maven 配置
├── src/main/java/io/agentscope/tutorial/chapter11/
│   ├── Chapter11Application.java    # Spring Boot 启动类
│   ├── config/
│   │   ├── SkillConfig.java        # 技能系统配置
│   │   └── ModelConfig.java        # 模型配置
│   ├── entity/
│   │   └── SkillDefinition.java    # 技能定义实体
│   ├── repository/
│   │   └── SkillRepository.java    # 技能数据访问
│   ├── service/
│   │   ├── SkillService.java       # 技能业务服务
│   │   └── ChatService.java        # 聊天服务
│   ├── skill/
│   │   ├── DataAnalysisSkill.java  # 数据分析技能
│   │   └── CodeReviewSkill.java    # 代码审查技能
│   └── controller/
│       ├── SkillController.java    # 技能管理 API
│       └── ChatController.java     # 聊天 API
├── src/main/resources/
│   ├── application.yml             # 应用配置
│   ├── application-dev.yml         # 开发环境配置
│   ├── schema.sql                  # 数据库 Schema
│   └── skills/
│       ├── data-analysis/SKILL.md  # 数据分析技能定义
│       └── code-review/SKILL.md     # 代码审查技能定义
└── src/test/java/
    └── SkillServiceTest.java       # 技能服务测试
```

## 核心类说明

### AgentSkill
AgentScope 核心技能类，表示一个可加载的技能单元。

```java
AgentSkill skill = new AgentSkill(
    "data-analysis",          // 名称
    "数据分析技能",            // 描述
    "# 数据分析\n\n...",       // 技能内容
    resources                 // 关联资源
);
```

### SkillBox
技能管理工具箱，负责技能注册、激活、提示词生成等。

```java
SkillBox skillBox = new SkillBox(toolkit);
skillBox.registerSkill(skill);
skillBox.setSkillActive("data-analysis_custom", true);
String prompt = skillBox.getSkillPrompt();
```

### SkillService
技能业务服务，封装技能的业务逻辑。

```java
// 注册技能
skillService.registerSkill(skill);

// 激活技能
skillService.activateSkill("data-analysis_custom");

// 获取技能提示词
String prompt = skillService.getSkillPrompt();
```

## REST API

### 技能管理

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/skills` | 获取所有技能 |
| GET | `/skills/{id}` | 获取技能详情 |
| POST | `/skills` | 创建技能 |
| PUT | `/skills/{id}` | 更新技能 |
| DELETE | `/skills/{skillId}` | 删除技能 |
| POST | `/skills/{skillId}/activate` | 激活技能 |
| POST | `/skills/{skillId}/deactivate` | 停用技能 |
| GET | `/skills/search` | 搜索技能 |
| GET | `/skills/stats` | 获取统计信息 |
| GET | `/skills/prompt` | 获取技能提示词 |

### 聊天

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/chat` | 发送聊天消息 |
| GET | `/chat/history/{sessionId}` | 获取聊天历史 |
| DELETE | `/chat/history/{sessionId}` | 清除会话 |
| GET | `/chat/recommend-skills` | 技能推荐 |

## 配置说明

### application.yml

```yaml
agentscope:
  model:
    api-key: ${DASHSCOPE_API_KEY}  # 阿里云百炼 API Key
    model-name: qwen-plus

  skill:
    storage-path: classpath:skills/  # 技能存储路径
    auto-load: true                  # 自动加载技能

  chat:
    system-prompt: 你是一个有用的AI助手...
    skill-prompt-prefix: 你可以使用以下技能...
```

## 运行

### 开发环境启动

```bash
# 编译项目
mvn clean compile

# 运行应用
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 访问 API
curl http://localhost:8080/api/skills

# 访问 H2 控制台
http://localhost:8080/api/h2-console
```

### 运行测试

```bash
mvn test
```

## 技能格式

技能使用 SKILL.md 格式定义，包含 YAML frontmatter：

```markdown
---
name: skill-name
description: 技能描述
version: 1.0.0
author: Author Name
tags:
  - tag1
  - tag2
---

# 技能名称

技能详细说明...

## 使用方法

...
```

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.5
- **AgentScope**: 1.1.0-SNAPSHOT
- **数据库**: H2 (开发) / MySQL (生产)
- **工具库**: Lombok, Commons Lang3