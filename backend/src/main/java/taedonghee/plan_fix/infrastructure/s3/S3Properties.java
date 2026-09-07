package taedonghee.plan_fix.infrastructure.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS S3 프로퍼티 (application.yaml 및 application-secret.yml에서 주입)
 */
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        String bucket,
        String region,
        String accessKey,
        String secretKey
) {
    public S3Properties {
        if (region == null || region.isBlank()) {
            region = "ap-northeast-2";
        }
    }
}
