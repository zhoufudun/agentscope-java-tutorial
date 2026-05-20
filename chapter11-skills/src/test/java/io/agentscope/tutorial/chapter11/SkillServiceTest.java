package io.agentscope.tutorial.chapter11;

import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.skill.util.SkillUtil;
import io.agentscope.tutorial.chapter11.entity.SkillDefinition;
import io.agentscope.tutorial.chapter11.repository.SkillRepository;
import io.agentscope.tutorial.chapter11.service.SkillService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 技能服务测试
 *
 * <p>测试技能系统的核心功能：
 * <ul>
 *   <li>技能注册和查询</li>
 *   <li>技能激活和停用</li>
 *   <li>技能搜索和过滤</li>
 *   <li>SkillBox 集成</li>
 *   <li>技能版本管理</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("技能服务测试")
class SkillServiceTest {

    /**
     * 技能服务
     */
    @Autowired
    private SkillService skillService;

    /**
     * 技能仓库
     */
    @Autowired
    private SkillRepository skillRepository;

    /**
     * SkillBox
     */
    @Autowired
    private SkillBox skillBox;

    /**
     * 测试用的技能内容
     */
    private static final String TEST_SKILL_MD = """
            ---
            name: test-skill
            description: 测试技能 - 用于单元测试
            version: 1.0.0
            author: AgentScope Tutorial
            tags:
              - test
              - demo
            ---
            # 测试技能

            这是一个用于测试的技能。

            ## 使用方法

            1. 准备数据
            2. 执行分析
            3. 返回结果
            """;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        skillRepository.deleteAll();
    }

    // ==================== 技能注册测试 ====================

    @Test
    @DisplayName("测试从内容注册技能")
    void testRegisterSkillFromContent() {
        // 注册技能
        SkillDefinition skill = skillService.registerSkillFromContent(
                TEST_SKILL_MD,
                Map.of("scripts/test.py", "print('hello')"),
                "test"
        );

        assertNotNull(skill);
        assertNotNull(skill.getId());
        assertEquals("test-skill", skill.getName());
        assertEquals("测试技能 - 用于单元测试", skill.getDescription());
        assertEquals("1.0.0", skill.getVersion());
        assertTrue(skill.isActivated());
    }

    @Test
    @DisplayName("测试注册 AgentSkill")
    void testRegisterAgentSkill() {
        AgentSkill agentSkill = new AgentSkill(
                "demo-skill",
                "演示技能",
                "# Demo Skill\n\n这是一个演示技能。",
                Map.of(),
                "test"
        );

        SkillDefinition skill = skillService.registerSkill(agentSkill);

        assertNotNull(skill);
        assertEquals("demo-skill", skill.getName());
        assertEquals("test", skill.getSource());
        assertTrue(skillService.getAllSkillIds().contains("demo-skill_test"));
    }

    @Test
    @DisplayName("测试更新已存在的技能")
    void testUpdateExistingSkill() {
        // 首次注册
        SkillDefinition first = skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        assertEquals("1.0.0", first.getVersion());

        // 再次注册（更新）
        String updatedContent = TEST_SKILL_MD.replace("version: 1.0.0", "version: 2.0.0");
        SkillDefinition second = skillService.registerSkillFromContent(updatedContent, null, "test");

        // 验证版本递增
        assertEquals("test-skill", second.getName());
        assertEquals("2.0.1", second.getVersion()); // 2.0.0 -> 2.0.1
    }

    @Test
    @DisplayName("测试批量注册技能")
    void testBatchRegisterSkills() {
        AgentSkill skill1 = new AgentSkill("batch-1", "批量技能1", "内容1", null, "test");
        AgentSkill skill2 = new AgentSkill("batch-2", "批量技能2", "内容2", null, "test");

        List<SkillDefinition> results = skillService.registerSkills(List.of(skill1, skill2));

        assertEquals(2, results.size());
        assertEquals(2, skillService.getAllSkills().size());
    }

    // ==================== 技能查询测试 ====================

    @Test
    @DisplayName("测试获取所有技能")
    void testGetAllSkills() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "another-skill"), null, "test");

        List<SkillDefinition> skills = skillService.getAllSkills();
        assertEquals(2, skills.size());
    }

    @Test
    @DisplayName("测试获取已激活技能")
    void testGetActiveSkills() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        String anotherId = "another-skill_test";
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "another-skill"), null, "test");
        skillService.deactivateSkill(anotherId);

        List<SkillDefinition> activeSkills = skillService.getActiveSkills();
        assertEquals(1, activeSkills.size());
        assertEquals("test-skill", activeSkills.get(0).getName());
    }

    @Test
    @DisplayName("测试根据 ID 获取技能")
    void testGetSkillById() {
        SkillDefinition skill = skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");

        Optional<SkillDefinition> found = skillService.getSkillById(skill.getId());
        assertTrue(found.isPresent());
        assertEquals("test-skill", found.get().getName());
    }

    @Test
    @DisplayName("测试根据 skillId 获取技能")
    void testGetSkillBySkillId() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");

        Optional<SkillDefinition> found = skillService.getSkillBySkillId("test-skill_test");
        assertTrue(found.isPresent());
        assertEquals("test-skill", found.get().getName());
    }

    // ==================== 技能搜索测试 ====================

    @Test
    @DisplayName("测试搜索技能")
    void testSearchSkills() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.registerSkillFromContent(
                TEST_SKILL_MD.replace("test-skill", "data-analysis-skill")
                        .replace("测试技能", "数据分析技能"),
                null, "test"
        );

        // 搜索关键字
        List<SkillDefinition> results = skillService.searchSkills("测试");
        assertEquals(1, results.size());
        assertEquals("test-skill", results.get(0).getName());

        // 搜索标签
        results = skillService.searchSkills("demo");
        assertEquals(1, results.size());

        // 空关键字返回所有
        results = skillService.searchSkills("");
        assertEquals(2, results.size());
    }

    @Test
    @DisplayName("测试根据标签获取技能")
    void testGetSkillsByTag() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");

        List<SkillDefinition> results = skillService.getSkillsByTag("test");
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(s -> s.getName().equals("test-skill")));
    }

    @Test
    @DisplayName("测试根据来源获取技能")
    void testGetSkillsBySource() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test-source");
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "another-skill"), null, "test-source");

        List<SkillDefinition> results = skillService.getSkillsBySource("test-source");
        assertEquals(2, results.size());
    }

    // ==================== 技能激活测试 ====================

    @Test
    @DisplayName("测试激活技能")
    void testActivateSkill() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        String skillId = "test-skill_test";

        // 初始应该已激活
        assertTrue(skillService.isSkillActive(skillId));

        // 停用
        skillService.deactivateSkill(skillId);
        assertFalse(skillService.isSkillActive(skillId));

        // 激活
        SkillDefinition activated = skillService.activateSkill(skillId);
        assertTrue(activated.isActivated());
        assertTrue(skillService.isSkillActive(skillId));
    }

    @Test
    @DisplayName("测试停用技能")
    void testDeactivateSkill() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        String skillId = "test-skill_test";

        SkillDefinition deactivated = skillService.deactivateSkill(skillId);
        assertFalse(deactivated.isActivated());
        assertFalse(skillService.isSkillActive(skillId));
    }

    @Test
    @DisplayName("测试批量激活技能")
    void testBatchActivateSkills() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "skill-2"), null, "test");

        // 停用所有
        skillService.deactivateSkills(List.of("test-skill_test", "skill-2_test"));

        // 批量激活
        skillService.activateSkills(List.of("test-skill_test"));

        assertTrue(skillService.isSkillActive("test-skill_test"));
        assertFalse(skillService.isSkillActive("skill-2_test"));
    }

    // ==================== 技能删除测试 ====================

    @Test
    @DisplayName("测试删除技能")
    void testDeleteSkill() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        String skillId = "test-skill_test";

        assertTrue(skillService.getSkillBySkillId(skillId).isPresent());

        skillService.deleteSkill(skillId);

        assertFalse(skillService.getSkillBySkillId(skillId).isPresent());
        assertFalse(skillService.getAllSkillIds().contains(skillId));
    }

    @Test
    @DisplayName("测试删除不存在的技能抛出异常")
    void testDeleteNonExistentSkill() {
        assertThrows(IllegalArgumentException.class, () -> {
            skillService.deleteSkill("non-existent-skill_test");
        });
    }

    // ==================== SkillBox 集成测试 ====================

    @Test
    @DisplayName("测试获取技能提示词")
    void testGetSkillPrompt() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");

        String prompt = skillService.getSkillPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("test-skill") || prompt.contains("技能"));
    }

    @Test
    @DisplayName("测试获取所有注册的技能 ID")
    void testGetAllSkillIds() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "skill-2"), null, "test");

        Set<String> skillIds = skillService.getAllSkillIds();
        assertEquals(2, skillIds.size());
        assertTrue(skillIds.contains("test-skill_test"));
        assertTrue(skillIds.contains("skill-2_test"));
    }

    @Test
    @DisplayName("测试检查技能是否存在")
    void testSkillExists() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");

        assertTrue(skillService.getSkillBySkillId("test-skill_test").isPresent());
        assertFalse(skillService.getSkillBySkillId("non-existent_test").isPresent());
    }

    // ==================== 统计测试 ====================

    @Test
    @DisplayName("测试获取技能统计")
    void testGetStats() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.registerSkillFromContent(TEST_SKILL_MD.replace("test-skill", "skill-2"), null, "test");

        // 停用一个
        skillService.deactivateSkill("skill-2_test");

        SkillService.SkillStats stats = skillService.getStats();
        assertEquals(2, stats.getTotalSkills());
        assertEquals(1, stats.getActiveSkills());
        assertEquals(1, stats.getInactiveSkills());
        assertFalse(stats.getTotalSkillIds().isEmpty());
    }

    // ==================== 辅助方法测试 ====================

    @Test
    @DisplayName("测试重置所有技能状态")
    void testResetAllSkillStates() {
        skillService.registerSkillFromContent(TEST_SKILL_MD, null, "test");
        skillService.activateSkill("test-skill_test");

        // 重置
        skillService.resetAllSkillStates();

        // 验证状态已重置
        assertFalse(skillService.isSkillActive("test-skill_test"));
    }

    @Test
    @DisplayName("测试从 SKILL.md 创建 AgentSkill")
    void testCreateAgentSkillFromContent() {
        Map<String, String> resources = Map.of(
                "scripts/analyze.py", "import pandas as pd",
                "config/settings.json", "{\"threshold\": 0.5}"
        );

        AgentSkill skill = SkillUtil.createFrom(TEST_SKILL_MD, resources, "test");

        assertEquals("test-skill", skill.getName());
        assertEquals("测试技能 - 用于单元测试", skill.getDescription());
        assertFalse(skill.getResources().isEmpty());
        assertEquals(2, skill.getResources().size());
    }

    @Test
    @DisplayName("测试 SkillDefinition 与 AgentSkill 互相转换")
    void testEntityConversion() {
        // 创建 AgentSkill
        AgentSkill agentSkill = new AgentSkill(
                "convert-skill",
                "转换测试技能",
                "# Convert Test\n\nTesting conversion.",
                Map.of("test.txt", "content"),
                "test"
        );

        // 转换为实体
        SkillDefinition entity = SkillDefinition.fromAgentSkill(agentSkill);
        assertEquals("convert-skill", entity.getName());
        assertEquals("转换测试技能", entity.getDescription());

        // 转换回 AgentSkill
        AgentSkill converted = entity.toAgentSkill(entity.getResources());
        assertEquals("convert-skill", converted.getName());
        assertEquals("转换测试技能", converted.getDescription());
    }
}