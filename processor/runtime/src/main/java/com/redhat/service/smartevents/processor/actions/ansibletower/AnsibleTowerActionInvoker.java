package com.redhat.service.smartevents.processor.actions.ansibletower;

import com.redhat.service.smartevents.processor.actions.ActionInvoker;

import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.WebClient;

public class AnsibleTowerActionInvoker implements ActionInvoker {

    private final String host;
    private final String username;
    private final String password;
    private final String jobTemplateId;
    private final WebClient client;

    public AnsibleTowerActionInvoker(String host, String username, String password, String jobTemplateId, WebClient client) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.jobTemplateId = jobTemplateId;
        this.client = client;
    }

    @Override
    public void onEvent(String event) {
        String endpoint = String.format("%s/api/v2/job_templates/%s/launch/", host, jobTemplateId);
        HttpRequest<Buffer> request = client.postAbs(endpoint)
                .basicAuthentication(username, password);
        request.sendJsonObjectAndForget(new JsonObject(event));
    }
}
