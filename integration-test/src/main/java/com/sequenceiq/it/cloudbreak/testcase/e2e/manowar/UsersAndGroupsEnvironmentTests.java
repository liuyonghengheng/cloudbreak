package com.sequenceiq.it.cloudbreak.testcase.e2e.manowar;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class UsersAndGroupsEnvironmentTests extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
//        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
        createEnvironmentWithNetwork(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @UseSpotInstances
    @Description(
            given = "there is a running environment",
            when = "add freeIpa to the running environment then synchronize all users",
            then = "freeIpa should be created then synchronized successfully at environment")
    public void testCreateNewFreeIpaThenSyncronize(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.describeUserSync())
                .await(OperationState.COMPLETED)
                .when(freeIpaTestClient.sync())
                .await(OperationState.COMPLETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT, enabled = false)
    @UseSpotInstances
    @Description(
            given = "there is a running environment",
            when = "add freeIpa to the running environment then syncornize all users",
            then = "freeIpa should be created then syncronized successfully at environment")
    public void testGetUsersFromFreeIpa(TestContext testContext) {
        String freeIpa = resourcePropertyProvider().getName();

        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(freeIpa, FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create(), key(freeIpa))
                .await(AVAILABLE)
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.describeLastSync())
                .await(OperationState.COMPLETED)
                .when(freeIpaTestClient.sync())
                .await(OperationState.COMPLETED)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.ENV_ADMIN_A))
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.sync())
                .await(OperationState.COMPLETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running environment",
            when = "add freeIpa to the running environment then syncornize all users",
            then = "freeIpa should be created then syncronized successfully at environment")
    public void testCreateFullEnvironment(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(FreeIpaTestDto.class)
                .withTelemetry("telemetry")
                .when(freeIpaTestClient.create())
                .await(AVAILABLE)
                .given(SdxTestDto.class)
                .withCloudStorage()
                .when(sdxTestClient.create())
                .await(SdxClusterStatusResponse.RUNNING)
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .await(STACK_AVAILABLE)
                .validate();
    }
}
