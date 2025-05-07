package com.aolda.itda.dto.certificate;

import com.aolda.itda.entity.certificate.Challenge;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CertificateDTO {

    private Long id;          // 인증서 고유 ID
//    private String projectId;            // 프로젝트 식별자
    private String domain;               // SSL 인증받을 도메인 주소
    private String email;                // 도메인 소유자의 이메일
    private LocalDateTime expiresAt;     // 인증서 만료일
    private LocalDateTime createdAt;    // 인증서 생성일
    private LocalDateTime updatedAt;    // 인증서 업데이트일
    private Challenge challenge;         // 챌린지 방식 (HTTP, DNS_CLOUDFLARE)
    private Boolean isDeleted;           // 삭제 여부 (soft delete)
    private String apiToken;
}
/* 이메일, 챌린지 방식, http인지 dns인지... "*/
//도메인, 소유자 이메일, 챌린지 방식 확실하게 들어가야함!!
/*erd 보고 만들기*/
//Challenge 키는 따로 (private으로 api 키 받기)