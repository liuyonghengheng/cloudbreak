package com.sequenceiq.freeipa.flow.freeipa.diagnostics.handler;

import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionHandlerSelectors.UPLOAD_DIAGNOSTICS_EVENT;
import static com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionStateSelectors.START_DIAGNOSTICS_CLEANUP_EVENT;

import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.reactor.api.handler.EventSenderAwareHandler;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.DiagnosticsFlowService;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionEvent;
import com.sequenceiq.freeipa.flow.freeipa.diagnostics.event.DiagnosticsCollectionFailureEvent;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class DiagnosticsUploadHandler extends EventSenderAwareHandler<DiagnosticsCollectionEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticsUploadHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private DiagnosticsFlowService diagnosticsFlowService;

    protected DiagnosticsUploadHandler(EventSender eventSender) {
        super(eventSender);
    }

    @Override
    public void accept(Event<DiagnosticsCollectionEvent> event) {
        DiagnosticsCollectionEvent data = event.getData();
        Long resourceId = data.getResourceId();
        String resourceCrn = data.getResourceCrn();
        Map<String, Object> parameters = data.getParameters();
        try {
            LOGGER.debug("Diagnostics upload started. resourceCrn: '{}', parameters: '{}'", resourceCrn, parameters);
            diagnosticsFlowService.upload(resourceId, parameters);
            DiagnosticsCollectionEvent diagnosticsCollectionEvent = DiagnosticsCollectionEvent.builder()
                    .withResourceCrn(resourceCrn)
                    .withResourceId(resourceId)
                    .withSelector(START_DIAGNOSTICS_CLEANUP_EVENT.selector())
                    .withParameters(parameters)
                    .build();
            eventSender().sendEvent(diagnosticsCollectionEvent, event.getHeaders());
        } catch (Exception e) {
            LOGGER.debug("Diagnostics upload failed. resourceCrn: '{}', parameters: '{}'.", resourceCrn, parameters, e);
            DiagnosticsCollectionFailureEvent failureEvent = new DiagnosticsCollectionFailureEvent(resourceId, e, resourceCrn, parameters);
            eventBus.notify(failureEvent.selector(), new Event<>(event.getHeaders(), failureEvent));
        }
    }

    @Override
    public String selector() {
        return UPLOAD_DIAGNOSTICS_EVENT.selector();
    }
}