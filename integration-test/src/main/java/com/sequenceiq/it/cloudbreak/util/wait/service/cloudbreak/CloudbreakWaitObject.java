package com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.ENABLE_SECURITY_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_CREATION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.EXTERNAL_DATABASE_DELETION_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.PRE_DELETE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitObject;

public class CloudbreakWaitObject implements WaitObject {

    private static final Map<String, Status> STACK_DELETED = Map.of(STATUS, Status.DELETE_COMPLETED);

    private static final Map<String, Status> STACK_FAILED = Map.of(STATUS, Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    private final CloudbreakClient client;

    private final String name;

    private final Map<String, Status> desiredStatuses;

    private final String accountId;

    private StackStatusV4Response stackStatus;

    public CloudbreakWaitObject(CloudbreakClient client, String name, Map<String, Status> desiredStatuses, String accountId) {
        this.client = client;
        this.name = name;
        this.desiredStatuses = desiredStatuses;
        this.accountId = accountId;
    }

    public DistroXV1Endpoint getDistroxEndpoint() {
        return client.getCloudbreakClient().distroXV1Endpoint();
    }

    public StackV4Endpoint getStackEndpoint() {
        return client.getCloudbreakClient().stackV4Endpoint();
    }

    public Long getWorkspaceId() {
        return client.getWorkspaceId();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isDeleted() {
        Map<String, String> deletedStatuses = Map.of(STATUS, DELETE_COMPLETED.name(), "clusterStatus", DELETE_COMPLETED.name());
        return deletedStatuses.equals(actualStatuses());
    }

    @Override
    public boolean isDeletionInProgress() {
        Set<Status> deleteInProgressStatuses = Set.of(PRE_DELETE_IN_PROGRESS, DELETE_IN_PROGRESS, EXTERNAL_DATABASE_DELETION_IN_PROGRESS);
        return !Sets.intersection(Set.of(actualStatuses().values()), deleteInProgressStatuses).isEmpty();
    }

    @Override
    public boolean isCreateFailed() {
        List<Status> actualStatuses = new ArrayList<>(actualStatusesEnum().values());
        return actualStatuses.contains(CREATE_FAILED);
    }

    @Override
    public boolean isDeletionCheck() {
        return desiredStatuses.equals(STACK_DELETED);
    }

    @Override
    public boolean isFailedCheck() {
        return desiredStatuses.equals(STACK_FAILED);
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public boolean isFailed() {
        Set<Status> failedStatuses = Set.of(UPDATE_FAILED, CREATE_FAILED, ENABLE_SECURITY_FAILED, DELETE_FAILED, START_FAILED, STOP_FAILED,
                EXTERNAL_DATABASE_CREATION_FAILED, EXTERNAL_DATABASE_DELETION_FAILED);
        return !Sets.intersection(Set.of(actualStatusesEnum().values()), failedStatuses).isEmpty();
    }

    @Override
    public void fetchData() {
        stackStatus = getStackEndpoint().getStatusByName(getWorkspaceId(), name, getAccountId());
    }

    @Override
    public boolean isDeleteFailed() {
        List<String> actualStatuses = new ArrayList<>(actualStatuses().values());
        return actualStatuses.contains(DELETE_FAILED.name());
    }

    @Override
    public Map<String, String> actualStatuses() {
        if (stackStatus == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, stackStatus.getStatus().name(), "clusterStatus", stackStatus.getClusterStatus().name());
    }

    private Map<String, Status> actualStatusesEnum() {
        if (stackStatus == null) {
            return Collections.emptyMap();
        }
        return Map.of(STATUS, stackStatus.getStatus(), "clusterStatus", stackStatus.getClusterStatus());
    }

    @Override
    public Map<String, String> actualStatusReason() {
        return Map.of(STATUS_REASON, stackStatus.getStatusReason(), "clusterStatusReason", stackStatus.getClusterStatusReason());
    }

    @Override
    public Map<String, String> getDesiredStatuses() {
        Map<String, String> ret = new HashMap<>();
        desiredStatuses.forEach((key, value) -> ret.put(key, value.name()));
        return ret;
    }
}
