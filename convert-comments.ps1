# AgentScope Java 教程 - 中文注释批量转换脚本

$tutorialDir = "D:/Work/ai/agents/agentscope-java/tutorial"
$totalFiles = 0
$modifiedFiles = 0

# 注释替换映射表
$replacements = @(
    # 应用启动类
    @{ Old = "Spring Boot application entry point"; New = "Spring Boot 应用启动入口" },
    @{ Old = "Main application class"; New = "主应用类" },
    @{ Old = "Main method to start the application"; New = "启动应用的主方法" },
    @{ Old = "Main method"; New = "主方法" },
    @{ Old = "Application entry point"; New = "应用入口" },

    # 配置类
    @{ Old = "Configuration class for AgentScope"; New = "AgentScope 配置类" },
    @{ Old = "Configuration class"; New = "配置类" },
    @{ Old = "Agent configuration properties"; New = "Agent 配置属性" },
    @{ Old = "Model configuration properties"; New = "模型配置属性" },
    @{ Old = "Properties for AgentScope"; New = "AgentScope 属性配置" },
    @{ Old = "Configuration properties"; New = "配置属性" },
    @{ Old = "Hook configuration properties"; New = "Hook 配置属性" },
    @{ Old = "Harness configuration properties"; New = "Harness 配置属性" },
    @{ Old = "Configuration for RAG"; New = "RAG 配置" },
    @{ Old = "Qdrant configuration properties"; New = "Qdrant 向量数据库配置" },

    # 控制器
    @{ Old = "REST API controller"; New = "REST API 控制器" },
    @{ Old = "Chat REST API controller"; New = "聊天 REST API 控制器" },
    @{ Old = "Controller for chat operations"; New = "聊天操作控制器" },
    @{ Old = "Controller for Agent operations"; New = "Agent 操作控制器" },
    @{ Old = "Controller for skill management"; New = "技能管理控制器" },
    @{ Old = "Controller for session management"; New = "会话管理控制器" },
    @{ Old = "Controller for approval workflow"; New = "审批工作流控制器" },
    @{ Old = "Controller for code execution"; New = "代码执行控制器" },
    @{ Old = "Controller for workspace management"; New = "工作区管理控制器" },
    @{ Old = "Controller for audit operations"; New = "审计操作控制器" },
    @{ Old = "Controller for document operations"; New = "文档操作控制器" },
    @{ Old = "Controller for plan management"; New = "计划管理控制器" },
    @{ Old = "Game controller for Werewolf"; New = "狼人杀游戏控制器" },

    # 服务层
    @{ Old = "Service class for Agent operations"; New = "Agent 操作服务类" },
    @{ Old = "Service class for chat operations"; New = "聊天操作服务类" },
    @{ Old = "Service for managing skills"; New = "技能管理服务" },
    @{ Old = "Service for managing memory"; New = "记忆管理服务" },
    @{ Old = "Service for managing models"; New = "模型管理服务" },
    @{ Old = "Service for managing sessions"; New = "会话管理服务" },
    @{ Old = "Service for managing plans"; New = "计划管理服务" },
    @{ Old = "Service for managing approval workflow"; New = "审批工作流管理服务" },
    @{ Old = "Service for code execution"; New = "代码执行服务" },
    @{ Old = "Service for workspace management"; New = "工作区管理服务" },
    @{ Old = "Service for document processing"; New = "文档处理服务" },
    @{ Old = "Service for vector operations"; New = "向量操作服务" },
    @{ Old = "Service for RAG operations"; New = "RAG 检索增强生成服务" },
    @{ Old = "Service for embedding operations"; New = "Embedding 向量化服务" },
    @{ Old = "Multi-agent coordination service"; New = "多代理协作服务" },
    @{ Old = "Service for enterprise assistant"; New = "企业智能助手服务" },

    # 实体/模型
    @{ Old = "Entity class for chat message"; New = "聊天消息实体类" },
    @{ Old = "Entity class for chat session"; New = "聊天会话实体类" },
    @{ Old = "Entity class for skill definition"; New = "技能定义实体类" },
    @{ Old = "Entity class for memory record"; New = "记忆记录实体类" },
    @{ Old = "Entity class for audit log"; New = "审计日志实体类" },
    @{ Old = "Entity class for approval request"; New = "审批请求实体类" },
    @{ Old = "Entity class for plan record"; New = "计划记录实体类" },
    @{ Old = "DTO for chat request"; New = "聊天请求数据传输对象" },
    @{ Old = "DTO for chat response"; New = "聊天响应数据传输对象" },
    @{ Old = "DTO for execution request"; New = "执行请求数据传输对象" },
    @{ Old = "DTO for execution result"; New = "执行结果数据传输对象" },
    @{ Old = "Data transfer object"; New = "数据传输对象" },
    @{ Old = "Model class for document"; New = "文档模型类" },
    @{ Old = "Model class for game state"; New = "游戏状态模型类" },
    @{ Old = "Model class for player"; New = "玩家模型类" },

    # 仓库
    @{ Old = "Repository for chat messages"; New = "聊天消息数据访问层" },
    @{ Old = "Repository for chat sessions"; New = "聊天会话数据访问层" },
    @{ Old = "Repository for skill definitions"; New = "技能定义数据访问层" },
    @{ Old = "Repository for memory records"; New = "记忆记录数据访问层" },
    @{ Old = "Repository for audit logs"; New = "审计日志数据访问层" },
    @{ Old = "Repository for approval requests"; New = "审批请求数据访问层" },
    @{ Old = "Repository for plan records"; New = "计划记录数据访问层" },
    @{ Old = "Data access layer"; New = "数据访问层" },
    @{ Old = "JPA repository"; New = "JPA 数据访问接口" },

    # Hook 钩子
    @{ Old = "Hook for logging events"; New = "事件日志钩子" },
    @{ Old = "Hook for collecting metrics"; New = "指标收集钩子" },
    @{ Old = "Hook for auditing tool calls"; New = "工具调用审计钩子" },
    @{ Old = "Hook for timing measurement"; New = "性能计时钩子" },
    @{ Old = "Hook for approval workflow"; New = "审批工作流钩子" },
    @{ Old = "Hook for plan changes"; New = "计划变更钩子" },
    @{ Old = "Lifecycle hook"; New = "生命周期钩子" },

    # 工具类
    @{ Old = "Tool for weather queries"; New = "天气查询工具" },
    @{ Old = "Tool for calculator operations"; New = "计算器操作工具" },
    @{ Old = "Tool for datetime operations"; New = "日期时间操作工具" },
    @{ Old = "Tool for search operations"; New = "搜索操作工具" },
    @{ Old = "Tool for HTTP requests"; New = "HTTP 请求工具" },
    @{ Old = "Built-in tools for file operations"; New = "文件操作内置工具" },
    @{ Old = "Tool for memory management"; New = "记忆管理工具" },
    @{ Old = "Tool for sending email"; New = "邮件发送工具" },
    @{ Old = "Tool for data deletion"; New = "数据删除工具" },
    @{ Old = "Tool for data analysis"; New = "数据分析工具" },
    @{ Old = "Tool for code review"; New = "代码审查工具" },

    # 沙箱
    @{ Old = "In-memory sandbox implementation"; New = "内存沙箱实现" },
    @{ Old = "Sandbox state management"; New = "沙箱状态管理" },
    @{ Old = "Sandbox client implementation"; New = "沙箱客户端实现" },
    @{ Old = "Sandbox filesystem spec"; New = "沙箱文件系统规范" },

    # Agent
    @{ Old = "Supervisor Agent for order management"; New = "订单管理监督 Agent" },
    @{ Old = "Cashier Agent for payment processing"; New = "支付处理收银 Agent" },
    @{ Old = "Barista Agent for drink preparation"; New = "饮品制作吧台 Agent" },
    @{ Old = "Moderator Agent for Werewolf game"; New = "狼人杀游戏主持人 Agent" },
    @{ Old = "Villager Agent for Werewolf game"; New = "狼人杀村民 Agent" },
    @{ Old = "Werewolf Agent for Werewolf game"; New = "狼人杀狼人 Agent" },
    @{ Old = "Enterprise assistant Agent"; New = "企业智能助手 Agent" },
    @{ Old = "Knowledge base Agent"; New = "知识库 Agent" },
    @{ Old = "Schedule management Agent"; New = "日程管理 Agent" },
    @{ Old = "Report generation Agent"; New = "报表生成 Agent" },
    @{ Old = "Router Agent for intent classification"; New = "意图分类路由 Agent" },
    @{ Old = "Order Agent for order queries"; New = "订单查询 Agent" },
    @{ Old = "Query Agent for general queries"; New = "通用查询 Agent" },
    @{ Old = "Synthesis Agent for result aggregation"; New = "结果汇总合成 Agent" },

    # 游戏相关
    @{ Old = "Game state management"; New = "游戏状态管理" },
    @{ Old = "Werewolf game core logic"; New = "狼人杀游戏核心逻辑" },

    # 通用注释
    @{ Old = "Main entry point"; New = "主入口" },
    @{ Old = "Entry point"; New = "入口点" },
    @{ Old = "Constructor"; New = "构造函数" },
    @{ Old = "Default constructor"; New = "默认构造函数" },
    @{ Old = "Getters and setters"; New = "获取器和设置器" },
    @{ Old = "Getter method"; New = "获取方法" },
    @{ Old = "Setter method"; New = "设置方法" },
    @{ Old = "Returns the"; New = "返回" },
    @{ Old = "Sets the"; New = "设置" },
    @{ Old = "Param "; New = "参数 " },
    @{ Old = "Return "; New = "返回 " },
    @{ Old = "Returns "; New = "返回 " },
    @{ Old = "Throws "; New = "抛出异常 " },
    @{ Old = "Exception thrown"; New = "抛出异常" },
    @{ Old = "Method to"; New = "方法用于" },
    @{ Old = "This method"; New = "该方法" },
    @{ Old = "This class"; New = "该类" },
    @{ Old = "This interface"; New = "该接口" },
    @{ Old = "This field"; New = "该字段" },
    @{ Old = "This property"; New = "该属性" },
    @{ Old = "This parameter"; New = "该参数" },
    @{ Old = "This method is used to"; New = "该方法用于" },
    @{ Old = "This class is used to"; New = "该类用于" },
    @{ Old = "Used to"; New = "用于" },
    @{ Old = "Used for"; New = "用于" },
    @{ Old = "Indicates whether"; New = "指示是否" },
    @{ Old = "Whether to"; New = "是否" },
    @{ Old = "True if"; New = "如果为真" },
    @{ Old = "Default value"; New = "默认值" },
    @{ Old = "Default"; New = "默认" },
    @{ Old = "Optional parameter"; New = "可选参数" },
    @{ Old = "Required parameter"; New = "必填参数" },

    # 方法注释
    @{ Old = "Handle chat message"; New = "处理聊天消息" },
    @{ Old = "Handle streaming chat"; New = "处理流式聊天" },
    @{ Old = "Get chat history"; New = "获取聊天历史" },
    @{ Old = "Clear chat history"; New = "清除聊天历史" },
    @{ Old = "Get session information"; New = "获取会话信息" },
    @{ Old = "Create a new session"; New = "创建新会话" },
    @{ Old = "Delete a session"; New = "删除会话" },
    @{ Old = "List all sessions"; New = "列出所有会话" },
    @{ Old = "Get all skills"; New = "获取所有技能" },
    @{ Old = "Activate a skill"; New = "激活技能" },
    @{ Old = "Deactivate a skill"; New = "停用技能" },
    @{ Old = "Register a new skill"; New = "注册新技能" },
    @{ Old = "Get audit records"; New = "获取审计记录" },
    @{ Old = "Get audit statistics"; New = "获取审计统计" },
    @{ Old = "Get pending approvals"; New = "获取待审批列表" },
    @{ Old = "Approve a request"; New = "批准请求" },
    @{ Old = "Reject a request"; New = "拒绝请求" },
    @{ Old = "Execute code in sandbox"; New = "在沙箱中执行代码" },
    @{ Old = "Execute a command"; New = "执行命令" },
    @{ Old = "List files in workspace"; New = "列出工作区文件" },
    @{ Old = "Read a file"; New = "读取文件" },
    @{ Old = "Write a file"; New = "写入文件" },
    @{ Old = "Delete a file"; New = "删除文件" },
    @{ Old = "Upload a document"; New = "上传文档" },
    @{ Old = "Chunk a document"; New = "文档分块" },
    @{ Old = "Retrieve relevant documents"; New = "检索相关文档" },
    @{ Old = "Create a new plan"; New = "创建新计划" },
    @{ Old = "Update a plan"; New = "更新计划" },
    @{ Old = "Complete a plan"; New = "完成计划" },
    @{ Old = "Get plan history"; New = "获取计划历史" },
    @{ Old = "Get performance metrics"; New = "获取性能指标" },
    @{ Old = "Health check endpoint"; New = "健康检查端点" },
    @{ Old = "Start a new game"; New = "开始新游戏" },
    @{ Old = "Make a move in game"; New = "在游戏中执行操作" },
    @{ Old = "Get game state"; New = "获取游戏状态" },

    # 注释模板
    @{ Old = "Start of the application"; New = "应用启动" },
    @{ Old = "End of the method"; New = "方法结束" },
    @{ Old = "TODO: "; New = "待办: " },
    @{ Old = "FIXME: "; New = "待修复: " },
    @{ Old = "NOTE: "; New = "注意: " },
    @{ Old = "Note: "; New = "注意: " },
    @{ Old = "IMPORTANT: "; New = "重要: " },
    @{ Old = "Important: "; New = "重要: " },
    @{ Old = "WARNING: "; New = "警告: " },
    @{ Old = "Warning: "; New = "警告: " },
    @{ Old = "See also: "; New = "另请参阅: " },
    @{ Old = "See "; New = "参见 " },
    @{ Old = "For more information"; New = "更多信息请参见" },
    @{ Old = "For example"; New = "例如" },
    @{ Old = "Example: "; New = "示例: " },
    @{ Old = "Examples: "; New = "示例: " },

    # 测试相关
    @{ Old = "Test class for "; New = "测试类 - " },
    @{ Old = "Unit test for "; New = "单元测试 - " },
    @{ Old = "Integration test for "; New = "集成测试 - " },
    @{ Old = "Test method for "; New = "测试方法 - " },
    @{ Old = "Test case for "; New = "测试用例 - " },
    @{ Old = "Before each test"; New = "每个测试前执行" },
    @{ Old = "After each test"; New = "每个测试后执行" },
    @{ Old = "Before all tests"; New = "所有测试前执行" },
    @{ Old = "After all tests"; New = "所有测试后执行" },
    @{ Old = "Setup method"; New = "设置方法" },
    @{ Old = "Teardown method"; New = "清理方法" },
    @{ Old = "Mock the "; New = "模拟 " },
    @{ Old = "Initialize mocks"; New = "初始化模拟对象" },
    @{ Old = "Verify the result"; New = "验证结果" },
    @{ Old = "Assert that"; New = "断言" },
    @{ Old = "Verify that"; New = "验证" },
    @{ Old = "Test passed"; New = "测试通过" },
    @{ Old = "Test failed"; New = "测试失败" },

    # 异常
    @{ Old = "If the parameter is null"; New = "如果参数为空" },
    @{ Old = "If the parameter is empty"; New = "如果参数为空" },
    @{ Old = "If the parameter is invalid"; New = "如果参数无效" },
    @{ Old = "If the session is not found"; New = "如果会话未找到" },
    @{ Old = "If the agent is not found"; New = "如果 Agent 未找到" },
    @{ Old = "If the skill is not found"; New = "如果技能未找到" },
    @{ Old = "Throws exception if"; New = "如果...则抛出异常" },
    @{ Old = "Illegal argument exception"; New = "非法参数异常" },
    @{ Old = "Null pointer exception"; New = "空指针异常" },

    # 性能/线程
    @{ Old = "Thread-safe"; New = "线程安全" },
    @{ Old = "Not thread-safe"; New = "非线程安全" },
    @{ Old = "Performance optimization"; New = "性能优化" },
    @{ Old = "Time complexity"; New = "时间复杂度" },
    @{ Old = "Space complexity"; New = "空间复杂度" },
    @{ Old = "Asynchronous operation"; New = "异步操作" },
    @{ Old = "Synchronous operation"; New = "同步操作" },
    @{ Old = "Non-blocking"; New = "非阻塞" },
    @{ Old = "Blocking"; New = "阻塞" },

    # 生命周期
    @{ Old = "Lifecycle hook"; New = "生命周期钩子" },
    @{ Old = "Pre-call hook"; New = "调用前钩子" },
    @{ Old = "Post-call hook"; New = "调用后钩子" },
    @{ Old = "Pre-reasoning hook"; New = "推理前钩子" },
    @{ Old = "Post-reasoning hook"; New = "推理后钩子" },
    @{ Old = "Pre-acting hook"; New = "行动前钩子" },
    @{ Old = "Post-acting hook"; New = "行动后钩子" },
    @{ Old = "Event listener"; New = "事件监听器" },
    @{ Old = "Event handler"; New = "事件处理器" },

    # Spring 相关
    @{ Old = "Spring Boot configuration"; New = "Spring Boot 配置" },
    @{ Old = "Spring configuration class"; New = "Spring 配置类" },
    @{ Old = "Auto-configuration class"; New = "自动配置类" },
    @{ Old = "Bean definition"; New = "Bean 定义" },
    @{ Old = "Bean creation"; New = "Bean 创建" },
    @{ Old = "Bean initialization"; New = "Bean 初始化" },
    @{ Old = "Bean destruction"; New = "Bean 销毁" },
    @{ Old = "Dependency injection"; New = "依赖注入" },
    @{ Old = "Autowired"; New = "自动注入" },
    @{ Old = "Component scan"; New = "组件扫描" },
    @{ Old = "Profile configuration"; New = "环境配置" },
    @{ Old = "Development profile"; New = "开发环境" },
    @{ Old = "Production profile"; New = "生产环境" },
    @{ Old = "Test profile"; New = "测试环境" },

    # 数据库/JPA
    @{ Old = "JPA entity"; New = "JPA 实体" },
    @{ Old = "JPA repository interface"; New = "JPA 仓库接口" },
    @{ Old = "Entity listener"; New = "实体监听器" },
    @{ Old = "Database migration"; New = "数据库迁移" },
    @{ Old = "Database transaction"; New = "数据库事务" },
    @{ Old = "Transactional annotation"; New = "事务注解" },
    @{ Old = "Persistence context"; New = "持久化上下文" },
    @{ Old = "Entity manager"; New = "实体管理器" },

    # 日志
    @{ Old = "Log the error"; New = "记录错误日志" },
    @{ Old = "Log the warning"; New = "记录警告日志" },
    @{ Old = "Log the information"; New = "记录信息日志" },
    @{ Old = "Log the debug information"; New = "记录调试日志" },
    @{ Old = "Logger instance"; New = "日志记录器实例" },
    @{ Old = "Logging level"; New = "日志级别" },

    # 验证
    @{ Old = "Validate the input"; New = "验证输入" },
    @{ Old = "Input validation"; New = "输入验证" },
    @{ Old = "Parameter validation"; New = "参数验证" },
    @{ Old = "Validation failed"; New = "验证失败" },
    @{ Old = "Validation passed"; New = "验证通过" },
    @{ Old = "Invalid input"; New = "无效输入" },
    @{ Old = "Valid input"; New = "有效输入" },

    # 缓存
    @{ Old = "Cache the result"; New = "缓存结果" },
    @{ Old = "Cache eviction"; New = "缓存清除" },
    @{ Old = "Cache invalidation"; New = "缓存失效" },
    @{ Old = "Cache hit"; New = "缓存命中" },
    @{ Old = "Cache miss"; New = "缓存未命中" },
    @{ Old = "Cache key"; New = "缓存键" },
    @{ Old = "Cache value"; New = "缓存值" },

    # 序列化
    @{ Old = "Serialize the object"; New = "序列化对象" },
    @{ Old = "Deserialize the object"; New = "反序列化对象" },
    @{ Old = "JSON serialization"; New = "JSON 序列化" },
    @{ Old = "JSON deserialization"; New = "JSON 反序列化" },
    @{ Old = "Serialization failed"; New = "序列化失败" },
    @{ Old = "Deserialization failed"; New = "反序列化失败" },

    # 并发
    @{ Old = "Concurrent access"; New = "并发访问" },
    @{ Old = "Synchronized block"; New = "同步块" },
    @{ Old = "Atomic operation"; New = "原子操作" },
    @{ Old = "Race condition"; New = "竞态条件" },
    @{ Old = "Deadlock"; New = "死锁" },
    @{ Old = "Thread pool"; New = "线程池" },
    @{ Old = "Executor service"; New = "执行器服务" },

    # 错误处理
    @{ Old = "Error handling"; New = "错误处理" },
    @{ Old = "Exception handling"; New = "异常处理" },
    @{ Old = "Handle the exception"; New = "处理异常" },
    @{ Old = "Catch the exception"; New = "捕获异常" },
    @{ Old = "Throw an exception"; New = "抛出异常" },
    @{ Old = "Rethrow the exception"; New = "重新抛出异常" },
    @{ Old = "Wrap the exception"; New = "包装异常" },
    @{ Old = "Error message"; New = "错误消息" },
    @{ Old = "Exception message"; New = "异常消息" },

    # 重试
    @{ Old = "Retry mechanism"; New = "重试机制" },
    @{ Old = "Retry policy"; New = "重试策略" },
    @{ Old = "Max retry attempts"; New = "最大重试次数" },
    @{ Old = "Retry delay"; New = "重试延迟" },
    @{ Old = "Backoff strategy"; New = "退避策略" },

    # 监控
    @{ Old = "Metrics collection"; New = "指标收集" },
    @{ Old = "Performance metrics"; New = "性能指标" },
    @{ Old = "Monitoring endpoint"; New = "监控端点" },
    @{ Old = "Health check"; New = "健康检查" },
    @{ Old = "Liveness probe"; New = "存活探针" },
    @{ Old = "Readiness probe"; New = "就绪探针" },
    @{ Old = "Startup probe"; New = "启动探针" },

    # 安全
    @{ Old = "Security check"; New = "安全检查" },
    @{ Old = "Authentication"; New = "认证" },
    @{ Old = "Authorization"; New = "授权" },
    @{ Old = "Permission check"; New = "权限检查" },
    @{ Old = "Access control"; New = "访问控制" },
    @{ Old = "Sanitize input"; New = "输入净化" },
    @{ Old = "Input sanitization"; New = "输入净化" },
    @{ Old = "Security vulnerability"; New = "安全漏洞" },

    # 部署
    @{ Old = "Deployment configuration"; New = "部署配置" },
    @{ Old = "Docker configuration"; New = "Docker 配置" },
    @{ Old = "Kubernetes configuration"; New = "Kubernetes 配置" },
    @{ Old = "Helm chart"; New = "Helm 图表" },
    @{ Old = "Container image"; New = "容器镜像" },
    @{ Old = "Image build"; New = "镜像构建" },
    @{ Old = "Multi-stage build"; New = "多阶段构建" },

    # 代码质量
    @{ Old = "Code quality"; New = "代码质量" },
    @{ Old = "Code review"; New = "代码审查" },
    @{ Old = "Code smell"; New = "代码异味" },
    @{ Old = "Technical debt"; New = "技术债务" },
    @{ Old = "Refactoring needed"; New = "需要重构" },
    @{ Old = "Best practice"; New = "最佳实践" },

    # 其他通用
    @{ Old = "Initialization"; New = "初始化" },
    @{ Old = "Initialize the "; New = "初始化 " },
    @{ Old = "Cleanup the "; New = "清理 " },
    @{ Old = "Clean up resources"; New = "清理资源" },
    @{ Old = "Release resources"; New = "释放资源" },
    @{ Old = "Load configuration"; New = "加载配置" },
    @{ Old = "Save configuration"; New = "保存配置" },
    @{ Old = "Read from file"; New = "从文件读取" },
    @{ Old = "Write to file"; New = "写入文件" },
    @{ Old = "Read from database"; New = "从数据库读取" },
    @{ Old = "Write to database"; New = "写入数据库" },
    @{ Old = "Read from cache"; New = "从缓存读取" },
    @{ Old = "Write to cache"; New = "写入缓存" },
    @{ Old = "Connect to "; New = "连接到 " },
    @{ Old = "Disconnect from "; New = "断开连接 " },
    @{ Old = "Connection pool"; New = "连接池" },
    @{ Old = "Connection timeout"; New = "连接超时" },
    @{ Old = "Request timeout"; New = "请求超时" },
    @{ Old = "Operation timeout"; New = "操作超时" },
    @{ Old = "Timeout exceeded"; New = "超时" },
    @{ Old = "Retry count"; New = "重试次数" },
    @{ Old = "Max retries"; New = "最大重试次数" },
    @{ Old = "Backoff time"; New = "退避时间" },
    @{ Old = "Circuit breaker"; New = "断路器" },
    @{ Old = "Fallback mechanism"; New = "降级机制" },
    @{ Old = "Rate limiting"; New = "限流" },
    @{ Old = "Throttling"; New = "节流" },
    @{ Old = "Bulkhead pattern"; New = "舱壁模式" },

    # RAG 相关
    @{ Old = "Vector search"; New = "向量搜索" },
    @{ Old = "Semantic search"; New = "语义搜索" },
    @{ Old = "Hybrid search"; New = "混合搜索" },
    @{ Old = "Text chunking"; New = "文本分块" },
    @{ Old = "Embedding generation"; New = "Embedding 生成" },
    @{ Old = "Vector storage"; New = "向量存储" },
    @{ Old = "Similarity score"; New = "相似度分数" },
    @{ Old = "Cosine similarity"; New = "余弦相似度" },
    @{ Old = "Top-k retrieval"; New = "Top-k 检索" },
    @{ Old = "Context window"; New = "上下文窗口" },
    @{ Old = "Context compression"; New = "上下文压缩" },

    # Agent 相关
    @{ Old = "Agent reasoning"; New = "Agent 推理" },
    @{ Old = "Agent planning"; New = "Agent 规划" },
    @{ Old = "Agent memory"; New = "Agent 记忆" },
    @{ Old = "Agent tool use"; New = "Agent 工具使用" },
    @{ Old = "Agent state"; New = "Agent 状态" },
    @{ Old = "Agent lifecycle"; New = "Agent 生命周期" },
    @{ Old = "Thought-Action-Observation cycle"; New = "思考-行动-观察循环" },
    @{ Old = "ReAct pattern"; New = "ReAct 模式" },
    @{ Old = "Chain of thought"; New = "思维链" },
    @{ Old = "Tool calling"; New = "工具调用" },
    @{ Old = "Function calling"; New = "函数调用" },
    @{ Old = "System prompt"; New = "系统提示词" },
    @{ Old = "User prompt"; New = "用户提示词" },
    @{ Old = "Prompt engineering"; New = "提示词工程" },
    @{ Old = "Prompt template"; New = "提示词模板" },
    @{ Old = "Context injection"; New = "上下文注入" },
    @{ Old = "Streaming response"; New = "流式响应" },
    @{ Old = "Non-streaming response"; New = "非流式响应" },

    # 多代理
    @{ Old = "Multi-agent collaboration"; New = "多代理协作" },
    @{ Old = "Agent communication"; New = "代理通信" },
    @{ Old = "Agent coordination"; New = "代理协调" },
    @{ Old = "Agent handoff"; New = "代理交接" },
    @{ Old = "Agent router"; New = "代理路由" },
    @{ Old = "Agent supervisor"; New = "代理监督者" },
    @{ Old = "Agent pipeline"; New = "代理流水线" },
    @{ Old = "Agent workflow"; New = "代理工作流" },
    @{ Old = "Task decomposition"; New = "任务分解" },
    @{ Old = "Task allocation"; New = "任务分配" },
    @{ Old = "Result aggregation"; New = "结果聚合" },
    @{ Old = "Parallel execution"; New = "并行执行" },
    @{ Old = "Sequential execution"; New = "顺序执行" },

    # 技能
    @{ Old = "Skill definition"; New = "技能定义" },
    @{ Old = "Skill registration"; New = "技能注册" },
    @{ Old = "Skill activation"; New = "技能激活" },
    @{ Old = "Skill deactivation"; New = "技能停用" },
    @{ Old = "Skill discovery"; New = "技能发现" },
    @{ Old = "Skill repository"; New = "技能仓库" },
    @{ Old = "Skill versioning"; New = "技能版本管理" },
    @{ Old = "Skill box"; New = "技能容器" },

    # Harness
    @{ Old = "Sandbox isolation"; New = "沙箱隔离" },
    @{ Old = "Sandbox filesystem"; New = "沙箱文件系统" },
    @{ Old = "Sandbox workspace"; New = "沙箱工作区" },
    @{ Old = "Code execution sandbox"; New = "代码执行沙箱" },
    @{ Old = "Sandbox security"; New = "沙箱安全" },
    @{ Old = "Sandbox policy"; New = "沙箱策略" },
    @{ Old = "Sandbox lifecycle"; New = "沙箱生命周期" },

    # A2A
    @{ Old = "Agent-to-agent protocol"; New = "Agent 间通信协议" },
    @{ Old = "Agent card"; New = "Agent 卡片" },
    @{ Old = "Agent discovery"; New = "Agent 发现" },
    @{ Old = "Agent registry"; New = "Agent 注册中心" },
    @{ Old = "Message transport"; New = "消息传输" },
    @{ Old = "Message format"; New = "消息格式" },
    @{ Old = "RPC call"; New = "远程过程调用" },
    @{ Old = "JSON-RPC"; New = "JSON 远程过程调用" },

    # 审批
    @{ Old = "Approval workflow"; New = "审批工作流" },
    @{ Old = "Approval request"; New = "审批请求" },
    @{ Old = "Pending approval"; New = "待审批" },
    @{ Old = "Approved"; New = "已批准" },
    @{ Old = "Rejected"; New = "已拒绝" },
    @{ Old = "Approver"; New = "审批人" },
    @{ Old = "Approval timeout"; New = "审批超时" },
    @{ Old = "Human-in-the-loop"; New = "人类在环" },
    @{ Old = "Human intervention"; New = "人工介入" },

    # 部署
    @{ Old = "Deployment environment"; New = "部署环境" },
    @{ Old = "Environment variables"; New = "环境变量" },
    @{ Old = "Configuration management"; New = "配置管理" },
    @{ Old = "Secrets management"; New = "密钥管理" },
    @{ Old = "Service discovery"; New = "服务发现" },
    @{ Old = "Load balancing"; New = "负载均衡" },
    @{ Old = "High availability"; New = "高可用" },
    @{ Old = "Scalability"; New = "可扩展性" },
    @{ Old = "Horizontal scaling"; New = "水平扩展" },
    @{ Old = "Vertical scaling"; New = "垂直扩展" },
    @{ Old = "Auto-scaling"; New = "自动扩缩容" },

    # 监控
    @{ Old = "Distributed tracing"; New = "分布式追踪" },
    @{ Old = "OpenTelemetry"; New = "OpenTelemetry 可观测性框架" },
    @{ Old = "Prometheus metrics"; New = "Prometheus 指标" },
    @{ Old = "Grafana dashboard"; New = "Grafana 仪表盘" },
    @{ Old = "Log aggregation"; New = "日志聚合" },
    @{ Old = "Alerting"; New = "告警" },

    # 性能
    @{ Old = "Performance optimization"; New = "性能优化" },
    @{ Old = "Memory optimization"; New = "内存优化" },
    @{ Old = "CPU optimization"; New = "CPU 优化" },
    @{ Old = "Network optimization"; New = "网络优化" },
    @{ Old = "Database optimization"; New = "数据库优化" },
    @{ Old = "Query optimization"; New = "查询优化" },
    @{ Old = "Index optimization"; New = "索引优化" },
    @{ Old = "Connection pooling"; New = "连接池" },
    @{ Old = "Batch processing"; New = "批处理" },
    @{ Old = "Caching strategy"; New = "缓存策略" },

    # 测试
    @{ Old = "Unit testing"; New = "单元测试" },
    @{ Old = "Integration testing"; New = "集成测试" },
    @{ Old = "End-to-end testing"; New = "端到端测试" },
    @{ Old = "Mocking"; New = "模拟" },
    @{ Old = "Test coverage"; New = "测试覆盖率" },
    @{ Old = "Test fixture"; New = "测试夹具" },
    @{ Old = "Test suite"; New = "测试套件" },
    @{ Old = "Test case"; New = "测试用例" },
    @{ Old = "Assertion"; New = "断言" },
    @{ Old = "Test data"; New = "测试数据" },
    @{ Old = "Test setup"; New = "测试设置" },
    @{ Old = "Test teardown"; New = "测试清理" }
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "AgentScope Java 教程 - 中文注释批量转换脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 获取所有 Java 文件
$javaFiles = Get-ChildItem -Path $tutorialDir -Filter "*.java" -Recurse | Sort-Object FullName

Write-Host "找到 $($javaFiles.Count) 个 Java 文件" -ForegroundColor Green
Write-Host ""

foreach ($file in $javaFiles) {
    $totalFiles++
    $content = Get-Content -Path $file.FullName -Raw -Encoding UTF8
    $originalContent = $content
    $modified = $false

    # 应用所有替换规则
    foreach ($rule in $replacements) {
        if ($content -match [regex]::Escape($rule.Old)) {
            $content = $content -replace [regex]::Escape($rule.Old), $rule.New
            $modified = $true
        }
    }

    # 处理常见的类注释模板
    $content = $content -replace '(?s)/\*\*\s*\*\s+This class.*?\*/', {
        $match = $args[0].Value
        if ($match -match 'Spring Boot') {
            return @'
/**
 * Spring Boot 应用启动类
 * 配置并启动 AgentScope 教程应用
 */
'@
        } elseif ($match -match 'controller|Controller') {
            return @'
/**
 * REST API 控制器
 * 提供 HTTP 接口供前端调用
 */
'@
        } elseif ($match -match 'service|Service') {
            return @'
/**
 * 业务服务类
 * 封装核心业务逻辑处理
 */
'@
        } elseif ($match -match 'config|Config') {
            return @'
/**
 * 配置类
 * 管理应用配置和 Bean 定义
 */
'@
        } elseif ($match -match 'entity|Entity') {
            return @'
/**
 * JPA 实体类
 * 映射数据库表结构
 */
'@
        } elseif ($match -match 'repository|Repository') {
            return @'
/**
 * 数据访问层接口
 * 提供数据库 CRUD 操作
 */
'@
        } elseif ($match -match 'hook|Hook') {
            return @'
/**
 * 生命周期钩子
 * 监听 Agent 执行过程中的事件
 */
'@
        } elseif ($match -match 'tool|Tool') {
            return @'
/**
 * Agent 工具类
 * 提供可被 Agent 调用的功能方法
 */
'@
        } else {
            return $match
        }
    }

    # 处理方法注释 - @param, @return, @throws
    $content = $content -replace '@param\s+(\w+)\s+(\w.*)', {
        $paramName = $args[0].Groups[1].Value
        $paramDesc = $args[0].Groups[2].Value
        $translatedDesc = $paramDesc
        foreach ($rule in $replacements) {
            $translatedDesc = $translatedDesc -replace [regex]::Escape($rule.Old), $rule.New
        }
        "@param $paramName $translatedDesc"
    }

    $content = $content -replace '@return\s+(\w.*)', {
        $returnDesc = $args[0].Groups[1].Value
        $translatedDesc = $returnDesc
        foreach ($rule in $replacements) {
            $translatedDesc = $translatedDesc -replace [regex]::Escape($rule.Old), $rule.New
        }
        "@return $translatedDesc"
    }

    $content = $content -replace '@throws\s+(\w+)\s+(\w.*)', {
        $exType = $args[0].Groups[1].Value
        $exDesc = $args[0].Groups[2].Value
        $translatedDesc = $exDesc
        foreach ($rule in $replacements) {
            $translatedDesc = $translatedDesc -replace [regex]::Escape($rule.Old), $rule.New
        }
        "@throws $exType $translatedDesc"
    }

    # 如果内容有变化，写回文件
    if ($modified -or $content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -Encoding UTF8 -NoNewline
        $modifiedFiles++
        Write-Host "[$modifiedFiles] 已更新: $($file.FullName.Replace($tutorialDir, ''))" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "转换完成！" -ForegroundColor Green
Write-Host "处理文件总数: $totalFiles" -ForegroundColor White
Write-Host "更新文件数: $modifiedFiles" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan