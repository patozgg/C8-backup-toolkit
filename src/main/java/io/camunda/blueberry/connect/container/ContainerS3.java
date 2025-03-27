package io.camunda.blueberry.connect.container;

import io.camunda.blueberry.config.BlueberryConfig;
import org.springframework.stereotype.Component;

@Component
public class ContainerS3 implements Container {

    public static final String S3 = "s3";
    private final BlueberryConfig blueberryConfig;

    public ContainerS3(BlueberryConfig blueberryConfig) {
        this.blueberryConfig = blueberryConfig;
    }

    @Override
    public String getType() {
        return S3;
    }

    @Override
    public String getElasticsearchPayload(String basePathInsideContainer) {
        String bucket = blueberryConfig.getS3Bucket();
        String region = blueberryConfig.getS3Region();
        return String.format("""
                {
                  "type": "%s",
                  "settings": {
                    "bucket": "%s",
                    "base_path": "%s",
                    "region": "%s"
                  }
                }
                """, S3, bucket, basePathInsideContainer, region);
    }

    @Override
    public String getInformation() {
        String bucket = blueberryConfig.getS3Bucket();
        String region = blueberryConfig.getS3Region();
        return "Container type[" + S3 + "] bucket[" + bucket + "] region[" + region + "]";
    }
}