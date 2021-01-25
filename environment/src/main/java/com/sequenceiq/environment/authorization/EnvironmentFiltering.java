package com.sequenceiq.environment.authorization;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;

@Component
public class EnvironmentFiltering implements AuthorizationFiltering<List<EnvironmentDto>> {

    private final EnvironmentService environmentService;

    public EnvironmentFiltering(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @Override
    public List<AuthorizationResource> getAllResources(Map<String, Object> args) {
        return environmentService.findAsAuthorizationResourcesInAccount(ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    public List<EnvironmentDto> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return environmentService.findAllByIds(authorizedResourceIds);
    }

    @Override
    public List<EnvironmentDto> getAll(Map<String, Object> args) {
        return environmentService.listByAccountId(ThreadBasedUserCrnProvider.getAccountId());
    }
}
