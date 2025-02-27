package com.redhat.service.smartevents.integration.tests.v1.steps;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.service.smartevents.infra.core.utils.CloudEventUtils;
import com.redhat.service.smartevents.integration.tests.common.AwaitilityOnTimeOutHandler;
import com.redhat.service.smartevents.integration.tests.common.Constants;
import com.redhat.service.smartevents.integration.tests.context.TestContext;
import com.redhat.service.smartevents.integration.tests.context.resolver.ContextResolver;
import com.redhat.service.smartevents.integration.tests.resources.IngressResource;
import com.redhat.service.smartevents.integration.tests.v1.common.BridgeUtils;

import io.cucumber.java.en.And;
import io.cucumber.java.en.When;
import io.restassured.http.Header;
import io.restassured.http.Headers;

public class IngressSteps {

    private TestContext context;

    public IngressSteps(TestContext context) {
        this.context = context;
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is available within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> {
                            try {
                                IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint);
                            } catch (IOException e) {
                                throw new RuntimeException("Error with inputstream", e);
                            }
                        }))
                .atMost(Duration.ofMinutes(timeoutMinutes))
                .pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    try {
                        IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                                .then()
                                .statusCode(405);
                    } catch (UnknownHostException | SSLException e) {
                        // The DNS entry was not propagated into DNS resolvers yet
                        Assertions.fail("Ingress DNS entry is not available yet", e);
                    }
                });
    }

    @And("^the Ingress of Bridge \"([^\"]*)\" is not available within (\\d+) (?:minute|minutes)$")
    public void ingressOfBridgeIsNotAvailableWithinMinutes(String testBridgeName, int timeoutMinutes) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);

        Awaitility.await()
                .conditionEvaluationListener(new AwaitilityOnTimeOutHandler(
                        () -> {
                            try {
                                IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint);
                            } catch (IOException e) {
                                throw new RuntimeException("Error with inputstream", e);
                            }
                        }))
                .atMost(Duration.ofMinutes(timeoutMinutes)).pollInterval(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    try {
                        IngressResource.optionsJsonEmptyEventResponse(context.getManagerToken(), endpoint)
                                .then()
                                .statusCode(Matchers.anyOf(Matchers.is(404), Matchers.is(503)));
                    } catch (UnknownHostException | SSLException e) {
                        // The DNS was properly deleted and the record expired.
                        context.getScenario().log("An exception '" + e.getMessage() + "' was raised when trying to call the bridge ingress, considering the bridge as deleted.");
                    }
                });
    }

    @When("^send a cloud event to the Ingress of the Bridge \"([^\"]*)\":$")
    public void sendCloudEventToIngressOfBridge(String testBridgeName, String cloudEvent) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);
        cloudEvent = ContextResolver.resolveWithScenarioContext(context, cloudEvent);

        adjustSendAndCheckCloudEvent(endpoint, cloudEvent, Constants.CE_JSON_CONTENT_TYPE);
    }

    @When("^send a cloud event to the endpoint URL \"([^\"]*)\":$")
    public void ingressOfBridgeIsAvailableWithinMinutes(String endpoint, String cloudEvent) {
        endpoint = ContextResolver.resolveWithScenarioContext(context, endpoint);
        cloudEvent = ContextResolver.resolveWithScenarioContext(context, cloudEvent);

        adjustSendAndCheckCloudEvent(endpoint, cloudEvent, Constants.CE_JSON_CONTENT_TYPE);
    }

    @When("^send a json event to the Ingress of the Bridge \"([^\"]*)\" with headers (\"[^\"]+\":\"[^\"]+\"(?:,\"[^\"]+\":\"[^\"]+\")*):$")
    public void sendCloudEventToIngressOfBridgeWithPathAndDefaultHeaders(String testBridgeName, String headers, String cloudEvent) {
        String endpoint = BridgeUtils.getOrRetrieveBridgeEventsEndpoint(context, testBridgeName);
        Headers parsedHeaders = parseHeaders(headers);

        String testCloudEventId = getCloudEventIdFromHeaders(parsedHeaders);
        context.storeCloudEventInContext(testCloudEventId);

        parsedHeaders = adjustCloudEventHeaderParameters(parsedHeaders, context.getCloudEventSystemId(testCloudEventId));

        sendAndCheckCloudEvent(endpoint, cloudEvent, parsedHeaders, Constants.JSON_CONTENT_TYPE);
    }

    private void adjustSendAndCheckCloudEvent(String endpoint, String cloudEvent, String contentType) {
        String testCloudEventId = getCloudEventId(cloudEvent);
        context.storeCloudEventInContext(testCloudEventId);

        cloudEvent = adjustCloudEventParameters(cloudEvent, context.getCloudEventSystemId(testCloudEventId));

        sendAndCheckCloudEvent(endpoint, cloudEvent, new Headers(), contentType);
    }

    private void sendAndCheckCloudEvent(String endpoint, String cloudEvent, Headers headers, String contentType) {
        String token = context.getManagerToken();
        try (InputStream cloudEventStream = new ByteArrayInputStream(cloudEvent.getBytes(StandardCharsets.UTF_8))) {
            IngressResource.postCloudEventResponse(token, endpoint, cloudEventStream, headers, contentType)
                    .then()
                    .statusCode(202);
        } catch (IOException e) {
            throw new RuntimeException("Error with inputstream", e);
        }
    }

    private String getCloudEventId(String cloudEvent) {
        ObjectNode ce;
        try {
            ce = (ObjectNode) CloudEventUtils.getMapper().readTree(cloudEvent);
            return ce.get("id") == null ? null : ce.get("id").asText();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing cloud event JSON content", e);
        }
    }

    private String getCloudEventIdFromHeaders(Headers headers) {
        for (Header header : headers.asList()) {
            if (header.getName().equals("Ce-Id")) {
                return header.getValue();
            }
        }
        throw new RuntimeException("Cloud event id not found in headers: " + headers);
    }

    private String adjustCloudEventParameters(String cloudEvent, String newCloudEventId) {
        ObjectNode ce;
        try {
            ce = (ObjectNode) CloudEventUtils.getMapper().readTree(cloudEvent);
            ce.put("id", newCloudEventId);
            ce.put("time", Instant.now().toString());
            return ce.toString();
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error processing cloud event JSON content", e);
        }
    }

    private Headers adjustCloudEventHeaderParameters(Headers headers, String newCloudEventId) {
        Map<String, Header> patchedHeaders = new HashMap<>();

        patchedHeaders.put("Ce-Id", new Header("Ce-Id", newCloudEventId));
        // Add optional timestamp header
        patchedHeaders.put("Ce-Time", new Header("Ce-Time", Instant.now().toString()));

        // Add all other headers
        for (Header header : headers.asList()) {
            if (!patchedHeaders.containsKey(header.getName())) {
                patchedHeaders.put(header.getName(), header);
            }
        }

        return new Headers(patchedHeaders.values().toArray(new Header[0]));
    }

    private Headers parseHeaders(String headers) {
        List<Header> parsedHeaders = new ArrayList<>();

        for (String header : headers.split(",")) {
            String[] headerValues = header.split(":");
            if (headerValues.length != 2) {
                throw new RuntimeException("Expected just header name and value separated by `:`, got: " + header);
            }
            parsedHeaders.add(new Header(headerValues[0].replaceAll("\"", ""), headerValues[1].replaceAll("\"", "")));
        }

        return new Headers(parsedHeaders);
    }
}
