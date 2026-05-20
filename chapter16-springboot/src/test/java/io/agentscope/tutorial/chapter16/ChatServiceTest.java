/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agentscope.tutorial.chapter16;

import io.agentscope.tutorial.chapter16.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChatService 单元测试.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ChatServiceTest {

    @Autowired
    private ChatService chatService;

    private String testSessionId;

    @BeforeEach
    void setUp() {
        // 每个测试前创建新会话
        testSessionId = chatService.createSession("测试会话");
    }

    @Test
    void testCreateSession() {
        assertNotNull(testSessionId);
        assertFalse(testSessionId.isBlank());
    }

    @Test
    void testAddMessage() {
        // 添加用户消息
        long msgId = chatService.addMessage(testSessionId, "user", "你好");
        assertTrue(msgId > 0);

        // 验证消息数量
        int count = chatService.getMessageCount(testSessionId);
        assertEquals(1, count);
    }

    @Test
    void testGetMessages() {
        // 添加测试消息
        chatService.addMessage(testSessionId, "user", "你好");
        chatService.addMessage(testSessionId, "assistant", "你好！有什么可以帮助你的吗？");

        // 获取消息
        List<?> messages = chatService.getMessages(testSessionId);
        assertEquals(2, messages.size());
    }

    @Test
    void testResetSession() {
        // 添加消息
        chatService.addMessage(testSessionId, "user", "测试");
        assertEquals(1, chatService.getMessageCount(testSessionId));

        // 重置会话
        chatService.resetSession(testSessionId);
        assertEquals(0, chatService.getMessageCount(testSessionId));
    }

    @Test
    void testDeleteSession() {
        boolean deleted = chatService.deleteSession(testSessionId);
        assertTrue(deleted);
        assertEquals(0, chatService.getMessageCount(testSessionId));
    }
}