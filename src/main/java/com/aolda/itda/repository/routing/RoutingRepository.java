package com.aolda.itda.repository.routing;

import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.entity.routing.Routing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoutingRepository extends JpaRepository<Routing, Long> {
    List<Routing> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);
    Optional<Routing> findByRoutingIdAndIsDeleted(Long routingId, Boolean isDeleted);
    Boolean existsByDomainAndIsDeleted(String domain, Boolean isDeleted);

    @Query("SELECT r FROM Routing r WHERE r.projectId = ?1 AND r.isDeleted = ?3 AND (r.domain LIKE %?2% OR r.instanceIp LIKE %?2% OR r.name LIKE %?2%)")
    List<Routing> findWithSearch(String projectId, String query, Boolean isDeleted);
}
