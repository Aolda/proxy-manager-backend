package com.aolda.itda.repository.forwarding;

import com.aolda.itda.entity.forwarding.Forwarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForwardingRepository extends JpaRepository<Forwarding, Long> {
    List<Forwarding> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);
    Optional<Forwarding> findByForwardingIdAndIsDeleted(Long forwardingId, Boolean isDeleted);
    Boolean existsByInstanceIpAndInstancePortAndIsDeleted(String instanceIp, String instancePort, Boolean isDeleted);
    Boolean existsByServerPortAndIsDeleted(String serverPort, Boolean isDeleted);
}
