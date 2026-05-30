package com.mateusesp.game.dto;

import com.mateusesp.game.model.Jogador;
import com.mateusesp.game.model.Sala;

import java.util.Set;

public class SalaResponse {
    private String idSala;
    private int capacidadeMaxima;
    private Set<Jogador> jogadores;
    private EstadoJogoDto estadoJogo;

    public SalaResponse() {
    }

    public static SalaResponse from(Sala sala) {
        SalaResponse res = new SalaResponse();
        res.setIdSala(sala.getIdSala());
        res.setCapacidadeMaxima(sala.getCapacidadeMaxima());
        res.setJogadores(sala.getJogadores());
        res.setEstadoJogo(EstadoJogoDto.from(sala.getEstadoJogo()));
        return res;
    }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public void setCapacidadeMaxima(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public Set<Jogador> getJogadores() {
        return jogadores;
    }

    public void setJogadores(Set<Jogador> jogadores) {
        this.jogadores = jogadores;
    }

    public EstadoJogoDto getEstadoJogo() {
        return estadoJogo;
    }

    public void setEstadoJogo(EstadoJogoDto estadoJogo) {
        this.estadoJogo = estadoJogo;
    }
}
