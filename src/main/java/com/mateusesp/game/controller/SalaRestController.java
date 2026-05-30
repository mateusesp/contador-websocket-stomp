package com.mateusesp.game.controller;

import com.mateusesp.game.dto.CriarSalaRequest;
import com.mateusesp.game.dto.SalaResponse;
import com.mateusesp.game.model.Sala;
import com.mateusesp.game.service.ServicoJogo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/rooms")
public class SalaRestController {

    private final ServicoJogo servicoJogo;

    public SalaRestController(ServicoJogo servicoJogo) {
        this.servicoJogo = servicoJogo;
    }

    @PostMapping
    public ResponseEntity<SalaResponse> criarSala(@RequestBody CriarSalaRequest request) {
        Sala sala = servicoJogo.criarSala(request.getCapacidadeMaxima());
        return ResponseEntity.ok(SalaResponse.from(sala));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalaResponse> obterSala(@PathVariable("id") String id) {
        Sala sala = servicoJogo.getSala(id);
        return ResponseEntity.ok(SalaResponse.from(sala));
    }

    @GetMapping
    public ResponseEntity<Collection<SalaResponse>> listarSalas() {
        Collection<SalaResponse> responses = servicoJogo.getTodasSalas().stream()
                .map(SalaResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
