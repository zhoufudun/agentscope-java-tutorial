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
 * 狼人杀游戏主类 - 多 Agent 博弈的核心控制器
 *
 * <p>负责控制游戏流程，包括：
 * <ul>
 *   <li>初始化游戏和玩家</li>
 *   <li>执行夜晚阶段（狼人击杀、女巫救人/毒人、预言家查验）</li>
 *   <li>执行白天阶段（死讯公布、讨论、投票放逐）</li>
 *   <li>检查胜负条件</li>
 * </ul>
 *
 * <h2>多 Agent 博弈设计</h2>
 * <p>这是一个多 Agent 协作的典型应用，每个玩家角色都由一个独立的 ReActAgent 控制。
 * 与奶茶店案例的协作模式不同，狼人杀展示了<b>对抗性多 Agent 系统</b>：
 * <ul>
 *   <li><b>信息不对称</b>：狼人知道彼此身份，村民不知道</li>
 *   <li><b>目标对立</b>：狼人阵营 vs 村民阵营</li>
 *   <li><b>策略博弈</b>：欺骗、推理、投票策略</li>
 *   <li><b>动态淘汰</b>：玩家死亡后退出游戏</li>
 * </ul>
 *
 * <h2>游戏流程控制</h2>
 * <pre>
 * 初始化 -> 夜晚阶段 -> 检查胜负 -> 白天阶段 -> 检查胜负 -> 循环...
 *              |                        |
 *         狼人击杀                   公布死讯
 *         女巫行动                   自由讨论
 *         预言家查验                 投票放逐
 * </pre>
 *
 * <h2>事件驱动架构</h2>
 * <p>通过 GameEventListener 支持实时游戏事件推送，便于：
 * <ul>
 *   <li>前端 SSE 实时更新</li>
 *   <li>游戏日志记录</li>
 *   <li>状态监控和调试</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class WerewolfGame {

    private static final Logger logger = LoggerFactory.getLogger(WerewolfGame.class);

    /** 最大回合数 - 防止游戏无限循环 */
    private static final int MAX_ROUNDS = 15;

    /** 讨论轮数 - 白天讨论阶段的轮数 */
    private static final int DISCUSSION_ROUNDS = 3;

    /** 玩家名称列表 - 用于初始化玩家 */
    private static final List<String> PLAYER_NAMES = List.of(
            "小红", "小明", "小华", "小丽", "小强",
            "小芳", "小刚", "小美", "小亮"
    );

    /** 游戏状态 - 维护玩家、回合、死亡等信息 */
    private GameState gameState;

    /** 主持人 Agent - 控制游戏流程，不参与博弈 */
    private final ReActAgent moderatorAgent;

    /** 村民 Agent 模板 - 用于创建好人阵营的 Agent */
    private final ReActAgent villagerAgent;

    /** 狼人 Agent 模板 - 用于创建狼人阵营的 Agent */
    private final ReActAgent werewolfAgent;

    /** 事件监听器列表 - 支持多个监听器订阅游戏事件 */
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
     *
     * <p>游戏初始化过程：
     * <ol>
     *   <li>创建角色池（3村民 + 3狼人 + 预言家 + 女巫 + 猎人）</li>
     *   <li>随机打乱角色顺序，实现随机分配</li>
     *   <li>为每个玩家创建对应的 Agent（根据角色选择不同的提示词模板）</li>
     *   <li>初始化游戏状态</li>
     * </ol>
     *
     * <p><b>Agent 动态创建：</b>每个玩家都有独立的 Agent，
     * 系统提示词包含该玩家的角色信息和行为策略指导。
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
     *
     * <p><b>狼人 Agent 的系统提示词设计要点：</b>
     * <ul>
     *   <li>明确告知其狼人身份和目标</li>
     *   <li>强调伪装和欺骗策略</li>
     *   <li>指导夜间配合击杀和白天发言技巧</li>
     * </ul>
     *
     * @param name 玩家名称
     * @param role 角色类型
     * @return 配置好的狼人 Agent
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
     * 创建村民阵营 Agent
     *
     * <p><b>村民阵营 Agent 的系统提示词设计要点：</b>
     * <ul>
     *   <li>明确告知角色身份和特殊技能（如有）</li>
     *   <li>强调推理和观察的重要性</li>
     *   <li>指导如何在讨论中分享观点</li>
     * </ul>
     *
     * <p>村民阵营包括：普通村民、预言家、女巫、猎人，
     * 每个角色有不同的技能描述，但共享相同的阵营目标。
     *
     * @param name 玩家名称
     * @param role 角色类型
     * @return 配置好的村民阵营 Agent
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
     * 夜晚阶段 - 狼人杀游戏的核心阶段之一
     *
     * <p><b>夜晚阶段流程：</b>
     * <ol>
     *   <li><b>狼人击杀</b>：狼人选择击杀目标（多个狼人需要达成共识）</li>
     *   <li><b>女巫行动</b>：女巫决定是否使用解药救人或毒药毒人</li>
     *   <li><b>预言家查验</b>：预言家选择一名玩家查看其身份</li>
     * </ol>
     *
     * <p><b>多 Agent 协调：</b>
     * <ul>
     *   <li>狼人之间需要信息同步和决策协调</li>
     *   <li>女巫需要知道被击杀者才能决定是否救人</li>
     *   <li>预言家独立行动，获取信息用于白天讨论</li>
     * </ul>
     *
     * @return 夜晚阶段的执行结果描述
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
     * 白天阶段 - 狼人杀游戏的核心阶段之一
     *
     * <p><b>白天阶段流程：</b>
     * <ol>
     *   <li><b>死讯公布</b>：主持人宣布昨夜死亡玩家（被击杀或被毒）</li>
     *   <li><b>猎人技能</b>：如果猎人死亡，可选择带走一名玩家</li>
     *   <li><b>遗言</b>：死亡玩家发表遗言</li>
     *   <li><b>自由讨论</b>：存活玩家发言，分析和推理</li>
     *   <li><b>投票放逐</b>：投票选出最可疑的玩家</li>
     * </ol>
     *
     * <p><b>博弈要点：</b>
     * <ul>
     *   <li>村民通过发言内容判断狼人</li>
     *   <li>狼人需要伪装，引导投票方向</li>
     *   <li>特殊角色（如预言家）需要决定是否暴露信息</li>
     * </ul>
     *
     * @return 白天阶段的执行结果描述
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
     * 投票阶段 - 民主决策机制
     *
     * <p><b>投票机制：</b>
     * <ul>
     *   <li>每个存活玩家投一票给其他玩家</li>
     *   <li>票数最多的玩家被放逐出局</li>
     *   <li>平票时无人出局</li>
     * </ul>
     *
     * <p><b>策略考量：</b>
     * <ul>
     *   <li>狼人会尽量分散票数或集中投给好人</li>
     *   <li>预言家的信息在此阶段发挥关键作用</li>
     *   <li>观察投票行为可以推断玩家身份</li>
     * </ul>
     *
     * @return 被投票出局的玩家，如果平票则返回 null
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

    // ==================== 内部类定义 ====================

    /**
     * 游戏事件类 - 用于游戏过程中的状态变化通知
     *
     * <p>事件驱动设计使得游戏引擎与 UI/日志等组件解耦。
     * 支持的事件类型包括游戏开始、夜间击杀、女巫救人、投票出局等。
     */
    public static class GameEvent {
        /**
         * 事件类型枚举
         *
         * <p>定义了狼人杀游戏中所有关键事件：
         * <ul>
         *   <li>GAME_STARTED - 游戏开始</li>
         *   <li>NIGHT_KILL - 狼人夜间击杀</li>
         *   <li>WITCH_HEAL - 女巫使用解药</li>
         *   <li>SEER_CHECK - 预言家查验身份</li>
         *   <li>DAY_DISCUSSION - 白天讨论开始</li>
         *   <li>PLAYER_KILLED - 玩家被杀死</li>
         *   <li>PLAYER_VOTED_OUT - 玩家被投票出局</li>
         *   <li>HUNTER_SHOOT - 猎人发动技能</li>
         *   <li>GAME_ENDED - 游戏结束</li>
         * </ul>
         */
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
     *
     * <p>实现此接口以订阅游戏事件，典型用途：
     * <ul>
     *   <li>SSE 推送：将事件实时推送到前端</li>
     *   <li>日志记录：记录游戏详细过程</li>
     *   <li>AI 分析：收集数据用于策略分析</li>
     * </ul>
     */
    public interface GameEventListener {
        /**
         * 处理游戏事件
         *
         * @param event 游戏事件对象，包含事件类型和描述消息
         */
        void onGameEvent(GameEvent event);
    }
}