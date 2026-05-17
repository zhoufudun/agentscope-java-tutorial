package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.session.Session;
import io.agentscope.core.state.SessionKey;
import io.agentscope.core.state.SimpleSessionKey;
import io.agentscope.tutorial.chapter03.entity.ChatMessage;
import io.agentscope.tutorial.chapter03.entity.ChatSession;
import io.agentscope.tutorial.chapter03.repository.MessageRepository;
import io.agentscope.tutorial.chapter03.repository.SessionRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 会话服务 - 管理会话的创建、加载和持久化
 *
 * 核心功能：
 * - 会话 CRUD 操作
 * - 消息持久化
 * - 与 AgentScope Session 集成
 *
 * @author AgentScope Tutorial
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final Session agentscopeSession;

    public SessionService(SessionRepository sessionRepository,
                          MessageRepository messageRepository,
                          Session agentscopeSession) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.agentscopeSession = agentscopeSession;
    }

    @PostConstruct
    public void init() {
        try {
            // 确保数据目录存在
            Path dataPath = Paths.get("./data");
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
                log.info("创建数据目录: {}", dataPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("初始化数据目录失败: {}", e.getMessage());
        }
    }

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题（可选）
     * @return 创建的会话实体
     */
    @Transactional
    public ChatSession createSession(String userId, String title) {
        String sessionId = UUID.randomUUID().toString();
        String effectiveTitle = (title != null && !title.isEmpty()) ? title : "新会话";

        ChatSession session = new ChatSession(sessionId, userId, effectiveTitle);
        ChatSession saved = sessionRepository.save(session);

        // 同时保存到 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.save(key, "session_info", saved);

        log.info("创建新会话: sessionId={}, userId={}, title={}", sessionId, userId, effectiveTitle);
        return saved;
    }

    /**
     * 获取或创建会话
     *
     * @param sessionId 会话ID
     * @param userId     用户ID
     * @return 会话实体
     */
    @Transactional
    public ChatSession getOrCreateSession(String sessionId, String userId) {
        Optional<ChatSession> existing = sessionRepository.findById(sessionId);
        if (existing.isPresent()) {
            return existing.get();
        }
        return createSession(userId, null);
    }

    /**
     * 根据会话ID获取会话
     *
     * @param sessionId 会话ID
     * @return 会话实体（如果不存在则返回空）
     */
    public Optional<ChatSession> getSession(String sessionId) {
        return sessionRepository.findById(sessionId);
    }

    /**
     * 加载会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    public List<Msg> loadMessages(String sessionId) {
        List<ChatMessage> entities = messageRepository.findBySessionIdOrderBySequenceNumAsc(sessionId);
        List<Msg> messages = new ArrayList<>();

        for (ChatMessage entity : entities) {
            messages.add(entity.toMsg());
        }

        log.debug("加载会话消息: sessionId={}, count={}", sessionId, messages.size());
        return messages;
    }

    /**
     * 保存单条消息到会话
     *
     * @param sessionId 会话ID
     * @param message   AgentScope 消息对象
     */
    @Transactional
    public void saveMessage(String sessionId, Msg message) {
        // 获取下一个序列号
        int nextSequence = messageRepository.getNextSequenceNum(sessionId);

        // 创建消息实体
        ChatMessage entity = ChatMessage.fromMsg(message, sessionId, nextSequence);
        messageRepository.save(entity);

        // 更新会话的消息计数
        updateSessionMessageCount(sessionId);

        // 保存到 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        List<Msg> messages = loadMessages(sessionId);
        agentscopeSession.save(key, "messages", messages);

        log.debug("保存消息: sessionId={}, messageId={}, sequence={}", sessionId, message.getId(), nextSequence);
    }

    /**
     * 批量保存消息
     *
     * @param sessionId 会话ID
     * @param messages  消息列表
     */
    @Transactional
    public void saveMessages(String sessionId, List<Msg> messages) {
        int sequence = messageRepository.getNextSequenceNum(sessionId);

        for (Msg message : messages) {
            ChatMessage entity = ChatMessage.fromMsg(message, sessionId, sequence++);
            messageRepository.save(entity);
        }

        // 更新会话消息计数
        updateSessionMessageCount(sessionId);

        // 保存到 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.save(key, "messages", messages);

        log.debug("批量保存消息: sessionId={}, count={}", sessionId, messages.size());
    }

    /**
     * 清空会话的所有消息
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void clearMessages(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setMessageCount(0);
            sessionRepository.save(session);
        }

        // 清理 AgentScope Session 中的消息
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.save(key, "messages", new ArrayList<>());

        log.info("清空会话消息: sessionId={}", sessionId);
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void deleteSession(String sessionId) {
        // 先删除消息
        messageRepository.deleteBySessionId(sessionId);
        // 再删除会话
        sessionRepository.deleteById(sessionId);

        // 清理 AgentScope Session
        SessionKey key = SimpleSessionKey.of(sessionId);
        agentscopeSession.delete(key);

        log.info("删除会话: sessionId={}", sessionId);
    }

    /**
     * 获取用户的所有会话
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    public List<ChatSession> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    /**
     * 更新会话的消息计数
     */
    private void updateSessionMessageCount(String sessionId) {
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            long count = messageRepository.countBySessionId(sessionId);
            session.setMessageCount((int) count);
            sessionRepository.save(session);
        }
    }

    /**
     * 检查会话是否存在
     *
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        return sessionRepository.existsById(sessionId);
    }

    /**
     * 获取会话的消息数量
     *
     * @param sessionId 会话ID
     * @return 消息数量
     */
    public long getMessageCount(String sessionId) {
        return messageRepository.countBySessionId(sessionId);
    }

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     新标题
     */
    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setTitle(title);
            sessionRepository.save(session);
            log.info("更新会话标题: sessionId={}, title={}", sessionId, title);
        }
    }
}