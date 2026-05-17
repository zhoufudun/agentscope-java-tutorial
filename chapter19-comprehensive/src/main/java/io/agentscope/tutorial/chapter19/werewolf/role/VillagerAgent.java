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
package io.agentscope.tutorial.chapter19.werewolf.role;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * 村民角色 Agent
 *
 * <p>负责在狼人杀游戏中扮演村民角色，进行逻辑推理和讨论。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class VillagerAgent {

    private static final Logger logger = LoggerFactory.getLogger(VillagerAgent.class);

    private final ReActAgent villagerAgent;

    public VillagerAgent(
            @Qualifier("villagerAgent") ReActAgent villagerAgent) {
        this.villagerAgent = villagerAgent;
        logger.info("VillagerAgent 初始化完成");
    }

    /**
     * 获取发言
     *
     * @param gameContext 游戏上下文
     * @return 发言内容
     */
    public String speak(String gameContext) {
        logger.info("村民发言: {}", gameContext);

        String prompt = String.format("""
                %s

                请以村民的身份发表你的看法，分析谁可能是狼人，并给出你的投票建议。
                发言要积极主动，展现出想要找出狼人的态度。
                """, gameContext);

        try {
            Msg msg = Msg.builder()
                    .name("moderator")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = villagerAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("村民发言失败", e);
            return "村民暂时无法发言";
        }
    }

    /**
     * 投票
     *
     * @param candidates 候选玩家列表
     * @return 投票目标
     */
    public String vote(String candidates) {
        logger.info("村民投票: {}", candidates);

        String prompt = String.format("""
                请选择你要投票放逐的玩家。

                候选玩家：
                %s

                请分析每个玩家的发言和表现，选择你认为最可能是狼人的玩家。
                只回复玩家名称即可。
                """, candidates);

        try {
            Msg msg = Msg.builder()
                    .name("moderator")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = villagerAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("村民投票失败", e);
            return "弃票";
        }
    }

    private String extractText(Msg msg) {
        if (msg == null) {
            return "无响应";
        }
        return msg.getContent().stream()
                .filter(block -> block instanceof TextBlock)
                .map(block -> ((TextBlock) block).getText())
                .findFirst()
                .orElse("无法解析响应");
    }
}