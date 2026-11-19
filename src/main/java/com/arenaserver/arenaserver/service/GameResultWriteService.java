package com.arenaserver.arenaserver.service;

import com.arenaserver.arenaserver.domain.Disc;
import com.arenaserver.arenaserver.domain.FinishedGameResult;
import com.arenaserver.arenaserver.mapper.GameResultMapper;
import com.arenaserver.arenaserver.mapper.PlayerStatsMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameResultWriteService {
    private final GameResultMapper resultMapper;
    private final PlayerStatsMapper statsMapper;

    public GameResultWriteService(
            GameResultMapper resultMapper,
            PlayerStatsMapper statsMapper) {
        this.resultMapper = resultMapper;
        this.statsMapper = statsMapper;
    }

    @Transactional
    public void insertAndUpdateStats(FinishedGameResult result) {
        int inserted = resultMapper.insert(result);

        if (inserted != 1)
            throw new IllegalStateException("failed to insert game result");

        if (result.winner() == Disc.EMPTY)
            return;

        String winnerId = switch (result.winner()) {
            case RED -> result.redPlayerId();
            case YELLOW -> result.yellowPlayerId();
            default -> throw new IllegalArgumentException("invalid winner");
        };

        int affected = statsMapper.incrementWins(winnerId);

        if (affected != 1)
            throw new IllegalStateException("failed to update wins : " + winnerId);
    }
}
