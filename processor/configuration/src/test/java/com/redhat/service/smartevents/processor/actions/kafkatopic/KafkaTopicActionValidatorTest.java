package com.redhat.service.smartevents.processor.actions.kafkatopic;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;

import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class KafkaTopicActionValidatorTest {

    @Inject
    KafkaTopicActionValidator validator;

    private Action createActionForTopic(String topicName) {
        Action b = new Action();
        b.setType(KafkaTopicAction.TYPE);
        Map<String, String> params = new HashMap<>();
        params.put(KafkaTopicAction.TOPIC_PARAM, topicName);
        b.setParameters(params);
        return b;
    }

    @Test
    void isValid() {
        Action action = createActionForTopic("myTopic");
        assertThat(validator.isValid(action).isValid()).isTrue();
    }

    @Test
    void isValid_noTopicIsNotValid() {
        Action action = createActionForTopic("myTopic");
        action.getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(action);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    void isValid_emptyTopicStringIsNotValid() {
        Action action = createActionForTopic("myTopic");
        action.getParameters().remove(KafkaTopicAction.TOPIC_PARAM);
        ValidationResult validationResult = validator.isValid(action);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getMessage()).isEqualTo(KafkaTopicActionValidator.INVALID_TOPIC_PARAM_MESSAGE);
    }

    @Test
    void isValid_nullParametersIsNotValid() {
        Action action = createActionForTopic("myTopic");
        action.setParameters(null);
        ValidationResult validationResult = validator.isValid(action);

        assertThat(validationResult.isValid()).isFalse();
    }
}
