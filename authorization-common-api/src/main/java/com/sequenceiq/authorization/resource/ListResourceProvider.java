package com.sequenceiq.authorization.resource;

import java.util.List;
import java.util.Map;

public interface ListResourceProvider<T> {

    List<AuthorizationResource> getAuthorizationResources(Map<String, Object> params);

    T getResult(List<Long> authorizedResourceIds);

    T getLegacyResult();
}
