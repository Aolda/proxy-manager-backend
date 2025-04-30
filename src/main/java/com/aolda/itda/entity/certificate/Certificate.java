package com.aolda.itda.entity.certificate;

import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(length = 64)
    private String domain;

    @Column(length = 64)
    private String email;

    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    private Challenge challenge;

    private Boolean isDeleted;

    @Column(length = 256)
    private String description;

    public String formatDomain() {
        return domain == null ? null : domain.replace("*", "_");
    }

    public void setIsDeleted(boolean b) {
    }

    public void setDomain(String domain) {
    }

    public void setDescription(String description) {

    }
}
