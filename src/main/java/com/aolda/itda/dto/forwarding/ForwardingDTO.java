package com.aolda.itda.dto.forwarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForwardingDTO {

    private Long id;

    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$",
            message = "잘못된 IP 형식 (server)")
    private String serverIp;

    @NotBlank(message = "serverPort 값이 존재하지 않습니다")
    @Pattern(regexp = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$",
            message = "잘못된 포트 형식 (server)")
    private String serverPort;

    @NotBlank(message = "instanceIp 값이 존재하지 않습니다")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}(?:25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$",
            message = "잘못된 IP 형식 (instance)")
    private String instanceIp;

    @NotBlank(message = "instancePort 값이 존재하지 않습니다")
    @Pattern(regexp = "^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$",
            message = "잘못된 포트 형식 (instance)")
    private String instancePort;

    @NotBlank(message = "name 값이 존재하지 않습니다")
    private String name;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
