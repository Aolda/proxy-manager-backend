package com.aolda.itda.entity.forwarding;

import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "forwarding")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Forwarding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long forwardingId;

    @OneToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String projectId;

    private String serverIp;

    private String serverPort;

    private String instanceIp;

    private String instancePort;

    private Boolean isDeleted;
}
