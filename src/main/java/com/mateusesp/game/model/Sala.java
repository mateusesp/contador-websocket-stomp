package com.mateusesp.game.model;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class Sala {
    private final String idSala;
    private final int capacidadeMaxima;
    private final Set<Jogador> jogadores = ConcurrentHashMap.newKeySet();
    private final EstadoJogo estadoJogo;
    private final ReentrantLock lock = new ReentrantLock();

    public Sala(String idSala, int capacidadeMaxima) {
        this.idSala = idSala;
        this.capacidadeMaxima = capacidadeMaxima;
        this.estadoJogo = new EstadoJogo();
    }

    public String getIdSala() {
        return idSala;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public Set<Jogador> getJogadores() {
        return Collections.unmodifiableSet(jogadores);
    }

    public Set<Jogador> getJogadoresMutaveis() {
        return jogadores;
    }

    public EstadoJogo getEstadoJogo() {
        return estadoJogo;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public String toString() {
        return "Sala{" +
                "idSala='" + idSala + '\'' +
                ", capacidadeMaxima=" + capacidadeMaxima +
                ", quantidadeJogadores=" + jogadores.size() +
                ", estadoJogo=" + estadoJogo +
                '}';
    }
}
