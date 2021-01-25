package com.sequenceiq.cloudbreak.cloud.aws.client;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.ResponseMetadata;
import com.amazonaws.services.elasticfilesystem.AmazonElasticFileSystemClient;
import com.amazonaws.services.elasticfilesystem.model.CreateFileSystemRequest;
import com.amazonaws.services.elasticfilesystem.model.CreateFileSystemResult;
import com.amazonaws.services.elasticfilesystem.model.CreateMountTargetRequest;
import com.amazonaws.services.elasticfilesystem.model.CreateMountTargetResult;
import com.amazonaws.services.elasticfilesystem.model.CreateTagsRequest;
import com.amazonaws.services.elasticfilesystem.model.CreateTagsResult;
import com.amazonaws.services.elasticfilesystem.model.DeleteFileSystemRequest;
import com.amazonaws.services.elasticfilesystem.model.DeleteFileSystemResult;
import com.amazonaws.services.elasticfilesystem.model.DeleteMountTargetRequest;
import com.amazonaws.services.elasticfilesystem.model.DeleteMountTargetResult;
import com.amazonaws.services.elasticfilesystem.model.DeleteTagsRequest;
import com.amazonaws.services.elasticfilesystem.model.DeleteTagsResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeFileSystemsResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeLifecycleConfigurationRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeLifecycleConfigurationResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeMountTargetSecurityGroupsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeMountTargetSecurityGroupsResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeMountTargetsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeMountTargetsResult;
import com.amazonaws.services.elasticfilesystem.model.DescribeTagsRequest;
import com.amazonaws.services.elasticfilesystem.model.DescribeTagsResult;
import com.amazonaws.services.elasticfilesystem.model.ModifyMountTargetSecurityGroupsRequest;
import com.amazonaws.services.elasticfilesystem.model.ModifyMountTargetSecurityGroupsResult;
import com.amazonaws.services.elasticfilesystem.model.PutLifecycleConfigurationRequest;
import com.amazonaws.services.elasticfilesystem.model.PutLifecycleConfigurationResult;
import com.amazonaws.services.elasticfilesystem.model.UpdateFileSystemRequest;
import com.amazonaws.services.elasticfilesystem.model.UpdateFileSystemResult;
import com.sequenceiq.cloudbreak.service.Retry;

public class AmazonEfsRetryClient extends AmazonRetryClient {
    private final AmazonElasticFileSystemClient client;

    private final Retry retry;

    public AmazonEfsRetryClient(AmazonElasticFileSystemClient client, Retry retry) {
        this.client = client;
        this.retry = retry;
    }

    public CreateFileSystemResult createFileSystem(CreateFileSystemRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.createFileSystem(request)));
    }

    public CreateMountTargetResult createMountTarget(CreateMountTargetRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.createMountTarget(request)));
    }

    public CreateTagsResult createTags(CreateTagsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.createTags(request)));
    }

    public DeleteFileSystemResult deleteFileSystem(DeleteFileSystemRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.deleteFileSystem(request)));
    }

    public DeleteMountTargetResult deleteMountTarget(DeleteMountTargetRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.deleteMountTarget(request)));
    }

    public DeleteTagsResult deleteTags(DeleteTagsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.deleteTags(request)));
    }

    public DescribeFileSystemsResult describeFileSystems(DescribeFileSystemsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeFileSystems(request)));
    }

    public DescribeFileSystemsResult describeFileSystems() {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeFileSystems()));
    }

    public DescribeLifecycleConfigurationResult describeLifecycleConfiguration(DescribeLifecycleConfigurationRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeLifecycleConfiguration(request)));
    }

    public DescribeMountTargetSecurityGroupsResult describeMountTargetSecurityGroups(DescribeMountTargetSecurityGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeMountTargetSecurityGroups(request)));
    }

    public DescribeMountTargetsResult describeMountTargets(DescribeMountTargetsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeMountTargets(request)));
    }

    public DescribeTagsResult describeTags(DescribeTagsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.describeTags(request)));
    }

    public ModifyMountTargetSecurityGroupsResult modifyMountTargetSecurityGroups(ModifyMountTargetSecurityGroupsRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.modifyMountTargetSecurityGroups(request)));
    }

    public PutLifecycleConfigurationResult putLifecycleConfiguration(PutLifecycleConfigurationRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.putLifecycleConfiguration(request)));
    }

    public UpdateFileSystemResult updateFileSystem(UpdateFileSystemRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.updateFileSystem(request)));
    }

    public ResponseMetadata getCachedResponseMetadata(AmazonWebServiceRequest request) {
        return retry.testWith2SecDelayMax15Times(() -> mapThrottlingError(() -> client.getCachedResponseMetadata(request)));
    }
}
