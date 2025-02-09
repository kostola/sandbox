package com.redhat.service.smartevents.manager.v1.api.user;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.security.SecuritySchemes;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.redhat.service.smartevents.infra.core.exceptions.definitions.user.ItemNotFoundException;
import com.redhat.service.smartevents.infra.core.models.responses.ErrorsResponse;
import com.redhat.service.smartevents.infra.v1.api.V1APIConstants;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorCatalogResponse;
import com.redhat.service.smartevents.manager.v1.api.models.responses.ProcessorSchemaEntryResponse;
import com.redhat.service.smartevents.processor.ProcessorCatalogService;

import io.quarkus.security.Authenticated;

@Tag(name = "Schema Catalog", description = "The API that provide the catalog of the available action/source processors definition and their JSON schema.")
@SecuritySchemes(value = {
        @SecurityScheme(securitySchemeName = "bearer",
                type = SecuritySchemeType.HTTP,
                scheme = "Bearer")
})
@SecurityRequirement(name = "bearer")
@Path(V1APIConstants.V1_SCHEMA_API_BASE_PATH)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
@RegisterRestClient
public class SchemaAPI {

    private static final String ACTION_TYPE = "action";
    private static final String SOURCE_TYPE = "source";

    @Inject
    ProcessorCatalogService processorCatalogService;

    @Inject
    ObjectMapper mapper;

    @APIResponses(value = {
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ProcessorCatalogResponse.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get processor catalog", description = "Get the processor catalog with all the available sources and actions.")
    @GET
    public Response getCatalog() {
        List<ProcessorSchemaEntryResponse> entries = new ArrayList<>();
        entries.addAll(
                processorCatalogService
                        .getActionsCatalog()
                        .stream()
                        .map(x -> new ProcessorSchemaEntryResponse(x.getId(),
                                x.getName(),
                                x.getDescription(),
                                ACTION_TYPE,
                                V1APIConstants.V1_ACTIONS_SCHEMA_API_BASE_PATH + x.getId()))
                        .collect(Collectors.toList()));
        entries.addAll(processorCatalogService
                .getSourcesCatalog()
                .stream()
                .map(x -> new ProcessorSchemaEntryResponse(x.getId(),
                        x.getName(),
                        x.getDescription(),
                        SOURCE_TYPE,
                        V1APIConstants.V1_SOURCES_SCHEMA_API_BASE_PATH + x.getId()))
                .collect(Collectors.toList()));
        ProcessorCatalogResponse response = new ProcessorCatalogResponse(entries);
        return Response.ok(response).build();
    }

    @APIResponses(value = {
            // we can't use JsonSchema.class because of https://github.com/swagger-api/swagger-ui/issues/8046
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Object.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get source processor schema", description = "Get the source processor JSON schema.")
    @GET
    @Path("/sources/{id}")
    public Response getSourceProcessorSchema(@PathParam("id") String id) {
        if (processorCatalogService.getSourcesCatalog().stream().noneMatch(x -> x.getId().equals(id))) {
            throw new ItemNotFoundException(String.format("The processor json schema '%s' is not in the catalog.", id));
        }

        // We can't return a JsonSchema due to a StackOverflow exception in the jackson serialization
        return Response.ok(processorCatalogService.getSourceJsonSchema(id).getSchemaNode()).build();
    }

    @APIResponses(value = {
            // we can't use JsonSchema.class because of https://github.com/swagger-api/swagger-ui/issues/8046
            @APIResponse(description = "Success.", responseCode = "200",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Object.class))),
            @APIResponse(description = "Bad request.", responseCode = "400", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class))),
            @APIResponse(description = "Unauthorized.", responseCode = "401"),
            @APIResponse(description = "Forbidden.", responseCode = "403"),
            @APIResponse(description = "Internal error.", responseCode = "500", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorsResponse.class)))
    })
    @Operation(summary = "Get action processor schema", description = "Get the action processor JSON schema.")
    @GET
    @Path("/actions/{id}")
    public Response getActionProcessorSchema(@PathParam("id") String id) {
        if (processorCatalogService.getActionsCatalog().stream().noneMatch(x -> x.getId().equals(id))) {
            throw new ItemNotFoundException(String.format("The processor json schema '%s' is not in the catalog.", id));
        }

        // We can't return a JsonSchema due to a StackOverflow exception in the jackson serialization
        return Response.ok(processorCatalogService.getActionJsonSchema(id).getSchemaNode()).build();
    }
}
