package com.mumu.game.core.autoluban.config;


import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mumu.game.core.log.LogTopic;
import com.mumu.game.luban.cfg.CfgTables;

/**
 * LuBanConfiguration
 * Luban配置表自动加载配置类
 * @author liuzhen
 * @version 1.0.0 2026/5/5 21:20
 */
@Configuration
public class LuBanConfiguration {

    private static final LogTopic log = LogTopic.ACTION;

    @Bean
    public CfgTables cfgTables() {
        try {
            log.info("开始加载LuBan配置表...");
            CfgTables.IJsonLoader jsonLoader = createJsonLoader();
            CfgTables cfgTables = new CfgTables(jsonLoader);
            log.info("LuBan配置表加载成功");
            return cfgTables;
        } catch (IOException e) {
            log.error("LuBan配置表加载失败，请确保已运行Luban工具生成配置表文件到 auto_gen/src/main/resources/luban/output/ 目录", e);
            throw new RuntimeException("LuBan配置表加载失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建JSON加载器
     * 从classpath的luban/output/json目录加载配置表JSON文件
     *
     * @return IJsonLoader实例
     */
    private CfgTables.IJsonLoader createJsonLoader() {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        Map<String, JsonElement> jsonCache = new HashMap<>();

        return file -> {
            JsonElement cached = jsonCache.get(file);
            if (cached != null) {
                log.info("从缓存获取配置文件: {}", file);
                return cached;
            }

            String resourcePath = "luban/output/json/" + file + ".json";
            try {
                Resource[] resources = resourcePatternResolver.getResources("classpath*:" + resourcePath);
                if (resources.length == 0) {
                    log.error("找不到配置文件: {}", resourcePath);
                    throw new IOException("无法找到配置文件: " + resourcePath +
                            "。请确保已运行 Luban 工具生成配置表，文件应位于 auto_gen/src/main/resources/luban/output/json/ 目录");
                }

                Resource resource = resources[0];
                log.info("加载配置文件: {}, 存在: {}", resourcePath, resource.exists());

                try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    jsonCache.put(file, jsonElement);
                    log.info("成功加载配置文件: {}, 数据长度: {}", resourcePath, jsonElement.toString().length());
                    return jsonElement;
                }
            } catch (IOException e) {
                log.error("加载配置文件失败: {}", resourcePath, e);
                throw e;
            }
        };
    }
}
