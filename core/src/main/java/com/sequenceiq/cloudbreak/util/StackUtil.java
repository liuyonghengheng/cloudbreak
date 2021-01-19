package com.sequenceiq.cloudbreak.util;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.time.Duration;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.gs.collections.impl.tuple.AbstractImmutableEntry;
import com.gs.collections.impl.tuple.ImmutableEntry;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes.Volume;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeVolumes;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;

@Service
public class StackUtil {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private CredentialToCloudCredentialConverter credentialConverter;

    @Inject
    private CredentialClientService credentialClientService;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    public Set<Node> collectNodes(Stack stack) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectReachableNodes(Stack stack) {
        return stack.getInstanceGroups()
                .stream()
                .filter(ig -> ig.getNodeCount() != 0)
                .flatMap(ig -> ig.getReachableInstanceMetaDataSet().stream())
                .filter(im -> im.getDiscoveryFQDN() != null)
                .map(im -> {
                    String instanceId = im.getInstanceId();
                    String instanceType = im.getInstanceGroup().getTemplate().getInstanceType();
                    return new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                            im.getDiscoveryFQDN(), im.getInstanceGroupName());
                })
                .collect(Collectors.toSet());
    }

    public Set<Node> collectNodesFromHostnames(Stack stack, Set<String> hostnames) {
        Set<Node> agents = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getReachableInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && hostnames.contains(im.getDiscoveryFQDN())) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), im.getInstanceGroupName()));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectNodesWithDiskData(Stack stack) {
        Set<Node> agents = new HashSet<>();
        List<Resource> volumeSets = stack.getDiskResources();
        Map<String, Map<String, Object>> instanceToVolumeInfoMap = createInstanceToVolumeInfoMap(volumeSets);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        String dataVolumes = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("dataVolumes", "");
                        String serialIds = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("serialIds", "");
                        String fstab = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("fstab", "");
                        String uuids = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("uuids", "");
                        Integer dataBaseVolumeIndex = (Integer) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of())
                                .getOrDefault("dataBaseVolumeIndex", -1);
                        NodeVolumes nodeVolumes = new NodeVolumes(dataBaseVolumeIndex, dataVolumes, serialIds, fstab, uuids);
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType,
                                im.getDiscoveryFQDN(), im.getInstanceGroupName(), nodeVolumes));
                    }
                }
            }
        }
        return agents;
    }

    public Set<Node> collectNewNodesWithDiskData(Stack stack, Set<String> newNodeAddresses) {
        Set<Node> agents = new HashSet<>();
        List<Resource> volumeSets = stack.getDiskResources();
        Map<String, Map<String, Object>> instanceToVolumeInfoMap = createInstanceToVolumeInfoMap(volumeSets);
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            if (instanceGroup.getNodeCount() != 0) {
                for (InstanceMetaData im : instanceGroup.getNotDeletedInstanceMetaDataSet()) {
                    if (im.getDiscoveryFQDN() != null && newNodeAddresses.contains(im.getPrivateIp())) {
                        String instanceId = im.getInstanceId();
                        String instanceType = instanceGroup.getTemplate().getInstanceType();
                        String dataVolumes = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("dataVolumes", "");
                        String serialIds = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("serialIds", "");
                        String fstab = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("fstab", "");
                        String uuids = (String) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of()).getOrDefault("uuids", "");
                        Integer databaseVolumeIndex = (Integer) instanceToVolumeInfoMap.getOrDefault(instanceId, Map.of())
                                .getOrDefault("dataBaseVolumeIndex", -1);
                        NodeVolumes nodeVolumes = new NodeVolumes(databaseVolumeIndex, dataVolumes, serialIds, fstab, uuids);
                        agents.add(new Node(im.getPrivateIp(), im.getPublicIp(), instanceId, instanceType, im.getDiscoveryFQDN(), im.getInstanceGroupName(),
                                nodeVolumes));
                    }
                }
            }
        }
        return agents;
    }

    private Map<String, Map<String, Object>> createInstanceToVolumeInfoMap(List<Resource> volumeSets) {
        return volumeSets.stream()
                .map(volumeSet -> new ImmutableEntry<>(volumeSet.getInstanceId(),
                        resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class)))
                .map(entry -> {
                    List<Volume> volumes = entry.getValue().map(VolumeSetAttributes::getVolumes).orElse(List.of());
                    List<String> dataVolumes = volumes.stream().map(Volume::getDevice).collect(Collectors.toList());
                    List<String> serialIds = volumes.stream().map(Volume::getId).collect(Collectors.toList());
                    int dataBaseVolumeIndex = IntStream.range(0, volumes.size())
                            .filter(index -> volumes.get(index).getCloudVolumeUsageType() == CloudVolumeUsageType.DATABASE)
                            .findFirst()
                            .orElse(-1);
                    return new ImmutableEntry<String, Map<String, Object>>(entry.getKey(), Map.of(
                            "dataVolumes", String.join(" ", dataVolumes),
                            "serialIds", String.join(" ", serialIds),
                            "dataBaseVolumeIndex", dataBaseVolumeIndex,
                            "fstab", entry.getValue().map(VolumeSetAttributes::getFstab).orElse(""),
                            "uuids", entry.getValue().map(VolumeSetAttributes::getUuids).orElse("")));
                })
                .collect(Collectors.toMap(AbstractImmutableEntry::getKey, AbstractImmutableEntry::getValue));
    }

    private List<String> collectMappedVolumes(List<Volume> volumes, Predicate<Volume> volumeFilter, Function<Volume, String> volumeMapper) {
        return volumes.stream().filter(volumeFilter).map(volumeMapper).collect(Collectors.toList());
    }

    private Predicate<Volume> filterVolumesByUsageType(CloudVolumeUsageType volumeUsageType) {
        return (Volume volume) -> volume.getCloudVolumeUsageType() == volumeUsageType;
    }

    public String extractClusterManagerIp(StackView stackView) {
        return extractClusterManagerIp(stackView.getId());
    }

    public String extractClusterManagerIp(Stack stack) {
        if (!isEmpty(stack.getClusterManagerIp())) {
            return stack.getClusterManagerIp();
        }
        return extractClusterManagerIp(stack.getId());
    }

    private String extractClusterManagerIp(long stackId) {
        AtomicReference<String> result = new AtomicReference<>(null);
        instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stackId).ifPresent(imd -> result.set(imd.getPublicIpWrapper()));
        return result.get();
    }

    public String extractClusterManagerAddress(Stack stack) {
        String fqdn = loadBalancerConfigService.getLoadBalancerUserFacingFQDN(stack.getId());
        fqdn = isEmpty(fqdn) ? stack.getFqdn() : fqdn;

        if (isNotEmpty(fqdn)) {
            return fqdn;
        }

        String clusterManagerIp = stack.getClusterManagerIp();

        if (isNotEmpty(clusterManagerIp)) {
            return clusterManagerIp;
        }

        return extractClusterManagerIp(stack.getId());
    }

    public long getUptimeForCluster(Cluster cluster, boolean addUpsinceToUptime) {
        Duration uptime = Duration.ZERO;
        if (StringUtils.isNotBlank(cluster.getUptime())) {
            uptime = Duration.parse(cluster.getUptime());
        }
        if (cluster.getUpSince() != null && addUpsinceToUptime) {
            long now = new Date().getTime();
            uptime = uptime.plusMillis(now - cluster.getUpSince());
        }
        return uptime.toMillis();
    }

    public CloudCredential getCloudCredential(Stack stack) {
        Credential credential = credentialClientService.getByEnvironmentCrn(stack.getEnvironmentCrn());
        return credentialConverter.convert(credential);
    }
}
