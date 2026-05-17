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
 * 游戏状态管理类
 *
 * <p>负责：
 * <ul>
 *   <li>维护所有玩家状态</li>
 *   <li>管理夜晚行动结果</li>
 *   <li>判断游戏胜负</li>
 * </ul>
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
     * 条件：狼人数量 >= 村民数量
     */
    public boolean checkWerewolvesWin() {
        int aliveWerewolves = getAliveWerewolves().size();
        int aliveVillagers = getAliveVillagers().size();
        return aliveWerewolves > 0 && aliveWerewolves >= aliveVillagers;
    }

    /**
     * 检查村民是否胜利
     * 条件：狼人全部死亡
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