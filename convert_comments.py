# -*- coding: utf-8 -*-
"""
AgentScope Java 教程 - 中文注释批量转换脚本
"""

import os
import re
from pathlib import Path

# 教程目录
TUTORIAL_DIR = r"D:\Work\ai\agents\agentscope-java\tutorial"

# 注释替换映射表 (英文 -> 中文)
REPLACEMENTS = [
    # 应用启动类
    ("Spring Boot application entry point", "Spring Boot 应用启动入口"),
    ("Main application class", "主应用类"),
    ("Main method to start the application", "启动应用的主方法"),
    ("Main method", "主方法"),
    ("Application entry point", "应用入口"),

    # 配置类
    ("Configuration class for AgentScope", "AgentScope 配置类"),
    ("Configuration class", "配置类"),
    ("Agent configuration properties", "Agent 配置属性"),
    ("Model configuration properties", "模型配置属性"),
    ("Properties for AgentScope", "AgentScope 属性配置"),
    ("Configuration properties", "配置属性"),
    ("Hook configuration properties", "Hook 配置属性"),
    ("Harness configuration properties", "Harness 配置属性"),
    ("Configuration for RAG", "RAG 配置"),
    ("Qdrant configuration properties", "Qdrant 向量数据库配置"),

    # 控制器
    ("REST API controller", "REST API 控制器"),
    ("Chat REST API controller", "聊天 REST API 控制器"),
    ("Controller for chat operations", "聊天操作控制器"),
    ("Controller for Agent operations", "Agent 操作控制器"),
    ("Controller for skill management", "技能管理控制器"),
    ("Controller for session management", "会话管理控制器"),
    ("Controller for approval workflow", "审批工作流控制器"),
    ("Controller for code execution", "代码执行控制器"),
    ("Controller for workspace management", "工作区管理控制器"),
    ("Controller for audit operations", "审计操作控制器"),
    ("Controller for document operations", "文档操作控制器"),
    ("Controller for plan management", "计划管理控制器"),
    ("Game controller for Werewolf", "狼人杀游戏控制器"),

    # 服务层
    ("Service class for Agent operations", "Agent 操作服务类"),
    ("Service class for chat operations", "聊天操作服务类"),
    ("Service for managing skills", "技能管理服务"),
    ("Service for managing memory", "记忆管理服务"),
    ("Service for managing models", "模型管理服务"),
    ("Service for managing sessions", "会话管理服务"),
    ("Service for managing plans", "计划管理服务"),
    ("Service for managing approval workflow", "审批工作流管理服务"),
    ("Service for code execution", "代码执行服务"),
    ("Service for workspace management", "工作区管理服务"),
    ("Service for document processing", "文档处理服务"),
    ("Service for vector operations", "向量操作服务"),
    ("Service for RAG operations", "RAG 检索增强生成服务"),
    ("Service for embedding operations", "Embedding 向量化服务"),
    ("Multi-agent coordination service", "多代理协作服务"),
    ("Service for enterprise assistant", "企业智能助手服务"),

    # 实体/模型
    ("Entity class for chat message", "聊天消息实体类"),
    ("Entity class for chat session", "聊天会话实体类"),
    ("Entity class for skill definition", "技能定义实体类"),
    ("Entity class for memory record", "记忆记录实体类"),
    ("Entity class for audit log", "审计日志实体类"),
    ("Entity class for approval request", "审批请求实体类"),
    ("Entity class for plan record", "计划记录实体类"),
    ("DTO for chat request", "聊天请求数据传输对象"),
    ("DTO for chat response", "聊天响应数据传输对象"),
    ("DTO for execution request", "执行请求数据传输对象"),
    ("DTO for execution result", "执行结果数据传输对象"),
    ("Data transfer object", "数据传输对象"),
    ("Model class for document", "文档模型类"),
    ("Model class for game state", "游戏状态模型类"),
    ("Model class for player", "玩家模型类"),

    # 仓库
    ("Repository for chat messages", "聊天消息数据访问层"),
    ("Repository for chat sessions", "聊天会话数据访问层"),
    ("Repository for skill definitions", "技能定义数据访问层"),
    ("Repository for memory records", "记忆记录数据访问层"),
    ("Repository for audit logs", "审计日志数据访问层"),
    ("Repository for approval requests", "审批请求数据访问层"),
    ("Repository for plan records", "计划记录数据访问层"),
    ("Data access layer", "数据访问层"),
    ("JPA repository", "JPA 数据访问接口"),

    # Hook 钩子
    ("Hook for logging events", "事件日志钩子"),
    ("Hook for collecting metrics", "指标收集钩子"),
    ("Hook for auditing tool calls", "工具调用审计钩子"),
    ("Hook for timing measurement", "性能计时钩子"),
    ("Hook for approval workflow", "审批工作流钩子"),
    ("Hook for plan changes", "计划变更钩子"),
    ("Lifecycle hook", "生命周期钩子"),

    # 工具类
    ("Tool for weather queries", "天气查询工具"),
    ("Tool for calculator operations", "计算器操作工具"),
    ("Tool for datetime operations", "日期时间操作工具"),
    ("Tool for search operations", "搜索操作工具"),
    ("Tool for HTTP requests", "HTTP 请求工具"),
    ("Built-in tools for file operations", "文件操作内置工具"),
    ("Tool for memory management", "记忆管理工具"),
    ("Tool for sending email", "邮件发送工具"),
    ("Tool for data deletion", "数据删除工具"),
    ("Tool for data analysis", "数据分析工具"),
    ("Tool for code review", "代码审查工具"),

    # 沙箱
    ("In-memory sandbox implementation", "内存沙箱实现"),
    ("Sandbox state management", "沙箱状态管理"),
    ("Sandbox client implementation", "沙箱客户端实现"),
    ("Sandbox filesystem spec", "沙箱文件系统规范"),

    # Agent
    ("Supervisor Agent for order management", "订单管理监督 Agent"),
    ("Cashier Agent for payment processing", "支付处理收银 Agent"),
    ("Barista Agent for drink preparation", "饮品制作吧台 Agent"),
    ("Moderator Agent for Werewolf game", "狼人杀游戏主持人 Agent"),
    ("Villager Agent for Werewolf game", "狼人杀村民 Agent"),
    ("Werewolf Agent for Werewolf game", "狼人杀狼人 Agent"),
    ("Enterprise assistant Agent", "企业智能助手 Agent"),
    ("Knowledge base Agent", "知识库 Agent"),
    ("Schedule management Agent", "日程管理 Agent"),
    ("Report generation Agent", "报表生成 Agent"),
    ("Router Agent for intent classification", "意图分类路由 Agent"),
    ("Order Agent for order queries", "订单查询 Agent"),
    ("Query Agent for general queries", "通用查询 Agent"),
    ("Synthesis Agent for result aggregation", "结果汇总合成 Agent"),

    # 游戏相关
    ("Game state management", "游戏状态管理"),
    ("Werewolf game core logic", "狼人杀游戏核心逻辑"),

    # 通用注释
    ("Main entry point", "主入口"),
    ("Entry point", "入口点"),
    ("Constructor", "构造函数"),
    ("Default constructor", "默认构造函数"),
    ("Getters and setters", "获取器和设置器"),
    ("Getter method", "获取方法"),
    ("Setter method", "设置方法"),
    ("Returns the", "返回"),
    ("Sets the", "设置"),
    ("Param ", "参数 "),
    ("Return ", "返回 "),
    ("Returns ", "返回 "),
    ("Throws ", "抛出异常 "),
    ("Exception thrown", "抛出异常"),
    ("Method to", "方法用于"),
    ("This method", "该方法"),
    ("This class", "该类"),
    ("This interface", "该接口"),
    ("This field", "该字段"),
    ("This property", "该属性"),
    ("This parameter", "该参数"),
    ("This method is used to", "该方法用于"),
    ("This class is used to", "该类用于"),
    ("Used to", "用于"),
    ("Used for", "用于"),
    ("Indicates whether", "指示是否"),
    ("Whether to", "是否"),
    ("True if", "如果为真"),
    ("Default value", "默认值"),
    ("Default", "默认"),
    ("Optional parameter", "可选参数"),
    ("Required parameter", "必填参数"),

    # 方法注释
    ("Handle chat message", "处理聊天消息"),
    ("Handle streaming chat", "处理流式聊天"),
    ("Get chat history", "获取聊天历史"),
    ("Clear chat history", "清除聊天历史"),
    ("Get session information", "获取会话信息"),
    ("Create a new session", "创建新会话"),
    ("Delete a session", "删除会话"),
    ("List all sessions", "列出所有会话"),
    ("Get all skills", "获取所有技能"),
    ("Activate a skill", "激活技能"),
    ("Deactivate a skill", "停用技能"),
    ("Register a new skill", "注册新技能"),
    ("Get audit records", "获取审计记录"),
    ("Get audit statistics", "获取审计统计"),
    ("Get pending approvals", "获取待审批列表"),
    ("Approve a request", "批准请求"),
    ("Reject a request", "拒绝请求"),
    ("Execute code in sandbox", "在沙箱中执行代码"),
    ("Execute a command", "执行命令"),
    ("List files in workspace", "列出工作区文件"),
    ("Read a file", "读取文件"),
    ("Write a file", "写入文件"),
    ("Delete a file", "删除文件"),
    ("Upload a document", "上传文档"),
    ("Chunk a document", "文档分块"),
    ("Retrieve relevant documents", "检索相关文档"),
    ("Create a new plan", "创建新计划"),
    ("Update a plan", "更新计划"),
    ("Complete a plan", "完成计划"),
    ("Get plan history", "获取计划历史"),
    ("Get performance metrics", "获取性能指标"),
    ("Health check endpoint", "健康检查端点"),
    ("Start a new game", "开始新游戏"),
    ("Make a move in game", "在游戏中执行操作"),
    ("Get game state", "获取游戏状态"),

    # 测试相关
    ("Test class for ", "测试类 - "),
    ("Unit test for ", "单元测试 - "),
    ("Integration test for ", "集成测试 - "),
    ("Test method for ", "测试方法 - "),
    ("Test case for ", "测试用例 - "),
    ("Before each test", "每个测试前执行"),
    ("After each test", "每个测试后执行"),
    ("Before all tests", "所有测试前执行"),
    ("After all tests", "所有测试后执行"),
    ("Setup method", "设置方法"),
    ("Teardown method", "清理方法"),
    ("Mock the ", "模拟 "),
    ("Initialize mocks", "初始化模拟对象"),
    ("Verify the result", "验证结果"),
    ("Assert that", "断言"),
    ("Verify that", "验证"),
    ("Test passed", "测试通过"),
    ("Test failed", "测试失败"),

    # Spring 相关
    ("Spring Boot configuration", "Spring Boot 配置"),
    ("Spring configuration class", "Spring 配置类"),
    ("Auto-configuration class", "自动配置类"),
    ("Bean definition", "Bean 定义"),
    ("Bean creation", "Bean 创建"),
    ("Bean initialization", "Bean 初始化"),
    ("Bean destruction", "Bean 销毁"),
    ("Dependency injection", "依赖注入"),
    ("Autowired", "自动注入"),
    ("Component scan", "组件扫描"),
    ("Profile configuration", "环境配置"),
    ("Development profile", "开发环境"),
    ("Production profile", "生产环境"),
    ("Test profile", "测试环境"),

    # 数据库/JPA
    ("JPA entity", "JPA 实体"),
    ("JPA repository interface", "JPA 仓库接口"),
    ("Entity listener", "实体监听器"),
    ("Database migration", "数据库迁移"),
    ("Database transaction", "数据库事务"),
    ("Transactional annotation", "事务注解"),
    ("Persistence context", "持久化上下文"),
    ("Entity manager", "实体管理器"),

    # 日志
    ("Log the error", "记录错误日志"),
    ("Log the warning", "记录警告日志"),
    ("Log the information", "记录信息日志"),
    ("Log the debug information", "记录调试日志"),
    ("Logger instance", "日志记录器实例"),
    ("Logging level", "日志级别"),
]


