package com.sequenceiq.cloudbreak.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.component.RepositoryInfo;
import com.sequenceiq.cloudbreak.service.exception.RepositoryCannotFoundException;

@Service
@ConfigurationProperties("cb.clouderamanager")
public class DefaultClouderaManagerRepoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultClouderaManagerRepoService.class);

    private Map<String, RepositoryInfo> entries = new HashMap<>();

    public AmbariRepo getDefault(String osType) {
        for (Entry<String, RepositoryInfo> clouderaManagerInfoEntry : entries.entrySet()) {
            RepositoryInfo clouderaManagerInfo = clouderaManagerInfoEntry.getValue();
            if (clouderaManagerInfo.getRepo().get(osType) == null) {
                LOGGER.info("Missing Cloudera Manager ({}) repo information for os: {}", clouderaManagerInfo.getVersion(), osType);
                continue;
            }
            AmbariRepo repository = new AmbariRepo();
            repository.setPredefined(Boolean.FALSE);
            repository.setVersion(clouderaManagerInfo.getVersion());
            repository.setBaseUrl(clouderaManagerInfo.getRepo().get(osType).getBaseurl());
            repository.setGpgKeyUrl(clouderaManagerInfo.getRepo().get(osType).getGpgkey());
            return repository;
        }
        throw new RepositoryCannotFoundException("Repository informations cannot found by given osType!");
    }

    public Map<String, RepositoryInfo> getEntries() {
        return entries;
    }

    public void setEntries(Map<String, RepositoryInfo> entries) {
        this.entries = entries;
    }
}
