package com.projeto.entities.autenticacao;

import jakarta.persistence.*;

/** Estado persistente; não executa fluxos de autenticação ou administração. */
@Entity
@Table(name = "controle_seguranca")
public class ControleSeguranca {
    @Id
    @Column(name = "id", nullable = false)
    private Short id;

    protected ControleSeguranca() { }

    public ControleSeguranca(Short id) {
        this.id = id;
    }

    public Short getId() { return id; }
}
