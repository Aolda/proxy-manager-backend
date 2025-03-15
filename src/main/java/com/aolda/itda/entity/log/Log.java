package com.aolda.itda.entity.log;

import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "log")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long logId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String projectId;

    @Enumerated(EnumType.STRING)
    private ObjectType objectType;

    private Long objectId;

    @Enumerated(EnumType.STRING)
    private Action action;

    private String metadata;

}
