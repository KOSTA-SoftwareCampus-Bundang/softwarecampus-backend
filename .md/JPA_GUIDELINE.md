# JPA 기반 테이블 네이밍 가이드

---

---

- 수정 이력
    - 2025/6/17 : 가이드 1차 작성
    - 2025/10/19 : 내용상 일부 오류 수정

---

## ❓테이블 네이밍 가이드란?

- 팀 내 개발에서 사용되는 테이블에 관련된 전반적인 명명 규칙입니다.
- JPA의 자동 생성에 맞춰, 가능한 예외가 발생하지 않도록 규칙을 통일하는 것을 목표로 합니다.

---

## ✅ 1. 기본 원칙

- 테이블명, 컬럼명은 모두 **snake_case** 사용
- 클래스명은 **PascalCase**, 필드명은 **camelCase**로 정의
    - Spring Boot 기본 네이밍 전략에 따라 camelCase → snake_case로 변환
    - 컬럼명은 자동 생성되며, **필요할 경우에만 명시적으로 지정**

- **언더바 개수가 과하게 늘어나지 않도록, 한 단어로 취급하는 단어는 묶을 것**
    - preferWorkStyle (x)
        - JPA 변환을 거치면 prefer_work_style이 됨
    - preferWorkstyle (o)
        - JPA 변환을 거치면 prefer_workstyle이 됨

---

## ✅ 2. 테이블 네이밍 규칙

- 주체 기반 + 기능 또는 하위 항목 우선
- JPA 객체명을 PascalCase로 명명
    - 최종 테이블명은 JPA에 의해 `snake_case` 로 변환됨
    - 과도하게 길어지지 않도록 가능한 2-3단어 안에 끝나도록
    - 파생 테이블일 경우, Detail 등 접미사를 붙임
    - 최종 테이블명이 25자~30자를 초과하지 않도록 길이에 신경써서 명명
- JPA 객체명 예시 :
    - `BookRent` (일반 테이블)
    - `BookRentDetail` (파생 테이블 : Detail 등을 붙여서 표현)
- 최종 테이블명 예시:
    - `book_rent`
    - `book_rent_detail`

---

## ✅ 3. 컬럼 네이밍 규칙 예시 (자동 변환)

| 항목 | 클래스/필드명 | 자동 생성 컬럼명 |
| --- | --- | --- |
| PK | `id` | `id` |
| FK | `user` | `user_id` |
| 일반 필드 | `createdAt` | `created_at` |
| 복합 객체명 | `bookRent` | `book_rent_id` |

---

## ✅ 4. 기본키 (PK) 컨벤션

- 모든 엔티티의 PK는 필드명을 **무조건 `id`** 로 통일
    - JPA의 자동 생성 규칙에 맞춤
- 예시:

```java
@Entity
public class BookRent {
    @Id
    private Long id; // → 컬럼명: id
}
```

---

## ✅ 5. 외래키 (FK) 컨벤션

- 필드명은 참조 객체명 (예: `user`, `bookRent`)
- 자동 생성 컬럼명은 `<객체명>_id` 형식이 됨
- 예시:

```java
@ManyToOne
private User user; // → 컬럼명: user_id

@ManyToOne
private BookRent bookRent; // → 컬럼명: book_rent_id
```

- 단, 참조 대상의 PK가 `id`가 아닌 경우에는 명시적으로 `@JoinColumn(name = "...")` 지정 필요

```java
@ManyToOne
@JoinColumn(name = "book_rent_uid") // 예외 상황 처리
private BookRent bookRent;
```

---

## ✅ 6. 예외적 명시가 필요한 경우

- 참조 대상의 PK 컬럼명이 `id`가 아닌 경우 (예: `book_rent_uid`)
- 자동 생성된 컬럼명이 중복되거나 과도하게 길어질 수 있는 경우(예: 조인 테이블에서 `user_user_id` 등)
- 이런 경우에만 `@JoinColumn(name = ...)` 혹은 `@Column(name = ...)`으로 명시적으로 사용

---

## ✅ 7. 예시 클래스 구조

```java
@Entity
public class BookRent {
    @Id
    private Long id;
    private LocalDateTime rentDate;
}
```

```java
@Entity
public class BookRentDetail {
    @Id
    private Long id;
    @ManyToOne
    private BookRent bookRent; // → book_rent_id 자동 생성
}
```

---

## ✅ 8. 추가 팁

- 시간 컬럼은 `created_at`, `updated_at`으로 명명
    - 스프링 데이터 감사 기능 사용: `@CreatedDate`, `@LastModifiedDate`
    - 전제: `@EnableJpaAuditing` 활성화, 엔티티에 `@EntityListeners(AuditingEntityListener.class)` 적용
- 컬럼 충돌이나 예상과 다른 스키마가 생성되었을 경우 로그에서 확인
    - `spring.jpa.hibernate.ddl-auto=create`로 DDL(스키마) 생성 로그 확인
    - `spring.jpa.show-sql=true`는 실행 SQL(DML) 출력용이며 DDL과 용도가 다름
- Enum은 `@Enumerated(EnumType.STRING)` 사용

---

## ✅ 요약

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| PK | `id` | `id` |
| FK | 참조 객체명 | `user` → `user_id` |
| 자동 컬럼명 | camelCase → snake_case | `createdAt` → `created_at` |
| 명시 필요한 경우 | PK가 `id`가 아닐 때 | `@JoinColumn(name = "...")` |

---

