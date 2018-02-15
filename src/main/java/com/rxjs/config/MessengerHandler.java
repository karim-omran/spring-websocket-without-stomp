package com.rxjs.config;

import com.google.gson.Gson;
import com.rxjs.models.Message;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MessengerHandler extends TextWebSocketHandler {

    private final Map<WebSocketSession, String> namedWebSocketSessions;
    private final List<Message> chatMessages;
    private final Gson gson;

    public MessengerHandler() {
        this.namedWebSocketSessions = new ConcurrentHashMap<>();
        this.chatMessages = new CopyOnWriteArrayList<>();
        this.gson = new Gson();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException {
        Message chatMessage = gson.fromJson(message.getPayload(), Message.class);

        if (Objects.equals(chatMessage.getType(), Message.MessageType.JOIN_REQUEST)) {
            TextMessage joinMessage = new TextMessage(gson.toJson(getOpenSessionMessage(chatMessage.getSender())));
            for (Map.Entry<WebSocketSession, String> namedWebSocketSession : this.namedWebSocketSessions.entrySet()) {
                namedWebSocketSession.getKey().sendMessage(joinMessage);
            }

            for (Message oldMessage : this.chatMessages) {
                session.sendMessage(new TextMessage(this.gson.toJson(oldMessage)));
            }

            this.namedWebSocketSessions.put(session, chatMessage.getSender());
        } else if (Objects.equals(chatMessage.getType(), Message.MessageType.CHAT)) {
            chatMessages.add(chatMessage);
            for (Map.Entry<WebSocketSession, String> namedWebSocketSession : this.namedWebSocketSessions.entrySet()) {
                if (!session.equals(namedWebSocketSession.getKey())) {
                    namedWebSocketSession.getKey().sendMessage(new TextMessage(gson.toJson(chatMessage)));
                }
            }
        }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        TextMessage leaveMessage = new TextMessage(gson.toJson(this.getCloseSessionMessage(this.namedWebSocketSessions.get(session))));
        this.namedWebSocketSessions.remove(session);
        for (Map.Entry<WebSocketSession, String> namedWebSocketSession : this.namedWebSocketSessions.entrySet()) {
            namedWebSocketSession.getKey().sendMessage(leaveMessage);
        }
    }

    private Message getOpenSessionMessage(String name) {
        Message chatMessage = new Message();
        chatMessage.setSender(name);
        chatMessage.setContent("Joined session.");
        chatMessage.setType(Message.MessageType.JOIN_REQUEST);
        return chatMessage;
    }

    private Message getCloseSessionMessage(String name) {
        Message chatMessage = new Message();
        chatMessage.setSender(name);
        chatMessage.setContent("Left session.");
        chatMessage.setType(Message.MessageType.LEAVE_REQUEST);
        return chatMessage;
    }

}
