package com.arenaserver.arenaserver.domain;

import java.util.List;

/**
 * 游戏快照记录类
 * @param cells 棋盘
 * @param status    游戏状态
 * @param currentTurn   当前回合
 * @param winner    赢家
 * @param version   版本号，此处用 moveCount 暂替
 */
public record GameSnapshot(
        List<List<Disc>> cells,
        GameStatus status,
        Disc currentTurn,
        Disc winner,
        int version
) {}
