package com.redhat.service.smartevents.executor;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.CloudEventDeserializationException;
import com.redhat.service.smartevents.infra.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.utils.CloudEventUtils;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.jackson.JsonCloudEventData;

@ApplicationScoped
public class ExecutorService {

    /**
     * Channel used for receiving events.
     */
    public static final String EVENTS_IN_CHANNEL = "events-in";

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorService.class);
    private static final String CLOUD_EVENT_SOURCE = "RHOSE";

    @Inject
    Executor executor;

    @Inject
    ObjectMapper mapper;

    private Function<String, CloudEvent> toCloudEvent;

    @PostConstruct
    void init() {
        toCloudEvent = executor.getProcessor().getType() == ProcessorType.SOURCE
                ? this::wrapToCloudEvent
                : CloudEventUtils::decode;
    }

    @Incoming(EVENTS_IN_CHANNEL)
    public CompletionStage<Void> processEvent(final Message<String> message) {
        try {
            executor.onEvent(toCloudEvent.apply(message.getPayload()));
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Processor with id '{}' on bridge '{}' failed to handle Event. The message is acked anyway.",
                    executor.getProcessor().getId(), executor.getProcessor().getBridgeId(), e);
        }
        return message.ack();
    }

    private CloudEvent wrapToCloudEvent(String event) {
        try {
            return CloudEventBuilder.v1()
                    .withId(UUID.randomUUID().toString())
                    .withSource(URI.create(CLOUD_EVENT_SOURCE))
                    .withType(String.format("%sSource", executor.getProcessor().getDefinition().getRequestedSource().getType()))
                    .withData(JsonCloudEventData.wrap(mapper.readTree(event)))
                    .build();
        } catch (JsonProcessingException e2) {
            LOG.error("JsonProcessingException when generating CloudEvent for '{}'", event, e2);
            throw new CloudEventDeserializationException("Failed to generate event map");
        }
    }
}
