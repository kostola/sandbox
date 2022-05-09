package com.redhat.service.smartevents.processor.actions.ansibletower;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.infra.validations.ValidationResult;
import com.redhat.service.smartevents.processor.GatewayValidator;

@ApplicationScoped
public class AnsibleTowerActionValidator implements AnsibleTowerAction, GatewayValidator<Action> {

    public static final String MALFORMED_HOST_PARAM_MESSAGE = "Malformed \"host\" URL";
    public static final String INVALID_PROTOCOL_MESSAGE = "The \"host\" protocol must be either \"http\" or \"https\"";

    private static final String PROTOCOL_HTTP = "http";
    private static final String PROTOCOL_HTTPS = "https";

    private static final Map<String, String> EXPECTED_PARAMS = Map.of(
            HOST_PARAM, missingParameterMessage(HOST_PARAM),
            USERNAME_PARAM, missingParameterMessage(USERNAME_PARAM),
            PASSWORD_PARAM, missingParameterMessage(PASSWORD_PARAM),
            JOB_TEMPLATE_ID_PARAM, missingParameterMessage(JOB_TEMPLATE_ID_PARAM));

    @Override
    public ValidationResult isValid(Action action) {
        if (action.getParameters() == null) {
            return ValidationResult.invalid();
        }

        for (var expectedParamEntry : EXPECTED_PARAMS.entrySet()) {
            if (!action.getParameters().containsKey(expectedParamEntry.getKey()) || action.getParameters().get(expectedParamEntry.getKey()).isEmpty()) {
                return ValidationResult.invalid(expectedParamEntry.getValue());
            }
        }

        String host = action.getParameters().get(HOST_PARAM);
        URL hostUrl;
        try {
            hostUrl = new URL(host);
        } catch (MalformedURLException e) {
            return ValidationResult.invalid(malformedUrlMessage(host, e));
        }

        String protocol = hostUrl.getProtocol();
        if (!PROTOCOL_HTTP.equalsIgnoreCase(protocol) && !PROTOCOL_HTTPS.equalsIgnoreCase(protocol)) {
            return ValidationResult.invalid(invalidProtocolMessage(protocol));
        }

        return ValidationResult.valid();
    }

    private static String invalidProtocolMessage(String actualProtocol) {
        return String.format("%s (found: \"%s\")", INVALID_PROTOCOL_MESSAGE, actualProtocol);
    }

    private static String malformedUrlMessage(String endpoint, MalformedURLException exception) {
        return String.format("%s \"%s\" (%s)", MALFORMED_HOST_PARAM_MESSAGE, endpoint, exception.getMessage());
    }

    private static String missingParameterMessage(String parameterName) {
        return "Missing or empty \"" + parameterName + "\" parameter";
    }
}
