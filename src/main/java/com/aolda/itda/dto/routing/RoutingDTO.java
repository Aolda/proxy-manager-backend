package com.aolda.itda.dto.routing;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoutingDTO {
    @NotBlank
    private String ip;
    @NotBlank
    private String domain;
    @NotBlank
    private String name;
    @NotBlank
    private String port;
    private Long id;
    @NotBlank
    private Long certificateId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @NotNull
    private Boolean caching;
}
