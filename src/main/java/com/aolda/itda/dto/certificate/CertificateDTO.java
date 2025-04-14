package com.aolda.itda.dto.certificate;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateDTO {

    private Long certificateId;
    private String projectId;
    private String domain;
    private LocalDateTime expiredAt;   // 필요 시
    private Boolean isDeleted;
    private String description;        // 추가 설명
}
