package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.BridgeLifecycleException;
import com.redhat.service.smartevents.infra.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.processor.GatewayConfiguratorService;
import com.redhat.service.smartevents.processor.actions.input.InputAction;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

import static com.redhat.service.smartevents.processor.sources.slack.SlackSourceResolver.SLACK_SOURCE_CLOUD_EVENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@QuarkusTest
class SlackSourceResolverTest {

    private static final String TEST_BRIDGE_ID = "test-bridge-id";
    private static final String TEST_BRIDGE_ENDPOINT = "http://www.example.com/bridge01";
    private static final String TEST_CUSTOMER_ID = "test-customer-id";
    private static final String TEST_PROCESSOR_ID = "test-processor-id";
    private static final String TEST_PROCESSOR_TOPIC_NAME = "ob-test-processor-id";
    private static final String TEST_CHANNEL_PARAM = "test-channel";
    private static final String TEST_TOKEN_PARAM = "xoxb-abcdefg";

    private static final String UNAVAILABLE_BRIDGE_ID = "br-unavailable";
    private static final String UNKNOWN_BRIDGE_ID = "br-unknown";
    private static final String UNKNOWN_CUSTOMER_ID = "customer-unknown";

    @Inject
    SlackSourceResolver slackSourceResolver;

    @InjectMock
    GatewayConfiguratorService gatewayConfiguratorServiceMock;

    @BeforeEach
    void beforeEach() {
        reset(gatewayConfiguratorServiceMock);

        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(TEST_BRIDGE_ID, TEST_CUSTOMER_ID)).thenReturn(TEST_BRIDGE_ENDPOINT);
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(UNAVAILABLE_BRIDGE_ID, TEST_CUSTOMER_ID)).thenThrow(new BridgeLifecycleException("Unavailable bridge"));
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(not(or(eq(UNAVAILABLE_BRIDGE_ID), eq(TEST_BRIDGE_ID))), eq(TEST_CUSTOMER_ID)))
                .thenThrow(new ItemNotFoundException("Bridge not found"));
        when(gatewayConfiguratorServiceMock.getBridgeEndpoint(any(), not(eq(TEST_CUSTOMER_ID)))).thenThrow(new ItemNotFoundException("Customer not found"));
    }

    @Test
    void testTransform() {
        Source source = buildTestSource();

        Action transformedAction = slackSourceResolver.resolve(source, TEST_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID);

        assertThat(transformedAction.getType()).isEqualTo(InputAction.TYPE);
        assertThat(transformedAction.getParameters())
                .containsEntry(InputAction.ENDPOINT_PARAM, TEST_BRIDGE_ENDPOINT)
                .containsEntry(InputAction.CLOUD_EVENT_TYPE, SLACK_SOURCE_CLOUD_EVENT_TYPE);
    }

    @Test
    void testTransformWithUnavailableBridge() {
        Source source = buildTestSource();
        assertThatExceptionOfType(BridgeLifecycleException.class).isThrownBy(() -> slackSourceResolver.resolve(source, TEST_CUSTOMER_ID, UNAVAILABLE_BRIDGE_ID, TEST_PROCESSOR_ID));
    }

    @Test
    void testTransformWithUnknownBridge() {
        Source source = buildTestSource();
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> slackSourceResolver.resolve(source, TEST_CUSTOMER_ID, UNKNOWN_BRIDGE_ID, TEST_PROCESSOR_ID));
    }

    @Test
    void testTransformWithUnknownCustomer() {
        Source source = buildTestSource();
        assertThatExceptionOfType(ItemNotFoundException.class).isThrownBy(() -> slackSourceResolver.resolve(source, UNKNOWN_CUSTOMER_ID, TEST_BRIDGE_ID, TEST_PROCESSOR_ID));
    }

    private Source buildTestSource() {
        Map<String, String> parameters = Map.of(
                SlackSource.CHANNEL_PARAM, TEST_CHANNEL_PARAM,
                SlackSource.TOKEN_PARAM, TEST_TOKEN_PARAM);

        Source source = new Source();
        source.setParameters(parameters);
        return source;
    }
}
