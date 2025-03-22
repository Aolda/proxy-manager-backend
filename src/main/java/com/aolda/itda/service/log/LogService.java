package com.aolda.itda.service.log;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.log.LogDTO;
import com.aolda.itda.entity.log.Log;
import com.aolda.itda.exception.CustomException;
import com.aolda.itda.exception.ErrorCode;
import com.aolda.itda.repository.log.LogRepository;
import com.aolda.itda.repository.log.LogQueryDSL;
import com.aolda.itda.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LogService {

    private final LogRepository logRepository;
    private final LogQueryDSL logQueryDSL;
    private final AuthService authService;

    /* CUD 로그 조회 */
    public LogDTO getLog(Long logId, List<String> projects) {
        Log log = logRepository.findById(logId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_LOG));

        /* 프로젝트 권한 검증 */
        authService.validateProjectAuth(projects, log.getProjectId());

        return log.toLogDTO();
    }

    /* CUD 로그 목록 조회 */
    public PageResp<LogDTO> getLogs(String projectId, String type, String username, String action, Boolean isASC,
                                    Pageable pageable, Map<String, String> user) {

        if (projectId == null) {
            try {
                if(!authService.isAdmin(user)) throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
            }
            catch (Exception e) {
                throw new CustomException(ErrorCode.UNAUTHORIZED_USER);
            }
        }

        return logQueryDSL.getLogs(projectId, type, username, action, isASC, pageable);
    }

}
