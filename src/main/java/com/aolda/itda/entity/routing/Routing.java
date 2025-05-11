package com.aolda.itda.entity.routing;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.dto.routing.RoutingDTO;
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

    @ManyToOne
    @JoinColumn(name = "certificate_id")
    private Certificate certificate;

    @Column(length = 64)
    private String projectId;

    @Column(length = 64)
    private String domain;

    @Column(length = 32)
    private String instanceIp;

    @Column(length = 8)
    private String instancePort;

    private Boolean isDeleted;

    private Boolean caching;

    @Column(length = 256)
    private String name;

    public RoutingDTO toRoutingDTO() {
        return RoutingDTO.builder()
                .id(routingId)
                .name(name)
                .port(instancePort)
                .ip(instanceIp)
                .certificateId(certificate == null ? null : certificate.getCertificateId())
                .caching(caching)
                .domain(domain)
                .createdAt(getCreatedAt())
                .updatedAt(getUpdatedAt())
                .build();
    }

    public void edit(RoutingDTO dto, Certificate certificate) {
        this.name = dto.getName() != null ? dto.getName() : this.name;
        this.instanceIp = dto.getIp() != null ? dto.getIp() : this.instanceIp;
        this.instancePort = dto.getPort() != null ? dto.getPort() : this.instancePort;
        this.caching = dto.getCaching() != null ? dto.getCaching() : this.caching;
        this.domain = dto.getDomain() != null ? dto.getDomain() : this.domain;
        this.certificate = certificate;
    }
    public void delete() {
        this.isDeleted = true;
    }
}
