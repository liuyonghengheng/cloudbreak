package com.sequenceiq.authorization.resource;

import java.util.List;

public interface ListResourceProvider<T> {

    List<AuthorizationResource> getAuthorizationResources();

    T getResult(List<Long> authorizedResourceIds);

    T getLegacyResult();
}
