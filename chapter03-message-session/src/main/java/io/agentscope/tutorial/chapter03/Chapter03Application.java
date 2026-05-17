package io.agentscope.tutorial.chapter03;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java 第三章演示应用
 *
 * 本应用展示消息与会话管理的完整实现：
 * - 多轮对话 Agent
 * - H2 数据库会话持久化
 * - 自定义消息格式器
 * - JPA 数据持久化
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter03Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter03Application.class, args);
    }
}