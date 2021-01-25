package com.sequenceiq.it.cloudbreak.actor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

@Component
public class CloudbreakUserCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudbreakUserCache.class);

    private Map<String, List<CloudbreakUser>> usersByAccount;

    private boolean initialized;

    @Value("${integrationtest.user.mow.accountKey:default}")
    private String realUmsUserAccount;

    @Value("${integrationtest.user.mow.environmentKey:dev}")
    private String realUmsUserEnvironment;

    public CloudbreakUser getByDisplayName(String name) {
        return getByDisplayName(name, getRealUmsUserEnvironment(), getRealUmsUserAccount());
    }

    public CloudbreakUser getByDisplayName(String name, String environmentKey, String accountKey) {
        if (usersByAccount == null) {
            initUsers(environmentKey, accountKey);
        }
        CloudbreakUser user = usersByAccount.values().stream().flatMap(Collection::stream)
                .filter(u -> u.getDisplayName().equals(name)).findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no real ums user with name %s in account %s", name, accountKey)));
        LOGGER.info(" Real UMS user has been found in cache:: \nname: {} \ncrn: {} \naccessKey: {} \nsecretKey: {} \nadmin: {} ", user.getDisplayName(),
                user.getCrn(), user.getAccessKey(), user.getSecretKey(), user.getAdmin());
        return user;
    }

    public void setUsersByAccount(Map<String, List<CloudbreakUser>> users) {
        this.usersByAccount = users;
    }

    public Map<String, List<CloudbreakUser>> getUsersByAccount() {
        return usersByAccount;
    }

    public String getRealUmsUserAccount() {
        return realUmsUserAccount;
    }

    public void setRealUmsUserAccount(String realUmsUserAccountKey) {
        this.realUmsUserAccount = realUmsUserAccountKey;
    }

    public String getRealUmsUserEnvironment() {
        return realUmsUserEnvironment;
    }

    public void setRealUmsUserEnvironment(String realUmsUserEnvironmentKey) {
        this.realUmsUserEnvironment = realUmsUserEnvironmentKey;
    }

    public void initUsers(String environmentKey, String accountKey) {
        String userConfigPath = "ums-users/api-credentials.json";
        LOGGER.info("Real UMS environmentKey: {} and accountKey: {} at [{}] path", environmentKey, accountKey, userConfigPath);
        try {
            String accountId = null;
            List<CloudbreakUser> cloudbreakUsers = new ArrayList<CloudbreakUser>();
            JSONObject usersByEnvAndAcc = new JSONObject(FileReaderUtils.readFileFromClasspathQuietly(userConfigPath));
            JSONArray devEnvironment = usersByEnvAndAcc.getJSONArray(environmentKey);
            for (int i = 0; i < devEnvironment.length(); i++) {
                JSONObject jsonObject1 = (JSONObject) devEnvironment.get(i);
                JSONArray jsonarray1 = (JSONArray) jsonObject1.get(accountKey);
                for (int j = 0; j < jsonarray1.length(); j++) {
                    accountId = Crn.fromString(((JSONObject) jsonarray1.get(j)).getString("crn")).getAccountId();
                    String displayName = ((JSONObject) jsonarray1.get(j)).getString("displayName");
                    String desc = ((JSONObject) jsonarray1.get(j)).getString("desc");
                    String crn = ((JSONObject) jsonarray1.get(j)).getString("crn");
                    String accessKey = ((JSONObject) jsonarray1.get(j)).getString("accessKey");
                    String secretKey = ((JSONObject) jsonarray1.get(j)).getString("secretKey");
                    boolean admin = Boolean.parseBoolean(((JSONObject) jsonarray1.get(j)).getString("admin"));
                    cloudbreakUsers.add(new CloudbreakUser(accessKey, secretKey, displayName, crn, desc, admin));
                }
            }
            setUsersByAccount(Map.of(accountId, cloudbreakUsers));
            initialized = true;
        } catch (Exception e) {
            throw new TestFailException(e.getMessage(), e.getCause());
        }
        usersByAccount.values().stream().flatMap(Collection::stream).forEach(user -> {
            LOGGER.info(" Initialized real UMS user \nname: {} \ncrn: {} \naccessKey: {} \nsecretKey: {} \nadmin: {} ", user.getDisplayName(), user.getCrn(),
                    user.getAccessKey(), user.getSecretKey(), user.getAdmin());
            CloudbreakUser.validateRealUmsUser(user);
        });
    }

    public String getAdminAccessKeyByAccountId(String accountId) {
        return usersByAccount.get(accountId).stream().filter(CloudbreakUser::getAdmin).findFirst()
                .orElseThrow(() -> new TestFailException(String.format("There is no account admin test user for UMS account %s", accountId))).getAccessKey();
    }

    public boolean isInitialized() {
        return initialized;
    }
}