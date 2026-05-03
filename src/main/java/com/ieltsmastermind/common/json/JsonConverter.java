package com.ieltsmastermind.common.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonConverter {

    private final ObjectMapper objectMapper;

    public JsonConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** Java object -> JsonNode (no exceptions) */
    public JsonNode toJsonNode(Object value) {
        return objectMapper.valueToTree(value);
    }

    /** Java object -> JSON string */
    public String toJsonString(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize object to JSON", e);
        }
    }

    /** JsonNode -> typed object */
    public <T> T fromJsonNode(JsonNode node, Class<T> clazz) {
        return objectMapper.convertValue(node, clazz);
    }

    /** JsonNode -> typed object (generic, e.g. List<DocNode>) */
    public <T> T fromJsonNode(JsonNode node, TypeReference<T> typeRef) {
        return objectMapper.convertValue(node, typeRef);
    }
}
