package com.bazaarvoice.config.deserializer;

import jdk.nashorn.internal.objects.NativeString;
import org.junit.Test;

import java.awt.Color;

import static org.junit.Assert.*;

public class ColorDeserializerTest {

    @Test
    public void deserializeColorString() {
        String colorString = "#36398E";
        int rgb = Integer.parseInt(colorString.substring(1), 16);
        Color actualColor = new Color(rgb);
        assertEquals(54, actualColor.getRed());
        assertEquals(57, actualColor.getGreen());
        assertEquals(142, actualColor.getBlue());
    }

    @Test
    public void deserializeBlackColor() {
        String colorString = "#ffffff";
        int rgb = Integer.parseInt(colorString.substring(1), 16);
        Color actualColor = new Color(rgb);
        assertEquals(255, actualColor.getRed());
        assertEquals(255, actualColor.getGreen());
        assertEquals(255, actualColor.getBlue());
    }
}