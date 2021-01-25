package com.sequenceiq.common.api.backup.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.backup.doc.BackupModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupCloudwatchParams implements Serializable {

    @ApiModelProperty
    @NotNull
    private String instanceProfile;

    @ApiModelProperty(BackupModelDescription.CLOUDWATCH_PARAMS_REGION)
    private String region;

    @ApiModelProperty(BackupModelDescription.CLOUDWATCH_PARAMS)
    private BackupCloudwatchStreamKey streamKey = BackupCloudwatchStreamKey.HOSTNAME;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    public BackupCloudwatchStreamKey getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(BackupCloudwatchStreamKey streamKey) {
        this.streamKey = streamKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @JsonIgnore
    public static BackupCloudwatchParams copy(BackupCloudwatchParams cloudwatchParams) {
        BackupCloudwatchParams newCloudwatchParams = null;
        if (cloudwatchParams != null) {
            newCloudwatchParams = new BackupCloudwatchParams();
            newCloudwatchParams.setStreamKey(cloudwatchParams.getStreamKey());
            newCloudwatchParams.setInstanceProfile(cloudwatchParams.getInstanceProfile());
            newCloudwatchParams.setRegion(cloudwatchParams.getRegion());
        }
        return newCloudwatchParams;
    }

    @Override
    public String toString() {
        return "CloudwatchParams{" +
                "instanceProfile='" + instanceProfile + '\'' +
                ", region='" + region + '\'' +
                ", streamKey=" + streamKey +
                '}';
    }
}
