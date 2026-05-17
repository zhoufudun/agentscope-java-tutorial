# Chapter 10: RAG (Retrieval-Augmented Generation)

## 概述

本章教程演示如何使用 AgentScope Java 构建完整的 RAG 系统，实现：

- 文档上传和智能分块
- 多模型支持（Ollama/OpenAI/DashScope）的文本向量化
- Qdrant 向量数据库存储和检索
- 语义相似度搜索
- 混合检索（向量 + 关键词）

## 技术栈

- **Java**: 21
- **Spring Boot**: 3.2.5
- **AgentScope**: 1.1.0-SNAPSHOT
- **向量数据库**: Qdrant v1.7.4
- **Embedding 模型**: Ollama (nomic-embed-text)

## 快速开始

### 1. 启动依赖服务

```bash
# 启动 Qdrant 向量数据库
docker-compose -f docker-compose-qdrant.yml up -d

# 启动 Ollama (用于本地 embedding)
ollama serve

# 拉取 embedding 模型
ollama pull nomic-embed-text
```

### 2. 运行应用

```bash
cd chapter10-rag
mvn spring-boot:run
```

### 3. 访问 API

应用将在 http://localhost:8080 启动。

## API 接口

### 文档管理

#### 上传文档
```bash
curl -X POST http://localhost:8080/api/documents/text \
  -H "Content-Type: application/json" \
  -d '{
    "title": "AgentScope Java Tutorial",
    "content": "AgentScope is a powerful Java framework for building AI agents...",
    "metadata": {"author": "Tutorial Author", "category": "education"}
  }'
```

#### 文本分块
```bash
curl -X POST http://localhost:8080/api/documents/chunk \
  -H "Content-Type: application/json" \
  -d '{"content": "Your long document text here..."}'
```

### RAG 查询

#### RAG 查询（检索 + 生成）
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is AgentScope?",
    "topK": 5,
    "scoreThreshold": 0.6
  }'
```

#### 纯检索
```bash
curl -G "http://localhost:8080/api/chat/retrieve" \
  --data-urlencode "query=What is AgentScope?" \
  --data "topK=5"
```

#### 混合搜索
```bash
curl -X POST http://localhost:8080/api/chat/hybrid \
  -H "Content-Type: application/json" \
  -d '{"query": "AgentScope features", "topK": 5}'
```

### 系统信息

#### 获取系统配置
```bash
curl http://localhost:8080/api/chat/stats
```

## 配置说明

### application.yml

```yaml
# RAG 配置
rag:
  embedding:
    modelType: OLLAMA          # OLLAMA | OPENAI | DASHSCOPE
    modelName: nomic-embed-text
    dimensions: 768
    baseUrl: http://localhost:11434

  chunking:
    strategy: PARAGRAPH         # CHARACTER | PARAGRAPH | TOKEN | SEMANTIC
    chunkSize: 500
    overlapSize: 50

  retrieval:
    topK: 5
    scoreThreshold: 0.5
    hybridSearch: false

# Qdrant 配置
qdrant:
  location: http://localhost:6333
  collectionName: tutorial_rag
  dimensions: 768
```

## 核心组件

### 服务层

| 服务 | 描述 |
|------|------|
| `DocumentService` | 文档处理和分块 |
| `EmbeddingService` | 文本向量化 |
| `VectorStoreService` | Qdrant 向量存储 |
| `RAGService` | RAG 查询核心逻辑 |

### 配置类

| 配置 | 描述 |
|------|------|
| `RAGConfig` | RAG 系统配置（embedding、chunking、retrieval） |
| `QdrantConfig` | Qdrant 连接配置 |

## 分块策略

| 策略 | 说明 | 适用场景 |
|------|------|----------|
| `CHARACTER` | 按固定字符数切分 | 短文本、结构化数据 |
| `PARAGRAPH` | 按段落边界切分 | 文档、报告 |
| `TOKEN` | 按 token 近似数切分 | 代码、格式化文本 |
| `SEMANTIC` | 按语义边界切分 | 需要保持完整语义的场景 |

## 扩展主题

### 混合检索

混合检索结合向量搜索和关键词搜索，可以提高检索质量：

```java
// 在 RAGService 中启用混合搜索
RAGConfig.RetrievalConfig config = new RAGConfig.RetrievalConfig();
config.setHybridSearch(true);
config.setKeywordWeight(0.3);
```

### 自定义 Embedding 模型

支持多种 embedding 提供商：

```java
// Ollama (本地)
modelType: OLLAMA
baseUrl: http://localhost:11434
modelName: nomic-embed-text

// OpenAI
modelType: OPENAI
apiKey: your-api-key
modelName: text-embedding-3-small

// DashScope (阿里云)
modelType: DASHSCOPE
apiKey: your-api-key
modelName: text-embedding-v3
```

### 向量数据库

当前使用 Qdrant，也可替换为其他支持的向量数据库：

- Milvus
- Elasticsearch
- PostgreSQL + pgvector

## 运行测试

```bash
# 运行单元测试
cd chapter10-rag
mvn test

# 运行带外部服务的集成测试
export OLLAMA_URL=http://localhost:11434
export QDRANT_URL=http://localhost:6333
mvn test -Dtest=RAGServiceTest
```

## 注意事项

1. **Qdrant 必须先启动**：应用启动前确保 Qdrant 正在运行
2. **Ollama 模型**：使用 Ollama 时需要先拉取 embedding 模型
3. **维度匹配**：确保 embedding 模型输出维度与 Qdrant 配置一致
4. **生产环境**：生产部署时请修改 API 密钥和连接配置

## 项目结构

```
chapter10-rag/
├── pom.xml
├── docker-compose-qdrant.yml
├── src/main/java/io/agentscope/tutorial/chapter10/
│   ├── Chapter10Application.java
│   ├── config/
│   │   ├── RAGConfig.java
│   │   └── QdrantConfig.java
│   ├── service/
│   │   ├── DocumentService.java
│   │   ├── EmbeddingService.java
│   │   ├── VectorStoreService.java
│   │   └── RAGService.java
│   ├── model/
│   │   ├── Document.java
│   │   └── DocumentStatus.java
│   └── controller/
│       ├── DocumentController.java
│       └── ChatController.java
└── src/main/resources/
    ├── application.yml
    └── application-dev.yml
```