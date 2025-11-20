package com.softwarecampus.backend.repository.banner;

import com.softwarecampus.backend.domain.banner.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {

    /**
     *  메인 페이지 노출용: 활성화 상태이며 삭제되지 않은 배너를 순서대로 조회
     */
    List<Banner> findByIsActivatedTrueAndIsDeletedFalseOrderBySequenceAsc();

    /**
     *  관리자용 : 삭제되지 않은 모든 배너를 순서대로 조회
     */
    List<Banner> findByIsDeletedFalseOrderBySequenceAsc();

}
