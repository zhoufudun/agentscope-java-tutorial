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
package io.agentscope.tutorial.chapter13;

import io.agentscope.tutorial.chapter13.hook.AuditHook;
import io.agentscope.tutorial.chapter13.hook.LoggingHook;
import io.agentscope.tutorial.chapter13.hook.MetricsHook;
import io.agentscope.tutorial.chapter13.hook.TimingHook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hook 系统测试类.
 *
 * <p>测试各种 Hook 的功能和优先级
 *
 * @author AgentScope Tutorial
 */
class HookTest {

    @Nested
    @DisplayName("LoggingHook 测试")
    class LoggingHookTest {

        private LoggingHook loggingHook;

        @BeforeEach
        void setUp() {
            loggingHook = new LoggingHook(false);
        }

        @Test
        @DisplayName("默认构造函数应不记录流式 chunks")
        void test默认构造函数() {
            LoggingHook hook = new LoggingHook();
            assertEquals(900, hook.priority());
        }

        @Test
        @DisplayName("应返回正确的优先级")
        void testPriority() {
            assertEquals(900, loggingHook.priority());
        }
    }

    @Nested
    @DisplayName("MetricsHook 测试")
    class MetricsHookTest {

        private MetricsHook metricsHook;

        @BeforeEach
        void setUp() {
            metricsHook = new MetricsHook(false);
            metricsHook.reset();
        }

        @Test
        @DisplayName("初始状态应为零")
        void testInitialState() {
            assertEquals(0, metricsHook.getTotalCallCount());
            assertEquals(0, metricsHook.getTotalCallTimeMs());
            assertEquals(0, metricsHook.getTotalErrorCount());
            assertEquals(0, metricsHook.getAverageCallTimeMs());
        }

        @Test
        @DisplayName("应返回正确的优先级")
        void testPriority() {
            assertEquals(950, metricsHook.priority());
        }

        @Test
        @DisplayName("重置后状态应为零")
        void testReset() {
            metricsHook.reset();
            assertEquals(0, metricsHook.getTotalCallCount());
            assertEquals(0, metricsHook.getTotalCallTimeMs());
        }

        @Test
        @DisplayName("应正确获取工具调用统计")
        void testGetToolCallStats() {
            assertNotNull(metricsHook.getToolCallStats());
            assertTrue(metricsHook.getToolCallStats().isEmpty());
        }

        @Test
        @DisplayName("toString 应返回有效字符串")
        void testToString() {
            String str = metricsHook.toString();
            assertNotNull(str);
            assertTrue(str.contains("MetricsHook"));
        }
    }

    @Nested
    @DisplayName("AuditHook 测试")
    class AuditHookTest {

        private AuditHook auditHook;

        @BeforeEach
        void setUp() {
            auditHook = new AuditHook(Set.of("delete_file", "send_email"));
            auditHook.clearRecords();
        }

        @Test
        @DisplayName("默认构造函数应正常工作")
        void test默认构造函数() {
            AuditHook hook = new AuditHook();
            assertEquals(60, hook.priority());
        }

        @Test
        @DisplayName("应返回正确的优先级")
        void testPriority() {
            assertEquals(60, auditHook.priority());
        }

        @Test
        @DisplayName("应正确获取审计记录")
        void testGetAuditRecords() {
            List<AuditHook.ToolAuditRecord> records = auditHook.getAuditRecords();
            assertNotNull(records);
            assertTrue(records.isEmpty());
        }

        @Test
        @DisplayName("应正确按工具名称过滤记录")
        void testGetAuditRecordsByTool() {
            List<AuditHook.ToolAuditRecord> records = auditHook.getAuditRecordsByTool("calculate");
            assertNotNull(records);
        }

        @Test
        @DisplayName("应正确获取敏感工具记录")
        void testGetSensitiveToolRecords() {
            List<AuditHook.ToolAuditRecord> records = auditHook.getSensitiveToolRecords();
            assertNotNull(records);
        }

        @Test
        @DisplayName("清空记录后应为空")
        void testClearRecords() {
            auditHook.clearRecords();
            assertEquals(0, auditHook.getRecordCount());
        }
    }

    @Nested
    @DisplayName("TimingHook 测试")
    class TimingHookTest {

        private TimingHook timingHook;

        @BeforeEach
        void setUp() {
            timingHook = new TimingHook(true);
        }

        @Test
        @DisplayName("默认构造函数应正常工作")
        void test默认构造函数() {
            TimingHook hook = new TimingHook();
            assertEquals(100, hook.priority());
        }

        @Test
        @DisplayName("应返回正确的优先级")
        void testPriority() {
            assertEquals(100, timingHook.priority());
        }

        @Test
        @DisplayName("初始迭代次数应为零")
        void testInitialIteration() {
            assertEquals(0, timingHook.getCurrentIteration());
        }
    }

    @Nested
    @DisplayName("钩子优先级测试")
    class PriorityTest {

        @Test
        @DisplayName("不同钩子应有不同的优先级")
        void testDifferentPriorities() {
            LoggingHook loggingHook = new LoggingHook();
            MetricsHook metricsHook = new MetricsHook();
            AuditHook auditHook = new AuditHook();
            TimingHook timingHook = new TimingHook();

            // 验证优先级排序
            assertTrue(auditHook.priority() < timingHook.priority());
            assertTrue(timingHook.priority() < loggingHook.priority());
            assertTrue(loggingHook.priority() < metricsHook.priority());
        }

        @Test
        @DisplayName("优先级值应符合设计规范")
        void testPriorityValues() {
            AuditHook auditHook = new AuditHook();
            TimingHook timingHook = new TimingHook();
            LoggingHook loggingHook = new LoggingHook();
            MetricsHook metricsHook = new MetricsHook();

            // 系统关键：0-50
            assertTrue(auditHook.priority() <= 100);

            // 业务逻辑：51-500
            assertTrue(timingHook.priority() <= 500);

            // 监控日志：501-1000
            assertTrue(loggingHook.priority() <= 1000);
            assertTrue(metricsHook.priority() <= 1000);
        }
    }
}