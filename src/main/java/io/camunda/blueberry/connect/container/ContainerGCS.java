package io.camunda.blueberry.connect.container;

import io.camunda.blueberry.config.BlueberryConfig;
import org.springframework.stereotype.Component;

@Component
public class ContainerGCS implements Container {

    public static final String GCS = "gcs";
    private final BlueberryConfig blueberryConfig;

    public ContainerGCS(BlueberryConfig blueberryConfig) {
        this.blueberryConfig = blueberryConfig;
    }

    @Override
    public String getType() {
        return GCS;
    }

    @Override
    public String getElasticsearchPayload(String basePathInsideContainer) {
        String bucket = blueberryConfig.getGcsBucket();

        return String.format("""
                {
                  "type": "%s",
                  "settings": {
                    "bucket": "%s",
                    "base_path": "%s",
                    "client": "default"                
                  }
                }
                """, GCS, bucket, basePathInsideContainer);
    }

    @Override
    public String getInformation() {
        String bucket = blueberryConfig.getGcsBucket();
        return "Container type[" + GCS + "] bucket[" + bucket + "]";
    }
}