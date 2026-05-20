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
package io.agentscope.tutorial.chapter18.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AgentScope Java Tutorial - Chapter 18: Spring Boot Integration Example.
 *
 * <p>This application demonstrates how to integrate AgentScope with Spring Boot.
 * It uses the agentscope-spring-boot-starter for automatic configuration
 * and provides REST endpoints for agent interaction.
 *
 * <p>Access:
 * <ul>
 *   <li>API Endpoints: http://localhost:8080/api/chat</li>
 *   <li>Actuator Health: http://localhost:8080/actuator/health</li>
 *   <li>H2 Console: http://localhost:8080/h2-console (dev profile)</li>
 * </ul>
 *
 * @author AgentScope Tutorial
 */
@SpringBootApplication
public class AgentScopeSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentScopeSpringApplication.class, args);
    }
}