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
 * 会话服务测试
 *
 * 使用 H2 内存数据库进行单元测试
 *
 * @author AgentScope Tutorial
 */
@SpringBootTest
@ActiveProfiles("dev")
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Test
    void testCreateSession() {
        // 创建会话
        ChatSession session = sessionService.createSession("test_user", "测试会话");

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals("test_user", session.getUserId());
        assertEquals("测试会话", session.getTitle());
        assertEquals(0, session.getMessageCount());

        // 清理
        sessionService.deleteSession(session.getSessionId());
    }

    @Test
    void testGetOrCreateSession() {
        String sessionId = "test-get-or-create";

        // 创建会话
        ChatSession session1 = sessionService.getOrCreateSession(sessionId, "test_user");
        assertNotNull(session1);
        assertEquals(sessionId, session1.getSessionId());

        // 获取已存在的会话
        ChatSession session2 = sessionService.getOrCreateSession(sessionId, "test_user");
        assertEquals(session1.getSessionId(), session2.getSessionId());

        // 清理
        sessionService.deleteSession(sessionId);
    }

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

        // 加载消息
        List<Msg> history = sessionService.loadMessages(sessionId);
        assertEquals(2, history.size());
        assertEquals("你好", history.get(0).getTextContent());
        assertEquals("你好！有什么可以帮你的吗？", history.get(1).getTextContent());

        // 清理
        sessionService.deleteSession(sessionId);
    }

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

        // 清理
        sessionService.deleteSession(sessionId);
    }

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

    @Test
    void testMessageEntityConversion() {
        // 测试 Msg 到 ChatMessage 的转换
        Msg msg = Msg.builder()
                .role(MsgRole.USER)
                .textContent("测试内容")
                .name("测试用户")
                .build();

        ChatMessage entity = ChatMessage.fromMsg(msg, "test-session", 0);

        assertNotNull(entity.getMessageId());
        assertEquals("test-session", entity.getSessionId());
        assertEquals("USER", entity.getRole());
        assertEquals("测试内容", entity.getContent());
        assertEquals("测试用户", entity.getName());
        assertEquals(0, entity.getSequenceNum());
    }

    @Test
    void testChatMessageToMsg() {
        // 测试 ChatMessage 到 Msg 的转换
        ChatMessage entity = new ChatMessage();
        entity.setMessageId("msg-123");
        entity.setSessionId("session-456");
        entity.setRole("ASSISTANT");
        entity.setContent("助手回复内容");
        entity.setName("AI助手");
        entity.setSequenceNum(1);

        Msg msg = entity.toMsg();

        assertEquals("msg-123", msg.getId());
        assertEquals(MsgRole.ASSISTANT, msg.getRole());
        assertEquals("助手回复内容", msg.getTextContent());
        assertEquals("AI助手", msg.getName());
    }

    @Test
    void testJsonLogFormatter() {
        JsonLogFormatter formatter = new JsonLogFormatter(true, true);

        List<Msg> messages = List.of(
                Msg.builder().role(MsgRole.SYSTEM).textContent("你是助手").build(),
                Msg.builder().role(MsgRole.USER).textContent("问题").build(),
                Msg.builder().role(MsgRole.ASSISTANT).textContent("回答").build()
        );

        // 测试格式化
        String formatted = formatter.formatMessagesToString(messages);
        assertNotNull(formatted);
        assertTrue(formatted.contains("SYSTEM"));
        assertTrue(formatted.contains("USER"));
        assertTrue(formatted.contains("ASSISTANT"));
    }

    @Test
    void testInMemoryAgentStateStore() {
        // 测试 AgentScope 2.x 的 InMemoryAgentStateStore
        // （替代已删除的 io.agentscope.core.session.InMemorySession + SimpleSessionKey）
        // 2.x 用 (userId, sessionId) 字符串对定位会话，userId 传 null 表示匿名槽
        var store = new InMemoryAgentStateStore();
        String sessionId = "memory-test";

        assertFalse(store.exists(null, sessionId));

        // 保存会话状态：用 AgentState 承载一条消息作为会话上下文
        AgentState state = AgentState.builder()
                .sessionId(sessionId)
                .context(List.of(Msg.builder()
                        .role(MsgRole.USER)
                        .textContent("Hello World")
                        .build()))
                .build();
        store.save(null, sessionId, "agent_state", state);

        assertTrue(store.exists(null, sessionId));

        // 读取并校验上下文
        AgentState loaded = store.get(null, sessionId, "agent_state", AgentState.class).orElseThrow();
        assertEquals(1, loaded.getContext().size());
        assertEquals("Hello World", loaded.getContext().get(0).getTextContent());

        // 清理
        store.delete(null, sessionId);
        assertFalse(store.exists(null, sessionId));
    }

    @Test
    void testMultiTurnConversation() {
        String sessionId = "test-multi-turn";

        // 创建会话
        sessionService.createSession("test_user", "多轮对话测试");

        // 第一轮
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.USER)
                .textContent("你好")
                .build());
        sessionService.saveMessage(sessionId, Msg.builder()
                .role(MsgRole.ASSISTANT)
                .textContent("你好！")
                .build());

        // 第二轮
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

        // 清理
        sessionService.deleteSession(sessionId);
    }

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

        sessionService.createSession("test_user", "元数据测试");
        sessionService.saveMessage(sessionId, msg);

        // 验证消息已保存
        List<Msg> history = sessionService.loadMessages(sessionId);
        assertEquals(1, history.size());
        assertNotNull(history.get(0).getMetadata());

        // 清理
        sessionService.deleteSession(sessionId);
    }

    @Test
    void testSessionExists() {
        // 创建新会话
        ChatSession session = sessionService.createSession("test_user", "存在性测试");

        // 验证会话存在
        assertTrue(sessionService.sessionExists(session.getSessionId()));
        assertFalse(sessionService.sessionExists("non-existent-session"));

        // 清理
        sessionService.deleteSession(session.getSessionId());
        assertFalse(sessionService.sessionExists(session.getSessionId()));
    }
}