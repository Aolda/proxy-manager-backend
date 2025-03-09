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
@Table(name = "routing")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long certificateId;

    @OneToOne
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    private String projectId;

    private String domain;

    private String email;

    private LocalDateTime expiredAt;

    @Enumerated(EnumType.STRING)
    private Challenge challenge;

    private Boolean isDeleted;

    private String metadata;


}
