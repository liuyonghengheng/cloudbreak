package com.sequenceiq.authorization.config;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.Maps;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ListResourceProvider;
import com.sequenceiq.authorization.service.ResourceBasedCrnProvider;
import com.sequenceiq.authorization.service.defaults.DefaultResourceChecker;

@Configuration
public class AuthorizationConfig {

    @Inject
    private Optional<List<DefaultResourceChecker>> defaultResourceCheckers;

    @Inject
    private Optional<List<ResourceBasedCrnProvider>> resourceBasedCrnProviders;

    @Inject
    private Optional<List<ListResourceProvider<?>>> listResourceProviders;

    @Bean
    public Map<AuthorizationResourceType, DefaultResourceChecker> defaultResourceCheckerMap() {
        if (defaultResourceCheckers.isPresent()) {
            Map<AuthorizationResourceType, DefaultResourceChecker> bean = new EnumMap<>(AuthorizationResourceType.class);
            for (DefaultResourceChecker defaultResourceChecker : defaultResourceCheckers.get()) {
                bean.put(defaultResourceChecker.getResourceType(), defaultResourceChecker);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<AuthorizationResourceType, ResourceBasedCrnProvider> resourceBasedCrnProviderMap() {
        if (resourceBasedCrnProviders.isPresent()) {
            Map<AuthorizationResourceType, ResourceBasedCrnProvider> bean = new EnumMap<>(AuthorizationResourceType.class);
            for (ResourceBasedCrnProvider resourceBasedCrnProvider : resourceBasedCrnProviders.get()) {
                bean.put(resourceBasedCrnProvider.getResourceType(), resourceBasedCrnProvider);
            }
            return Maps.immutableEnumMap(bean);
        } else {
            return Map.of();
        }
    }

    @Bean
    public Map<Class<ListResourceProvider<?>>, ListResourceProvider<?>> listResourceProviders() {
        if (listResourceProviders.isPresent()) {
            Map<Class<ListResourceProvider<?>>, ListResourceProvider<?>> bean = new HashMap<>();
            for (ListResourceProvider<?> listResourceProvider : listResourceProviders.get()) {
                bean.put((Class<ListResourceProvider<?>>) listResourceProvider.getClass(), listResourceProvider);
            }
            return Map.copyOf(bean);
        } else {
            return Map.of();
        }
    }
}
