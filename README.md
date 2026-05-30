## Jogo Multiplayer Realtime

Servidor backend em Java e Spring Boot para gerenciar salas de jogos multiplayer em tempo real usando WebSocket e protocolo STOMP. O estado do sistema é gerenciado inteiramente em memória de forma thread-safe.

## Requisitos e Stack
- Java 21
- Spring Boot 3.3.0
- Maven

## Como rodar o projeto

Para iniciar a aplicação localmente:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```
O servidor vai rodar na porta `8080`. 

## Como rodar o simulador de concorrência (Testes)

Um dos simuladores de concorrência foi escrito como um teste de integração JUnit (`SimuladorConcorrenciaTest`). Ele é executado em 2 steps:
1. **Entrada concorrente na mesma sala**: Cria 20 conexões simultâneas, onde os jogadores estão tentando entrar em uma sala com capacidade de 5 jogadores. Porém, somente 5 acessam a sala.
2. **Incrementos simultâneos (contador)**: Os jogadores ficam enviando requisições de incremento no botão de forma paralela até atingir 250 cliques no total.

Para rodar os testes:
```bash
./mvnw clean test
```

## Explicação de arquitetura e estrutura utilizado para tratar a concorrencia e thread-safe
- **Lock por sala**: Para não acontecer problemas de concorrência
entre as salas o ReentrantLock foi usado dentro do Domínio “Sala”. E falando em requisitos de escalabilidade o Reentrant lock funciona melhor do que o @Synchronized, pois com o Synchronized ele iria travar todo o service "ServicoJogo", impedindo que outros usuarios conseguissem fazer outras ações enquanto uma ação estivesse sendo executada em outra sala.
- **Evitar conexões duplicadas e estouro de limite**: A entrada na sala é feita no interceptador de subscrição (`SUBSCRIBE` do STOMP). O fluxo de validar capacidade, verificar duplicidade do jogador e iniciar o jogo roda sob o lock da sala correspondente, isso evita conexões duplicadas e evita que o limite maximo da sala seja excedido
- **Gerenciamento das sessoes**: O gerenciamento das sessoes é feito através do `ConcurrentHashMap<String, Sala>` as chaves são os IDs das salas e os valores são as salas. Separado dessa forma as salas ficam armazenadas em memoria de forma isolada, isso garante o requisito "thread-safe", o Map do java já é thread-safe por padrão.

### WebSocket 
- **Endpoint**: `ws://localhost:8080/ws-game`
- **Inscrição (Join)**: `/topic/rooms/{roomId}` (o cliente tenta entrar na sala ao se inscrever neste tópico. Pode ser enviado o header nativo `playerName` para definir o nome do jogador).
- **Ações (Incrementar)**: Enviar mensagem para `/app/rooms/{roomId}/action`
- **Eventos enviados pelo servidor**:
  - `JOGADOR_ENTROU_SALA`: Um jogador entrou.
  - `JOGADOR_SAIU_SALA`: Um jogador desconectou.
  - `JOGO_INICIADO`: A sala atingiu a capacidade máxima.
  - `ACAO_JOGADOR`: Notificação de ação executada por alguém.
  - `ESTADO_JOGO_ALTERADO`: Novo estado consolidado do jogo, ocorre quando o contador é incrementado, quando o jogo inicia, quando um jogador entra na sala e quando um jogador sai da sala.

### REST API
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Criar Sala**: `POST /rooms`
- **Consultar Sala**: `GET /rooms/{roomId}`
- **Listar Salas**: `GET /rooms`

---

## Como rodar o simulador de concorrência manual

Caso queira simular a concorrência no servidor:

1. **Terminal 1**: Inicie o servidor Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
2. **Terminal 2**: Em outra janela do terminal, execute o simulador autônomo:
   ```bash
   ./mvnw compile exec:java -Dexec.mainClass="com.mateusesp.game.simulator.SimuladorManual"
   ```

O simulador criará uma sala via REST HTTP, estabelecerá 20 conexões concorrentes via WebSocket, enviará inscrições simultâneas e disparará os incrementos de clique em paralelo, imprimindo o estado final retornado da sala.
