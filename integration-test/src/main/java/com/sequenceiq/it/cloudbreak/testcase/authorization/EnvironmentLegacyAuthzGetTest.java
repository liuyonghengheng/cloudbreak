package com.sequenceiq.it.cloudbreak.testcase.authorization;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;

public class EnvironmentLegacyAuthzGetTest extends AbstractMockTest {

    private static final String REAL_UMS_ACCOUNT_KEY = "legacy";

    private static final String REAL_UMS_ENVIRONMENT_KEY = "dev";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.LEGACY_NON_POWER, REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY);
        useRealUmsUser(testContext, AuthUserKeys.LEGACY_POWER, REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY);
        useRealUmsUser(testContext, AuthUserKeys.LEGACY_ACC_ENV_ADMIN, REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironment(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.LEGACY_NON_POWER);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.LEGACY_ACC_ENV_ADMIN,
                        REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY)))
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(false)
                .when(environmentTestClient.create(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.LEGACY_ACC_ENV_ADMIN,
                        REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY)))
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .given(FreeIpaTestDto.class)
                .withCatalog(getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIpaTestClient.create(), RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.LEGACY_ACC_ENV_ADMIN,
                        REAL_UMS_ENVIRONMENT_KEY, REAL_UMS_ACCOUNT_KEY)))
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.describe())
                .validate();
    }
}
