package io.agentscope.tutorial.chapter06;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java Tutorial - Chapter 06: Model Integration
 *
 * <p>This application demonstrates how to configure and use multiple model clients,
 * supporting DashScope, OpenAI, Ollama, Gemini and more.
 *
 * <p>Access:
 * <ul>
 *   <li>API Endpoints: http://localhost:8080/api/models</li>
 *   <li>H2 Console: http://localhost:8080/h2-console</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class Chapter06Application {

    public static void main(String[] args) {
        SpringApplication.run(Chapter06Application.class, args);
    }
}