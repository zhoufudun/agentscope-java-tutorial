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

/**
 * 狼人杀角色枚举
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
public enum Role {

    /**
     * 村民 - 无特殊能力，需要靠逻辑推理找出狼人
     */
    VILLAGER("村民", true),

    /**
     * 狼人 - 每晚可以击杀一名玩家
     */
    WEREWOLF("狼人", false),

    /**
     * 预言家 - 每晚可以查验一名玩家身份
     */
    SEER("预言家", true),

    /**
     * 女巫 - 拥有一瓶解药和一瓶毒药
     */
    WITCH("女巫", true),

    /**
     * 猎人 - 死亡时可以带走一名玩家
     */
    HUNTER("猎人", true);

    /** 显示名称 */
    private final String displayName;

    /** 是否属于好人阵营 */
    private final boolean villagerCamp;

    Role(String displayName, boolean villagerCamp) {
        this.displayName = displayName;
        this.villagerCamp = villagerCamp;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 是否属于好人阵营
     */
    public boolean isVillagerCamp() {
        return villagerCamp;
    }

    /**
     * 获取角色描述
     */
    public String getDescription() {
        return switch (this) {
            case VILLAGER -> "普通村民，没有特殊能力";
            case WEREWOLF -> "每晚可以击杀一名玩家";
            case SEER -> "每晚可以查验一名玩家的身份";
            case WITCH -> "拥有一瓶解药和一瓶毒药";
            case HUNTER -> "死亡时可以带走一名玩家";
        };
    }
}