package com.aolda.itda.entity.certificate;

import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "certificate")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long certificateId;

    @Column(length = 64)
    private String projectId;

    @Setter
    @Column(length = 64)
    private String domain;

    @Column(length = 64)
    @Setter
    private String email;

    @Setter
    private LocalDateTime expiresAt; //인증서 만료일
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    private Challenge challenge;

    private Boolean isDeleted;


    public String formatDomain() {
        return domain == null ? null : domain.replace("*", "_");
    }

    public void setIsDeleted(boolean b) {
        this.isDeleted = b;
    }

    @Transient
    private String apiToken;

}

