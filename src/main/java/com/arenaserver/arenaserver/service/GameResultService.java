package com.arenaserver.arenaserver.service;

import com.arenaserver.arenaserver.domain.FinishedGameResult;
import com.arenaserver.arenaserver.mapper.GameResultMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class GameResultService {
    public enum SaveOutcome {
        CREATED,
        ALREADY_SAVED,
    }

    private final GameResultWriteService writeService;
    private final GameResultMapper resultMapper;

    public GameResultService(
            GameResultWriteService writeService,
            GameResultMapper resultMapper) {
        this.writeService = writeService;
        this.resultMapper = resultMapper;
    }

    public SaveOutcome saveFinishedGame(FinishedGameResult result) {
        try {
            writeService.insertAndUpdateStats(result);
            return SaveOutcome.CREATED;
        } catch (DuplicateKeyException e) {
            FinishedGameResult existing = resultMapper.findByGameId(result.gameId());

            if (result.equals(existing))
                return SaveOutcome.ALREADY_SAVED;

            throw new IllegalStateException("conflicting game result: " + result.gameId(),
                                            e
            );
        }
    }
}
