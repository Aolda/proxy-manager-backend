package com.aolda.itda.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IdAndNameDTO {

    private String id;
    private String name;

    @QueryProjection
    public IdAndNameDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
