package com.mateusesp.game.controller;

import com.mateusesp.game.exception.JogadorJaNaSalaException;
import com.mateusesp.game.exception.JogadorNaoNaSalaException;
import com.mateusesp.game.exception.SalaCheiaException;
import com.mateusesp.game.exception.SalaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SalaNaoEncontradaException.class)
    public ResponseEntity<Map<String, String>> tratarSalaNaoEncontrada(SalaNaoEncontradaException e) {
        return construirResposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(SalaCheiaException.class)
    public ResponseEntity<Map<String, String>> tratarSalaCheia(SalaCheiaException e) {
        return construirResposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(JogadorJaNaSalaException.class)
    public ResponseEntity<Map<String, String>> tratarJogadorJaNaSala(JogadorJaNaSalaException e) {
        return construirResposta(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(JogadorNaoNaSalaException.class)
    public ResponseEntity<Map<String, String>> tratarJogadorNaoNaSala(JogadorNaoNaSalaException e) {
        return construirResposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarArgumentoInvalido(IllegalArgumentException e) {
        return construirResposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> tratarEstadoInvalido(IllegalStateException e) {
        return construirResposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private ResponseEntity<Map<String, String>> construirResposta(HttpStatus status, String mensagem) {
        Map<String, String> resposta = new HashMap<>();
        resposta.put("error", status.getReasonPhrase());
        resposta.put("message", mensagem);
        resposta.put("status", String.valueOf(status.value()));
        return new ResponseEntity<>(resposta, status);
    }
}
