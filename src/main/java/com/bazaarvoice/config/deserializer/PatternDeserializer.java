package com.bazaarvoice.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.regex.Pattern;

public class PatternDeserializer extends JsonDeserializer<Pattern> {
    @Override
    public Pattern deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode treeNode = jp.getCodec().readTree(jp);
        /* need to take field "pattern"
            JSON node looks like:
            "pattern" : {
                "pattern" : "\\p{Alnum}+",
                "flags" : 0
            }
        */

        return Pattern.compile(treeNode.get("pattern").asText(), treeNode.get("flags").asInt());
    }
}
