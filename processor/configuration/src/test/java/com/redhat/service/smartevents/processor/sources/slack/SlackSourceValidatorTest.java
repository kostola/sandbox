package com.redhat.service.smartevents.processor.sources.slack;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Source;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlackSourceValidatorTest {

    private static final String TEST_CHANNEL = "test-channel";
    private static final String TEST_TOKEN = "xoxb-abcdefg";

    @Inject
    SlackSourceValidator validator;

    @Test
    void isValidWithChannelAndToken() {
        assertIsValid(createSourceWithChannelAndToken(TEST_CHANNEL, TEST_TOKEN));
    }

    @Test
    void isInvalidWithNullParameters() {
        assertIsInvalid(createSourceWithParameters(null));
    }

    @Test
    void isInvalidWithEmptyParameters() {
        assertIsInvalid(createSourceWithParameters(Collections.emptyMap()), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingChannel() {
        assertIsInvalid(createSourceWithToken(TEST_TOKEN), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithNullChannel() {
        assertIsInvalid(createSourceWithChannelAndToken(null, TEST_TOKEN), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyChannel() {
        assertIsInvalid(createSourceWithChannelAndToken("", TEST_TOKEN), SlackSourceValidator.INVALID_CHANNEL_MESSAGE);
    }

    @Test
    void isInvalidWithMissingToken() {
        assertIsInvalid(createSourceWithChannel(TEST_CHANNEL), SlackSourceValidator.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void isInvalidWithNullToken() {
        assertIsInvalid(createSourceWithChannelAndToken(TEST_CHANNEL, null), SlackSourceValidator.INVALID_TOKEN_MESSAGE);
    }

    @Test
    void isInvalidWithEmptyToken() {
        assertIsInvalid(createSourceWithChannelAndToken(TEST_CHANNEL, ""), SlackSourceValidator.INVALID_TOKEN_MESSAGE);
    }

    private void assertIsValid(Source source) {
        ValidationResult validationResult = validator.isValid(source);
        assertThat(validationResult.isValid()).isTrue();
    }

    private void assertIsInvalid(Source source) {
        ValidationResult validationResult = validator.isValid(source);
        assertThat(validationResult.isValid()).isFalse();
    }

    private void assertIsInvalid(Source source, String errorMessage) {
        ValidationResult validationResult = validator.isValid(source);
        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).startsWith(errorMessage);
    }

    private Source createSourceWithChannel(String channel) {
        return createSourceWithParameters(Map.of(SlackSource.CHANNEL_PARAM, channel));
    }

    private Source createSourceWithToken(String token) {
        return createSourceWithParameters(Map.of(SlackSource.TOKEN_PARAM, token));
    }

    private Source createSourceWithChannelAndToken(String channel, String token) {
        Map<String, String> params = new HashMap<>();
        params.put(SlackSource.CHANNEL_PARAM, channel);
        params.put(SlackSource.TOKEN_PARAM, token);
        return createSourceWithParameters(params);
    }

    private Source createSourceWithParameters(Map<String, String> params) {
        Source b = new Source();
        b.setType(SlackSource.TYPE);
        b.setParameters(params);
        return b;
    }
}
