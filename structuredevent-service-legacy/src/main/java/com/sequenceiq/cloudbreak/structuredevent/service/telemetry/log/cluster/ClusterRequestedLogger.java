package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.cluster;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FAILED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_FINISHED;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterStatus.Value.CREATE_STARTED;

import java.util.EnumSet;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter.StructuredFlowEventToCDPDatalakeRequestedConverter;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.log.LegacyTelemetryEventLogger;
import com.sequenceiq.cloudbreak.structuredevent.service.telemetry.mapper.ClusterUseCaseMapper;
import com.sequenceiq.cloudbreak.usage.UsageReporter;

@Component
public class ClusterRequestedLogger implements LegacyTelemetryEventLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestedLogger.class);

    private static final EnumSet<UsageProto.CDPClusterStatus.Value> TRIGGER_CASES = EnumSet.of(CREATE_STARTED, CREATE_FINISHED, CREATE_FAILED);

    @Inject
    private UsageReporter usageReporter;

    @Inject
    private ClusterUseCaseMapper clusterUseCaseMapper;

    @Inject
    private StructuredFlowEventToCDPDatalakeRequestedConverter converter;

    @Override
    public void log(StructuredFlowEvent structuredFlowEvent) {

        FlowDetails flow = structuredFlowEvent.getFlow();
        UsageProto.CDPClusterStatus.Value useCase = clusterUseCaseMapper.useCase(flow);
        LOGGER.debug("Telemetry use case: {}", useCase);

        if (TRIGGER_CASES.contains(useCase)) {
            usageReporter.cdpDatalakeRequested(converter.convert(structuredFlowEvent));
        }
    }
}