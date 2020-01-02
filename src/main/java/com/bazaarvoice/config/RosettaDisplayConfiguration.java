package com.bazaarvoice.config;

import com.bazaarvoice.prr.config.DisplayConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class RosettaDisplayConfiguration extends DisplayConfiguration {
    private String displayCode;
    private String internalCode;
    private String applicationCode;
    @JsonProperty("bundle")
    private String bundleName;
    @JsonProperty ("package")
    private String packageId;
    private List<String> clusters = Collections.emptyList();
    private String platform;
}
