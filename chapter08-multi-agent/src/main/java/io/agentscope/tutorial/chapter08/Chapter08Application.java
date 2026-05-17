package io.agentscope.tutorial.chapter08;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第八章：多代理架构模式 - Spring Boot 启动类
 *
 * 本案例演示智能客户服务中心，整合 Pipeline 和 Routing 模式：
 * - 路由层：接收查询，智能分类分发到专业代理
 * - 处理层：每个专业代理使用顺序流水线完成处理
 * - 汇总层：监督者代理汇总多源输出
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter08Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter08Application.class, args);
    }
}