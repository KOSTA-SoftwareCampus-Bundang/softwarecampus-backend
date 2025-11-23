package com.softwarecampus.backend.dto.user;

import com.softwarecampus.backend.domain.common.AccountType;
import com.softwarecampus.backend.domain.user.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Account 엔티티의 Redis 캐싱용 DTO
 * Phase 13: UserDetails 캐싱을 위한 간소화된 Account 데이터
 * 
 * 역할:
 * - JPA 엔티티 직접 캐싱 문제 해결 (Lazy Loading, Proxy 객체, 복잡한 관계)
 * - JSON 직렬화/역직렬화 안정성 확보
 * - 인증/인가에 필요한 최소한의 데이터만 포함
 * 
 * @author Phase 13
 * @since 2025-11-19
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCacheDto implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String email;
    private String password;
    private AccountType accountType;
    
    /**
     * Account 엔티티를 DTO로 변환
     * 
     * @param account Account 엔티티
     * @return AccountCacheDto
     */
    public static AccountCacheDto from(Account account) {
        return AccountCacheDto.builder()
                .id(account.getId())
                .email(account.getEmail())
                .password(account.getPassword())
                .accountType(account.getAccountType())
                .build();
    }
}
