package com.mateusesp.game.simulator;

import com.mateusesp.game.dto.CriarSalaRequest;
import com.mateusesp.game.dto.SalaResponse;
import com.mateusesp.game.model.Sala;
import com.mateusesp.game.service.ServicoJogo;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimuladorConcorrenciaTest {

    private static final Logger log = LoggerFactory.getLogger(SimuladorConcorrenciaTest.class);

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ServicoJogo servicoJogo;

    @Test
    public void testEntradaEIncrementosConcorrentes() throws Exception {
        int capacidade = 5;
        CriarSalaRequest createRequest = new CriarSalaRequest(capacidade);
        ResponseEntity<SalaResponse> createResponse = restTemplate.postForEntity("/rooms", createRequest, SalaResponse.class);
        assertEquals(200, createResponse.getStatusCode().value());
        
        SalaResponse salaResponse = createResponse.getBody();
        assertNotNull(salaResponse);
        String salaId = salaResponse.getIdSala();

        int totalClientes = 20;
        ExecutorService executor = Executors.newFixedThreadPool(totalClientes);
        String wsUrl = "ws://localhost:" + port + "/ws-game";

        List<WebSocketStompClient> stompClients = new ArrayList<>();
        List<CompletableFuture<StompSession>> connectionFutures = new ArrayList<>();
        List<TestSessionHandler> handlers = new ArrayList<>();

        for (int i = 0; i < totalClientes; i++) {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            stompClients.add(stompClient);

            TestSessionHandler handler = new TestSessionHandler(i);
            handlers.add(handler);

            CompletableFuture<StompSession> future = stompClient.connectAsync(wsUrl, handler);
            connectionFutures.add(future);
        }

        List<StompSession> activeSessions = new ArrayList<>();
        for (int i = 0; i < totalClientes; i++) {
            try {
                StompSession session = connectionFutures.get(i).get(5, TimeUnit.SECONDS);
                activeSessions.add(session);
            } catch (Exception e) {
                log.error("[WS] Falha ao conectar o cliente {}: {}", i, e.getMessage());
            }
        }

        CyclicBarrier barrier = new CyclicBarrier(activeSessions.size());
        CountDownLatch latch = new CountDownLatch(activeSessions.size());
        
        List<CompletableFuture<Void>> subscribeTasks = new ArrayList<>();

        for (int i = 0; i < activeSessions.size(); i++) {
            final int index = i;
            final StompSession session = activeSessions.get(index);
            final TestSessionHandler handler = handlers.get(index);

            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                try {
                    barrier.await();
                    
                    StompHeaders headers = new StompHeaders();
                    headers.setDestination("/topic/rooms/" + salaId);
                    headers.set("playerName", "ConcurrentPlayer_" + index);

                    session.subscribe(headers, handler);
                } catch (Exception e) {
                    log.error("[Thread SUBSCRIBE] Erro no cliente {}: {}", index, e.getMessage());
                } finally {
                    latch.countDown();
                }
            }, executor);
            subscribeTasks.add(task);
        }

        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(1500);

        Sala salaReal = servicoJogo.getSala(salaId);

        assertEquals(capacidade, salaReal.getJogadores().size(), "A sala excedeu a capacidade limite configurada!");
        assertTrue(salaReal.getEstadoJogo().isIniciado(), "O jogo deveria estar iniciado");

        int successfulSubscribedClients = 0;
        List<StompSession> joinedSessions = new ArrayList<>();
        for (int i = 0; i < activeSessions.size(); i++) {
            StompSession session = activeSessions.get(i);
            TestSessionHandler handler = handlers.get(i);
            if (session.isConnected() && !handler.hasError.get()) {
                successfulSubscribedClients++;
                joinedSessions.add(session);
            }
        }
        assertEquals(capacidade, joinedSessions.size(), "O número de conexões ativas não condiz com a capacidade da sala");

        int incrementsPerPlayer = 50;
        int expectedTotalIncrements = capacidade * incrementsPerPlayer;
        CountDownLatch incrementLatch = new CountDownLatch(joinedSessions.size() * incrementsPerPlayer);
        
        for (StompSession session : joinedSessions) {
            executor.submit(() -> {
                for (int k = 0; k < incrementsPerPlayer; k++) {
                    try {
                        session.send("/app/rooms/" + salaId + "/action", null);
                    } catch (Exception e) {
                        log.error("[Ação] Falha ao enviar clique do jogador {}: {}", session.getSessionId(), e.getMessage());
                    } finally {
                        incrementLatch.countDown();
                    }
                    try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                }
            });
        }

        assertTrue(incrementLatch.await(10, TimeUnit.SECONDS), "Timeout ao enviar as ações de incremento");
        Thread.sleep(1500);

        int finalCount = salaReal.getEstadoJogo().getContador();
        
        assertEquals(expectedTotalIncrements, finalCount, "Houve perda de incrementos sob concorrência!");

        for (StompSession session : activeSessions) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
        executor.shutdown();
    }

    private static class TestSessionHandler extends StompSessionHandlerAdapter {
        private final int id;
        private final AtomicBoolean hasError = new AtomicBoolean(false);

        public TestSessionHandler(int id) {
            this.id = id;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            if (headers.get("type") != null && headers.get("type").equals("ERROR")) {
                hasError.set(true);
            }
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
            hasError.set(true);
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Object.class;
        }
    }
}
