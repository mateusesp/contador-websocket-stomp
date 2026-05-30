package com.mateusesp.game.model;

import java.util.Objects;

public class Jogador {
    private final String idSessao;
    private final String nome;

    public Jogador(String idSessao, String nome) {
        this.idSessao = idSessao;
        this.nome = nome;
    }

    public String getIdSessao() {
        return idSessao;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Jogador jogador = (Jogador) o;
        return Objects.equals(idSessao, jogador.idSessao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idSessao);
    }

    @Override
    public String toString() {
        return "Jogador{" +
                "idSessao='" + idSessao + '\'' +
                ", nome='" + nome + '\'' +
                '}';
    }
}
