package com.aolda.itda.entity.routing;

import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "routing")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Routing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long routingId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "certificate_id")
    private Certificate certificate;

    private String projectId;

    private String domain;

    private String instanceIp;

    private Boolean isDeleted;


}
