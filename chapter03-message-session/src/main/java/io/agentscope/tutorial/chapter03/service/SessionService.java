package io.agentscope.tutorial.chapter03.service;

import io.agentscope.core.message.Msg;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.AgentStateStore;
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
 * - 消息持久化（JPA/H2）
 * - 与 AgentScope 2.x 会话状态存储（AgentStateStore）集成
 *
 * <p>迁移说明（1.1.0 → 2.0.3）：
 * 原来注入的 io.agentscope.core.session.Session（已删除）替换为
 * io.agentscope.core.state.AgentStateStore。会话状态以 AgentState 形式持久化，
 * 定位不再用 SessionKey/SimpleSessionKey，而是用 (userId, sessionId) 字符串对。
 * 该状态存储与 JPA/H2 并行运行：JPA 负责会话元数据 + 消息明细，AgentStateStore
 * 负责保存 AgentScope 原生的会话上下文（List&lt;Msg&gt;），用于教学演示两种持久化方式。
 *
 * @author AgentScope Tutorial
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    /** AgentStateStore 中保存会话状态所用的键（对应磁盘上的 agent_state.json）。 */
    private static final String AGENT_STATE_KEY = "agent_state";

    private final SessionRepository sessionRepository;
    private final MessageRepository messageRepository;
    private final AgentStateStore agentStateStore;

    public SessionService(SessionRepository sessionRepository,
                          MessageRepository messageRepository,
                          AgentStateStore agentStateStore) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.agentStateStore = agentStateStore;
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
        return sessionRepository.findById(sessionId).orElseGet(() -> {
            // 会话不存在时，使用调用方指定的 sessionId 创建（而非随机 UUID），
            // 以便调用方后续能用同一 sessionId 保存消息、查询历史。
            ChatSession session = new ChatSession(sessionId, userId, "新会话");
            ChatSession saved = sessionRepository.save(session);
            log.info("按指定 ID 创建会话: sessionId={}, userId={}", sessionId, userId);
            return saved;
        });
    }

    /**
     * 确保指定的会话存在，不存在则自动创建（占位用户）。
     *
     * <p>chat_messages 对 chat_sessions 有外键约束，保存消息前必须保证会话已存在，
     * 否则会触发外键冲突导致事务回滚。
     *
     * @param sessionId 会话ID
     */
    private void ensureSessionExists(String sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            sessionRepository.save(new ChatSession(sessionId, "unknown", "自动创建的会话"));
            log.debug("自动创建缺失的会话: sessionId={}", sessionId);
        }
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
        // 保存消息前确保会话已存在（避免外键约束冲突）
        ensureSessionExists(sessionId);

        // 获取下一个序列号
        int nextSequence = messageRepository.getNextSequenceNum(sessionId);

        // 创建消息实体
        ChatMessage entity = ChatMessage.fromMsg(message, sessionId, nextSequence);
        messageRepository.save(entity);

        // 更新会话的消息计数
        updateSessionMessageCount(sessionId);

        // 同步会话上下文到 AgentScope 状态存储
        syncAgentState(sessionId);

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
        // 保存消息前确保会话已存在（避免外键约束冲突）
        ensureSessionExists(sessionId);

        int sequence = messageRepository.getNextSequenceNum(sessionId);

        for (Msg message : messages) {
            ChatMessage entity = ChatMessage.fromMsg(message, sessionId, sequence++);
            messageRepository.save(entity);
        }

        // 更新会话消息计数
        updateSessionMessageCount(sessionId);

        // 同步会话上下文到 AgentScope 状态存储
        syncAgentState(sessionId);

        log.debug("批量保存消息: sessionId={}, count={}", sessionId, messages.size());
    }

    /**
     * 清空会话的所有消息
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void clearMessages(String sessionId) {
        // 先解析 userId（会话此时仍存在），用于定位状态存储中的会话槽
        String userId = resolveUserId(sessionId);

        messageRepository.deleteBySessionId(sessionId);

        ChatSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) {
            session.setMessageCount(0);
            sessionRepository.save(session);
        }

        // 清理 AgentScope 状态存储中的会话上下文
        // 2.x 用 (userId, sessionId) 字符串对定位，不再使用已删除的 SessionKey/SimpleSessionKey
        try {
            agentStateStore.delete(userId, sessionId);
        } catch (Exception e) {
            log.warn("清理会话状态存储失败: sessionId={}, error={}", sessionId, e.getMessage());
        }

        log.info("清空会话消息: sessionId={}", sessionId);
    }

    /**
     * 删除会话及其所有消息
     *
     * @param sessionId 会话ID
     */
    @Transactional
    public void deleteSession(String sessionId) {
        // 删除会话前先解析 userId，删除后就查不到了
        String userId = resolveUserId(sessionId);

        // 先删除消息
        messageRepository.deleteBySessionId(sessionId);
        // 再删除会话
        sessionRepository.deleteById(sessionId);

        // 同步清理 AgentScope 状态存储中的整个会话槽（含 agent_state）
        try {
            agentStateStore.delete(userId, sessionId);
        } catch (Exception e) {
            log.warn("清理会话状态存储失败: sessionId={}, error={}", sessionId, e.getMessage());
        }

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

    // ==================== AgentScope 状态存储（AgentStateStore）集成 ====================

    /**
     * 从 AgentScope 状态存储中读取会话上下文。
     *
     * <p>这是 2.x 的原生读取路径，与 {@link #loadMessages(String)} 从数据库读取形成对照，
     * 用于演示 {@link AgentStateStore#get} 的用法。会话上下文保存在 {@link AgentState} 中，
     * 通过 {@link AgentState#getContext()} 返回 {@code List<Msg>}。
     *
     * @param sessionId 会话ID
     * @return 会话上下文消息列表；若状态存储中不存在则返回空列表
     */
    public List<Msg> loadContextFromStateStore(String sessionId) {
        String userId = resolveUserId(sessionId);
        return agentStateStore.get(userId, sessionId, AGENT_STATE_KEY, AgentState.class)
                .map(AgentState::getContext)
                .orElseGet(List::of);
    }

    /**
     * 将会话当前的完整上下文同步写入 AgentScope 状态存储。
     *
     * <p>每次保存消息后调用，把数据库中的完整历史打包成一个 {@link AgentState}，
     * 通过 {@link AgentStateStore#save} 以 (userId, sessionId, key) 的形式落盘。
     * 任何异常都不会影响主流程（JPA 持久化），仅记录告警。
     *
     * @param sessionId 会话ID
     */
    private void syncAgentState(String sessionId) {
        try {
            String userId = resolveUserId(sessionId);
            List<Msg> context = loadMessages(sessionId);
            AgentState state = AgentState.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .context(context)
                    .build();
            agentStateStore.save(userId, sessionId, AGENT_STATE_KEY, state);
            log.debug("同步会话状态到 AgentStateStore: sessionId={}, contextSize={}", sessionId, context.size());
        } catch (Exception e) {
            log.warn("同步 AgentState 到状态存储失败: sessionId={}, error={}", sessionId, e.getMessage());
        }
    }

    /**
     * 解析会话对应的 userId（用于定位状态存储中的会话槽）。
     *
     * @param sessionId 会话ID
     * @return 会话所属用户ID；会话不存在时返回 {@code null}（匿名槽）
     */
    private String resolveUserId(String sessionId) {
        return sessionRepository.findById(sessionId)
                .map(ChatSession::getUserId)
                .orElse(null);
    }
}