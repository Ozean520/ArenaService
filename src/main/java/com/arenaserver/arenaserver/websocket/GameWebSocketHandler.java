package com.arenaserver.arenaserver.websocket;

import com.arenaserver.arenaserver.domain.*;
import com.arenaserver.arenaserver.room.GameRoom;
import com.arenaserver.arenaserver.room.RoomManager;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final RoomManager roomManager;
    private final SessionManager sessionManager;

    public GameWebSocketHandler(ObjectMapper objectMapper,
                                RoomManager roomManager,
                                SessionManager sessionManager) {
        this.objectMapper = objectMapper;
        this.roomManager = roomManager;
        this.sessionManager = sessionManager;
    }

    private void sendError(
            WebSocketSession session,
            String code,
            String message
    ) throws IOException {

        sendJson(session, Map.of(
                "type", "ERROR",
                "code", code,
                "message", message
        ));
    }

    private void sendJson(WebSocketSession session,
                          Map<String, ?> data) throws IOException {

        sessionManager.sendTo(session.getId(), data);
    }

    /**
     * 建立连接
     * @param session 玩家 session
     * @throws Exception 异常
     */
    @Override
    public void afterConnectionEstablished (@Nonnull WebSocketSession session) throws Exception {

        /* 建立连接前先注册 */
        sessionManager.register(session);

        sendJson(session, Map.of(
                "type", "WELCOME",
                "playerId", session.getId()
        ));
    }

    /**
     * 连接关闭
     * @param session 断开的 session
     * @param status
     */
    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status) {

        sessionManager.unregister(session.getId());
    }

    /**
     * 消息处理方法
     * 该方法应该实现：
     * 1. 将 Json 转换为 JsonNode
     * 2. 读取消息类型
     * 3. 先解决房间创建的请求，即判断是否为 CREATE_ROOM
     * 4. 生成 UUID 作为 roomId
     * 5. 房主标识为 session ID
     * 6. 告知客户端房间创建成功
     * @param session 传进来的 session
     * @param message session 携带的 message
     * @throws Exception 如有异常则抛出
     */
    @Override
    protected void handleTextMessage(WebSocketSession session,
                                     TextMessage message) throws Exception {
        // 1
        JsonNode root;

        /* 处理 Json 格式不合法的情况 */
        try {
            root = objectMapper.readTree(message.getPayload());
        } catch (JacksonException e) {
            sendJson(session, Map.of(
                    "type", "ERROR",
                    "code", "INVALID_JSON",
                    "message", "Invalid JSON format"
            ));
            return;
        }

        if (root == null || !root.isObject()) {
            sendError(session, "INVALID_REQUEST", "Request must be a JSON object");
            return;
        }

        // 2
        JsonNode typeNode = root.get("type");

        if (typeNode == null ||
            !typeNode.isString() ||
            typeNode.asString().isBlank()) {

            sendError(session, "INVALID_REQUEST", "Invalid message type");
            return;
        }

        String type = typeNode.asString();

        // 3
        switch (type) {
            case "CREATE_ROOM" -> {
                // 4
                String roomId = UUID.randomUUID().toString();

                // 5
                String hostPlayerId = session.getId();

                roomManager.createRoom(roomId, hostPlayerId);

                sendJson(session, Map.of("type", "ROOM_CREATED",
                                        "roomId", roomId));
            }
            case "JOIN_ROOM" -> {
                /* 加入房间分支*/
                JsonNode roomNode = root.get("roomId");

                if (roomNode == null || !roomNode.isString() ||
                        roomNode.asString().isBlank()) {

                    sendError(session, "INVALID_REQUEST", "Invalid room id");
                    return;
                }

                String roomId = roomNode.asString();
                String playerId = session.getId();

                boolean joined;

                try {
                    joined = roomManager.joinRoom(roomId, playerId);
                } catch (IllegalArgumentException |
                        IllegalStateException e) {
                    sendError(session, "INVALID_REQUEST", "Room join failed");
                    return;
                }

                if (!joined) {
                    sendError(session, "INVALID_REQUEST", "Room join failed");
                    return;
                }

                List<String> playerIds = roomManager.getRoom(roomId).getPlayerIds();

                for (int i = 0; i < playerIds.size(); i++) {
                    String color = i == 0 ? "RED" : "YELLOW";

                    sessionManager.sendTo(playerIds.get(i), Map.of("type", "GAME_STARTED",
                            "roomId", roomId,
                            "yourDisc", color,
                            "currentTurn", "RED"));
                }

            }
            case "MOVE" -> {
                /* 实现落子部分 */
                JsonNode roomNode = root.get("roomId");

                if (roomNode == null ||
                    !roomNode.isString() ||
                    roomNode.asString().isBlank()) {

                    sendError(session, "INVALID_REQUEST", "Invalid room id");
                    return;
                }

                String roomId = roomNode.asString();

                JsonNode colNode = root.get("column");

                if  (colNode == null || !colNode.isInt()) {
                    sendError(session, "INVALID_REQUEST", "Invalid column");
                    return;
                }

                int col = colNode.asInt();

                String playerId = session.getId();

                MoveResult result;

                try {
                    result = roomManager.play(roomId, playerId, col);
                } catch (IllegalArgumentException |
                        IllegalStateException e) {
                    sendError(session, "INVALID_REQUEST", "Move not allowed");
                    return;
                }

                List<String> playerIds = roomManager.getRoom(roomId).getPlayerIds();

                String nextTurn;
                String nowTurn = result.disc().name();

                if (result.status() == GameStatus.FINISHED) nextTurn = Disc.EMPTY.name();
                else if (nowTurn.equals("RED")) nextTurn = "YELLOW";
                else nextTurn = "RED";


                for (String id : playerIds) {
                    sessionManager.sendTo(id, Map.of("type", "GAME_UPDATE",
                            "roomId", roomId,
                            "row", result.row(),
                            "column", result.col(),
                            "disc", nowTurn,
                            "status", result.status().name(),
                            "winner", result.winner().name(),
                            "nextTurn", nextTurn));
                }

            }
            case "SYNC_GAME" -> {
                /* 客户端请求快照 */
                JsonNode roomNode = root.get("roomId");

                if (roomNode == null ||
                    !roomNode.isString() ||
                    roomNode.asString().isBlank()) {
                    sendError(session, "INVALID_REQUEST", "Invalid room id");
                    return;
                }

                String roomId = roomNode.asString();

                String playerId = session.getId();

                GameRoom room;
                GameSnapshot snapshot;

                try {
                    room = roomManager.getRoom(roomId);
                    snapshot = room.snapshotFor(playerId);
                } catch (IllegalArgumentException |
                        IllegalStateException e) {
                    sendError(session, "INVALID_REQUEST", "Invalid room id");
                    return;
                }

                sessionManager.sendTo(playerId, Map.of("type", "GAME_SNAPSHOT",
                        "roomId", roomId,
                        "cells", snapshot.cells(),
                        "status", snapshot.status().name(),
                        "currentTurn", snapshot.currentTurn(),
                        "winner", snapshot.winner(),
                        "version", snapshot.version()
                ));
            }
            default -> sendError(session, "UNKNOWN_TYPE", "Invalid message type");
        }

        System.out.println("Received: " + message.getPayload());
    }
}
