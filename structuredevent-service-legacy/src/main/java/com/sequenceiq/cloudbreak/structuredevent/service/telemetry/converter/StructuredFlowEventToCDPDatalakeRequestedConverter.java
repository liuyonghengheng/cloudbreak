package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToCDPDatalakeRequestedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventToCDPDatalakeRequestedConverter.class);

    @Inject
    private StructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    public UsageProto.CDPDatalakeRequested convert(StructuredFlowEvent structuredFlowEvent) {
        if (structuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPDatalakeRequested.Builder cdpDatalakeRequested = UsageProto.CDPDatalakeRequested.newBuilder();
        cdpDatalakeRequested.setOperationDetails(operationDetailsConverter.convert(structuredFlowEvent));

        UsageProto.CDPDatalakeRequested ret = cdpDatalakeRequested.build();
        LOGGER.debug("Converted telemetry event: {}", ret);
        return ret;
    }
}
