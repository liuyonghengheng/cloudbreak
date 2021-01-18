package com.sequenceiq.authorization.service.list;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.ListResourceProvider;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.authorization.service.model.AuthorizedList;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@Service
public class ListAuthorizationService {

    private static final String NO_RIGHT_IN_ACCOUNT = "You have no right to perform %s in account %s.";

    @Inject
    private Map<Class<ListResourceProvider<?>>, ListResourceProvider<?>> listResourceProviders;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public Object filterList(FilterListBasedOnPermissions annotation, Crn userCrn, ProceedingJoinPoint proceedingJoinPoint, Optional<String> requestId) {
        AuthorizationResourceAction action = annotation.action();
        ListResourceProvider<?> listResourceProvider = listResourceProviders.get(annotation.provider());
        AuthorizedList authorizedList;
        if (entitlementService.listFilteringEnabled(userCrn.getAccountId())) {
            List<AuthorizationResource> resources = listResourceProvider.getAuthorizationResources();
            List<Long> authorizedResourceIds = getAuthorizedResourceIds(userCrn, action, resources, requestId);
            authorizedList = new AuthorizedList(listResourceProvider.getResult(authorizedResourceIds));
        } else {
            checkAccountRight(userCrn, action, requestId);
            authorizedList = new AuthorizedList(listResourceProvider.getLegacyResult());
        }
        try {
            return proceedingJoinPoint.proceed(new Object[]{authorizedList});
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public List<Long> getAuthorizedResourceIds(Crn userCrn, AuthorizationResourceAction action, List<AuthorizationResource> resources,
            Optional<String> requestId) {
        if (CollectionUtils.isEmpty(resources)) {
            return List.of();
        }
        Map<Optional<String>, List<AuthorizationResource>> resourcesByParents = new LinkedHashMap<>();
        resources.forEach(resource -> resourcesByParents
                .computeIfAbsent(resource.getParentResourceCrn(), ignored -> new ArrayList<>())
                .add(resource));
        List<String> resourceCrns = new ArrayList<>();
        for (Entry<Optional<String>, List<AuthorizationResource>> entry : resourcesByParents.entrySet()) {
            Optional<String> parentResource = entry.getKey();
            List<AuthorizationResource> subResources = entry.getValue();
            if (parentResource.isPresent()) {
                resourceCrns.add(parentResource.get());
            }
            resourceCrns.addAll(subResources
                    .stream()
                    .map(AuthorizationResource::getResourceCrn)
                    .collect(Collectors.toList()));
        }
        String userCrnAsString = userCrn.toString();
        List<Boolean> result = grpcUmsClient.hasRightsOnResources(userCrnAsString, userCrnAsString, resourceCrns, action.getRight(), requestId);
        List<Long> authorizedResourceIds = new ArrayList<>();
        Iterator<Boolean> resultIterator = result.iterator();
        for (Entry<Optional<String>, List<AuthorizationResource>> entry : resourcesByParents.entrySet()) {
            Optional<String> parentResource = entry.getKey();
            List<AuthorizationResource> subResources = entry.getValue();
            if (parentResource.isPresent() && resultIterator.next()) {
                for (AuthorizationResource authorizationResource : subResources) {
                    resultIterator.next();
                    authorizedResourceIds.add(authorizationResource.getId());
                }
            } else {
                for (AuthorizationResource authorizationResource : subResources) {
                    if (resultIterator.next()) {
                        authorizedResourceIds.add(authorizationResource.getId());
                    }
                }
            }
        }
        return authorizedResourceIds;
    }

    private void checkAccountRight(Crn userCrn, AuthorizationResourceAction action, Optional<String> requestId) {
        String userCrnAsString = userCrn.toString();
        if (entitlementService.isAuthorizationEntitlementRegistered(userCrn.getAccountId())) {
            String right = action.getRight();
            if (!grpcUmsClient.checkAccountRight(userCrnAsString, userCrnAsString, right, requestId)) {
                throw new AccessDeniedException(String.format(NO_RIGHT_IN_ACCOUNT, right, userCrn.getAccountId()));
            }
        } else {
            String legacyRight = umsRightProvider.getLegacyRight(action);
            if (!grpcUmsClient.checkAccountRightLegacy(userCrnAsString, userCrnAsString, legacyRight, requestId)) {
                throw new AccessDeniedException(String.format(NO_RIGHT_IN_ACCOUNT, legacyRight, userCrn.getAccountId()));
            }
        }
    }
}
