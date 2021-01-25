package com.sequenceiq.cloudbreak.authorization;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.sequenceiq.authorization.resource.AuthorizationFiltering;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.cloudbreak.domain.view.RecipeView;
import com.sequenceiq.cloudbreak.service.recipe.RecipeService;

@Component
public class RecipeFiltering implements AuthorizationFiltering<Set<RecipeView>> {

    public static final String WORKSPACE_ID = "WORKSPACE_ID";

    @Inject
    private RecipeService recipeService;

    @Override
    public List<AuthorizationResource> getAllResources(Map<String, Object> args) {
        return recipeService.findAsAuthorizationResourcesInWorkspace(getWorkspaceId(args));
    }

    @Override
    public Set<RecipeView> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return Sets.newLinkedHashSet(recipeService.findAllViewById(authorizedResourceIds));
    }

    @Override
    public Set<RecipeView> getAll(Map<String, Object> args) {
        return recipeService.findAllViewByWorkspaceId(getWorkspaceId(args));
    }

    private Long getWorkspaceId(Map<String, Object> params) {
        return (Long) params.get(WORKSPACE_ID);
    }
}
