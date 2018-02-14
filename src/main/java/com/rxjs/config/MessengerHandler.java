package com.rxjs.config;

import com.google.gson.Gson;
import com.rxjs.models.ChatMessage;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class MessengerHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions;
    private final List<ChatMessage> chatMessages;
    private final Gson gson;

    public MessengerHandler() {
        this.sessions = new CopyOnWriteArrayList<>();
        this.chatMessages = new CopyOnWriteArrayList<>();
        this.gson = new Gson();
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message)
            throws InterruptedException, IOException {
        ChatMessage chatMessage = gson.fromJson(message.getPayload(), ChatMessage.class);
        chatMessage.setSender(session.getId());
        chatMessages.add(chatMessage);
        for (WebSocketSession webSocketSession : sessions) {
            if (!session.equals(webSocketSession)) {
                webSocketSession.sendMessage(new TextMessage(gson.toJson(chatMessage)));
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        for (WebSocketSession webSocketSession : sessions) {
            webSocketSession.sendMessage(new TextMessage(gson.toJson(this.getOpenSessionMessage(session))));
        }
        this.sessions.add(session);

        for (ChatMessage chatMessage : chatMessages) {
            session.sendMessage(new TextMessage(gson.toJson(chatMessage)));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        this.sessions.remove(session);
        for (WebSocketSession webSocketSession : sessions) {
            webSocketSession.sendMessage(new TextMessage(gson.toJson(this.getCloseSessionMessage(session))));

        }
    }

    private ChatMessage getOpenSessionMessage(WebSocketSession session) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(session.getId());
        chatMessage.setContent("Joined session.");
        chatMessage.setType(ChatMessage.MessageType.JOIN);
        return chatMessage;
    }

    private ChatMessage getCloseSessionMessage(WebSocketSession session) {
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setSender(session.getId());
        chatMessage.setContent("Leaved session.");
        chatMessage.setType(ChatMessage.MessageType.LEAVE);
        return chatMessage;
    }

}
