package com.arenaserver.arenaserver.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PlayerStatsMapper {

    @Insert("""
        INSERT INTO player_stats (player_id, wins) 
        VALUES (#{playerId}, 1)
        ON DUPLICATE KEY UPDATE
                wins = wins + 1

    """)
    int incrementWins(@Param("playerId") String playerId);
}