def replace_in_file(file_path):
    """替换单个文件中的注释"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except UnicodeDecodeError:
        try:
            with open(file_path, 'r', encoding='gbk') as f:
                content = f.read()
        except:
            return False

    original_content = content
    modified = False

    # 应用所有替换规则
    for eng, chinese in REPLACEMENTS:
        if eng in content:
            content = content.replace(eng, chinese)
            modified = True

    # 处理类注释模板 - // 注释
    # 处理简单的行注释
    lines = content.split('\n')
    for i, line in enumerate(lines):
        stripped = line.strip()
        # 处理 // 注释
        if stripped.startswith('//'):
            comment = stripped[2:].strip()
            # 尝试替换注释内容
            for eng, chinese in REPLACEMENTS:
                if comment.lower() == eng.lower() or comment == eng:
                    lines[i] = line.replace(comment, chinese)
                    modified = True
                    break

    content = '\n'.join(lines)

    # 如果有修改，写回文件
    if modified and content != original_content:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


def main():
    """主函数"""
    print("=" * 50)
    print("AgentScope Java 教程 - 中文注释批量转换")
    print("=" * 50)
    print()

    # 查找所有 Java 文件
    java_files = list(Path(TUTORIAL_DIR).rglob("*.java"))
    print(f"找到 {len(java_files)} 个 Java 文件")
    print()

    modified_count = 0

    for i, file_path in enumerate(java_files, 1):
        rel_path = file_path.relative_to(TUTORIAL_DIR)
        if replace_in_file(file_path):
            modified_count += 1
            print(f"[{modified_count}] 已更新: {rel_path}")

    print()
    print("=" * 50)
    print("转换完成！")
    print(f"处理文件总数: {len(java_files)}")
    print(f"更新文件数: {modified_count}")
    print("=" * 50)


if __name__ == "__main__":
    main()