package com.mateusesp.game.model;

import java.util.concurrent.atomic.AtomicInteger;

public class EstadoJogo {
    private final AtomicInteger contador = new AtomicInteger(0);
    private volatile boolean iniciado = false;

    public int getContador() {
        return contador.get();
    }

    public void incrementarContador() {
        contador.incrementAndGet();
    }

    public boolean isIniciado() {
        return iniciado;
    }

    public void setIniciado(boolean iniciado) {
        this.iniciado = iniciado;
    }

    @Override
    public String toString() {
        return "EstadoJogo{" +
                "contador=" + contador.get() +
                ", iniciado=" + iniciado +
                '}';
    }
}
