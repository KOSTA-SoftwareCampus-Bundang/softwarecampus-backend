package com.softwarecampus.backend.repository.board;

import com.softwarecampus.backend.domain.board.BoardRecommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardRecommendRepository extends JpaRepository<BoardRecommend, Long> {

    @Query("SELECT br from BoardRecommend br join br.board join br.account where br.board.id=:boardId and br.account.id=:userId")
    BoardRecommend findByBoardIdAndUserId(@Param("boardId") Long boardId, @Param("userId") Long userId);

}
