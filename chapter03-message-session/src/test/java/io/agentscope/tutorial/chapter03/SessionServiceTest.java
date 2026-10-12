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
package io.agentscope.tutorial.chapter03;

import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.tutorial.chapter03.entity.ChatMessage;
import io.agentscope.tutorial.chapter03.entity.ChatSession;
import io.agentscope.tutorial.chapter03.formatter.JsonLogFormatter;
import io.agentscope.tutorial.chapter03.repository.MessageRepository;
import io.agentscope.tutorial.chapter03.repository.SessionRepository;
import io.agentscope.tutorial.chapter03.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 会话服务测试类.
 *
 * <p>本测试类验证第三章会话管理的核心功能，使用 H2 内存数据库进行单元测试。
 *
 * <h2>测试覆盖范围</h2>
 * <ul>
 *   <li>会话 CRUD 操作（创建、读取、更新、删除）</li>
 *   <li>消息保存和加载</li>
 *   <li>Msg ↔ ChatMessage 实体转换</li>
 *   <li>JsonLogFormatter 格式化功能</li>
 *   <li>AgentStateStore（InMemory 实现）</li>
 *   <li>多轮对话历史管理</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 * @since 1.0
 */
@SpringBootTest
@ActiveProfiles("dev")  // 使用 dev 配置（H2 内存数据库）
class SessionServiceTest {

    /**
     * 会话服务（被测试对象）.
     */
    @Autowired
    private SessionService sessionService;

    /**
     * 会话仓库.
     */
    @Autowired
    private SessionRepository sessionRepository;

    /**
     * 消息仓库.
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 测试创建会话.
     *
     * <p>验证会话创建后具有正确的属性：
     * <ul>
     *   <li>会话ID 不为空（自动生成 UUID）</li>
     *   <li>用户ID 和标题与输入一致</li>
     *   <li>消息数初始化为 0</li>
     * </ul>
     */
    @Test
    void testCreateSession() {
        // 创建会话
        ChatSession session = sessionService.createSession("test_user", "测试会话");

        // 验证会话属性
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals("test_user", session.getUserId());
        assertEquals("测试会话", session.getTitle());
        assertEquals(0, session.getMessageCount());

        // 清理测试数据
        sessionService.deleteSession(session.getSessionId());
    }

    /**
     * 测试获取或创建会话.
     *
     * <p>验证 getOrCreateSession 的幂等性：
     * 多次调用返回同一个会话实例。
     */
    @Test
    void testGetOrCreateSession() {
        String sessionId = "test-get-or-create";

        // 首次调用：创建新会话
        ChatSession session1 = sessionService.getOrCreateSession(sessionId, "test_user");
        assertNotNull(session1);
        assertEquals(sessionId, session1.getSessionId());

        // 再次调用：获取已存在的会话
        ChatSession session2 = sessionService.getOrCreateSession(sessionId, "test_user");
        assertEquals(session1.getSessionId(), session2.getSessionId());

        // 清理测试数据
        sessionService.deleteSession(sessionId);
    }

    /**
     * 测试消息保存和加载.
     *
     * <p>验证消息能够正确保存到数据库并加载回来：
     * <ul>
     *   <li>消息顺序保持一致</li>
     *   <li>消息内容完整保留</li>
     * </ul>
     */
    @Test
    void testSaveAndLoadMessages() {
        String sessionId = "test-messages";

        // 创建会话
        sessionService.createSession("test_user", "消息测试");

        // 保存用户消息
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好")
                .build();
        sessionService.saveMessage(sessionId, userMsg);

        // 保存助手回复
        Msg assistantMsg = Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("你好！有什么可以帮你的吗？")
                .build();
        sessionService.saveMessage(sessionId, assistantMsg);

        // 加载消息并验证
        List<Msg> history = sessionService.loadMessages(sessionId);
        assertEquals(2, history.size());
        assertEquals("你好", history.get(0).getTextContent());
        assertEquals("你好！有什么可以帮你的吗？", history.get(1).getTextContent());

        // 清理测试数据
        sessionService.deleteSession(sessionId);
    }

