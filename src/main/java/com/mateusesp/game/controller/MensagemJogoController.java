package com.mateusesp.game.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.mateusesp.game.service.ServicoJogo;

@Controller
public class MensagemJogoController {

    private static final Logger log = LoggerFactory.getLogger(MensagemJogoController.class);

    private final ServicoJogo servicoJogo;

    public MensagemJogoController(ServicoJogo servicoJogo) {
        this.servicoJogo = servicoJogo;
    }

    @MessageMapping("/rooms/{roomId}/action")
    public void executarAcao(@DestinationVariable("roomId") String salaId, SimpMessageHeaderAccessor headerAccessor) {
        String sessaoId = headerAccessor.getSessionId();
        log.info("Ação de clique recebida: Sala = {}, Sessão = {}", salaId, sessaoId);

        if (sessaoId != null) {
            servicoJogo.executarAcao(salaId, sessaoId);
        } else {
            log.error("Erro ao processar ação: O header sessaoId não foi localizado.");
        }
    }
}
