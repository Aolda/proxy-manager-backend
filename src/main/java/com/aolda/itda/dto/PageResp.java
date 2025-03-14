package com.aolda.itda.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResp<T> {
    private Integer totalPages;
    private Integer totalElements;
    private Integer size;
    private List<T> contents;
    private Boolean first;
    private Boolean last;

    public static <T> PageRespBuilder<T> builderFor(Class<T> clazz) {
        return (PageRespBuilder<T>) new PageRespBuilder<>();
    }
}
