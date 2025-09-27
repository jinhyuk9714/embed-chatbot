// ====================================================================
// File: server/src/main/java/com/example/embedchatbot/config/RagConfig.java
// ====================================================================
package com.example.embedchatbot.config;

import com.example.embedchatbot.rag.RagProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({RagProperties.class})
public class RagConfig {}
