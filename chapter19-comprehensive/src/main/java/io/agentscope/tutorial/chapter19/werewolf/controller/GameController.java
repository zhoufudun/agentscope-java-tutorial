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
package io.agentscope.tutorial.chapter19.werewolf.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.tutorial.chapter19.werewolf.game.WerewolfGame;
import io.agentscope.tutorial.chapter19.werewolf.role.ModeratorAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 狼人杀游戏控制器
 *
 * <p>提供狼人杀游戏的 Web 接口，包括：
 * <ul>
 *   <li>开始游戏</li>
 *   <li>获取游戏状态</li>
 *   <li>SSE 实时推送游戏进程</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@RestController
@RequestMapping("/api/werewolf")
public class GameController {

    private static final Logger logger = LoggerFactory.getLogger(GameController.class);

    private final ReActAgent moderatorAgent;
    private final ReActAgent villagerAgent;
    private final ReActAgent werewolfAgent;
    private final ModeratorAgent moderatorRoleAgent;

    private WerewolfGame currentGame;
    private final ExecutorService gameExecutor;

    public GameController(
            @Qualifier("werewolfModeratorAgent") ReActAgent moderatorAgent,
            @Qualifier("villagerAgent") ReActAgent villagerAgent,
            @Qualifier("werewolfAgent") ReActAgent werewolfAgent,
            @Autowired ModeratorAgent moderatorRoleAgent) {
        this.moderatorAgent = moderatorAgent;
        this.villagerAgent = villagerAgent;
        this.werewolfAgent = werewolfAgent;
        this.moderatorRoleAgent = moderatorRoleAgent;
        this.gameExecutor = Executors.newCachedThreadPool();
        logger.info("GameController 初始化完成");
    }

    /**
     * 开始新游戏
     *
     * @return 游戏ID
     */
    @PostMapping("/start")
    public ResponseEntity<String> startGame() {
        logger.info("开始新游戏");

        try {
            // 创建游戏实例
            currentGame = new WerewolfGame(moderatorAgent, villagerAgent, werewolfAgent);

            // 添加事件监听器
            currentGame.addEventListener(event ->
                    logger.info("游戏事件: {} - {}", event.getType(), event.getMessage()));

            // 异步执行游戏
            gameExecutor.submit(() -> {
                try {
                    String result = currentGame.startGame();
                    logger.info("游戏执行完成");
                } catch (Exception e) {
                    logger.error("游戏执行失败", e);
                }
            });

            return ResponseEntity.ok("游戏已开始！");
        } catch (Exception e) {
            logger.error("启动游戏失败", e);
            return ResponseEntity.internalServerError()
                    .body("启动游戏失败：" + e.getMessage());
        }
    }

    /**
     * 获取游戏状态
     */
    @GetMapping("/status")
    public ResponseEntity<String> getGameStatus() {
        if (currentGame == null) {
            return ResponseEntity.ok("游戏未开始");
        }
        return ResponseEntity.ok("游戏进行中...");
    }

    /**
     * SSE 流式观看游戏
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGame() {
        logger.info("建立 SSE 连接");

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        if (currentGame == null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("游戏未开始，请先调用 /api/werewolf/start"));
                emitter.complete();
            } catch (IOException e) {
                logger.error("SSE 发送失败", e);
            }
            return emitter;
        }

        // 添加事件监听器
        currentGame.addEventListener(event -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(event.getType().name().toLowerCase())
                        .data(event.getMessage()));
            } catch (IOException e) {
                logger.error("SSE 发送失败", e);
            }
        });

        emitter.onCompletion(() -> logger.info("SSE 连接关闭"));
        emitter.onTimeout(() -> logger.info("SSE 连接超时"));

        return emitter;
    }

    /**
     * 获取游戏规则说明
     */
    @GetMapping("/rules")
    public ResponseEntity<String> getRules() {
        String rules = """
                ===== 狼人杀游戏规则 =====

                【角色介绍】
                - 村民: 无特殊能力，需要靠逻辑找出狼人
                - 狼人: 每晚可以击杀一名玩家
                - 预言家: 每晚可以查验一名玩家身份
                - 女巫: 拥有一瓶解药和一瓶毒药
                - 猎人: 死亡时可以带走一名玩家

                【游戏流程】
                1. 夜晚阶段：狼人→女巫→预言家
                2. 白天阶段：死讯公布→自由讨论→投票放逐

                【胜负条件】
                - 狼人胜利：狼人数量 >= 村民数量
                - 村民胜利：所有狼人死亡
                """;
        return ResponseEntity.ok(rules);
    }
}