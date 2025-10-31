package com.aolda.itda.repository.forwarding;

import com.aolda.itda.entity.forwarding.Forwarding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ForwardingRepository extends JpaRepository<Forwarding, Long> {
    List<Forwarding> findByProjectIdAndIsDeleted(String projectId, Boolean isDeleted);
    Optional<Forwarding> findByForwardingIdAndIsDeleted(Long forwardingId, Boolean isDeleted);
    Boolean existsByInstanceIpAndInstancePortAndIsDeleted(String instanceIp, String instancePort, Boolean isDeleted);
    Boolean existsByServerPortAndIsDeleted(String serverPort, Boolean isDeleted);

    @Query("SELECT f FROM Forwarding f WHERE f.projectId = ?1 AND f.isDeleted = ?3 AND (f.instanceIp LIKE %?2% OR f.serverPort LIKE %?2% OR f.name LIKE %?2%)")
    List<Forwarding> findWithSearch(String projectId, String query, Boolean isDeleted);

    @Query("SELECT f.serverPort FROM Forwarding f WHERE f.isDeleted = ?2")
    List<Integer> findAllUsedServerPortsByProjectIdAndIsDeleted(Boolean isDeleted);
}
