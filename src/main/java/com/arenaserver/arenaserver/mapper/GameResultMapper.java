package com.arenaserver.arenaserver.mapper;

import com.arenaserver.arenaserver.domain.FinishedGameResult;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface GameResultMapper {

    @Insert("""
        INSERT INTO game_result (
            game_id,
            red_player_id,
            yellow_player_id,
            winner,
            move_count,
            finished_at
        ) VALUES (
            #{gameId},
            #{redPlayerId},
            #{yellowPlayerId},
            #{winner},
            #{moveCount},
            #{finishedAt}
        )
        """)
    int insert(FinishedGameResult result);

    @Select("""
    SELECT
        game_id,
        red_player_id,
        yellow_player_id,
        winner,
        move_count,
        finished_at
    FROM game_result
    WHERE game_id = #{gameId}
    """)
    FinishedGameResult findByGameId(@Param("gameId") String gameId);
}
