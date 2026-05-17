package io.agentscope.tutorial.chapter11.config;

import io.agentscope.core.skill.SkillBox;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.file.ReadFileTool;
import io.agentscope.core.tool.file.WriteFileTool;
import io.agentscope.core.tool.coding.ShellCommandTool;
import io.agentscope.tutorial.chapter11.service.SkillService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgs构造函数;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 技能系统配置
 *
 * <p>负责初始化 SkillBox 和相关组件，包括：
 * <ul>
 *   <li>SkillBox 实例创建和工具绑定</li>
 *   <li>代码执行工具配置</li>
 *   <li>技能自动加载</li>
 *   <li>技能提示词配置</li>
 * </ul>
 *
 * <p>配置示例（application.yml）：
 * <pre>{@code
 * agentscope:
 *   skill:
 *     storage-path: classpath:skills/
 *     auto-load: true
 *     toolkit:
 *       code-execution-enabled: true
 * }</pre>
 *
 * @author AgentScope Tutorial
 * @see SkillBox
 * @see Toolkit
 */
@Configuration
@RequiredArgs构造函数
@Slf4j
public class SkillConfig {

    /**
     * 技能服务
     */
    private final SkillService skillService;

    /**
     * 技能文件存储路径
     */
    @Value("${agentscope.skill.storage-path:classpath:skills/}")
    private String storagePath;

    /**
     * 是否自动加载技能
     */
    @Value("${agentscope.skill.auto-load:true}")
    private boolean autoLoad;

    /**
     * 代码执行是否启用
     */
    @Value("${agentscope.skill.toolkit.code-execution-enabled:true}")
    private boolean codeExecutionEnabled;

    /**
     * 代码执行工作目录
     */
    @Value("${agentscope.skill.code-execution.work-dir:${java.io.tmpdir}/agentscope-skills}")
    private String workDir;

    /**
     * 允许的 Shell 命令
     */
    @Value("${agentscope.skill.code-execution.shell-commands:python,python3,node}")
    private String shellCommands;

    /**
     * 是否启用读取文件工具
     */
    @Value("${agentscope.skill.toolkit.read-enabled:true}")
    private boolean readEnabled;

    /**
     * 是否启用写入文件工具
     */
    @Value("${agentscope.skill.toolkit.write-enabled:true}")
    private boolean writeEnabled;

    /**
     * 创建 Toolkit Bean
     *
     * <p>Toolkit 是工具管理的核心组件，负责：
     * <ul>
     *   <li>工具注册和分组</li>
     *   <li>工具执行和上下文管理</li>
     *   <li>元工具支持</li>
     * </ul>
     *
     * @return Toolkit 实例
     */
    @Bean
    public Toolkit toolkit() {
        return new Toolkit();
    }

    /**
     * 创建 SkillBox Bean
     *
     * <p>SkillBox 是技能管理的核心组件，负责：
     * <ul>
     *   <li>技能注册和激活管理</li>
     *   <li>技能提示词生成</li>
     *   <li>技能工具创建</li>
     *   <li>技能文件上传</li>
     * </ul>
     *
     * <p>SkillBox 需要与 Toolkit 绑定，以便在技能激活时同步工具组状态。
     *
     * @param toolkit Toolkit 实例（注入）
     * @return SkillBox 实例
     */
    @Bean
    public SkillBox skillBox(Toolkit toolkit) {
        SkillBox skillBox = new SkillBox(toolkit);
        // 绑定 toolkit 到 skillBox（Deep copy 后重新绑定）
        skillBox.bindToolkit(toolkit);

        // 配置代码执行能力
        if (codeExecutionEnabled) {
            configureCodeExecution(skillBox);
        }

        // 注册技能加载工具（load_skill_through_path）
        skillBox.registerSkillLoadTool();

        log.info("SkillBox initialized with code-execution-enabled={}, auto-load={}",
                codeExecutionEnabled, autoLoad);
        return skillBox;
    }

    /**
     * 配置代码执行能力
     */
    private void configureCodeExecution(SkillBox skillBox) {
        // 解析允许的命令
        Set<String> allowedCommands = parseCommands(shellCommands);

        // 配置 Shell 命令工具
        ShellCommandTool shellTool = new ShellCommandTool(
                workDir,
                allowedCommands,
                null // 无需用户确认
        );

        // 使用 CodeExecutionBuilder 链式配置
        skillBox.codeExecution()
                .workDir(workDir)
                .includeFolders(Set.of("scripts/", "assets/"))
                .includeExtensions(Set.of(".py", ".js", ".sh", ".sql"))
                .withShell(shellTool)
                .withRead()
                .withWrite()
                .enable();

        log.info("Code execution configured with workDir={}, allowedCommands={}",
                workDir, allowedCommands);
    }

