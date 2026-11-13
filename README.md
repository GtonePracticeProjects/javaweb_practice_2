# Sample Bank — Spring Boot + JPA/Hibernate + QueryDSL + Vue + MariaDB

은행 계정/계좌 관리 예제입니다. **JPA 쿼리 작성 방식 4가지**(Spring Data 파생 쿼리 / JPQL / Native SQL / QueryDSL)를 한 프로젝트에서 모두 사용합니다.

```
backend/    Spring Boot 3.3 (Java 17+), Spring Data JPA, Hibernate 6, QueryDSL 5, MariaDB
frontend/   Vue 3 + Vite + vue-router + axios
db/init.sql DB/계정 생성 스크립트
docker-compose.yml  MariaDB 11.4
```

## 실행

```bash
# 1) DB (Docker 없이 쓰려면 db/init.sql 을 직접 MariaDB에 실행)
docker compose up -d

# 2) 백엔드  (http://localhost:8080)  — 최초 실행 시 테이블 + 데모 데이터 자동 생성
cd backend
mvn spring-boot:run

# 3) 프론트엔드 개발 서버 (http://localhost:5173, /api 는 8080 으로 프록시)
cd frontend
npm install
npm run dev
```

`npm run build` 를 하면 결과물이 `backend/src/main/resources/static` 으로 복사되어 백엔드 하나(`localhost:8080`)로 화면까지 서비스됩니다.

DB 접속 정보는 환경변수로 변경 가능: `DB_URL`, `DB_USER`, `DB_PASSWORD` (기본 `jdbc:mariadb://localhost:3306/bankdb`, `bank` / `bankpw`).

데모 계정: `hong / Hong1234!`, `kim / Kim12345!`, 관리자 `admin / Admin1234!`

테스트(H2, MariaDB 모드)는 DB 없이 실행됩니다: `cd backend && mvn test`

## 쿼리 방식별 위치

| 방식 | 위치 | 용도 |
|---|---|---|
| **파생 쿼리** (`findByXxx`) | `MemberRepository`, `AccountRepository`, `AccountTransactionRepository.findByAccountId` | 로그인ID 조회, 중복 체크, 계좌 목록, 페이징 이력 (`@EntityGraph` 포함) |
| **JPQL `@Query`** | 각 `*Repository` | fetch join, `select new` 생성자 표현식(DTO), 집계(`sum`), `@Lock(PESSIMISTIC_WRITE)` 행 잠금, `@Modifying` 벌크 UPDATE(휴면 처리) |
| **Native SQL** `nativeQuery=true` | `MemberRepository.findByEmailDomainNative`(엔티티 반환), `AccountRepository.summarizeByType`, `AccountTransactionRepository.monthlyReport`(인터페이스 프로젝션) | 통계/리포트성 SQL |
| **QueryDSL** | `MemberRepositoryImpl`(동적 where + left join + group by + having + 페이징), `AccountTransactionRepositoryImpl`(BooleanBuilder 동적 검색, `Projections.constructor`, 월별 group by) | 관리자 회원 검색, 내 거래 조회, 월별 통계 |

Q클래스는 컴파일 시 `target/generated-sources/annotations` 에 생성됩니다(IDE에서 해당 폴더를 Generated Sources 로 인식시켜 주세요).

## Hibernate / 은행 도메인 포인트

- `@Version` 낙관적 락 + 입출금·이체 시 `select ... for update`(비관적 락). 이체는 계좌번호 정렬 순서로 락을 잡아 교차 이체 데드락을 방지 (`AccountService.transfer`)
- 이체는 한 트랜잭션에서 출금·입금·원장 2건 기록. 1일 이체 한도(JPQL 집계) 체크
- 로그인 실패 5회 시 계정 잠금 — 예외를 던져도 실패 횟수가 커밋되도록 `noRollbackFor` (`AuthService.login`)
- `@DynamicUpdate`, `@CreatedDate/@LastModifiedDate`(Auditing), `default_batch_fetch_size`, `open-in-view=false`
- 벌크 JPQL UPDATE 는 영속성 컨텍스트/Auditing 을 우회하므로 `clearAutomatically` 와 `updatedAt` 수동 세팅
- 비밀번호는 BCrypt, 수신 계좌 조회 시 예금주명 마스킹, 타인 계좌 접근은 404

