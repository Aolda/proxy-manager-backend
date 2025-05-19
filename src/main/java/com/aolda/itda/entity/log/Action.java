package com.aolda.itda.entity.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Action {
    CREATE, UPDATE, DELETE;

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
