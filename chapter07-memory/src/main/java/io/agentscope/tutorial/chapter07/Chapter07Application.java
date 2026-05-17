package io.agentscope.tutorial.chapter07;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java Tutorial - Chapter 07: Memory System
 *
 * <p>This application demonstrates a comprehensive memory system implementation
 * with the following features:
 *
 * <ul>
 *   <li><b>Persistent Memory</b> - Conversation history stored in H2/MySQL database</li>
 *   <li><b>Multi-Session Isolation</b> - Separate memory per session/user</li>
 *   <li><b>Memory Compression</b> - Automatic summarization of long conversations</li>
 *   <li><b>Memory Search</b> - Full-text search across conversation history</li>
 *   <li><b>Memory Tools</b> - Agent-accessible tools for memory management</li>
 * </ul>
 *
 * <p>Access:
 * <ul>
 *   <li>API Documentation: http://localhost:8080/api/memory</li>
 *   <li>Chat API: http://localhost:8080/api/chat</li>
 *   <li>H2 Console: http://localhost:8080/h2-console (dev profile only)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Send a message
 * curl -X POST "http://localhost:8080/api/chat/send?sessionId=user001" \
 *   -H "Content-Type: application/json" \
 *   -d '{"message": "Hello, my name is John"}'
 *
 * // Search memory
 * curl "http://localhost:8080/api/memory/search?sessionId=user001&keyword=John"
 *
 * // Get memory stats
 * curl "http://localhost:8080/api/memory/stats?sessionId=user001"
 * }</pre>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter07Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter07Application.class, args);
    }
}