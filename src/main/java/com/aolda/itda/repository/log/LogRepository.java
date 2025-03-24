package com.aolda.itda.repository.log;

import com.aolda.itda.entity.log.Log;
import com.aolda.itda.entity.routing.Routing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LogRepository extends JpaRepository<Log, Long> {
}
