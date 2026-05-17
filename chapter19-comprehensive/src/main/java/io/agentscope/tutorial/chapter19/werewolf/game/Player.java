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

/**
 * 玩家实体类
 *
 * <p>代表狼人杀游戏中的一个玩家，包含：
 * <ul>
 *   <li>玩家信息（名称）</li>
 *   <li>角色信息</li>
 *   <li>存活状态</li>
 *   <li>关联的 Agent</li>
 *   <li>女巫的药水状态</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public class Player {

    /** 玩家名称 */
    private final String name;

    /** 角色 */
    private final Role role;

    /** 是否存活 */
    private boolean alive;

    /** 女巫是否有解药 */
    private boolean witchHasHealPotion;

    /** 女巫是否有毒药 */
    private boolean witchHasPoisonPotion;

    /** 关联的 Agent */
    private final ReActAgent agent;

    /**
     * 构造函数
     *
     * @param name 玩家名称
     * @param role 角色
     * @param agent 关联的 Agent
     */
    public Player(String name, Role role, ReActAgent agent) {
        this.name = name;
        this.role = role;
        this.alive = true;
        this.witchHasHealPotion = (role == Role.WITCH);
        this.witchHasPoisonPotion = (role == Role.WITCH);
        this.agent = agent;
    }

    /**
     * 玩家死亡
     */
    public void kill() {
        this.alive = false;
    }

    /**
     * 玩家复活（如女巫救人）
     */
    public void resurrect() {
        this.alive = true;
    }

    /**
     * 女巫使用解药
     */
    public void useHealPotion() {
        this.witchHasHealPotion = false;
    }

    /**
     * 女巫使用毒药
     */
    public void usePoisonPotion() {
        this.witchHasPoisonPotion = false;
    }

    /**
     * 获取玩家名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取角色
     */
    public Role getRole() {
        return role;
    }

    /**
     * 是否存活
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * 女巫是否有解药
     */
    public boolean isWitchHasHealPotion() {
        return witchHasHealPotion;
    }

    /**
     * 女巫是否有毒药
     */
    public boolean isWitchHasPoisonPotion() {
        return witchHasPoisonPotion;
    }

    /**
     * 获取关联的 Agent
     */
    public ReActAgent getAgent() {
        return agent;
    }

    /**
     * 获取角色显示名称
     */
    public String getRoleDisplayName() {
        return role.getDisplayName();
    }

    @Override
    public String toString() {
        return String.format("%s(%s, %s)",
                name, role.getDisplayName(), alive ? "存活" : "死亡");
    }
}