    /**
     * 解析命令列表
     */
    private Set<String> parseCommands(String commands) {
        Set<String> result = new HashSet<>();
        if (commands != null && !commands.isBlank()) {
            for (String cmd : commands.split(",")) {
                String trimmed = cmd.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
        }
        return result;
    }

    /**
     * 初始化完成后自动加载技能
     */
    @PostConstruct
    public void initSkills() {
        if (!autoLoad) {
            log.info("Auto-load disabled, skipping skill initialization");
            return;
        }

        try {
            loadSkillsFromStorage();
        } catch (Exception e) {
            log.warn("Failed to auto-load skills: {}", e.getMessage());
        }
    }

    /**
     * 从存储路径加载技能
     */
    private void loadSkillsFromStorage() {
        log.info("Loading skills from storage path: {}", storagePath);

        if (storagePath.startsWith("classpath:")) {
            loadSkillsFromClasspath(storagePath);
        } else {
            loadSkillsFromFileSystem(storagePath);
        }
    }

    /**
     * 从 ClassPath 加载技能
     */
    private void loadSkillsFromClasspath(String path) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = path.replace("classpath:", "classpath*:");
            if (!pattern.endsWith("/")) {
                pattern += "/";
            }
            pattern += "*/SKILL.md";

            Resource[] resources = resolver.getResources(pattern);
            log.info("Found {} skill files", resources.length);

            for (Resource resource : resources) {
                try {
                    String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    String skillDir = extractSkillDir(resource);

                    // 加载关联资源
                    java.util.Map<String, String> resources2 = loadDirectoryResources(resource, skillDir);

                    io.agentscope.core.skill.AgentSkill skill =
                            io.agentscope.core.skill.util.SkillUtil.createFrom(content, resources2);

                    // 注册到数据库
                    skillService.registerSkill(skill);

                    log.info("Loaded skill: {}", skill.getName());
                } catch (Exception e) {
                    log.warn("Failed to load skill from {}: {}", resource, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("Failed to scan classpath for skills: {}", e.getMessage());
        }
    }

    /**
     * 从文件系统加载技能
     */
    private void loadSkillsFromFileSystem(String path) {
        Path skillDir = Paths.get(path.replace("classpath:", ""));
        if (!Files.exists(skillDir)) {
            log.warn("Skill storage path does not exist: {}", skillDir);
            return;
        }

        try (Stream<Path> paths = Files.walk(skillDir)) {
            paths.filter(p -> p.endsWith("SKILL.md"))
                    .forEach(skillFile -> {
                        try {
                            String content = Files.readString(skillFile, StandardCharsets.UTF_8);
                            String skillDirName = skillFile.getParent().getFileName().toString();

                            // 加载关联资源
                            java.util.Map<String, String> resources = new java.util.HashMap<>();
                            Path parentDir = skillFile.getParent();
                            try (Stream<Path> files = Files.walk(parentDir)) {
                                files.filter(f -> !f.equals(skillFile) && !Files.isDirectory(f))
                                        .forEach(f -> {
                                            try {
                                                String resourcePath = parentDir.relativize(f).toString();
                                                String resourceContent = Files.readString(f, StandardCharsets.UTF_8);
                                                resources.put(resourcePath, resourceContent);
                                            } catch (IOException e) {
                                                log.warn("Failed to read resource: {}", f);
                                            }
                                        });
                            }

                            io.agentscope.core.skill.AgentSkill skill =
                                    io.agentscope.core.skill.util.SkillUtil.createFrom(content, resources);

                            skillService.registerSkill(skill);
                            log.info("Loaded skill from file: {}", skill.getName());
                        } catch (Exception e) {
                            log.warn("Failed to load skill from {}: {}", skillFile, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to scan filesystem for skills: {}", e.getMessage());
        }
    }

    /**
     * 从 Resource 提取技能目录名
     */
    private String extractSkillDir(Resource resource) {
        try {
            String path = resource.getURL().getPath();
            // classpath:/skills/data-analysis/SKILL.md -> data-analysis
            String[] parts = path.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if ("skills".equals(parts[i])) {
                    return i + 1 < parts.length ? parts[i + 1] : "";
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    /**
     * 加载目录中的资源文件
     */
    private java.util.Map<String, String> loadDirectoryResources(Resource skillFile, String skillDir) {
        java.util.Map<String, String> resources = new java.util.HashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            String pattern = "classpath*:skills/" + skillDir + "/**/*";
            Resource[] dirResources = resolver.getResources(pattern);

            for (Resource r : dirResources) {
                if (!r.getFilename().equals("SKILL.md")) {
                    try {
                        String content = new String(r.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        String path = r.getFilename();
                        resources.put(path != null ? path : "file", content);
                    } catch (Exception e) {
                        log.warn("Failed to load resource: {}", r);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load directory resources: {}", e.getMessage());
        }
        return resources;
    }
}