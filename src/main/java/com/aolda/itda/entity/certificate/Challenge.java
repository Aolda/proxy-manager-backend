package com.aolda.itda.entity.certificate;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Challenge {
    HTTP, DNS_CLOUDFLARE;

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
