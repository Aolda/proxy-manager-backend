package com.aolda.itda.dto.log;

import com.aolda.itda.dto.auth.IdAndNameDTO;
import com.aolda.itda.entity.log.Action;
import com.aolda.itda.entity.log.ObjectType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogDTO {
    private Long id;
    private IdAndNameDTO user;
    private Action action;
    private ObjectType type;
    private Long objectId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @QueryProjection
    public LogDTO(Long id, IdAndNameDTO user, Action action, ObjectType type, Long objectId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.action = action;
        this.type = type;
        this.objectId = objectId;
        this.description = description;
        this.createdAt = createdAt;
    }
}
