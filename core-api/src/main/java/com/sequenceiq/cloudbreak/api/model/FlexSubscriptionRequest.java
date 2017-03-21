package com.sequenceiq.cloudbreak.api.model;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions;
import com.sequenceiq.cloudbreak.doc.ModelDescriptions.FlexSubscriptionModelDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlexSubscriptionRequest implements JsonEntity {

    @NotNull
    @ApiModelProperty(value = ModelDescriptions.NAME, required = true)
    private String name;

    @ApiModelProperty(value = FlexSubscriptionModelDescription.FLEX_SUBSCRIPTION_ID, readOnly = true)
    @Pattern(regexp = "^(FLEX-[0-9]{10}$)", message = "The given Flex subscription id is not valid!")
    private String subscriptionId;

    @ApiModelProperty(value = FlexSubscriptionModelDescription.SMARTSENSE_SUBSCRIPTION_ID, readOnly = true)
    private Long smartSenseSubscriptionId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public Long getSmartSenseSubscriptionId() {
        return smartSenseSubscriptionId;
    }

    public void setSmartSenseSubscriptionId(Long smartSenseSubscriptionId) {
        this.smartSenseSubscriptionId = smartSenseSubscriptionId;
    }

}
