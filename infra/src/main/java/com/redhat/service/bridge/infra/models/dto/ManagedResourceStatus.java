package com.redhat.service.bridge.infra.models.dto;

import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public enum ManagedResourceStatus {
    ACCEPTED("accepted"),
    PROVISIONING("provisioning"),
    READY("ready"),
    DEPROVISION("deprovision"),
    DELETING("deleting"),
    DELETED("deleted"),
    FAILED("failed");

    @JsonValue
    String status;

    ManagedResourceStatus(String status) {
        this.status = status;
    }
}
