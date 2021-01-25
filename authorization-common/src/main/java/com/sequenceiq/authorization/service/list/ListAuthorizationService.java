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
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationFiltering;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.InternalCrnBuilder;

@Service
public class ListAuthorizationService {

    private static final String NO_RIGHT_IN_ACCOUNT = "You have no right to perform %s in account %s.";

    private static final ThreadLocal<Object> AUTHORIZED_DATA = new ThreadLocal<>();

    @Inject
    private Map<Class<AuthorizationFiltering<?>>, AuthorizationFiltering<?>> listResourceProviders;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private ListParamsUtil listParamsUtil;

    public <T> T getResultAs() {
        return (T) AUTHORIZED_DATA.get();
    }

    public Object filterList(FilterListBasedOnPermissions annotation, Crn userCrn, ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature,
            Optional<String> requestId) {
        AuthorizationResourceAction action = annotation.action();
        AuthorizationFiltering<?> authorizationFiltering = listResourceProviders.get(annotation.filter());
        Object authorizedData;
        Map<String, Object> arguments = listParamsUtil.getFilterParams(methodSignature.getMethod(), proceedingJoinPoint);
        if (InternalCrnBuilder.isInternalCrn(userCrn)) {
            authorizedData = authorizationFiltering.getAll(arguments);
        } else {
            if (entitlementService.listFilteringEnabled(userCrn.getAccountId())) {
                List<AuthorizationResource> resources = authorizationFiltering.getAllResources(arguments);
                List<Long> authorizedResourceIds = getAuthorizedResourceIds(userCrn, action, resources, requestId);
                authorizedData = authorizationFiltering.filterByIds(authorizedResourceIds, arguments);
            } else {
                checkAccountRight(userCrn, action, requestId);
                authorizedData = authorizationFiltering.getAll(arguments);
            }
        }
        try {
            AUTHORIZED_DATA.set(authorizedData);
            return proceedingJoinPoint.proceed();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            AUTHORIZED_DATA.remove();
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
