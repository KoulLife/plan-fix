package taedonghee.plan_fix.infrastructure.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Gemini 관련 프로퍼티 (application.yaml 및 application-secret.yml에서 주입)
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(
        String apiKey,
        String modelName
) {
    public GeminiProperties {
        if (modelName == null || modelName.isBlank()) {
            modelName = "gemini-3.5-flash-lite";
        }
    }
}
