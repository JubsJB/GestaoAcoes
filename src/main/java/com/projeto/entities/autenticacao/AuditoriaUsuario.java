package com.projeto.entities.autenticacao;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Estado persistente; não executa fluxos de autenticação ou administração. */
@Entity
@Table(name = "auditoria_usuario")
public class AuditoriaUsuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ator_id", nullable = true)
    private Usuario ator;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alvo_id", nullable = false)
    private Usuario alvo;
    @Column(name = "acao", nullable = false, length = 40)
    private String acao;
    @Column(name = "resultado", nullable = false, length = 20)
    private String resultado;
    @Column(name = "registrada_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime registradaEm;

    protected AuditoriaUsuario() { }

    public AuditoriaUsuario(Usuario ator, Usuario alvo, String acao, String resultado, OffsetDateTime registradaEm) {
        this.ator = ator;
        this.alvo = alvo;
        this.acao = acao;
        this.resultado = resultado;
        this.registradaEm = registradaEm;
    }

    public Long getId() { return id; }

    public Usuario getAtor() { return ator; }

    public Usuario getAlvo() { return alvo; }

    public String getAcao() { return acao; }

    public String getResultado() { return resultado; }

    public OffsetDateTime getRegistradaEm() { return registradaEm; }
}
