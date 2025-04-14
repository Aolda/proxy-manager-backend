package com.aolda.itda.repository.log;

import com.aolda.itda.dto.PageResp;
import com.aolda.itda.dto.auth.QIdAndNameDTO;
import com.aolda.itda.dto.log.LogDTO;
import com.aolda.itda.dto.log.QLogDTO;
import com.aolda.itda.entity.log.Action;
import com.aolda.itda.entity.log.ObjectType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.aolda.itda.entity.log.QLog.*;
import static com.aolda.itda.entity.user.QUser.*;

@Repository
@RequiredArgsConstructor
public class LogQueryDSL {

    private final JPAQueryFactory jpaQueryFactory;

    /* log 목록 반환 */
    public PageResp<LogDTO> getLogs(String projectId, String type,
                                    String username, String action, Boolean isASC, Pageable pageable) {

        List<LogDTO> content = jpaQueryFactory
                .select(new QLogDTO(
                        log.logId,
                        new QIdAndNameDTO(user.keystoneId, user.keystoneUsername),
                        log.action,
                        log.objectType,
                        log.objectId,
                        log.description,
                        log.createdAt
                )).from(log)
                .join(log.user, user).on(log.user.eq(user))
                .where(getFilter(projectId, type, username, action))
                .orderBy(isASC ? log.logId.asc() : log.logId.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> cnt = jpaQueryFactory
                .select(log.count())
                .from(log)
                .where(getFilter(projectId, type, username, action));

        Page<LogDTO> page = PageableExecutionUtils.getPage(content, pageable, cnt::fetchOne);

        return PageResp.<LogDTO>builder()
                .contents(page.getContent())
                .first(page.isFirst())
                .last(page.isLast())
                .size(page.getSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .build();
    }

    /* Where 필터 */
    private BooleanBuilder getFilter(String projectId, String type,
                                     String username, String action) {
        BooleanBuilder builder = new BooleanBuilder();

        /* 프로젝트 조건 */
        if (projectId != null) {
            builder.and(log.projectId.eq(projectId));
        }

        /* 오브젝트 타입 조건 */
        if (type != null) {
            switch (type) {
                case "certificate" -> builder.and(log.objectType.eq(ObjectType.CERTIFICATE));
                case "forwarding" -> builder.and(log.objectType.eq(ObjectType.FORWARDING));
                case "routing" -> builder.and(log.objectType.eq(ObjectType.ROUTING));
            }
        }


        /* 사용자 ID 조건 */
        if (username != null) {
            builder.and(log.user.keystoneUsername.contains(username));
        }

        /* CUD 조건 */
        if (action != null) {
            switch (action) {
                case "create" -> builder.and(log.action.eq(Action.CREATE));
                case "update" -> builder.and(log.action.eq(Action.UPDATE));
                case "delete" -> builder.and(log.action.eq(Action.DELETE));
            }
        }
        return builder;
    }
}
