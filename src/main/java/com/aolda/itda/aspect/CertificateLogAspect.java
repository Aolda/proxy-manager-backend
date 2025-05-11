package com.aolda.itda.aspect;

import com.aolda.itda.dto.certificate.CertificateDTO;
import com.aolda.itda.entity.certificate.Certificate;
import com.aolda.itda.entity.log.Action;
import com.aolda.itda.entity.log.Log;
import com.aolda.itda.entity.log.ObjectType;
import com.aolda.itda.entity.user.User;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.certificate.CertificateRepository;
import com.aolda.itda.repository.log.LogRepository;
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
public class CertificateLogAspect {

    private final CertificateRepository certificateRepository;
    private final UserRepository userRepository;
    private final LogRepository logRepository;
    private final EntityManager entityManager;

    /* Create 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.certificate.*Service.*create*(..))"
            , returning = "result")
    public void createLogging(JoinPoint joinPoint, CertificateDTO result) {

        /* 사용자 조회 */
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        Map<String, String> tmp = (Map<String, String>) request.getAttribute("user");
        User user = userRepository.findByKeystoneId(tmp.get("id")).orElseThrow(
                () -> new CustomException(ErrorCode.NOT_FOUND_USER)
        );

        /* 생성된 엔티티 조회 */
        Certificate certificate = certificateRepository.findByCertificateIdAndIsDeleted(result.getId(), false).orElse(null);

        /* 로그 메세지 작성 */
        String description = "domain: " + certificate.getDomain() + "\n"
                + "email: " + certificate.getEmail() + "\n"
                + "challenge: " + certificate.getChallenge() + "\n"
                + "expiresAt: " + certificate.getExpiresAt();

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.CERTIFICATE)
                .objectId(certificate.getCertificateId())
                .action(Action.CREATE)
                .projectId(certificate.getProjectId())
                .description(description)
                .build());
    }

    /* Delete 로깅 */
    @AfterReturning(pointcut = "execution(* com.aolda.itda.service.certificate.*Service.*delete*(..))")
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
        Certificate certificate = certificateRepository.findByCertificateIdAndIsDeleted(id, true).orElse(null);

        /* 로그 메세지 작성 */
        String description = "domain: " + certificate.getDomain() + "\n"
                + "email: " + certificate.getEmail() + "\n"
                + "challenge: " + certificate.getChallenge() + "\n"
                + "expiresAt: " + certificate.getExpiresAt();

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.CERTIFICATE)
                .objectId(certificate.getCertificateId())
                .action(Action.DELETE)
                .projectId(certificate.getProjectId())
                .description(description)
                .build());
    }

    /* Update(edit) 로깅 */
    @Around("execution(* com.aolda.itda.service.certificate.*Service.*edit*(..))")
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
        Certificate old = certificateRepository.findByCertificateIdAndIsDeleted(id, false).orElse(null);
        if (old != null) {
            entityManager.detach(old);
        }

        /* 메소드 진행 */
        Object result = joinPoint.proceed();

        /* 변경 후 엔티티 조회 */
        Certificate newObj = certificateRepository.findByCertificateIdAndIsDeleted(id, false).orElse(null);

        /* 로그 메세지 작성 */
        String description = "domain: " + old.getDomain() + (old.getDomain().equals(newObj.getDomain()) ? "" : " -> " + newObj.getDomain()) + "\n"
                + "email: " + old.getEmail() + (old.getEmail().equals(newObj.getEmail()) ? "" : " -> " + newObj.getEmail()) + "\n"
                + "challenge: " + old.getChallenge() + (old.getChallenge() == newObj.getChallenge() ? "" : " -> " + newObj.getChallenge()) + "\n"
                + "expiresAt: " + old.getExpiresAt() + (old.getExpiresAt().equals(newObj.getExpiresAt()) ? "" : " -> " + newObj.getExpiresAt());

        /* 로그 엔티티 저장 */
        logRepository.save(Log.builder()
                .user(user)
                .objectType(ObjectType.CERTIFICATE)
                .objectId(newObj.getCertificateId())
                .action(Action.UPDATE)
                .projectId(newObj.getProjectId())
                .description(description)
                .build());
        return result;
    }
} 