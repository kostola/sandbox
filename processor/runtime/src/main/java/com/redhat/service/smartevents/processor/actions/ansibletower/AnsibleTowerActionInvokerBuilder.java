package com.redhat.service.smartevents.processor.actions.ansibletower;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.GatewayProviderException;
import com.redhat.service.smartevents.infra.models.dto.ProcessorDTO;
import com.redhat.service.smartevents.infra.models.gateways.Action;
import com.redhat.service.smartevents.processor.actions.AbstractWebClientInvokerBuilder;
import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.ext.web.client.WebClientOptions;

@ApplicationScoped
public class AnsibleTowerActionInvokerBuilder extends AbstractWebClientInvokerBuilder implements AnsibleTowerAction {

    @Override
    protected WebClientOptions getWebClientOptions(WebClientOptions options) {
        // only for demo purposes
        return options.setTrustAll(true).setVerifyHost(false);
    }

    @Override
    public ActionInvoker build(ProcessorDTO processor, Action action) {
        String host = Optional.ofNullable(action.getParameters().get(HOST_PARAM))
                .orElseThrow(() -> buildNoParamException(processor, HOST_PARAM));
        String username = Optional.ofNullable(action.getParameters().get(USERNAME_PARAM))
                .orElseThrow(() -> buildNoParamException(processor, USERNAME_PARAM));
        String password = Optional.ofNullable(action.getParameters().get(PASSWORD_PARAM))
                .orElseThrow(() -> buildNoParamException(processor, PASSWORD_PARAM));
        String jobTemplateId = Optional.ofNullable(action.getParameters().get(JOB_TEMPLATE_ID_PARAM))
                .orElseThrow(() -> buildNoParamException(processor, JOB_TEMPLATE_ID_PARAM));
        return new AnsibleTowerActionInvoker(host, username, password, jobTemplateId, webClient);
    }

    private static GatewayProviderException buildNoParamException(ProcessorDTO processor, String paramName) {
        String message = String.format("There is no %s specified in the parameters for Action on Processor '%s' on Bridge '%s'",
                paramName, processor.getId(), processor.getBridgeId());
        return new GatewayProviderException(message);
    }
}
