package com.mateusesp.game.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.mateusesp.game.exception.JogadorJaNaSalaException;
import com.mateusesp.game.exception.JogadorNaoNaSalaException;
import com.mateusesp.game.exception.SalaCheiaException;
import com.mateusesp.game.exception.SalaNaoEncontradaException;
import com.mateusesp.game.model.EventoJogo;
import com.mateusesp.game.model.Jogador;
import com.mateusesp.game.model.Sala;
import com.mateusesp.game.model.TipoEventoJogo;

@Service
public class ServicoJogo {

    private static final Logger log = LoggerFactory.getLogger(ServicoJogo.class);

    private final ConcurrentHashMap<String, Sala> salas = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessaoJogadorParaSalaMap = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public ServicoJogo(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public Sala criarSala(int capacidadeMaxima) {
        if (capacidadeMaxima <= 0) {
            throw new IllegalArgumentException("A capacidade da sala deve ser maior que zero");
        }
        String salaId = UUID.randomUUID().toString();
        Sala sala = new Sala(salaId, capacidadeMaxima);
        salas.put(salaId, sala);
        log.info("Sala criada: {} com capacidade máxima: {}", salaId, capacidadeMaxima);
        return sala;
    }

    public void entrarSala(String salaId, String sessaoId, String nomeJogador) {
        Sala sala = salas.get(salaId);
        if (sala == null) {
            throw new SalaNaoEncontradaException("A sala " + salaId + " não existe");
        }

        sala.getLock().lock();
        try {
            String salaAtualId = sessaoJogadorParaSalaMap.get(sessaoId);
            if (salaAtualId != null) {
                if (salaAtualId.equals(salaId)) {
                    log.debug("Sessão {} já está na sala {}", sessaoId, salaId);
                    return;
                }
                throw new JogadorJaNaSalaException("O jogador já está registrado na sala: " + salaAtualId);
            }

            int tamanhoAtual = sala.getJogadoresMutaveis().size();
            if (tamanhoAtual >= sala.getCapacidadeMaxima()) {
                throw new SalaCheiaException("A sala " + salaId + " está cheia (limite: " + sala.getCapacidadeMaxima() + ")");
            }

            Jogador jogador = new Jogador(sessaoId, nomeJogador);
            sala.getJogadoresMutaveis().add(jogador);
            sessaoJogadorParaSalaMap.put(sessaoId, salaId);
            log.info("Jogador {} ({}) registrado internamente na sala {}", nomeJogador, sessaoId, salaId);

        } finally {
            sala.getLock().unlock();
        }
    }

    public void notificarEntrada(String sessaoId) {
        String salaId = sessaoJogadorParaSalaMap.get(sessaoId);
        if (salaId == null) {
            return;
        }

        Sala sala = salas.get(salaId);
        if (sala == null) {
            return;
        }

        sala.getLock().lock();
        try {
            Jogador jogador = sala.getJogadoresMutaveis().stream()
                    .filter(p -> p.getIdSessao().equals(sessaoId))
                    .findFirst()
                    .orElse(null);

            if (jogador != null) {
                log.info("Enviando notificações de entrada para o jogador {} ({}) na sala {}", jogador.getNome(), sessaoId, salaId);

                enviarEventoJogo(new EventoJogo(TipoEventoJogo.JOGADOR_ENTROU_SALA, salaId, sessaoId, jogador));

                if (sala.getJogadoresMutaveis().size() == sala.getCapacidadeMaxima() && !sala.getEstadoJogo().isIniciado()) {
                    sala.getEstadoJogo().setIniciado(true);
                    log.info("Capacidade máxima atingida na sala {}. Iniciando a partida!", salaId);
                    enviarEventoJogo(new EventoJogo(TipoEventoJogo.JOGO_INICIADO, salaId, null, sala.getEstadoJogo()));
                }

                enviarEventoJogo(new EventoJogo(TipoEventoJogo.ESTADO_JOGO_ALTERADO, salaId, null, sala.getEstadoJogo()));
            }
        } finally {
            sala.getLock().unlock();
        }
    }

    public void sairSala(String sessaoId) {
        String salaId = sessaoJogadorParaSalaMap.remove(sessaoId);
        if (salaId == null) {
            log.debug("A sessão {} não estava em nenhuma sala ativa", sessaoId);
            return;
        }

        Sala sala = salas.get(salaId);
        if (sala == null) {
            log.warn("A sala {} associada à sessão {} não foi encontrada", salaId, sessaoId);
            return;
        }

        sala.getLock().lock();
        try {
            Jogador jogadorRemover = sala.getJogadoresMutaveis().stream()
                    .filter(p -> p.getIdSessao().equals(sessaoId))
                    .findFirst()
                    .orElse(null);

            if (jogadorRemover != null) {
                sala.getJogadoresMutaveis().remove(jogadorRemover);
                log.info("Jogador {} ({}) saiu da sala {}", jogadorRemover.getNome(), sessaoId, salaId);

                enviarEventoJogo(new EventoJogo(TipoEventoJogo.JOGADOR_SAIU_SALA, salaId, sessaoId, jogadorRemover));

                enviarEventoJogo(new EventoJogo(TipoEventoJogo.ESTADO_JOGO_ALTERADO, salaId, null, sala.getEstadoJogo()));
            }
        } finally {
            sala.getLock().unlock();
        }
    }

    public void executarAcao(String salaId, String sessaoId) {
        Sala sala = salas.get(salaId);
        if (sala == null) {
            throw new SalaNaoEncontradaException("A sala " + salaId + " não existe");
        }

        sala.getLock().lock();
        try {
            Jogador jogador = sala.getJogadoresMutaveis().stream()
                    .filter(p -> p.getIdSessao().equals(sessaoId))
                    .findFirst()
                    .orElseThrow(() -> new JogadorNaoNaSalaException("O jogador " + sessaoId + " não está na sala " + salaId));

            sala.getEstadoJogo().incrementarContador();
            log.debug("Jogador {} incrementou o contador na sala {}. Novo valor: {}", jogador.getNome(), salaId, sala.getEstadoJogo().getContador());

            enviarEventoJogo(new EventoJogo(TipoEventoJogo.ACAO_JOGADOR, salaId, sessaoId, jogador));

            enviarEventoJogo(new EventoJogo(TipoEventoJogo.ESTADO_JOGO_ALTERADO, salaId, null, sala.getEstadoJogo()));

        } finally {
            sala.getLock().unlock();
        }
    }

    public Sala getSala(String salaId) {
        Sala sala = salas.get(salaId);
        if (sala == null) {
            throw new SalaNaoEncontradaException("Sala " + salaId + " não encontrada");
        }
        return sala;
    }

    public Collection<Sala> getTodasSalas() {
        return new ArrayList<>(salas.values());
    }

    private void enviarEventoJogo(EventoJogo evento) {
        String destino = "/topic/rooms/" + evento.getIdSala();
        messagingTemplate.convertAndSend(destino, evento);
    }
}
