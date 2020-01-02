package com.bazaarvoice.config.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.awt.Color;
import java.io.IOException;

public class ColorDeserializer extends JsonDeserializer<Color> {

    @Override
    public Color deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode treeNode = jp.getCodec().readTree(jp);
        String colorString = treeNode.asText();
        int rgb = Integer.parseInt(colorString.substring(1), 16);
        return new Color(rgb);
    }
}
