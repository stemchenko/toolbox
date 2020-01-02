package com.bazaarvoice.config.deserializer;

import com.bazaarvoice.cca.util.VersionNumber;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class VersionNumberDeserializer extends JsonDeserializer<VersionNumber> {
    @Override
    public VersionNumber deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return VersionNumber.parse(node.textValue(), VersionNumber.v5_6);
    }
}
