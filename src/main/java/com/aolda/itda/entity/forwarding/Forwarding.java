package com.aolda.itda.entity.forwarding;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.entity.BaseTimeEntity;
import com.aolda.itda.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forwarding")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Forwarding extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long forwardingId;

    @Column(length = 64)
    private String projectId;

    @Column(length = 32)
    private String serverIp;

    @Column(length = 8)
    private String serverPort;

    @Column(length = 32)
    private String instanceIp;

    @Column(length = 8)
    private String instancePort;

    private Boolean isDeleted;

    @Column(length = 256)
    private String name;

    public Forwarding(Forwarding forwarding) {
        this.forwardingId = forwarding.getForwardingId();
        this.projectId = forwarding.getProjectId();
        this.serverIp = forwarding.getServerIp();
        this.serverPort = forwarding.getServerPort();
        this.instanceIp = forwarding.getInstanceIp();
        this.instancePort = forwarding.getInstancePort();
        this.isDeleted = forwarding.getIsDeleted();
        this.name = forwarding.getName();
    }

    public ForwardingDTO toForwardingDTO() {
        return ForwardingDTO.builder()
                .id(forwardingId)
                .name(name)
                .serverPort(serverPort)
                .instanceIp(instanceIp)
                .instancePort(instancePort)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }

    public void edit(ForwardingDTO dto) {
        this.name = dto.getName() != null ? dto.getName() : this.name;
        this.serverPort = dto.getServerPort() != null &&
                dto.getServerPort().matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$")
                ? dto.getServerPort() : this.serverPort;
        this.instanceIp = dto.getInstanceIp() != null &&
                dto.getInstanceIp().matches("^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$")
                ? dto.getInstanceIp() : this.instanceIp;
        this.instancePort = dto.getInstancePort() != null &&
                dto.getInstancePort().matches("^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$")
                ? dto.getInstancePort() : this.instancePort;
    }

    public void delete() {
        this.isDeleted = true;
    }
}
