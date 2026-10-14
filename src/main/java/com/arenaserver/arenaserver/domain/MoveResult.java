package com.arenaserver.arenaserver.domain;

/**
 * 每次落子后，服务端收到的信息
 * @param row   落在哪一行
 * @param col   落在哪一列
 * @param disc  哪个玩家（红色或黄色）
 * @param status    游戏状态
 * @param winner    谁赢了
 */
public record MoveResult(
        int row,
        int col,
        Disc disc,
        GameStatus status,
        Disc winner /* 约定 empty 时表示游戏进行中或平局 */
) {}
