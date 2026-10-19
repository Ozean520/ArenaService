package com.arenaserver.arenaserver.websocket;

import com.arenaserver.arenaserver.room.RoomManager;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {
    private final ObjectMapper objectMapper;
    private final RoomManager roomManager;

    public GameWebSocketHandler(ObjectMapper objectMapper,
                                RoomManager roomManager) {
        this.objectMapper = objectMapper;
        this.roomManager = roomManager;
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
        String json = objectMapper.writeValueAsString(data);

        session.sendMessage(new TextMessage(json));
    }

    @Override
    public void afterConnectionEstablished (@Nonnull WebSocketSession session) throws Exception {

        sendJson(session, Map.of(
                "type", "WELCOME",
                "playerId", session.getId()
        ));
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
            }
            case "MOVE" -> {

            }
            default -> sendError(session, "UNKNOWN_TYPE", "Invalid message type");
        }

        System.out.println("Received: " + message.getPayload());
    }
}
