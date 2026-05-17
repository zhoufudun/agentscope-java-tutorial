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
package io.agentscope.tutorial.chapter18.quarkus;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.extern.slf4j.Slf4j;

/**
 * AgentScope Java Tutorial - Chapter 18: Quarkus Integration Example.
 *
 * <p>This application demonstrates how to integrate AgentScope with Quarkus.
 * It uses the agentscope-quarkus-extension for build-time optimization
 * and supports GraalVM native image compilation.
 *
 * <p>Key features:
 * <ul>
 *   <li>Build-time CDI (ArC - Arc CDI implementation)</li>
 *   <li>Hot reload in dev mode</li>
 *   <li>GraalVM native image support</li>
 *   <li>Ultra-fast startup and low memory footprint</li>
 * </ul>
 *
 * <p>Run in development mode:
 * <pre>
 * DASHSCOPE_API_KEY=your-key mvn quarkus:dev
 * </pre>
 *
 * <p>Run as JAR:
 * <pre>
 * DASHSCOPE_API_KEY=your-key mvn package && java -jar target/quarkus-app/quarkus-run.jar
 * </pre>
 *
 * <p>Build native image (requires GraalVM):
 * <pre>
 * mvn package -Pnative
 * ./target/chapter18-quarkus-1.0.0-runner
 * </pre>
 *
 * @author AgentScope Tutorial
 */
@Slf4j
@QuarkusMain
public class Application implements QuarkusApplication {

    public static void main(String[] args) {
        System.out.println("\n=== AgentScope Quarkus Example ===\n");
        System.out.println("Framework: Quarkus (cloud-native Java)");
        System.out.println("Key advantage: Ultra-fast startup, GraalVM native support\n");

        Quarkus.run(Application.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        System.out.println("Quarkus application started!");
        System.out.println("Access endpoints at: http://localhost:8080");
        System.out.println("  - POST /chat - Chat with the agent");
        System.out.println("  - GET  /health - Health check\n");

        // Keep the application running
        Quarkus.waitForExit();
        return 0;
    }
}