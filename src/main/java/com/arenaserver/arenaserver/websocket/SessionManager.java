package com.arenaserver.arenaserver.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    public SessionManager(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * session 注册
     * @param session 待注册的 session
     */
    public void register(WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                10_000,
                64 * 1024
        );

        sessions.put(session.getId(), safeSession);
    }

    /**
     * session 发送消息
     * @param playerId 玩家 ID
     * @param data  消息数据
     * @throws IOException
     */
    public void sendTo(String playerId, Map<String, ?> data) throws IOException {
        WebSocketSession session = sessions.get(playerId);

        if (session == null || !session.isOpen())
            throw new IOException("Player disconnected");

        String json =  objectMapper.writeValueAsString(data);

        session.sendMessage(new TextMessage(json));
    }

    public void unregister(String playerId) {
        sessions.remove(playerId);
    }
}
