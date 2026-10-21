package com.arenaserver.arenaserver.room;

import com.arenaserver.arenaserver.domain.Game;
import com.arenaserver.arenaserver.domain.GameSnapshot;
import com.arenaserver.arenaserver.domain.GameStatus;
import com.arenaserver.arenaserver.domain.MoveResult;
import lombok.Getter;

import java.util.List;

public class GameRoom {

    @Getter
    private final String roomId;

    private final String hostPlayerId;
    private String guestPlayerId;

    private Game game;

    public GameRoom(
            String roomId,
            String hostPlayerId
    ) {
        if (roomId == null || roomId.isBlank() ||
        hostPlayerId == null || hostPlayerId.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid room or host id");
        }

        this.roomId = roomId;
        this.hostPlayerId = hostPlayerId;
    }

    /**
     * 获取房间状态
     * 游戏状态为 null 时即没有创建游戏，则房间处于等待状态
     * @return 房间的状态
     */
    public synchronized RoomStatus getStatus() {
        if (game == null) return RoomStatus.WAITING;
        return game.getStatus() == GameStatus.PLAYING ?
                RoomStatus.PLAYING : RoomStatus.FINISHED;
    }

    /**
     * 加入房间方法
     * 应当实现如下功能：
     * 1. 验证 playerId
     * 2. 若玩家在房间里则不让进
     * 3. 检查房间是否允许加入
     * 4. 设置 guestPlayerId
     * 5. 创建 Game 开始游戏
     * @param playerId 加入房间的玩家Id
     * @return 是否加入成功
     */
    public synchronized boolean join(String playerId) {
        // 1
        if (playerId == null || playerId.isBlank())
            return false;

        // 2
        if (playerId.equals(hostPlayerId) ||
                playerId.equals(guestPlayerId))
            return false;

        // 3
        if (guestPlayerId != null)
            return false;

        /* 假设 game 未能成功创建，中途抛出异常，会导致房间有玩家但启动失败 */
        Game newGame = new Game(hostPlayerId, playerId);

        // 4
        guestPlayerId = playerId;

        /* 5，到这里说明游戏正常启动了 */
        game = newGame;

        return true;
    }

    /**
     * 进行游戏方法
     * 该方法应该满足：
     * 1. 检查游戏是否已开始
     * 2. 进行游戏
     * @param playerId 玩家 ID
     * @param col 操作的列
     * @return 本次操作的结果
     */
    public synchronized MoveResult play(String playerId, int col) {
        // 1
        if (getStatus() != RoomStatus.PLAYING)
            throw new IllegalStateException("Game is not playing");

        // 2
        return game.play(playerId, col);
    }

    public synchronized int getMoveCount() {
        return game == null ? 0 : game.getMoveCount();
    }

    /**
     * 获取房间玩家列表
     * @return 玩家列表
     */
    public synchronized List<String> getPlayerIds() {
        if (guestPlayerId == null)
            return List.of(hostPlayerId);

        return  List.of(hostPlayerId ,guestPlayerId);
    }

    /**
     * 游戏房间提供游戏快照
     * @param playerId 请求快照的玩家 ID
     * @return  游戏快照
     */
    public synchronized GameSnapshot snapshotFor(String playerId) {
        if (playerId == null || playerId.isBlank())
            throw new IllegalArgumentException("Invalid playerId");

        /* 只有游戏内玩家有权请求快照 */
        if (!playerId.equals(hostPlayerId) && !playerId.equals(guestPlayerId))
            throw new IllegalArgumentException("Player not in the room");

        if (game == null)
            throw new IllegalStateException("Game has not started");

        return game.snapshot();
    }
}
