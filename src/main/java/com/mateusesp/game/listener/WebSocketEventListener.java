package com.mateusesp.game.listener;

import com.mateusesp.game.service.ServicoJogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final ServicoJogo servicoJogo;

    public WebSocketEventListener(ServicoJogo servicoJogo) {
        this.servicoJogo = servicoJogo;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessaoId = headerAccessor.getSessionId();
        
        if (sessaoId != null) {
            log.info("Conexão WebSocket encerrada. ID da Sessão: {}", sessaoId);
            servicoJogo.sairSala(sessaoId);
        }
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessaoId = headerAccessor.getSessionId();
        
        if (destination != null && destination.startsWith("/topic/rooms/") && sessaoId != null) {
            log.info("Subscrição ativa registrada com sucesso. Sessão: {}, Destino: {}", sessaoId, destination);
            servicoJogo.notificarEntrada(sessaoId);
        }
    }
}
