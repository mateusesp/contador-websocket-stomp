package com.mateusesp.game.model;

public class EventoJogo {
    private TipoEventoJogo tipo;
    private String idSala;
    private String idJogador;
    private Object dados;

    public EventoJogo() {
    }

    public EventoJogo(TipoEventoJogo tipo, String idSala, String idJogador, Object dados) {
        this.tipo = tipo;
        this.idSala = idSala;
        this.idJogador = idJogador;
        this.dados = dados;
    }

    public TipoEventoJogo getTipo() {
        return tipo;
    }

    public void setTipo(TipoEventoJogo tipo) {
        this.tipo = tipo;
    }

    public String getIdSala() {
        return idSala;
    }

    public void setIdSala(String idSala) {
        this.idSala = idSala;
    }

    public String getIdJogador() {
        return idJogador;
    }

    public void setIdJogador(String idJogador) {
        this.idJogador = idJogador;
    }

    public Object getDados() {
        return dados;
    }

    public void setDados(Object dados) {
        this.dados = dados;
    }
}
