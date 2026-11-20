package com.projeto.entities.autenticacao;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** Estado persistente; não executa fluxos de autenticação ou administração. */
@Entity
@Table(name = "contador_abuso")
public class ContadorAbuso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoContadorAbuso tipo;
    @Column(name = "chave_hash", nullable = false, length = 64)
    private String chaveHash;
    @Column(name = "inicio_janela", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime inicioJanela;
    @Column(name = "expira_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiraEm;
    @Column(name = "tentativas", nullable = false)
    private int tentativas;
    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    protected ContadorAbuso() { }

    public ContadorAbuso(TipoContadorAbuso tipo, String chaveHash, OffsetDateTime inicioJanela, OffsetDateTime expiraEm, int tentativas) {
        this.tipo = tipo;
        this.chaveHash = chaveHash;
        this.inicioJanela = inicioJanela;
        this.expiraEm = expiraEm;
        this.tentativas = tentativas;
    }

    public Long getId() { return id; }

    public TipoContadorAbuso getTipo() { return tipo; }

    public String getChaveHash() { return chaveHash; }

    public OffsetDateTime getInicioJanela() { return inicioJanela; }

    public OffsetDateTime getExpiraEm() { return expiraEm; }

    public int getTentativas() { return tentativas; }
    public void registrarFalha() { tentativas++; }

    public long getVersao() { return versao; }
}
