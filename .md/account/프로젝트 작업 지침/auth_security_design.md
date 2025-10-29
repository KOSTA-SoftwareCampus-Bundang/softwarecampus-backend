# 로그인/회원가입/보안 기능 상세 설계안

**담당자:** 태윤
**목표:** Spring Boot와 Spring Security를 사용하여 안정적인 인증 및 인가 기능을 구현합니다.

---

### **1단계: 데이터베이스 모델링 (`Account` 엔티티)**

-   **목표:** `DB.sql`의 `account` 테이블을 Java 객체로 맵핑하여 JPA가 관리할 수 있도록 합니다.
-   **생성 파일:** `src/main/java/com/softwarecampus/backend/domain/Account.java`
-   **주요 코드:**
    ```java
    @Entity
    @Table(name = "account")
    public class Account {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false, unique = true)
        private String email;

        @Column(nullable = false)
        private String password; // 암호화된 비밀번호 저장

        @Column(nullable = false)
        private String nickname;

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private AccountType accountType; // 예: USER, ACADEMY, ADMIN

        // 주소, 소속, 직책 등 기타 필드 추가
        private String address;
        private String affiliation;
        private String position;

        // 생성일, 수정일 등 BaseEntity 또는 Auditing 기능으로 자동화
    }

    public enum AccountType {
        USER, ACADEMY, ADMIN
    }
    ```
-   **핵심:** `password` 필드에는 반드시 암호화된 문자열이 저장되어야 합니다.

---

### **2단계: 데이터 접근 계층 (`AccountRepository`)**

-   **목표:** `Account` 엔티티에 대한 데이터베이스 CRUD 작업을 위한 인터페이스를 정의합니다.
-   **생성 파일:** `src/main/java/com/softwarecampus/backend/repository/AccountRepository.java`
-   **주요 코드:**
    ```java
    public interface AccountRepository extends JpaRepository<Account, Long> {
        // 이메일로 사용자를 찾는 기능 (로그인, 중복 체크 시 사용)
        Optional<Account> findByEmail(String email);

        // 이메일 존재 여부 확인 기능 (회원가입 중복 체크 시 사용)
        boolean existsByEmail(String email);
    }
    ```
-   **핵심:** Spring Data JPA가 인터페이스 선언만으로도 필요한 SQL을 자동으로 생성해줍니다.

---

### **3단계: 회원가입 기능 구현**

-   **목표:** 클라이언트로부터 사용자 정보를 받아 데이터베이스에 새로운 사용자를 등록합니다.
-   **API 명세:**
    -   **Endpoint:** `POST /api/auth/signup`
    -   **Request Body (JSON):**
        ```json
        {
          "email": "test@example.com",
          "password": "password123!",
          "nickname": "테스트유저"
        }
        ```
    -   **Success Response:** `201 Created`
    -   **Error Responses:**
        -   `400 Bad Request`: 입력값 유효성 검증 실패 시
        -   `409 Conflict`: 이미 존재하는 이메일일 경우
-   **주요 로직 (AuthService):**
    1.  Request DTO(`SignupRequestDto`)로 받은 데이터의 유효성을 검증합니다.
    2.  `accountRepository.existsByEmail()`로 이메일 중복 여부를 확인합니다.
    3.  `PasswordEncoder`를 사용하여 비밀번호를 암호화합니다.
    4.  암호화된 비밀번호와 함께 `Account` 객체를 생성하여 `accountRepository.save()`로 저장합니다.

---

### **4단계: 보안 설정 (Spring Security)**

-   **목표:** 애플리케이션의 전반적인 보안 규칙을 설정합니다.
-   **생성 파일:** `src/main/java/com/softwarecampus/backend/config/SecurityConfig.java`
-   **주요 설정 (SecurityFilterChain):**
    -   `PasswordEncoder`로 `BCryptPasswordEncoder`를 Bean으로 등록합니다.
    -   URL별 접근 권한 설정:
        -   `permitAll()`: `/api/auth/**` (회원가입, 로그인 등)
        -   `authenticated()`: `/api/users/**` (마이페이지 등)
    -   로그인/로그아웃 처리, JWT 필터 연동 등을 설정합니다.
-   **핵심:** `UserDetailsService`를 구현하여 Spring Security가 로그인 시도 시 `AccountRepository::findByEmail`을 호출하도록 연결해야 합니다.

---

### **5단계: 로그인 기능 구현**

-   **목표:** 이메일과 비밀번호로 사용자를 인증하고, 인증 성공 시 접근 토큰(JWT)을 발급합니다.
-   **API 명세:**
    -   **Endpoint:** `POST /api/auth/login`
    -   **Request Body (JSON):**
        ```json
        {
          "email": "test@example.com",
          "password": "password123!"
        }
        ```
    -   **Success Response:** `200 OK`
        ```json
        {
          "accessToken": "ey..."
        }
        ```
    -   **Error Response:** `401 Unauthorized` (인증 실패 시)
-   **주요 로직 (AuthService):**
    1.  Spring Security의 `AuthenticationManager`를 통해 사용자 인증을 시도합니다.
    2.  인증이 성공하면, 해당 사용자의 정보(`email`, `role`)를 바탕으로 JWT를 생성합니다.
    3.  생성된 `accessToken`을 DTO에 담아 클라이언트에 반환합니다.

---

### **6단계: 마이페이지 기능 구현**

-   **목표:** 인증된 사용자가 자신의 정보를 조회할 수 있는 API를 제공합니다.
-   **API 명세:**
    -   **Endpoint:** `GET /api/users/me`
    -   **Request Header:** `Authorization: Bearer <accessToken>`
    -   **Success Response:** `200 OK`
        ```json
        {
          "email": "test@example.com",
          "nickname": "테스트유저",
          "address": "서울시 강남구",
          "affiliation": "소프트웨어캠퍼스"
        }
        ```
    -   **Error Response:** `401 Unauthorized` (토큰이 없거나 유효하지 않을 경우)
-   **주요 로직 (UserService):**
    1.  Security Context에서 현재 인증된 사용자의 이메일 정보를 가져옵니다.
    2.  `accountRepository.findByEmail()`로 사용자의 전체 정보를 조회합니다.
    3.  비밀번호 등 민감한 정보를 제외하고, 클라이언트에 보여줄 정보만 DTO(`MyPageResponseDto`)에 담아 반환합니다.
