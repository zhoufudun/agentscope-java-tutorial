package io.agentscope.tutorial.chapter11;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope 第十一章教程 - 技能系统
 *
 * <p>本应用演示了 AgentScope Java 的完整技能系统，包括：
 * <ul>
 *   <li>技能定义和元数据管理</li>
 *   <li>技能注册和动态加载</li>
 *   <li>技能仓库 (MySQL/H2 存储)</li>
 *   <li>SkillBox 工具包集成</li>
 *   <li>技能执行和上下文管理</li>
 *   <li>技能版本管理和历史追踪</li>
 *   <li>Agent 技能提示词自动生成</li>
 *   <li>技能执行监控和统计</li>
 * </ul>
 *
 * <p>核心类说明：
 * <ul>
 *   <li>{@link io.agentscope.core.skill.AgentSkill} - 技能定义核心类</li>
 *   <li>{@link io.agentscope.core.skill.SkillBox} - 技能管理工具箱</li>
 *   <li>{@link io.agentscope.core.skill.util.SkillUtil} - 技能创建工具类</li>
 *   <li>{@link service.SkillService} - 技能业务服务</li>
 *   <li>{@link service.ChatService} - 聊天服务（含技能集成）</li>
 * </ul>
 *
 * <p>REST API 端点：
 * <ul>
 *   <li>GET /skills - 获取所有技能列表</li>
 *   <li>GET /skills/{id} - 获取技能详情</li>
 *   <li>POST /skills - 创建新技能</li>
 *   <li>PUT /skills/{id} - 更新技能</li>
 *   <li>DELETE /skills/{id} - 删除技能</li>
 *   <li>POST /skills/{id}/activate - 激活技能</li>
 *   <li>POST /skills/{id}/deactivate - 停用技能</li>
 *   <li>GET /skills/{id}/versions - 获取技能版本历史</li>
 *   <li>POST /chat - 聊天（可使用技能）</li>
 *   <li>GET /chat/history - 获取聊天历史</li>
 * </ul>
 *
 * <p>启动后可访问：
 * <ul>
 *   <li>API: http://localhost:8080/api</li>
 *   <li>H2 Console: http://localhost:8080/api/h2-console (dev)</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootApplication
public class Chapter11Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter11Application.class, args);
    }
}