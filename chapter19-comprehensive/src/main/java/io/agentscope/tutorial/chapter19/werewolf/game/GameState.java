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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 游戏状态管理类 - 狼人杀游戏的核心数据模型
 *
 * <p>负责：
 * <ul>
 *   <li>维护所有玩家状态（存活、死亡、技能使用情况）</li>
 *   <li>管理夜晚行动结果（击杀、救人、毒人）</li>
 *   <li>判断游戏胜负条件</li>
 *   <li>记录游戏日志</li>
 * </ul>
 *
 * <h2>状态管理设计</h2>
 * <p>GameState 采用集中式状态管理，所有游戏状态都在此类中维护：
 * <ul>
 *   <li><b>玩家列表</b>：allPlayers 包含所有玩家，通过 isAlive 判断存活</li>
 *   <li><b>回合管理</b>：currentRound 记录当前回合数</li>
 *   <li><b>夜间行动</b>：lastNightVictim 等字段记录夜间行动结果</li>
 *   <li><b>游戏结束</b>：gameEnded 和 winner 记录游戏结果</li>
 * </ul>
 *
 * <h2>胜负条件</h2>
 * <ul>
 *   <li><b>狼人胜利</b>：存活狼人数量 >= 存活好人数量</li>
 *   <li><b>村民胜利</b>：所有狼人被消灭</li>
 * </ul>
 *
 * <h2>与 WerewolfGame 的关系</h2>
 * <p>WerewolfGame 负责游戏流程控制，GameState 负责状态数据管理。
 * 这种分离使得状态可以独立测试，也便于实现游戏回放等功能。
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class GameState {

    /** 所有玩家列表 */
    private final List<Player> allPlayers;

    /** 当前回合 */
    private int currentRound;

    /** 昨夜被击杀玩家 */
    private Player lastNightVictim;

    /** 被毒杀玩家 */
    private Player lastPoisonedVictim;

    /** 昨夜被救活标记 */
    private boolean lastVictimResurrected;

    /** 猎人开枪目标 */
    private Player hunterShootTarget;

    /** 游戏是否结束 */
    private boolean gameEnded;

    /** 获胜阵营 */
    private String winner;

    /** 游戏日志 */
    private final List<String> gameLog;

    /**
     * 构造函数
     *
     * @param players 初始玩家列表
     */
    public GameState(List<Player> players) {
        this.allPlayers = new ArrayList<>(players);
        this.currentRound = 0;
        this.gameEnded = false;
        this.winner = null;
        this.gameLog = new ArrayList<>();
        log("游戏初始化，共 " + players.size() + " 名玩家");
    }

    /**
     * 获取存活的玩家列表
     */
    public List<Player> getAlivePlayers() {
        return allPlayers.stream()
                .filter(Player::isAlive)
                .collect(Collectors.toList());
    }

    /**
     * 获取存活的狼人列表
     */
    public List<Player> getAliveWerewolves() {
        return getAlivePlayers().stream()
                .filter(p -> p.getRole() == Role.WEREWOLF)
                .collect(Collectors.toList());
    }

    /**
     * 获取存活的村民阵营玩家（好人）
     */
    public List<Player> getAliveVillagers() {
        return getAlivePlayers().stream()
                .filter(p -> p.getRole().isVillagerCamp())
                .collect(Collectors.toList());
    }

    /**
     * 获取预言家
     */
    public Player getSeer() {
        return allPlayers.stream()
                .filter(p -> p.getRole() == Role.SEER)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取女巫
     */
    public Player getWitch() {
        return allPlayers.stream()
                .filter(p -> p.getRole() == Role.WITCH)
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取猎人
     */
    public Player getHunter() {
        return allPlayers.stream()
                .filter(p -> p.getRole() == Role.HUNTER)
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查狼人是否胜利
     *
     * <p><b>狼人胜利条件：</b>狼人数量 >= 村民数量
     *
     * <p>这个条件意味着狼人在夜晚可以击杀最后的好人，
     * 游戏没有继续下去的必要。
     *
     * @return 如果狼人胜利返回 true
     */
    public boolean checkWerewolvesWin() {
        int aliveWerewolves = getAliveWerewolves().size();
        int aliveVillagers = getAliveVillagers().size();
        return aliveWerewolves > 0 && aliveWerewolves >= aliveVillagers;
    }

    /**
     * 检查村民是否胜利
     *
     * <p><b>村民胜利条件：</b>所有狼人死亡
     *
     * <p>当最后一个狼人被投票出局或被猎人带走时，
     * 村民阵营（包括所有好人角色）获胜。
     *
     * @return 如果村民胜利返回 true
     */
    public boolean checkVillagersWin() {
        return getAliveWerewolves().isEmpty();
    }

    /**
     * 检查游戏是否结束
     */
    public boolean isGameEnded() {
        return gameEnded;
    }

    /**
     * 设置游戏结束
     */
    public void setGameEnded(String winner) {
        this.gameEnded = true;
        this.winner = winner;
        log("游戏结束！获胜阵营：" + winner);
    }

    /**
     * 获取获胜阵营
     */
    public String getWinner() {
        return winner;
    }

    /**
     * 根据玩家名称查找玩家
     */
    public Player findPlayerByName(String name) {
        return allPlayers.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 进入下一回合
     *
     * <p>每个新回合开始时：
     * <ul>
     *   <li>回合计数器加一</li>
     *   <li>清除上一晚的临时状态（被杀者、被毒者、复活标记）</li>
     *   <li>清除猎人开枪目标</li>
     *   <li>记录回合日志</li>
     * </ul>
     */
    public void nextRound() {
        currentRound++;
        lastNightVictim = null;
        lastPoisonedVictim = null;
        lastVictimResurrected = false;
        hunterShootTarget = null;
        log("===== 第 " + currentRound + " 回合 =====");
    }

    /**
     * 获取当前回合
     */
    public int getCurrentRound() {
        return currentRound;
    }

    /**
     * 设置昨夜被击杀玩家
     */
    public void setLastNightVictim(Player victim) {
        this.lastNightVictim = victim;
    }

    /**
     * 获取昨夜被击杀玩家
     */
    public Player getLastNightVictim() {
        return lastNightVictim;
    }

    /**
     * 设置被毒杀玩家
     */
    public void setLastPoisonedVictim(Player victim) {
        this.lastPoisonedVictim = victim;
    }

    /**
     * 获取被毒杀玩家
     */
    public Player getLastPoisonedVictim() {
        return lastPoisonedVictim;
    }

    /**
     * 设置被救活标记
     */
    public void setLastVictimResurrected(boolean resurrected) {
        this.lastVictimResurrected = resurrected;
    }

    /**
     * 是否被救活
     */
    public boolean isLastVictimResurrected() {
        return lastVictimResurrected;
    }

    /**
     * 设置猎人开枪目标
     */
    public void setHunterShootTarget(Player target) {
        this.hunterShootTarget = target;
    }

    /**
     * 获取猎人开枪目标
     */
    public Player getHunterShootTarget() {
        return hunterShootTarget;
    }

    /**
     * 添加游戏日志
     */
    public void log(String message) {
        gameLog.add("[" + currentRound + "] " + message);
    }

    /**
     * 获取游戏日志
     */
    public List<String> getGameLog() {
        return new ArrayList<>(gameLog);
    }

    /**
     * 获取游戏状态摘要
     */
    public String getSummary() {
        int aliveCount = getAlivePlayers().size();
        int werewolfCount = getAliveWerewolves().size();
        int villagerCount = getAliveVillagers().size();

        return String.format(
                "游戏状态：\n" +
                "- 当前回合：%d\n" +
                "- 存活玩家：%d人\n" +
                "- 狼人：%d人\n" +
                "- 好人：%d人\n" +
                "- 游戏状态：%s",
                currentRound, aliveCount, werewolfCount, villagerCount,
                gameEnded ? "已结束（" + winner + "获胜）" : "进行中"
        );
    }
}