package com.sequenceiq.it.cloudbreak.actor;

import java.util.Base64;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class CloudbreakActor extends CloudbreakUserCache implements Actor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakActor.class);

    @Inject
    private TestParameter testParameter;

    @Override
    public CloudbreakUser defaultUser() {
        return new CloudbreakUser(testParameter.get(CloudbreakTest.ACCESS_KEY), testParameter.get(CloudbreakTest.SECRET_KEY));
    }

    @Override
    public CloudbreakUser secondUser() {
        String secondaryAccessKey = testParameter.get(CloudbreakTest.SECONDARY_ACCESS_KEY);
        String secondarySecretKey = testParameter.get(CloudbreakTest.SECONDARY_SECRET_KEY);
        if (StringUtils.hasLength(secondaryAccessKey)) {
            throw new IllegalStateException("Add a secondary accessKey to the test: integrationtest.cb.secondary.accesskey");
        }
        if (StringUtils.hasLength(secondarySecretKey)) {
            throw new IllegalStateException("Add a secondary secretKey to the test: integrationtest.cb.secondary.secretkey");
        }
        return new CloudbreakUser(testParameter.get(CloudbreakTest.SECONDARY_ACCESS_KEY), testParameter.get(CloudbreakTest.SECONDARY_SECRET_KEY));
    }

    @Override
    public CloudbreakUser create(String tenantName, String username) {
        String secretKey = testParameter.get(CloudbreakTest.SECRET_KEY);
        String crn = String.format("crn:cdp:iam:us-west-1:%s:user:%s", tenantName, username);
        String accessKey = Base64.getEncoder().encodeToString(crn.getBytes());
        return new CloudbreakUser(accessKey, secretKey, username + " at tenant " + tenantName);
    }

    @Override
    public CloudbreakUser useRealUmsUser(String key) {
        LOGGER.info("Getting the requested real UMS user by key: {}", key);
        return getByDisplayName(key);
    }

    public CloudbreakUser useRealUmsUser(String key, String environmentKey, String accountKey) {
        LOGGER.info("Getting the requested real UMS user by key: {} and environment: {} and account: {}", key, environmentKey, accountKey);
        return getByDisplayName(key, environmentKey, accountKey);
    }

    private void checkNonEmpty(String name, String value) {
        if (StringUtils.hasLength(value)) {
            throw new TestFailException(String.format("Following variable must be set whether as environment variables or (test) application.yml: %s",
                    name.replaceAll("\\.", "_").toUpperCase()));
        }
    }
}