package com.bazaarvoice.config.deserializer;

import com.bazaarvoice.prr.model.Client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class ClientDeserializer extends JsonDeserializer<Client> {
    @Override
    public Client deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return new Client(node.textValue());
    }
}
