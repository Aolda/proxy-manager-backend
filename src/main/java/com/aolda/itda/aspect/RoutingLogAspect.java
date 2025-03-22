package com.aolda.itda.aspect;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.dto.routing.RoutingDTO;
import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.entity.log.Action;
import com.aolda.itda.entity.log.Log;
import com.aolda.itda.entity.log.ObjectType;
import com.aolda.itda.entity.routing.Routing;
import com.aolda.itda.entity.user.User;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.repository.log.LogRepository;
import com.aolda.itda.repository.routing.RoutingRepository;
import com.aolda.itda.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class RoutingLogAspect {

    private final RoutingRepository routingRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final EntityManager entityManager;

    /* Create 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.routing.*Service.*create*(..))"
            , returning = "result")
    public void createLogging(JoinPoint joinPoint, RoutingDTO result) {

        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getSession().getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 생성된 엔티티 조회 */
        Routing routing = routingRepository.findByRoutingIdAndIsDeleted(result.getId(), false).orElse(null);

        /* 로그 메세지 작성 */
        String description = "name: " + routing.getName() + "\n"
                + "domain: " + routing.getDomain() + "\n"
                + "ip: " + routing.getInstanceIp() + "\n"
                + "ip: " + routing.getInstancePort() + "\n"
                + "certificateId: " + routing.getCertificate().getCertificateId() + "\n"
                + "caching: " + routing.getCaching() + "\n";

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(routing.getRoutingId())
                .action(Action.UPDATE)
                .projectId(routing.getProjectId())
                .description(description)
                .build());
    }

    /* Delete 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.routing.*Service.*delete*(..))")
    public void deleteLogging(JoinPoint joinPoint) {

        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getSession().getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 삭제된 엔티티 조회 */
        Object[] args = joinPoint.getArgs();

        Long id = (Long) args[0];
        Routing routing = routingRepository.findByRoutingIdAndIsDeleted(id, true).orElse(null);

        /* 로그 메세지 작성 */
        String description = "name: " + routing.getName() + "\n"
                + "domain: " + routing.getDomain() + "\n"
                + "ip: " + routing.getInstanceIp() + "\n"
                + "ip: " + routing.getInstancePort() + "\n"
                + "certificateId: " + routing.getCertificate().getCertificateId() + "\n"
                + "caching: " + routing.getCaching() + "\n";

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(routing.getRoutingId())
                .action(Action.UPDATE)
                .projectId(routing.getProjectId())
                .description(description)
                .build());
    }

    /* Update(edit) 로깅 */
    @Around("execution(* com.aolda.itda.service.forwarding.*Service.*edit*(..))")
    public Object editLogging(ProceedingJoinPoint joinPoint) throws Throwable {

        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getSession().getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 변경 전 엔티티 조회 */
        Object[] args = joinPoint.getArgs();

        Long id = (Long) args[0];
        Routing old = routingRepository.findByRoutingIdAndIsDeleted(id, false).orElse(null);
        if (old != null) {
            entityManager.detach(old);
        }

        /* 메소드 진행 */
        Object result = joinPoint.proceed();

        /* 변경 후 엔티티 조회 */
        Routing newObj = routingRepository.findByRoutingIdAndIsDeleted(id, false).orElse(null);

        /* 로그 메세지 작성 */
        String description = "name: " + old.getName() + (old.getName().equals(newObj.getName()) ? "" : (" -> " + newObj.getName())) + "\n"
                + "domain: " + old.getDomain() + (old.getDomain().equals(newObj.getDomain()) ? "" : (" -> " + newObj.getDomain())) + "\n"
                + "ip: " + (old.getInstanceIp().equals(newObj.getInstanceIp()) ? "" : (" -> " + newObj.getInstanceIp())) + "\n"
                + "port: " + (old.getInstancePort().equals(newObj.getInstancePort()) ? "" : (" -> " + newObj.getInstancePort())) + "\n"
                + "certificateId: " + (old.getCertificate().getCertificateId() == newObj.getCertificate().getCertificateId() ? "" : (" -> " + newObj.getCertificate().getCertificateId()))
                + "certificateId: " + (old.getCaching() == newObj.getCaching() ? "" : (" -> " + newObj.getCaching()));

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(newObj.getRoutingId())
                .action(Action.UPDATE)
                .projectId(newObj.getProjectId())
                .description(description)
                .build());
        return result;
    }
}
