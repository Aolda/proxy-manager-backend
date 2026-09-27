# 포트포워딩 PROXY protocol

`proxyProtocol`은 생성·조회·수정 API의 Boolean 필드입니다. 기본값과 기존 NULL 값은 false로 처리합니다. 생성 시 생략하면 비활성화되며, PATCH에서 생략하면 기존 설정을 유지합니다.

관리자만 이 필드를 전송할 수 있습니다. 일반 사용자가 true 또는 false를 명시하면 UNAUTHORIZED_USER로 거부합니다. 따라서 일반 사용자가 이름 등을 수정해도 관리자가 설정한 값은 바뀌지 않습니다.

true일 때만 해당 stream server에 `proxy_protocol on;`을 생성합니다. 기존 설정 파일은 배포만으로 다시 생성되지 않습니다. 대상 서비스의 PROXY protocol 수신 지원과 설정을 확인한 뒤 활성화합니다.

배포 전 운영 DB의 `forwarding.proxy_protocol` 컬럼 존재 여부를 확인합니다. 없으면 `proxy-protocol-migration.sql`을 한 번 적용합니다. 이 파일은 자동 실행되지 않습니다. Hibernate ddl-auto 설정은 배포 환경에서 제공하므로 스키마 자동 갱신 여부를 가정하지 않습니다. 백엔드와 DB를 먼저 갱신한 뒤 프론트엔드를 배포합니다.
