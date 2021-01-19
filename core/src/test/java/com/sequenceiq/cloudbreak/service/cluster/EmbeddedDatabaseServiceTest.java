package com.sequenceiq.cloudbreak.service.cluster;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class EmbeddedDatabaseServiceTest {
    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:test@cloudera.com";

    private static final String CLOUDPLATFORM = "cloudplatform";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @InjectMocks
    private EmbeddedDatabaseService underTest;

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabled() {
        // GIVEN
        Stack stack = createStack(1);
        Mockito.when(entitlementService.embeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(true);
        // WHEN
        boolean actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID, stack, null));
        // THEN
        assertTrue(actualResult);
    }

    @Test
    public void testIsEmbeddedDatabaseOnAttachedDiskEnabledWhenAttachedDiskEntitlementIsEnabledButNoDisksAttachedSupported() {
        // GIVEN
        Stack stack = createStack(0);
        Mockito.when(entitlementService.embeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID)).thenReturn(true);
        Mockito.when(cloudParameterCache.isVolumeAttachmentSupported(CLOUDPLATFORM)).thenReturn(false);
        // WHEN
        boolean actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.isEmbeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID, stack, null));
        // THEN
        assertFalse(actualResult);
    }
/*
    @Test
    public void testGetEmbeddedDatabaseInfoWithAttachmentEnabledWhenAttachedDiskEntitlementIsEnabledButNoTemplateProvided() {
        // GIVEN
        Stack stack = createStackWithoutTemplate();
        Mockito.when(entitlementService.embeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID)).thenReturn(true);
        // WHEN
        EmbeddedDatabaseInfo actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEmbeddedDatabaseInfo(ACCOUNT_ID, stack, null));
        // THEN
        assertFalse(actualResult.isEmbeddedDatabaseOnAttachedDiskEnabled());
        assertEquals(0, actualResult.getAttachedDisksCount());
    }

    @Test
    public void testGetEmbeddedDatabaseInfoWithAttachmentEnabledWhenAttachedDiskEntitlementIsDisabled() {
        // GIVEN
        Stack stack = createStack(5);
        Mockito.when(entitlementService.embeddedDatabaseOnAttachedDiskEnabled(ACCOUNT_ID)).thenReturn(false);
        // WHEN
        EmbeddedDatabaseInfo actualResult = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEmbeddedDatabaseInfo(ACCOUNT_ID, stack, null));
        // THEN
        assertFalse(actualResult.isEmbeddedDatabaseOnAttachedDiskEnabled());
        assertEquals(0, actualResult.getAttachedDisksCount());
    }
*/
    private Stack createStack(int volumeCount) {
        Stack stack = new Stack();
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        Template template = new Template();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        volumeTemplate.setVolumeCount(volumeCount);
        template.setVolumeTemplates(Set.of(volumeTemplate));
        masterGroup.setTemplate(template);
        stack.setInstanceGroups(Set.of(masterGroup));
        stack.setCloudPlatform(CLOUDPLATFORM);
        return stack;
    }

    private Stack createStackWithoutTemplate() {
        Stack stack = new Stack();
        InstanceGroup masterGroup = new InstanceGroup();
        masterGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceGroup(masterGroup);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        masterGroup.setInstanceMetaData(Set.of(instanceMetaData));
        stack.setInstanceGroups(Set.of(masterGroup));
        return stack;
    }
}
