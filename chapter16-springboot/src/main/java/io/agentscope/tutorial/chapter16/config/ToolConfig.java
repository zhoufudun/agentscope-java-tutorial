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
package io.agentscope.tutorial.chapter16.config;

import io.agentscope.core.tool.Toolkit;
import io.agentscope.tutorial.chapter16.tool.WeatherTools;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 工具配置类.
 *
 * <p>用于注册自定义工具。
 * Toolkit 由 agentscope-spring-boot-starter 自动装配。
 * 这里我们额外注册工具集。
 *
 * <p>可注册的工具类型：
 * <ul>
 *   <li>使用 @Tool 注解标注的工具类</li>
 *   <li>通过工具 Schema 注册的动态工具</li>
 * </ul>
 *
 * @see WeatherTools
 */
@Configuration
public class ToolConfig {

    /**
     * 注册自定义工具集
     *
     * <p>Toolkit 是原型作用域，这里注册的工具会被添加到每个新创建的 Agent 中。
     *
     * @return 配置好的工具集
     */
    @Bean
    public Toolkit agentscopeToolkit() {
        Toolkit toolkit = new Toolkit();
        // 注册天气查询工具（通过反射扫描 @Tool 注解）
        toolkit.registration()
                .tool(new WeatherTools())
                .apply();
        return toolkit;
    }
}