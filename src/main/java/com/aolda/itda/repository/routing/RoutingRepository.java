package com.aolda.itda.repository.routing;

import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.entity.routing.Routing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoutingRepository extends JpaRepository<Routing, Long> {
    List<Routing> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);
    Optional<Routing> findByRoutingIdAndIsDeleted(Long routingId, Boolean isDeleted);
    Boolean existsByDomainAndIsDeleted(String domain, Boolean isDeleted);
}