## REST API 요약

| | |
|---|---|
| `POST /api/auth/signup · login · logout`, `GET /api/auth/me` | 가입 / 세션 로그인 |
| `PUT /api/members/me`, `PUT /api/members/me/password` | 정보/비밀번호 변경 |
| `GET·POST /api/accounts`, `GET·DELETE /api/accounts/{id}` | 목록(+총자산) / 개설 / 상세 / 해지 |
| `POST /api/accounts/{id}/deposit · withdraw`, `POST /api/accounts/transfer` | 입금 / 출금 / 이체 |
| `GET /api/accounts/lookup?accountNumber=` | 예금주 조회 |
| `GET /api/accounts/{id}/transactions · summary · monthly` | 파생쿼리 / JPQL / Native |
| `GET /api/transactions?type=&minAmount=&from=&to=&keyword=` | QueryDSL 동적 검색 |
| `GET /api/admin/members`, `PATCH …/{id}/status`, `POST …/dormant-batch` | (ADMIN) QueryDSL 검색 / 상태 변경 / JPQL 벌크 |
| `GET /api/admin/stats/accounts · members · transactions` | (ADMIN) Native / JPQL / QueryDSL 통계 |

## 프론트 → 백엔드 호출 방식 데모 (메뉴: "호출 방식 데모", `#/demo`)

같은 API 를 여러 방식으로 호출하는 화면입니다. 각 화면의 "핵심 코드 보기"에 실제 사용한 코드가 있습니다.

| 화면 | 방식 | 코드 위치 |
|---|---|---|
| `/demo/axios` | axios 인스턴스, 인터셉터, 타임아웃, `AbortController` 취소, 병렬/순차 | `api/http.js`, `views/demo/AxiosDemo.vue` |
| `/demo/fetch` | 내장 fetch 래퍼(`res.ok` 처리), 디바운스 + 이전 요청 취소 | `api/fetchClient.js` |
| `/demo/composable` | `useApi` 컴포저블 (loading/error/data, 경쟁 상태 방지) | `composables/useApi.js` |
| `/demo/pinia` | Pinia 스토어로 형제 컴포넌트 간 서버 데이터 공유 | `stores/accounts.js`, `components/AccountStore*.vue` |
| `/demo/query` | TanStack Query: 캐시/staleTime, mutation 후 무효화, 페이지별 캐시, 자동 재시도 | `views/demo/QueryDemo.vue` |
| `/demo/jwt` | JWT Bearer 토큰 (쿠키 없이 `Authorization` 헤더) | `api/tokenClient.js`, 백엔드 `JwtProvider`, `POST /api/auth/token` |
| `/demo/realtime` | SSE(`EventSource`) 서버 푸시 vs 폴링 | 백엔드 `NotificationService`, `GET /api/notifications/stream` |
| `/demo/download` | Blob 파일 다운로드 + 진행률 | 백엔드 `GET /api/transactions/export` (CSV, 수식 주입 방지) |

이를 위해 백엔드에 추가된 것: JWT 발급/검증(`AuthInterceptor` 가 세션 또는 Bearer 토큰 둘 다 허용), SSE 알림(이체 커밋 후 수신자에게 푸시), CSV 내보내기, 데모용 `/api/demo/slow`, `/api/demo/fail`.
JWT 서명 키는 `JWT_SECRET` 환경변수로 바꾸세요(기본값은 개발용).

## 참고 (예제 단순화 사항)

- 인증은 JPA 에 집중하기 위해 Spring Security 대신 세션 + 인터셉터(`AuthInterceptor`)로 구현했습니다. 실서비스에서는 Spring Security(CSRF, 세션 정책 등)를 사용하세요.
- `ddl-auto=update` 는 예제용입니다. 운영에서는 Flyway/Liquibase + `validate` 를 권장합니다.
- 금액은 `BigDecimal(19,2)` 로 다룹니다.
