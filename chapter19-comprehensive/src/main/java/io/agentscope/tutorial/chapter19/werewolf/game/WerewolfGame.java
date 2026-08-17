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
package io.agentscope.tutorial.chapter19.werewolf.game;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 狼人杀游戏主类
 *
 * <p>负责控制游戏流程，包括：
 * <ul>
 *   <li>初始化游戏和玩家</li>
 *   <li>执行夜晚阶段（狼人击杀、女巫救人/毒人、预言家查验）</li>
 *   <li>执行白天阶段（死讯公布、讨论、投票放逐）</li>
 *   <li>检查胜负条件</li>
 * </ul>
 *
 * <p>这是一个多 Agent 协作的典型应用，每个玩家角色都由一个独立的 ReActAgent 控制。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class WerewolfGame {

    private static final Logger logger = LoggerFactory.getLogger(WerewolfGame.class);

    /** 最大回合数 */
    private static final int MAX_ROUNDS = 15;

    /** 讨论轮数 */
    private static final int DISCUSSION_ROUNDS = 3;

    /** 玩家名称列表 */
    private static final List<String> PLAYER_NAMES = List.of(
            "小红", "小明", "小华", "小丽", "小强",
            "小芳", "小刚", "小美", "小亮"
    );

    /** 游戏状态 */
    private GameState gameState;

    /** 主持人 Agent */
    private final ReActAgent moderatorAgent;

    /** 玩家 Agent 工厂 */
    private final ReActAgent villagerAgent;
    private final ReActAgent werewolfAgent;

    /** 事件监听器 */
    private final List<GameEventListener> eventListeners;

    /**
     * 构造函数
     */
    public WerewolfGame(
            ReActAgent moderatorAgent,
            ReActAgent villagerAgent,
            ReActAgent werewolfAgent) {
        this.moderatorAgent = moderatorAgent;
        this.villagerAgent = villagerAgent;
        this.werewolfAgent = werewolfAgent;
        this.eventListeners = new ArrayList<>();
    }

    /**
     * 添加事件监听器
     */
    public void addEventListener(GameEventListener listener) {
        eventListeners.add(listener);
    }

    /**
     * 开始游戏
     */
    public String startGame() {
        logger.info("开始狼人杀游戏...");

        // 初始化游戏
        initializeGame();
        emitEvent(new GameEvent(GameEvent.Type.GAME_STARTED, "游戏开始"));

        StringBuilder result = new StringBuilder();
        result.append("===== 狼人杀游戏开始 =====\n\n");
        result.append("玩家分配：\n");
        for (Player player : gameState.getAlivePlayers()) {
            result.append(String.format("  %s: %s\n", player.getName(), player.getRoleDisplayName()));
        }
        result.append("\n");

        // 主游戏循环
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            gameState.nextRound();
            result.append(String.format("----- 第 %d 回合 -----\n\n", round));

            // 夜晚阶段
            result.append("【夜晚阶段】\n");
            String nightResult = nightPhase();
            result.append(nightResult).append("\n");

            if (checkGameEnd(result)) break;

            // 白天阶段
            result.append("【白天阶段】\n");
            String dayResult = dayPhase();
            result.append(dayResult).append("\n");

            if (checkGameEnd(result)) break;
        }

        // 宣布结果
        announceWinner(result);

        emitEvent(new GameEvent(GameEvent.Type.GAME_ENDED, result.toString()));
        return result.toString();
    }

    /**
     * 初始化游戏
     */
    private void initializeGame() {
        // 创建角色列表
        List<Role> roles = new ArrayList<>();
        roles.add(Role.VILLAGER);
        roles.add(Role.VILLAGER);
        roles.add(Role.VILLAGER);
        roles.add(Role.WEREWOLF);
        roles.add(Role.WEREWOLF);
        roles.add(Role.WEREWOLF);
        roles.add(Role.SEER);
        roles.add(Role.WITCH);
        roles.add(Role.HUNTER);

        // 打乱角色顺序
        Collections.shuffle(roles);

        // 创建玩家
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < roles.size(); i++) {
            String name = PLAYER_NAMES.get(i);
            Role role = roles.get(i);

            // 根据角色选择 Agent
            ReActAgent agent;
            if (role == Role.WEREWOLF) {
                agent = createWerewolfAgent(name, role);
            } else {
                agent = createVillagerAgent(name, role);
            }

            players.add(new Player(name, role, agent));
        }

        // 创建游戏状态
        gameState = new GameState(players);
        logger.info("游戏初始化完成，共 {} 名玩家", players.size());
    }

    /**
     * 创建狼人 Agent
     */
    private ReActAgent createWerewolfAgent(String name, Role role) {
        String sysPrompt = String.format("""
                你是狼人杀游戏中的%s'%s'。

                你的身份是狼人，目标是杀死所有好人取得胜利。

                狼人策略：
                1. 在讨论中伪装成村民，不要暴露狼人身份
                2. 引导投票放逐村民或其他好人
                3. 夜晚与其他狼人配合选择击杀目标
                4. 必要时牺牲队友保全自己

                发言要自然，符合村民的立场。
                """, role.getDisplayName(), name);

        return ReActAgent.builder()
                .name(name)
                .sysPrompt(sysPrompt)
                .model(villagerAgent.getModel())
                .toolkit(villagerAgent.getToolkit())
                .maxIters(5)
                .build();
    }

    /**
     * 创建村民 Agent
     */
    private ReActAgent createVillagerAgent(String name, Role role) {
        String sysPrompt = String.format("""
                你是狼人杀游戏中的%s'%s'。

                你的目标是找出并放逐所有狼人。

                角色技能：
                %s

                村民策略：
                1. 仔细聆听其他玩家的发言
                2. 观察玩家的发言内容和投票行为
                3. 分析谁可能是狼人
                4. 在讨论中分享你的分析和推理

                发言要积极，展现出想要找出狼人的态度。
                """, role.getDisplayName(), name, role.getDescription());

        return ReActAgent.builder()
                .name(name)
                .sysPrompt(sysPrompt)
                .model(villagerAgent.getModel())
                .toolkit(villagerAgent.getToolkit())
                .maxIters(5)
                .build();
    }

    /**
     * 夜晚阶段
     */
    private String nightPhase() {
        StringBuilder result = new StringBuilder();
        result.append("夜幕降临，所有玩家闭眼...\n");

        // 狼人击杀阶段
        Player victim = werewolfKillPhase();
        if (victim != null) {
            gameState.setLastNightVictim(victim);
            result.append(String.format("狼人击杀了 %s。\n", victim.getName()));
            emitEvent(new GameEvent(GameEvent.Type.NIGHT_KILL, victim.getName()));
        }

        // 女巫行动阶段
        Player witch = gameState.getWitch();
        if (witch != null && witch.isAlive()) {
            String witchResult = witchActionPhase(witch);
            result.append(witchResult);
        }

        // 预言家查验阶段
        Player seer = gameState.getSeer();
        if (seer != null && seer.isAlive()) {
            String seerResult = seerCheckPhase(seer);
            result.append(seerResult);
        }

        result.append("天亮了，请所有玩家睁眼。\n");

        // 处理夜晚死亡
        Player nightVictim = gameState.getLastNightVictim();
        if (nightVictim != null && !gameState.isLastVictimResurrected()) {
            nightVictim.kill();
            result.append(String.format("%s(%s) 在夜晚被杀害。\n",
                    nightVictim.getName(), nightVictim.getRoleDisplayName()));
            emitEvent(new GameEvent(GameEvent.Type.PLAYER_KILLED, nightVictim.getName()));
        }

        return result.toString();
    }

    /**
     * 狼人击杀阶段
     */
    private Player werewolfKillPhase() {
        List<Player> werewolves = gameState.getAliveWerewolves();
        if (werewolves.isEmpty()) return null;

        // 获取存活的非狼人玩家作为目标候选
        List<Player> candidates = gameState.getAlivePlayers().stream()
                .filter(p -> p.getRole() != Role.WEREWOLF)
                .toList();

        if (candidates.isEmpty()) return null;

        // 简单策略：随机选择目标（或使用 Agent 决策）
        // 这里简化处理，随机选择
        Random random = new Random();
        return candidates.get(random.nextInt(candidates.size()));
    }

    /**
     * 女巫行动阶段
     */
    private String witchActionPhase(Player witch) {
        StringBuilder result = new StringBuilder();

        Player victim = gameState.getLastNightVictim();
        if (victim != null && witch.isWitchHasHealPotion()) {
            // 女巫看到被击杀的玩家
            result.append(String.format("女巫看到今晚%s被狼人击杀。\n", victim.getName()));
            // 简化处理：50%概率救人
            Random random = new Random();
            if (random.nextBoolean()) {
                victim.resurrect();
                witch.useHealPotion();
                gameState.setLastVictimResurrected(true);
                result.append(String.format("%s 使用解药救活了 %s。\n",
                        witch.getName(), victim.getName()));
                emitEvent(new GameEvent(GameEvent.Type.WITCH_HEAL, victim.getName()));
            }
        }

        return result.toString();
    }

    /**
     * 预言家查验阶段
     */
    private String seerCheckPhase(Player seer) {
        // 获取候选玩家
        List<Player> candidates = gameState.getAlivePlayers().stream()
                .filter(p -> !p.equals(seer))
                .toList();

        if (candidates.isEmpty()) return "";

        // 随机选择查验目标
        Random random = new Random();
        Player target = candidates.get(random.nextInt(candidates.size()));

        String identity = target.getRole() == Role.WEREWOLF ? "狼人" : "好人";
        String result = String.format("%s 查验了 %s，结果是：%s。\n",
                seer.getName(), target.getName(), identity);

        emitEvent(new GameEvent(GameEvent.Type.SEER_CHECK,
                String.format("%s->%s(%s)", seer.getName(), target.getName(), identity)));

        return result;
    }

    /**
     * 白天阶段
     */
    private String dayPhase() {
        StringBuilder result = new StringBuilder();

        // 猎人开枪
        Player hunter = gameState.getHunter();
        if (hunter != null && !hunter.isAlive()) {
            String hunterResult = hunterShootPhase(hunter);
            result.append(hunterResult);
        }

        // 公布死讯
        Player lastVictim = gameState.getLastNightVictim();
        if (lastVictim != null && !lastVictim.isAlive()) {
            result.append(String.format("%s 遗言：", lastVictim.getName()));
            result.append("'大家要仔细分辨...' \n");
        } else {
            result.append("昨晚是平安夜，没有人死亡。\n");
        }

        // 讨论阶段（简化处理）
        result.append("\n【自由讨论】\n");
        result.append("玩家们开始自由讨论...\n");
        emitEvent(new GameEvent(GameEvent.Type.DAY_DISCUSSION, "讨论开始"));

        // 投票阶段
        Player votedOut = votingPhase();
        if (votedOut != null) {
            votedOut.kill();
            result.append(String.format("\n投票结果：%s(%s) 被投票出局。\n",
                    votedOut.getName(), votedOut.getRoleDisplayName()));
            emitEvent(new GameEvent(GameEvent.Type.PLAYER_VOTED_OUT, votedOut.getName()));

            // 猎人技能
            if (votedOut.getRole() == Role.HUNTER) {
                result.append(String.format("%s 带走了 %s！\n",
                        votedOut.getName(), gameState.getHunterShootTarget().getName()));
            }
        } else {
            result.append("\n投票结果：本次投票平局，没有人被放逐。\n");
        }

        result.append("\n").append(gameState.getSummary()).append("\n");

        return result.toString();
    }

    /**
     * 猎人开枪阶段
     */
    private String hunterShootPhase(Player hunter) {
        // 简化处理：随机选择开枪目标
        List<Player> candidates = gameState.getAlivePlayers();
        if (candidates.isEmpty()) return "";

        Random random = new Random();
        Player target = candidates.get(random.nextInt(candidates.size()));

        gameState.setHunterShootTarget(target);
        target.kill();

        String result = String.format("%s(猎人) 发动技能，带走了 %s(%s)。\n",
                hunter.getName(), target.getName(), target.getRoleDisplayName());
        emitEvent(new GameEvent(GameEvent.Type.HUNTER_SHOOT, target.getName()));

        return result;
    }

    /**
     * 投票阶段
     */
    private Player votingPhase() {
        List<Player> alivePlayers = gameState.getAlivePlayers();
        if (alivePlayers.size() <= 1) return null;

        // 统计投票
        Map<Player, Integer> voteCount = new HashMap<>();

        for (Player player : alivePlayers) {
            // 简化处理：随机投票
            List<Player> voteTargets = alivePlayers.stream()
                    .filter(p -> !p.equals(player))
                    .toList();

            Random random = new Random();
            Player voteFor = voteTargets.get(random.nextInt(voteTargets.size()));
            voteCount.merge(voteFor, 1, Integer::sum);
        }

        // 找出票数最多的玩家
        Player votedOut = voteCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // 检查是否平票
        if (votedOut != null) {
            long maxVotes = voteCount.get(votedOut);
            long tiedCount = voteCount.values().stream()
                    .filter(v -> v == maxVotes)
                    .count();

            if (tiedCount > 1) {
                return null; // 平票，无人出局
            }
        }

        return votedOut;
    }

    /**
     * 检查游戏是否结束
     */
    private boolean checkGameEnd(StringBuilder result) {
        if (gameState.checkVillagersWin()) {
            gameState.setGameEnded("好人");
            return true;
        }
        if (gameState.checkWerewolvesWin()) {
            gameState.setGameEnded("狼人");
            return true;
        }
        return false;
    }

    /**
     * 宣布游戏结果
     */
    private void announceWinner(StringBuilder result) {
        String winner = gameState.getWinner();
        if ("好人".equals(winner)) {
            result.append("\n===== 游戏结束 =====\n");
            result.append("好人阵营胜利！所有狼人已被放逐。\n");
            emitEvent(new GameEvent(GameEvent.Type.GAME_ENDED, "好人胜利"));
        } else if ("狼人".equals(winner)) {
            result.append("\n===== 游戏结束 =====\n");
            result.append("狼人阵营胜利！狼人数量已超过好人。\n");
            emitEvent(new GameEvent(GameEvent.Type.GAME_ENDED, "狼人胜利"));
        } else {
            result.append("\n===== 游戏结束 =====\n");
            result.append("游戏达到最大回合数，平局。\n");
            emitEvent(new GameEvent(GameEvent.Type.GAME_ENDED, "平局"));
        }

        result.append("\n【游戏日志】\n");
        for (String log : gameState.getGameLog()) {
            result.append(log).append("\n");
        }
    }

    /**
     * 发送游戏事件
     */
    private void emitEvent(GameEvent event) {
        for (GameEventListener listener : eventListeners) {
            listener.onGameEvent(event);
        }
    }

    // ==================== Inner Classes ====================

    /**
     * 游戏事件类
     */
    public static class GameEvent {
        public enum Type {
            GAME_STARTED,
            NIGHT_KILL,
            WITCH_HEAL,
            SEER_CHECK,
            DAY_DISCUSSION,
            PLAYER_KILLED,
            PLAYER_VOTED_OUT,
            HUNTER_SHOOT,
            GAME_ENDED
        }

        private final Type type;
        private final String message;

        public GameEvent(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public Type getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 游戏事件监听器接口
     */
    public interface GameEventListener {
        void onGameEvent(GameEvent event);
    }
}