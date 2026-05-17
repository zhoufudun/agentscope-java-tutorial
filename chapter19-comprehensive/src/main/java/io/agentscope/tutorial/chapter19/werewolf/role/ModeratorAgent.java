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
 * 狼人杀游戏主持人 Agent
 *
 * <p>负责控制游戏流程，包括：
 * <ul>
 *   <li>宣布游戏开始和角色分配</li>
 *   <li>主持夜晚阶段</li>
 *   <li>主持白天阶段</li>
 *   <li>宣布游戏结果</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@Component
public class ModeratorAgent {

    private static final Logger logger = LoggerFactory.getLogger(ModeratorAgent.class);

    private final ReActAgent moderatorAgent;

    public ModeratorAgent(
            @Qualifier("werewolfModeratorAgent") ReActAgent moderatorAgent) {
        this.moderatorAgent = moderatorAgent;
        logger.info("ModeratorAgent 初始化完成");
    }

    /**
     * 开始游戏介绍
     *
     * @param playerNames 玩家名称列表
     * @return 游戏开始介绍
     */
    public String startGameIntro(String playerNames) {
        logger.info("主持人开始游戏介绍");

        String prompt = String.format("""
                欢迎来到狼人杀游戏！

                本局游戏玩家：%s

                游戏配置：
                - 村民: 3人
                - 狼人: 3人
                - 预言家: 1人
                - 女巫: 1人
                - 猎人: 1人

                请发布游戏开始公告，宣布角色分配情况。
                """, playerNames);

        try {
            Msg msg = Msg.builder()
                    .name("system")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = moderatorAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("游戏开始介绍失败", e);
            return "游戏开始！";
        }
    }

    /**
     * 夜晚阶段主持
     *
     * @param phase 当前阶段（werewolf/witch/seer）
     * @return 主持内容
     */
    public String night主持(String phase) {
        logger.info("主持人夜晚主持: {}", phase);

        String prompt = switch (phase) {
            case "werewolf" -> """
                    【夜晚阶段 - 狼人行动】
                    请宣布："狼人请睁眼，请讨论并决定今晚要击杀的目标。"
                    """ ;
            case "witch" -> """
                    【夜晚阶段 - 女巫行动】
                    请宣布："女巫请睁眼，请决定是否使用药水。"
                    """;
            case "seer" -> """
                    【夜晚阶段 - 预言家行动】
                    请宣布："预言家请睁眼，请选择今晚要查验的玩家。"
                    """;
            default -> "请继续主持游戏。";
        };

        try {
            Msg msg = Msg.builder()
                    .name("system")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = moderatorAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("夜晚主持失败", e);
            return "请继续游戏。";
        }
    }

    /**
     * 白天阶段主持
     *
     * @param deathInfo 死亡信息
     * @return 主持内容
     */
    public String day主持(String deathInfo) {
        logger.info("主持人白天主持");

        String prompt = String.format("""
                【白天阶段】
                %s

                请宣布天亮，公布夜晚结果，并宣布进入自由讨论阶段。
                """, deathInfo);

        try {
            Msg msg = Msg.builder()
                    .name("system")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = moderatorAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("白天主持失败", e);
            return "请继续游戏。";
        }
    }

    /**
     * 投票阶段主持
     *
     * @param alivePlayers 存活玩家列表
     * @return 投票主持内容
     */
    public String voting主持(String alivePlayers) {
        logger.info("主持人投票主持");

        String prompt = String.format("""
                【投票阶段】
                存活玩家：%s

                请宣布投票开始，要求每位玩家选择要放逐的玩家。
                """, alivePlayers);

        try {
            Msg msg = Msg.builder()
                    .name("system")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = moderatorAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("投票主持失败", e);
            return "请开始投票。";
        }
    }

    /**
     * 游戏结束宣布
     *
     * @param winner 获胜阵营
     * @param gameLog 游戏日志
     * @return 结束宣布
     */
    public String endGame(String winner, String gameLog) {
        logger.info("主持人宣布游戏结束: {}", winner);

        String prompt = String.format("""
                【游戏结束】

                获胜阵营：%s

                游戏日志：
                %s

                请宣布游戏结果并做总结。
                """, winner, gameLog);

        try {
            Msg msg = Msg.builder()
                    .name("system")
                    .role(MsgRole.USER)
                    .content(TextBlock.builder().text(prompt).build())
                    .build();

            Msg response = moderatorAgent.call(msg).block();
            return extractText(response);
        } catch (Exception e) {
            logger.error("结束宣布失败", e);
            return "游戏结束！";
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