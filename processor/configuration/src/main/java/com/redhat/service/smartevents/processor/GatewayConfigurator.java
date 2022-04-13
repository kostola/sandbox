package com.redhat.service.smartevents.processor;

import java.util.Optional;

import com.redhat.service.smartevents.infra.exceptions.definitions.user.ActionProviderException;
import com.redhat.service.smartevents.processor.actions.ActionConnector;
import com.redhat.service.smartevents.processor.actions.ActionResolver;
import com.redhat.service.smartevents.processor.actions.ActionValidator;
import com.redhat.service.smartevents.processor.sources.SourceConnector;
import com.redhat.service.smartevents.processor.sources.SourceResolver;
import com.redhat.service.smartevents.processor.sources.SourceValidator;

public interface GatewayConfigurator {

    /**
     * Get validator bean for specific action type. Required for every action.
     *
     * @param actionType desired action type
     * @return the validator bean
     * @throws ActionProviderException if bean is not found
     */
    ActionValidator getActionValidator(String actionType);

    /**
     * Get resolver bean for specific action type.
     * This bean is optional and must be implemented only for non-invokable actions
     * that needs to be resolved to an invokable actions to be executed.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<ActionResolver> getActionResolver(String actionType);

    /**
     * Get connector bean for specific action type.
     * This bean is optional and must be implemented only for actions that requires
     * a Managed Connector to work.
     *
     * @param actionType desired action type
     * @return {@link Optional} containing the bean if present, empty otherwise.
     */
    Optional<ActionConnector> getActionConnector(String actionType);

    /**
     * Get validator bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the validator bean
     * @throws ActionProviderException if bean is not found
     */
    SourceValidator getSourceValidator(String sourceType);

    /**
     * Get resolver bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the resolver bean
     * @throws ActionProviderException if bean is not found
     */
    SourceResolver getSourceResolver(String sourceType);

    /**
     * Get connector bean for specific source type. Required for every source.
     *
     * @param sourceType desired source type
     * @return the connector bean
     * @throws ActionProviderException if bean is not found
     */
    SourceConnector getSourceConnector(String sourceType);
}
