package com.mateusesp.game.dto;

import com.mateusesp.game.model.EstadoJogo;

public class EstadoJogoDto {
    private int contador;
    private boolean iniciado;

    public EstadoJogoDto() {
    }

    public static EstadoJogoDto from(EstadoJogo estado) {
        EstadoJogoDto dto = new EstadoJogoDto();
        dto.setContador(estado.getContador());
        dto.setIniciado(estado.isIniciado());
        return dto;
    }

    public int getContador() {
        return contador;
    }

    public void setContador(int contador) {
        this.contador = contador;
    }

    public boolean isIniciado() {
        return iniciado;
    }

    public void setIniciado(boolean iniciado) {
        this.iniciado = iniciado;
    }
}
