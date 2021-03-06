package com.sequenceiq.cloudbreak.service.publicendpoint;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.certificate.PkiUtil;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.LoadBalancer;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.securityconfig.SecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.LoadBalancerPersistenceService;
import com.sequenceiq.common.api.type.LoadBalancerType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Service
public class GatewayPublicEndpointManagementService extends BasePublicEndpointManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GatewayPublicEndpointManagementService.class);

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private SecurityConfigService securityConfigService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private LoadBalancerPersistenceService loadBalancerPersistenceService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    public boolean isCertRenewalTriggerable(Stack stack) {
        return manageCertificateAndDnsInPem()
                && stack != null
                && stack.getCluster() != null;
    }

    public boolean generateCertAndSaveForStackAndUpdateDnsEntry(Stack stack) {
        boolean success = false;
        if (manageCertificateAndDnsInPem()
                && stack != null
                && stack.getCluster() != null) {
            if (StringUtils.isEmpty(stack.getSecurityConfig().getUserFacingCert())) {
                success = generateCertAndSaveForStack(stack);
            }
            updateDnsEntryForCluster(stack);
            updateDnsEntryForLoadBalancers(stack);
        } else {
            LOGGER.info("External FQDN and valid certificate creation is disabled.");
        }
        return success;
    }

    public String updateDnsEntry(Stack stack, String gatewayIp) {
        LOGGER.info("Update dns entry");
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

        if (StringUtils.isEmpty(gatewayIp)) {
            Optional<InstanceMetaData> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
            if (gateway.isEmpty()) {
                LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
                return null;
            } else {
                gatewayIp = gateway.get().getPublicIpWrapper();
            }
        }
        String endpointName = getEndpointNameForStack(stack);
        LOGGER.info("Creating DNS entry with endpoint name: '{}', environment name: '{}' and gateway IP: '{}'", endpointName, environment.getName(), gatewayIp);
        List<String> ips = List.of(gatewayIp);
        boolean success = getDnsManagementService().createOrUpdateDnsEntryWithIp(userCrn, accountId, endpointName, environment.getName(), false, ips);
        if (success) {
            try {
                String fullQualifiedDomainName = getDomainNameProvider()
                        .getFullyQualifiedEndpointName(endpointName, environment.getName(), getWorkloadSubdomain(userCrn));
                if (fullQualifiedDomainName != null) {
                    LOGGER.info("Dns entry updated: ip: {}, FQDN: {}", gatewayIp, fullQualifiedDomainName);
                    return fullQualifiedDomainName;
                }
            } catch (Exception e) {
                LOGGER.info("Cannot generate fqdn: {}", e.getMessage(), e);
            }
        }
        return null;
    }

    public void updateDnsEntryForLoadBalancers(Stack stack) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerWithEndpoint(stack);

        if (loadBalancerOptional.isEmpty()) {
            LOGGER.error("Unable find appropriate load balancer in stack. Load balancer public domain name will not be registered.");
        } else {
            LOGGER.info("Updating load balancer DNS entries");
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());

            LoadBalancer loadBalancer = loadBalancerOptional.get();
            String endpoint = loadBalancer.getEndpoint();
            boolean dnsRegistered = false;
            if (loadBalancer.getDns() != null && loadBalancer.getHostedZoneId() != null) {
                LOGGER.info("Creating load balancer DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'",
                    endpoint, environment.getName(), loadBalancer.getDns());
                dnsRegistered = getDnsManagementService().createOrUpdateDnsEntryWithCloudDns(userCrn, accountId, endpoint,
                    environment.getName(), loadBalancer.getDns(), loadBalancer.getHostedZoneId());
            } else if (loadBalancer.getIp() != null) {
                LOGGER.info("Creating load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                    endpoint, environment.getName(), loadBalancer.getIp());
                dnsRegistered = getDnsManagementService().createOrUpdateDnsEntryWithIp(userCrn, accountId, endpoint,
                    environment.getName(), false, List.of(loadBalancer.getIp()));
            } else {
                LOGGER.warn("Could not find IP or cloud DNS info for load balancer with endpoint {} ." +
                    "DNS registration will be skipped.", loadBalancer.getEndpoint());
            }

            if (dnsRegistered) {
                setLoadBalancerFqdn(loadBalancer, endpoint, environment.getName(), userCrn);
            }
        }
    }

    private void setLoadBalancerFqdn(LoadBalancer loadBalancer, String endpoint, String envName, String userCrn) {
        loadBalancer.setFqdn(getDomainNameProvider().getFullyQualifiedEndpointName(
            endpoint, envName, getWorkloadSubdomain(userCrn)));
        loadBalancerPersistenceService.save(loadBalancer);
        LOGGER.info("Set load balancer's FQDN to {}.", loadBalancer.getFqdn());
    }

    public String deleteDnsEntry(Stack stack, String environmentName) {
        String actorCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (StringUtils.isEmpty(environmentName)) {
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            environmentName = environment.getName();
        }
        Optional<InstanceMetaData> gateway = Optional.ofNullable(stack.getPrimaryGatewayInstance());
        if (!gateway.isPresent()) {
            LOGGER.info("No running gateway or all node is terminated, we skip the dns entry deletion.");
            return null;
        }
        String ip = gateway.get().getPublicIpWrapper();
        if (ip == null) {
            return null;
        }
        String endpointName = getEndpointNameForStack(stack);
        LOGGER.info("Deleting DNS entry with endpoint name: '{}', environment name: '{}' and gateway IP: '{}'", endpointName, environmentName, ip);
        getDnsManagementService().deleteDnsEntryWithIp(actorCrn, accountId, endpointName, environmentName, false, List.of(ip));
        return ip;
    }

    public void deleteLoadBalancerDnsEntry(Stack stack, String environmentName) {
        Optional<LoadBalancer> loadBalancerOptional = getLoadBalancerWithEndpoint(stack);

        if (loadBalancerOptional.isEmpty()) {
            LOGGER.error("Unable to find appropriate load balancer in stack. Load balancer public domain name will not be deleted.");
        } else {
            String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            if (StringUtils.isEmpty(environmentName)) {
                DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
                environmentName = environment.getName();
            }

            LoadBalancer loadBalancer = loadBalancerOptional.get();
            String endpoint = loadBalancer.getEndpoint();
            if (loadBalancer.getDns() != null && loadBalancer.getHostedZoneId() != null) {
                LOGGER.info("Deleting load balancer DNS entry with endpoint name: '{}', environment name: '{}' and cloud DNS: '{}'",
                    endpoint, environmentName, loadBalancer.getDns());
                getDnsManagementService().deleteDnsEntryWithCloudDns(userCrn, accountId, endpoint,
                    environmentName, loadBalancer.getDns(), loadBalancer.getHostedZoneId());
            } else if (loadBalancer.getIp() != null) {
                LOGGER.info("Deleting load balancer DNS entry with endpoint name: '{}', environment name: '{}' and IP: '{}'",
                    endpoint, environmentName, loadBalancer.getIp());
                getDnsManagementService().deleteDnsEntryWithIp(userCrn, accountId, endpoint,
                    environmentName, false, List.of(loadBalancer.getIp()));
            }
        }
    }

    public boolean renewCertificate(Stack stack) {
        boolean result = true;
        if (isCertRenewalTriggerable(stack)) {
            LOGGER.info("Renew certificate for stack: '{}'", stack.getName());
            result = generateCertAndSaveForStack(stack);
            if (StringUtils.isEmpty(stack.getCluster().getFqdn())) {
                LOGGER.info("The cluster doesn't have public DNS entry, starting to create one.");
                updateDnsEntryForCluster(stack);
            }
        }
        return result;
    }

    private boolean generateCertAndSaveForStack(Stack stack) {
        boolean result = false;
        LOGGER.info("Acquire certificate from PEM service and save for stack");
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        SecurityConfig securityConfig = stack.getSecurityConfig();
        try {
            KeyPair keyPair = getKeyPairForStack(stack);
            String endpointName = getEndpointNameForStack(stack);
            Set<String> loadBalancerEndpoints = getLoadBalancerNamesForStack(stack);
            DetailedEnvironmentResponse environment = environmentClientService.getByCrn(stack.getEnvironmentCrn());
            String environmentName = environment.getName();
            String workloadSubdomain = getWorkloadSubdomain(userCrn);

            String commonName = getDomainNameProvider().getCommonName(endpointName, environmentName, workloadSubdomain);
            String fullyQualifiedEndpointName = getDomainNameProvider().getFullyQualifiedEndpointName(endpointName, environmentName, workloadSubdomain);
            List<String> subjectAlternativeNames = new ArrayList<>();
            subjectAlternativeNames.add(commonName);
            subjectAlternativeNames.add(fullyQualifiedEndpointName);
            for (String loadBalancerEndpoint : loadBalancerEndpoints) {
                String loadBalancerEndpointName = getDomainNameProvider().getFullyQualifiedEndpointName(
                    loadBalancerEndpoint, environmentName, workloadSubdomain);
                subjectAlternativeNames.add(loadBalancerEndpointName);
            }

            LOGGER.info("Acquiring certificate with common name:{} and SANs: {}", commonName, String.join(",", subjectAlternativeNames));
            PKCS10CertificationRequest csr = PkiUtil.csr(keyPair, commonName, subjectAlternativeNames);
            List<String> certs = getCertificateCreationService().create(userCrn, accountId, endpointName, environmentName, csr);
            securityConfig.setUserFacingCert(String.join("", certs));
            securityConfigService.save(securityConfig);
            result = true;
        } catch (Exception e) {
            LOGGER.info("The certification could not be generated by Public Endpoint Management service: " + e.getMessage(), e);
        }
        return result;
    }

    private KeyPair getKeyPairForStack(Stack stack) {
        KeyPair keyPair;
        SecurityConfig securityConfig = stack.getSecurityConfig();
        if (StringUtils.isEmpty(securityConfig.getUserFacingKey())) {
            keyPair = PkiUtil.generateKeypair();
            securityConfig.setUserFacingKey(PkiUtil.convert(keyPair.getPrivate()));
            securityConfigService.save(securityConfig);
        } else {
            keyPair = PkiUtil.fromPrivateKeyPem(securityConfig.getUserFacingKey());
            if (keyPair == null) {
                keyPair = PkiUtil.generateKeypair();
            }
        }
        return keyPair;
    }

    private void updateDnsEntryForCluster(Stack stack) {
        String fqdn = updateDnsEntry(stack, null);
        if (fqdn != null) {
            Cluster cluster = stack.getCluster();
            cluster.setFqdn(fqdn);
            clusterService.save(cluster);
            LOGGER.info("The '{}' domain name has been generated, registered through PEM service and saved for the cluster.", fqdn);
        }
    }

    private String getEndpointNameForStack(Stack stack) {
        return stack.getPrimaryGatewayInstance().getShortHostname();
    }

    private Set<String> getLoadBalancerNamesForStack(Stack stack) {
        return loadBalancerPersistenceService.findByStackId(stack.getId()).stream()
            .map(LoadBalancer::getEndpoint).collect(Collectors.toSet());
    }

    private Optional<LoadBalancer> getLoadBalancerWithEndpoint(Stack stack) {
        Optional<LoadBalancer> loadBalancerOptional;
        Set<LoadBalancer> loadBalancers = loadBalancerPersistenceService.findByStackId(stack.getId());
        if (loadBalancers.isEmpty()) {
            LOGGER.info("No load balancers in stack {}", stack.getId());
            loadBalancerOptional = Optional.empty();
        } else {
            loadBalancerOptional = loadBalancerConfigService.selectLoadBalancer(loadBalancers, LoadBalancerType.PUBLIC);

            if (loadBalancerOptional.isEmpty()) {
                LOGGER.error("Unable to determine load balancer type. Load balancer public domain name will not be registered.");
            } else if (Strings.isNullOrEmpty(loadBalancerOptional.get().getEndpoint())) {
                LOGGER.error("No endpoint set for load balancer. Can't register domain.");
                loadBalancerOptional = Optional.empty();
            } else {
                LOGGER.debug("Found load balancer {}", loadBalancerOptional.get().getEndpoint());
            }
        }

        return loadBalancerOptional;
    }
}
