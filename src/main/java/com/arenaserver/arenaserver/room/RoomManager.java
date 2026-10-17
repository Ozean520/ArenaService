package com.arenaserver.arenaserver.room;

import com.arenaserver.arenaserver.domain.MoveResult;

import java.util.concurrent.ConcurrentHashMap;

public class RoomManager {
    private final ConcurrentHashMap<String, GameRoom> rooms = new ConcurrentHashMap<>();

    /**
     * 创建新房间方法，roomId 唯一
     * 该方法应满足：
     * 1. 创建 GameRoom
     * 2. 使用 putIfAbsent 注册房间，保证线程安全
     * 3. 处理 roomId 已存在的情况
     * @param roomId    房间 id
     * @param hostPlayerId  房主 id
     * @return  创建好的新房间
     */
    public GameRoom createRoom(String roomId, String hostPlayerId) {
        // 1
        GameRoom room = new GameRoom(roomId, hostPlayerId);

        // 2
        GameRoom existing = rooms.putIfAbsent(roomId, room);

        // 3
        if (existing != null) throw new IllegalStateException(
                "Room already exists"
        );

        return room;
    }

    /**
     * 根据 id 查找房间方法
     * @param roomId    房间 id
     * @return  查找到的房间
     */
    public GameRoom getRoom(String roomId) {
        if (roomId == null || roomId.isBlank())
            throw new  IllegalArgumentException("Invalid Room ID");

        GameRoom room = rooms.get(roomId);

        if  (room == null) throw new IllegalStateException(
                "Room does not exist"
        );

        return room;
    }

    /**
     * 玩家加入指定房间方法
     * @param roomId    房间 id
     * @param playerId  玩家 id
     * @return  是否加入成功
     */
    public boolean joinRoom(String roomId, String playerId) {
        return getRoom(roomId).join(playerId);
    }

    /**
     * 玩家在指定房间落子
     * @param roomId    房间 id
     * @param playerId  玩家 id
     * @param col   落子的列
     * @return  本次落子结果
     */
    public MoveResult play(String roomId, String playerId, int col) {
        return getRoom(roomId).play(playerId, col);
    }
}

