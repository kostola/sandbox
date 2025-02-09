package com.redhat.service.smartevents.executor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.executor.filters.FilterEvaluator;
import com.redhat.service.smartevents.executor.filters.FilterEvaluatorFactory;
import com.redhat.service.smartevents.infra.core.utils.CloudEventUtils;
import com.redhat.service.smartevents.infra.v1.api.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.v1.api.models.gateways.Action;
import com.redhat.service.smartevents.infra.v1.api.models.processors.ProcessorType;
import com.redhat.service.smartevents.infra.v1.api.models.transformations.TransformationEvaluator;
import com.redhat.service.smartevents.infra.v1.api.models.transformations.TransformationEvaluatorFactory;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;
import com.redhat.service.smartevents.processor.actions.ActionRuntime;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

public class ExecutorImpl implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(ExecutorImpl.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProcessorDTO processor;
    private final boolean isSourceProcessor;
    private final FilterEvaluator filterEvaluator;
    private final TransformationEvaluator transformationEvaluator;
    private final ActionInvoker actionInvoker;

    private Timer processorProcessingTime;
    private Timer filterTimer;
    private Timer actionTimer;
    private Timer transformationTimer;

    public ExecutorImpl(
            ProcessorDTO processor,
            FilterEvaluatorFactory filterEvaluatorFactory,
            TransformationEvaluatorFactory transformationFactory,
            ActionRuntime actionRuntime,
            MeterRegistry registry) {
        this.processor = processor;
        this.isSourceProcessor = processor.getType() == ProcessorType.SOURCE;
        this.filterEvaluator = filterEvaluatorFactory.build(processor.getDefinition().getFilters());
        this.transformationEvaluator = transformationFactory.build(processor.getDefinition().getTransformationTemplate());

        Action action = processor.getDefinition().getResolvedAction();
        this.actionInvoker = actionRuntime.getInvokerBuilder(action.getType()).build(processor, action);

        initMetricFields(processor, registry);
    }

    @Override
    public ProcessorDTO getProcessor() {
        return processor;
    }

    @Override
    public void onEvent(CloudEvent event, Map<String, String> headers) {
        processorProcessingTime.record(() -> process(event, headers));
    }

    private void process(CloudEvent event, Map<String, String> headers) {
        Map<String, Object> eventMap = toEventMap(event);

        LOG.debug("Received event with id '{}' and type '{}' in processor with name '{}' of bridge '{}", event.getId(), event.getType(), processor.getName(), processor.getBridgeId());

        // Filter evaluation
        if (!matchesFilters(eventMap)) {
            LOG.debug("Filters of processor '{}' did not match for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
            return;
        }
        LOG.info("Filters of processor '{}' matched for event with id '{}' and type '{}'", processor.getId(), event.getId(), event.getType());
        // Transformation
        // transformations are currently supported only for sink processors
        String eventToSend = isSourceProcessor ? CloudEventUtils.encode(event) : applyTransformations(eventMap);
        // Action
        actionTimer.record(() -> actionInvoker.onEvent(eventToSend, headers));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toEventMap(CloudEvent event) {
        Map<String, Object> map = CloudEventUtils.getMapper().convertValue(event, Map.class);

        // The CloudEventDeserializer from the cloud-event sdk decodes the data as a String. If it's a json, we have to convert it.
        if (map.containsKey(CloudEventUtils.CE_DATA_FIELD_NAME)) {
            try {
                map.replace(CloudEventUtils.CE_DATA_FIELD_NAME, MAPPER.readValue(map.get(CloudEventUtils.CE_DATA_FIELD_NAME).toString(), Map.class));
            } catch (Exception e) {
                LOG.debug("Could not deserialize the data into a Map. It is kept as String.");
            }
        }

        return map;
    }

    private boolean matchesFilters(Map<String, Object> eventMap) {
        return Boolean.TRUE.equals(filterTimer.record(() -> filterEvaluator.evaluateFilters(eventMap)));
    }

    private String applyTransformations(Map<String, Object> eventMap) {
        return transformationTimer.record(() -> transformationEvaluator.render(eventMap));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExecutorImpl executor = (ExecutorImpl) o;
        return Objects.equals(processor, executor.processor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(processor);
    }

    private void initMetricFields(ProcessorDTO processor, MeterRegistry registry) {
        List<Tag> tags = Arrays.asList(
                Tag.of(MetricsConstants.BRIDGE_ID_TAG, processor.getBridgeId()), Tag.of(MetricsConstants.PROCESSOR_ID_TAG, processor.getId()));
        this.processorProcessingTime = registry.timer(MetricsConstants.PROCESSOR_PROCESSING_TIME_METRIC_NAME, tags);
        this.filterTimer = registry.timer(MetricsConstants.FILTER_PROCESSING_TIME_METRIC_NAME, tags);
        this.actionTimer = registry.timer(MetricsConstants.ACTION_PROCESSING_TIME_METRIC_NAME, tags);
        this.transformationTimer = registry.timer(MetricsConstants.TRANSFORMATION_PROCESSING_TIME_METRIC_NAME, tags);
    }
}
