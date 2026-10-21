package com.shoplab.gateway.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
public class OpenApiAggregationController {

    private static final List<String> HTTP_METHODS =
            List.of("get", "post", "put", "patch", "delete", "options", "head", "trace");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient = WebClient.create();

    @Value("${AUTH_SERVICE_URL:http://localhost:8081}")
    private String authServiceUrl;

    @Value("${USER_SERVICE_URL:http://localhost:8082}")
    private String userServiceUrl;

    @Value("${ORDER_SERVICE_URL:http://localhost:8083}")
    private String orderServiceUrl;

    @GetMapping(value = "/openapi/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> all() {
        return Mono.zip(
                        fetch(authServiceUrl, new ServiceSpec("Auth Service", "Auth")),
                        fetch(userServiceUrl, new ServiceSpec("User Service", "User")),
                        fetch(orderServiceUrl, new ServiceSpec("Order Service", "Order")))
                .map(tuple -> write(merge(List.of(tuple.getT1(), tuple.getT2(), tuple.getT3()))));
    }

    private Mono<ServiceSpec> fetch(String baseUrl, ServiceSpec service) {
        return webClient
                .get()
                .uri(baseUrl + "/v3/api-docs")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .map(json -> new ServiceSpec(service.name(), service.schemaPrefix(), read(json)));
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse OpenAPI document", e);
        }
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write aggregated OpenAPI document", e);
        }
    }

    private JsonNode merge(List<ServiceSpec> specs) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("openapi", "3.1.0");

        ObjectNode info = root.putObject("info");
        info.put("title", "ShopLab API");
        info.put("version", "1.0");

        root.putArray("servers").addObject().put("url", "/");

        ObjectNode paths = root.putObject("paths");
        ObjectNode components = root.putObject("components");
        ObjectNode schemas = components.putObject("schemas");
        ObjectNode securitySchemes = components.putObject("securitySchemes");
        ArrayNode tags = root.putArray("tags");

        for (ServiceSpec service : specs) {
            JsonNode spec = service.spec();
            tags.addObject().put("name", service.name()).put("description", service.name() + " endpoints");

            spec.path("components").path("schemas").fields().forEachRemaining(entry -> {
                JsonNode schema = entry.getValue().deepCopy();
                String prefixedName = service.schemaPrefix() + entry.getKey();
                rewriteSchemaRefs(schema, service.schemaPrefix());
                schemas.set(prefixedName, schema);
            });

            spec.path("components").path("securitySchemes").fields().forEachRemaining(entry -> {
                if (!securitySchemes.has(entry.getKey())) {
                    securitySchemes.set(entry.getKey(), entry.getValue().deepCopy());
                }
            });

            spec.path("paths").fields().forEachRemaining(entry -> {
                ObjectNode pathItem = (ObjectNode) entry.getValue().deepCopy();
                rewriteSchemaRefs(pathItem, service.schemaPrefix());

                for (String method : HTTP_METHODS) {
                    JsonNode operation = pathItem.get(method);
                    if (operation instanceof ObjectNode operationNode) {
                        operationNode.putArray("tags").add(service.name());
                        String operationId = operationNode.path("operationId").asText(null);
                        if (operationId != null && !operationId.isBlank()) {
                            operationNode.put("operationId", service.schemaPrefix().toLowerCase() + "_" + operationId);
                        }
                    }
                }
                paths.set(entry.getKey(), pathItem);
            });
        }

        return root;
    }

    private void rewriteSchemaRefs(JsonNode node, String schemaPrefix) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            JsonNode ref = object.get("$ref");
            if (ref != null && ref.isTextual() && ref.asText().startsWith("#/components/schemas/")) {
                object.put("$ref", "#/components/schemas/" + schemaPrefix
                        + ref.asText().substring("#/components/schemas/".length()));
            }
            object.fields().forEachRemaining(entry -> rewriteSchemaRefs(entry.getValue(), schemaPrefix));
        } else if (node.isArray()) {
            node.forEach(child -> rewriteSchemaRefs(child, schemaPrefix));
        }
    }

    private record ServiceSpec(String name, String schemaPrefix, JsonNode spec) {
        private ServiceSpec(String name, String schemaPrefix) {
            this(name, schemaPrefix, null);
        }
    }
}
