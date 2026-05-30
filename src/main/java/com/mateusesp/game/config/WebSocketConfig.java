package com.mateusesp.game.config;

import com.mateusesp.game.service.ServicoJogo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebSocketConfig.class);

    private final ServicoJogo servicoJogo;

    public WebSocketConfig(@Lazy ServicoJogo servicoJogo) {
        this.servicoJogo = servicoJogo;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-game")
                .setAllowedOriginPatterns("*");

        registry.addEndpoint("/ws-game")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    
                    if (destination != null && destination.startsWith("/topic/rooms/")) {
                        String salaId = destination.substring("/topic/rooms/".length());
                        String sessaoId = accessor.getSessionId();
                        
                        String nomeJogador = accessor.getFirstNativeHeader("playerName");
                        if (nomeJogador == null || nomeJogador.isBlank()) {
                            nomeJogador = "Player_" + (sessaoId != null && sessaoId.length() > 4 
                                    ? sessaoId.substring(sessaoId.length() - 4) 
                                    : sessaoId);
                        }

                        log.info("Inscrição interceptada: Sala = {}, Sessão = {}, Jogador = {}", salaId, sessaoId, nomeJogador);

                        try {
                            servicoJogo.entrarSala(salaId, sessaoId, nomeJogador);
                        } catch (Exception e) {
                            log.error("Inscrição rejeitada para sala: {}. Motivo: {}", salaId, e.getMessage());
                            throw new MessageDeliveryException(e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}
