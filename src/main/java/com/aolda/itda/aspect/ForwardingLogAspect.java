package com.aolda.itda.aspect;

import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.entity.log.Action;
import com.aolda.itda.entity.log.Log;
import com.aolda.itda.entity.log.ObjectType;
import com.aolda.itda.entity.user.User;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.certificate.CertificateRepository;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.repository.log.LogRepository;
import com.aolda.itda.repository.routing.RoutingRepository;
import com.aolda.itda.repository.user.UserRepository;
import com.aolda.itda.service.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
public class ForwardingLogAspect {

    private final ForwardingRepository forwardingRepository;
    private final LogRepository logRepository;
    private final EntityManager entityManager;
    private final UserRepository userRepository;

    /* Create 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.forwarding.*Service.*create*(..))"
    , returning = "result")
    public void createLogging(JoinPoint joinPoint, ForwardingDTO result) {

        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 생성된 엔티티 조회 */
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(result.getId(), false).orElse(null);

        /* 로그 메세지 작성 */
        String description = "name: " + forwarding.getName() + "\n"
                + "serverPort: " + forwarding.getServerPort() + "\n"
                + "instanceIp: " + forwarding.getInstanceIp() + "\n"
                + "instancePort: " + forwarding.getInstancePort();

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(forwarding.getForwardingId())
                .action(Action.CREATE)
                .projectId(forwarding.getProjectId())
                .description(description)
                .build());
    }

    /* Delete 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.forwarding.*Service.*delete*(..))")
    public void deleteLogging(JoinPoint joinPoint) {
        
        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 삭제된 엔티티 조회 */
        Object[] args = joinPoint.getArgs();

        Long id = (Long) args[0];
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(id, true).orElse(null);

        /* 로그 메세지 작성 */
        String description = "name: " + forwarding.getName() + "\n"
                + "serverPort: " + forwarding.getServerPort() + "\n"
                + "instanceIp: " + forwarding.getInstanceIp() + "\n"
                + "instancePort: " + forwarding.getInstancePort();

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(forwarding.getForwardingId())
                .action(Action.DELETE)
                .projectId(forwarding.getProjectId())
                .description(description)
                .build());
    }

    /* Update(edit) 로깅 */
    @Around("execution(* com.aolda.itda.service.forwarding.*Service.*edit*(..))")
    public Object editLogging(ProceedingJoinPoint joinPoint) throws Throwable {
        
        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 변경 전 엔티티 조회 */
        Object[] args = joinPoint.getArgs();

        Long id = (Long) args[0];
        Forwarding old = forwardingRepository.findByForwardingIdAndIsDeleted(id, false).orElse(null);
        if (old != null) {
            entityManager.detach(old);
        }
        
        /* 메소드 진행 */
        Object result = joinPoint.proceed();

        /* 변경 후 엔티티 조회*/
        Forwarding newObj = forwardingRepository.findByForwardingIdAndIsDeleted(id, false).orElse(null);
        
        /* 로그 메세지 작성 */
        String description = "name: " + old.getName() + (old.getName().equals(newObj.getName()) ? "" : (" -> " + newObj.getName())) + "\n"
                + "serverPort: " + old.getServerPort() + (old.getServerPort().equals(newObj.getServerPort()) ? "" : (" -> " + newObj.getServerPort())) + "\n"
                + "instanceIp: " + old.getInstanceIp() + (old.getInstanceIp().equals(newObj.getInstanceIp()) ? "" : (" -> " + newObj.getInstanceIp())) + "\n"
                + "instancePort: " + old.getInstancePort() + (old.getInstancePort().equals(newObj.getInstancePort()) ? "" : (" -> " + newObj.getInstancePort()));

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.FORWARDING)
                .objectId(newObj.getForwardingId())
                .action(Action.UPDATE)
                .projectId(newObj.getProjectId())
                .description(description)
                .build());
        return result;
    }



}
