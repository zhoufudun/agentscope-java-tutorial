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
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or the License
 * for the specific language governing permissions and
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
 * 狼人角色 Agent
 *
 * <p>负责在狼人杀游戏中扮演狼人角色，需要伪装自己并配合其他狼人。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class WerewolfAgent {

    private static final Logger logger = LoggerFactory.getLogger(WerewolfAgent.class);

    private final ReActAgent werewolfAgent;

    public WerewolfAgent(
            @Qualifier("werewolfAgent") ReActAgent werewolfAgent) {
        this.werewolfAgent = werewolfAgent;
        logger.info("WerewolfAgent 初始化完成");
    }

    /**
     * 狼人夜晚讨论
     *
     * @param gameContext 游戏上下文
     * @return 讨论意见
     */
    public String nightDiscussion(String gameContext) {
        logger.info("狼人夜间讨论");

        String prompt = String.format("""
                %s

                【狼人夜间会议】
                你需要与其他狼人讨论今晚要击杀的目标。选择一个没有特殊身份的好人作为目标。
                发言要简短，明确表达你的意见。
                """, gameContext);

        try {
            Msg msg = Msg.builder()
                    .name("werewolf_team")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = werewolfAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("狼人夜间讨论失败", e);
            return "狼人暂时无法讨论";
        }
    }

    /**
     * 狼人白天发言（需要伪装成村民）
     *
     * @param gameContext 游戏上下文
     * @return 发言内容
     */
    public String daySpeech(String gameContext) {
        logger.info("狼人白天发言");

        String prompt = String.format("""
                %s

                【白天讨论阶段】
                请以村民的身份发言，假装不知道其他狼人的身份。
                发言要自然，不要暴露自己是狼人。尽量引导投票给其他好人。
                """, gameContext);

        try {
            Msg msg = Msg.builder()
                    .name("moderator")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = werewolfAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("狼人白天发言失败", e);
            return "狼人暂时无法发言";
        }
    }

    /**
     * 狼人投票
     *
     * @param candidates 候选玩家列表
     * @return 投票目标
     */
    public String vote(String candidates) {
        logger.info("狼人投票");

        String prompt = String.format("""
                请选择你要投票放逐的玩家（作为狼人，你应该引导投票给好人）。

                候选玩家：
                %s

                选择一个好人作为投票目标。只回复玩家名称即可。
                """, candidates);

        try {
            Msg msg = Msg.builder()
                    .name("moderator")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = werewolfAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("狼人投票失败", e);
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