package com.arenaserver.arenaserver.domain;

import java.time.LocalDateTime;

public record FinishedGameResult(
        String gameId,
        String redPlayerId,
        String yellowPlayerId,
        Disc winner,
        int moveCount,
        LocalDateTime finishedAt
) {}
