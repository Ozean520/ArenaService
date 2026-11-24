package com.arenaserver.arenaserver.room;

import com.arenaserver.arenaserver.domain.FinishedGameResult;
import com.arenaserver.arenaserver.domain.MoveResult;

public record RoomPlayOutcome(
        MoveResult moveResult,
        FinishedGameResult newlyFinishedGame /* 约定普通合法落子时为 null */
) {}
