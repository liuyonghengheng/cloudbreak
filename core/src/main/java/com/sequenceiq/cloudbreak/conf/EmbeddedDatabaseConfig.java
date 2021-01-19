package com.sequenceiq.cloudbreak.conf;

import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cb.db.env.embedded.volume")
public class EmbeddedDatabaseConfig {
    private Integer size;

    private Map<String, String> platformVolumeTypeMap;

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Map<String, String> getPlatformVolumeTypeMap() {
        return platformVolumeTypeMap;
    }

    public void setPlatformVolumeTypeMap(Map<String, String> platformVolumeTypeMap) {
        this.platformVolumeTypeMap = platformVolumeTypeMap;
    }

    public Optional<String> getPlatformVolumeType(String cloudPlatform) {
        return Optional.ofNullable(platformVolumeTypeMap.get(cloudPlatform));
    }
}
