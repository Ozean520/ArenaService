package com.arenaserver.arenaserver.domain;

import lombok.Getter;

public class Game {
    private final Board board;  // 当前棋盘

    private final String redPlayerId;   // 红方 ID
    private final String yellowPlayerId;    // 黄方 ID

    @Getter
    private GameStatus status;  // 游戏状态
    @Getter
    private Disc currentTurn;   // 当前是谁的回合
    @Getter
    private Disc winner;    // 赢家

    @Getter
    private int moveCount;  // 成功落子数

    public Game(String redPlayerId,
                String yellowPlayerId) {
        if (    redPlayerId == null ||
                yellowPlayerId == null ||
                redPlayerId.isEmpty() ||
                yellowPlayerId.isEmpty() ||
                redPlayerId.equals(yellowPlayerId)
        ) throw new IllegalArgumentException("Invalid players");

        this.redPlayerId = redPlayerId;
        this.yellowPlayerId = yellowPlayerId;

        this.board = new Board();

        this.status = GameStatus.PLAYING;
        this.currentTurn = Disc.RED;
        this.winner = Disc.EMPTY;
        this.moveCount = 0;
    }

    /**
     * 保证游戏流程能够正常进行
     * 该方法应当满足：
     * 1. 判断游戏是否结束
     * 2. 根据 playerId 确定玩家颜色
     * 3. 判断是否轮到该玩家
     * 4. 确保游戏落子能正常执行
     * 5. 确保服务端能接收到落子后的信息
     * 6. 判断游戏是否出现赢家或平局
     * 7. 返回 MoveResult
     * @param playerId 玩家 ID
     * @param col   棋子落下的列
     * @return  MoveResult
     */
    public MoveResult play(String playerId, int col) {
        // 1
        if (status != GameStatus.PLAYING) throw new IllegalStateException("Game over");

        // 2
        Disc player;
        if (playerId == null) throw new IllegalArgumentException("Invalid playerId");
        else if (playerId.equals(yellowPlayerId)) player = Disc.YELLOW;
        else if (playerId.equals(redPlayerId))  player = Disc.RED;
        else throw new IllegalArgumentException("Invalid players");

        // 3
        if (currentTurn != player) throw new IllegalStateException("Not correct turn");

        // 4
        int row = board.drop(col, player);

        // 5
        moveCount++;

        // 6
        if (board.hasFourFrom(row, col)) {
            winner = player;
            currentTurn = Disc.EMPTY;
            status = GameStatus.FINISHED;
        }
        else if (moveCount == Board.ROWS * Board.COLS) {
            currentTurn = Disc.EMPTY;
            status = GameStatus.FINISHED;
        } else {
            if (playerId.equals(yellowPlayerId)) currentTurn = Disc.RED;
            else currentTurn = Disc.YELLOW;
        }

        // 7
        return new MoveResult(
                row, col,
                player,
                status,
                winner
        );
    }

    public Disc getCell(int row, int col) {
        return board.getCell(row, col);
    }

    /**
     * 生成游戏快照方法
     * @return 游戏快照
     */
    public GameSnapshot snapshot() {
        return new GameSnapshot(
                board.snapshotCells(),
                status,
                currentTurn,
                winner,
                moveCount
        );
    }
}
