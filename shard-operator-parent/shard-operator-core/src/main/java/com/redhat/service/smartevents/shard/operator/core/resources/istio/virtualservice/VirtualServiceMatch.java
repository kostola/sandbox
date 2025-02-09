package com.redhat.service.smartevents.shard.operator.core.resources.istio.virtualservice;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VirtualServiceMatch {

    private VirtualServiceURI uri;

    public VirtualServiceURI getUri() {
        return uri;
    }

    public void setUri(VirtualServiceURI uri) {
        this.uri = uri;
    }
}
