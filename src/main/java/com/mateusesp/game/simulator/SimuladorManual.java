package com.mateusesp.game.simulator;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class SimuladorManual {

    public static void main(String[] args) throws Exception {
        System.out.println("Iniciando simulador de concorrência");

        HttpClient httpClient = HttpClient.newHttpClient();
        String jsonPayload = "{\"capacidadeMaxima\":5}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/rooms"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        System.out.println("Criando sala de limite 5...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Erro ao criar sala. Status HTTP: " + response.statusCode());
            System.err.println("Resposta: " + response.body());
            return;
        }

        String body = response.body();
        String salaId = body.split("\"idSala\":\"")[1].split("\"")[0];
        System.out.println("Sala criada com sucesso! ID: " + salaId);

        int totalClientes = 20;
        String wsUrl = "ws://localhost:8080/ws-game";
        ExecutorService executor = Executors.newFixedThreadPool(totalClientes);

        List<WebSocketStompClient> stompClients = new ArrayList<>();
        List<CompletableFuture<StompSession>> connectionFutures = new ArrayList<>();
        List<ManualSessionHandler> handlers = new ArrayList<>();

        System.out.println("Estabelecendo " + totalClientes + " conexões");
        for (int i = 0; i < totalClientes; i++) {
            WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            stompClients.add(stompClient);

            ManualSessionHandler handler = new ManualSessionHandler(i);
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
                System.err.println("Falha ao conectar o cliente " + i + ": " + e.getMessage());
            }
        }
        System.out.println("Conexões estabelecidas com sucesso: " + activeSessions.size());

        System.out.println("Alinhando clientes para subscrição simultânea");
        CyclicBarrier barrier = new CyclicBarrier(activeSessions.size()); // Aqui utilizei o CyclicBarrier para disparar as açoes de subscribe simultaneamente
        CountDownLatch latch = new CountDownLatch(activeSessions.size());

        for (int i = 0; i < activeSessions.size(); i++) {
            final int index = i;
            final StompSession session = activeSessions.get(index);
            final ManualSessionHandler handler = handlers.get(index);

            executor.submit(() -> {
                try {
                    barrier.await();
                    StompHeaders headers = new StompHeaders();
                    headers.setDestination("/topic/rooms/" + salaId);
                    headers.set("playerName", "JogadorManual_" + index);

                    session.subscribe(headers, handler);
                } catch (Exception e) {
                    System.err.println("Erro no cliente " + index + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        Thread.sleep(1500);

        List<StompSession> joinedSessions = new ArrayList<>();
        for (int i = 0; i < activeSessions.size(); i++) {
            StompSession session = activeSessions.get(i);
            ManualSessionHandler handler = handlers.get(i);
            if (session.isConnected() && !handler.hasError.get()) {
                joinedSessions.add(session);
            }
        }
        System.out.println("Jogadores que conseguiram vaga na sala (limite 5): " + joinedSessions.size());

        int incrementosPorJogador = 50;
        int totalEsperado = joinedSessions.size() * incrementosPorJogador;
        System.out.println("Enviando " + totalEsperado + " ações de incremento de forma paralela");
        CountDownLatch incrementLatch = new CountDownLatch(totalEsperado);

        for (StompSession session : joinedSessions) {
            executor.submit(() -> {
                for (int k = 0; k < incrementosPorJogador; k++) {
                    try {
                        session.send("/app/rooms/" + salaId + "/action", null);
                    } catch (Exception e) {
                        System.err.println("Erro ao enviar incremento: " + e.getMessage());
                    } finally {
                        incrementLatch.countDown();
                    }
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
        }

        incrementLatch.await(10, TimeUnit.SECONDS);
        Thread.sleep(1500);

        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/rooms/" + salaId))
                .GET()
                .build();

        HttpResponse<String> getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        System.out.println("\nEstado final da sala:");
        System.out.println(getResponse.body());

        for (StompSession session : activeSessions) {
            if (session.isConnected()) {
                session.disconnect();
            }
        }
        executor.shutdown();
        System.out.println("Simulação concluída");
        System.exit(0);
    }

    private static class ManualSessionHandler extends StompSessionHandlerAdapter {

        private final int id;
        public final AtomicBoolean hasError = new AtomicBoolean(false);

        public ManualSessionHandler(int id) {
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
        public Type getPayloadType(StompHeaders headers) {
            return Object.class;
        }
    }
}
