package com.mateusesp.game.dto;

public class CriarSalaRequest {
    private int capacidadeMaxima;

    public CriarSalaRequest() {
    }

    public CriarSalaRequest(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public int getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public void setCapacidadeMaxima(int capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }
}
