package com.redhat.service.smartevents.infra.core.exceptions.definitions.platform;

public class ConnectorUpdateException extends InternalPlatformException {

    public ConnectorUpdateException(String message) {
        super(message);
    }

    public ConnectorUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
