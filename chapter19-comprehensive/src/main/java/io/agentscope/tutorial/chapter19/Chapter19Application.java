/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter19;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 第十九章教程示例应用启动类
 *
 * <p>本示例展示 AgentScope Java 的三个综合实战案例：
 * <ul>
 *   <li>案例1：奶茶店多代理系统 - 监督者模式协调多个专业Agent</li>
 *   <li>案例2：狼人杀游戏 - 多人游戏多Agent模拟</li>
 *   <li>案例3：企业智能助手 - 知识问答、日程管理、报表生成</li>
 * </ul>
 *
 * <p>启动后可通过以下端点交互：
 * <ul>
 *   <li>POST /api/boba/order - 奶茶店点单接口</li>
 *   <li>POST /api/werewolf/start - 开始狼人杀游戏</li>
 *   <li>POST /api/assistant/chat - 企业助手聊天接口</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
@EnableAsync
public class Chapter19Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter19Application.class, args);
        System.out.println("===============================================");
        System.out.println("AgentScope Java 教程 - 第十九章示例已启动");
        System.out.println("综合实战案例：");
        System.out.println("  1. 奶茶店多代理系统 (Supervisor模式)");
        System.out.println("  2. 狼人杀游戏 (多Agent博弈)");
        System.out.println("  3. 企业智能助手 (多能力协作)");
        System.out.println("===============================================");
    }
}