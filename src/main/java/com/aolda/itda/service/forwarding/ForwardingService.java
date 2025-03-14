package com.aolda.itda.service.forwarding;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.forwarding.ForwardingDTO;
import com.aolda.itda.entity.forwarding.Forwarding;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.forwarding.ForwardingRepository;
import com.aolda.itda.template.ForwardingTemplate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Transactional
@RequiredArgsConstructor
public class ForwardingService {

    @Value("${spring.server.base-ip}")
    private String serverBaseIp;
    private final ForwardingTemplate forwardingTemplate;
    private final ForwardingRepository forwardingRepository;

    /* 포트포워딩 정보 조회 */
    public ForwardingDTO getForwarding(Long forwardingId) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));
        return forwarding.toForwardingDTO();
    }

    /* 포트포워딩 목록 조회 */
    public PageResp<ForwardingDTO> getForwardings(String projectId) {

        return PageResp.<ForwardingDTO>builder()
                .contents(forwardingRepository.findByProjectIdAndIsDeleted(projectId, false)
                        .stream()
                        .map(Forwarding::toForwardingDTO)
                        .toList()).build();
    }

    /* 포트포워딩 생성 */
    public void createForwarding(String projectId, ForwardingDTO dto) {

        /* 입력 DTO 검증 */
        validateDTO(dto);

        /* 중복 검증 */
        if (forwardingRepository.existsByInstanceIpAndInstancePortAndIsDeleted(dto.getInstanceIp(), dto.getInstancePort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_INSTANCE_INFO);
        }

        if (forwardingRepository.existsByServerPortAndIsDeleted(dto.getServerPort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_SERVER_PORT);
        }

        /* 포트포워딩 엔티티 생성 */
        Forwarding forwarding = Forwarding.builder()
                .isDeleted(false)
                .projectId(projectId)
                .name(dto.getName())
                .serverIp(dto.getServerIp() == null ? serverBaseIp : dto.getServerIp())
                .serverPort(dto.getServerPort())
                .instanceIp(dto.getInstanceIp())
                .instancePort(dto.getInstancePort())
                .build();

        forwardingRepository.save(forwarding);

        /* nginx conf 파일 생성 및 예외 처리 */
        String content = forwardingTemplate.getPortForwardingWithTCP(dto.getServerPort(), dto.getInstanceIp(), dto.getInstancePort(), dto.getName());
        String confPath = "/data/nginx/stream/" + forwarding.getForwardingId() + ".conf";

        File file = new File(confPath);
        try {
            Path path = Paths.get(confPath);
            Files.createDirectories(path.getParent());
            if (!file.createNewFile()) {
                throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "중복된 포트포워딩 Conf 파일이 존재합니다");
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF);
        }

        /* conf 파일 작성 및 예외 처리 */
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true)); // 예외처리 필요
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (file.exists()) {
                file.delete();
            }
            throw new CustomException(ErrorCode.FAIL_CREATE_CONF, "포트포워딩 Conf 파일을 작성하지 못했습니다");
        }


    }

    /* 포트포워딩 정보 수정 */
    public void editForwarding(Long forwardingId, ForwardingDTO dto) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        /* 중복 검증 */
        if (forwardingRepository.existsByInstanceIpAndInstancePortAndIsDeleted(dto.getInstanceIp(), dto.getInstancePort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_INSTANCE_INFO);
        }

        if (forwardingRepository.existsByServerPortAndIsDeleted(dto.getServerPort(), false)) {
            throw new CustomException(ErrorCode.DUPLICATED_SERVER_PORT);
        }

        /* 정보 수정 */
        forwarding.edit(dto);
        forwardingRepository.save(forwarding);
    }

    /* 포트포워딩 삭제 (소프트) */
    public void deleteForwarding(Long forwardingId) {
        Forwarding forwarding = forwardingRepository.findByForwardingIdAndIsDeleted(forwardingId, false)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_FORWARDING));

        /* 파일 삭제 */
        String confPath = "/data/nginx/stream/" + forwarding.getForwardingId() + ".conf";
        File file = new File(confPath);
        if (!file.delete()) {
            throw new CustomException(ErrorCode.NOT_FOUND_FORWARDING, "Conf 파일이 존재하지 않아 삭제할 수 없습니다");
        }

        /* DB */
        forwarding.delete();
        forwardingRepository.save(forwarding);

    }

    /* 입력 DTO 검증 */
    private void validateDTO(ForwardingDTO dto) {

        for (ConstraintViolation<ForwardingDTO> violation : Validation.buildDefaultValidatorFactory().getValidator().validate(dto)) {
            throw new CustomException(ErrorCode.INVALID_CONF_INPUT, violation.getMessage());
        }

    }
}
