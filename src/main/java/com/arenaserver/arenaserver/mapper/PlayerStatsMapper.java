package com.arenaserver.arenaserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlayerStatsMapper {

    @Update("""
    UPDATE player_stats
    SET wins = wins + 1
    WHERE player_id = #{player_id}
    """)
    int incrementWins(@Param("playerId") String playerId);
}