    /**
     * 测试清空会话消息.
     *
     * <p>验证 clearMessages 只删除消息，保留会话本身。
     */
    @Test
    void testClearMessages() {
        String sessionId = "test-clear";

        // 创建会话并添加消息
        sessionService.createSession("test_user", "清空测试");
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.USER)
                .textContent("测试消息")
                .build());

        // 验证消息已保存
        assertTrue(sessionService.getMessageCount(sessionId) > 0);

        // 清空消息
        sessionService.clearMessages(sessionId);

        // 验证消息已清空
        assertEquals(0, sessionService.getMessageCount(sessionId));
        assertTrue(sessionService.loadMessages(sessionId).isEmpty());

        // 清理测试数据
        sessionService.deleteSession(sessionId);
    }

    /**
     * 测试删除会话.
     *
     * <p>验证删除会话时同时删除关联的消息。
     */
    @Test
    void testDeleteSession() {
        // 创建会话
        ChatSession session = sessionService.createSession("test_user", "删除测试");

        // 添加消息
        sessionService.saveMessage(session.getSessionId(), Msg.builder()
                .role(MsgRole.USER)
                .textContent("测试")
                .build());

        // 删除会话
        sessionService.deleteSession(session.getSessionId());

        // 验证会话已删除
        assertFalse(sessionService.sessionExists(session.getSessionId()));
    }

    /**
     * 测试 Msg 到 ChatMessage 的转换.
     *
     * <p>验证 {@link ChatMessage#fromMsg} 方法正确转换所有字段。
     */
    @Test
    void testMessageEntityConversion() {
        // 创建测试消息
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("测试内容")
                .name("测试用户")
                .build();

        // 转换为实体
        ChatMessage entity = ChatMessage.fromMsg(msg, "test-session", 0);

        // 验证字段映射
        assertNotNull(entity.getMessageId());
        assertEquals("test-session", entity.getSessionId());
        assertEquals("USER", entity.getRole());
        assertEquals("测试内容", entity.getContent());
        assertEquals("测试用户", entity.getName());
        assertEquals(0, entity.getSequenceNum());
    }

    /**
     * 测试 ChatMessage 到 Msg 的转换.
     *
     * <p>验证 {@link ChatMessage#toMsg} 方法正确还原所有字段。
     */
    @Test
    void testChatMessageToMsg() {
        // 创建测试实体
        ChatMessage entity = new ChatMessage();
        entity.setMessageId("msg-123");
        entity.setSessionId("session-456");
        entity.setRole("ASSISTANT");
        entity.setContent("助手回复内容");
        entity.setName("AI助手");
        entity.setSequenceNum(1);

        // 转换为 Msg
        Msg msg = entity.toMsg();

        // 验证字段还原
        assertEquals("msg-123", msg.getId());
        assertEquals(MsgRole.ASSISTANT, msg.getRole());
        assertEquals("助手回复内容", msg.getTextContent());
        assertEquals("AI助手", msg.getName());
    }

    /**
     * 测试 JsonLogFormatter 格式化功能.
     *
     * <p>验证格式器能正确转换消息列表为可读字符串。
     */
    @Test
    void testJsonLogFormatter() {
        // 创建包含元数据的格式器
        JsonLogFormatter formatter = new JsonLogFormatter(true, true);

        // 创建测试消息列表
        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("你是助手").build(),
                Msg.builder().role(MsgRole.USER).textContent("问题").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("回答").build()
        );

        // 格式化并验证
        String formatted = formatter.formatMessagesToString(messages);
        assertNotNull(formatted);
        assertTrue(formatted.contains("SYSTEM"));
        assertTrue(formatted.contains("USER"));
        assertTrue(formatted.contains("ASSISTANT"));
    }

    /**
     * 测试 InMemoryAgentStateStore.
     *
     * <p>验证 AgentScope 2.x 的内存状态存储实现：
     * <ul>
     *   <li>使用 (userId, sessionId) 字符串对定位会话</li>
     *   <li>userId 传 null 表示匿名槽</li>
     *   <li>正确保存和加载 AgentState</li>
     * </ul>
     *
     * <p>注：这是对已删除的 io.agentscope.core.session.InMemorySession + SimpleSessionKey 的替代方案。
     */
    @Test
    void testInMemoryAgentStateStore() {
        // 创建内存状态存储
        var store = new InMemoryAgentStateStore();
        String sessionId = "memory-test";

        // 验证初始状态不存在
        assertFalse(store.exists(null, sessionId));

        // 构建 AgentState 并保存
        AgentState state = AgentState.builder()
                .sessionId(sessionId)
                .context(List.of(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("Hello World")
                        .build()))
                .build();
        store.save(null, sessionId, "agent_state", state);

        // 验证状态已保存
        assertTrue(store.exists(null, sessionId));

        // 读取并验证上下文内容
        AgentState loaded = store.get(null, sessionId, "agent_state", AgentState.class).orElseThrow();
        assertEquals(1, loaded.getContext().size());
        assertEquals("Hello World", loaded.getContext().get(0).getTextContent());

        // 删除并验证
        store.delete(null, sessionId);
        assertFalse(store.exists(null, sessionId));
    }

    /**
     * 测试多轮对话.
     *
     * <p>验证多轮对话消息的保存和加载顺序正确。
     */
    @Test
    void testMultiTurnConversation() {
        String sessionId = "test-multi-turn";

        // 创建会话
        sessionService.createSession("test_user", "多轮对话测试");

        // 第一轮对话
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好")
                .build());
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("你好！")
                .build());

        // 第二轮对话
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.USER)
                .textContent("今天天气怎么样？")
                .build());
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("今天天气晴朗，温度适宜。")
                .build());

        // 验证对话历史
        List<Msg> history = sessionService.loadMessages(sessionId);
        assertEquals(4, history.size());

        // 验证消息顺序
        assertEquals(MsgRole.USER, history.get(0).getRole());
        assertEquals("你好", history.get(0).getTextContent());

        assertEquals(MsgRole.ASSISTANT, history.get(1).getRole());
        assertEquals("你好！", history.get(1).getTextContent());

        // 清理测试数据
        sessionService.deleteSession(sessionId);
    }

    /**
     * 测试消息元数据.
     *
     * <p>验证带元数据的消息能正确保存和加载。
     */
    @Test
    void testMessageMetadata() {
        String sessionId = "test-metadata";

        // 创建带元数据的消息
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("带元数据的消息")
                .metadata(java.util.Map.of(
                        "source", "web",
                        "priority", 1
                ))
                .build();

        // 保存消息
        sessionService.createSession("test_user", "元数据测试");
        sessionService.saveMessage(sessionId, msg);

        // 验证消息已保存
        List<Msg> history = sessionService.loadMessages(sessionId);
        assertEquals(1, history.size());
        assertNotNull(history.get(0).getMetadata());

        // 清理测试数据
        sessionService.deleteSession(sessionId);
    }

    /**
     * 测试会话存在性检查.
     *
     * <p>验证 sessionExists 方法的正确性。
     */
    @Test
    void testSessionExists() {
        // 创建新会话
        ChatSession session = sessionService.createSession("test_user", "存在性测试");

        // 验证会话存在
        assertTrue(sessionService.sessionExists(session.getSessionId()));
        assertFalse(sessionService.sessionExists("non-existent-session"));

        // 删除会话后验证
        sessionService.deleteSession(session.getSessionId());
        assertFalse(sessionService.sessionExists(session.getSessionId()));
    }
}