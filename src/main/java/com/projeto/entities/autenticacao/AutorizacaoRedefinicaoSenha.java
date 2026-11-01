package com.projeto.entities.autenticacao;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Estado persistente; não executa fluxos de autenticação ou administração. */
@Entity
@Table(name = "autorizacao_redefinicao_senha")
public class AutorizacaoRedefinicaoSenha {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autorizada_por_id", nullable = false)
    private Usuario autorizadaPor;
    @JsonIgnore
    @Column(name = "digest", nullable = false, length = 64)
    private String digest;
    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, length = 20)
    private MotivoRedefinicao motivo;
    @Column(name = "criada_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime criadaEm;
    @Column(name = "expira_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiraEm;
    @Column(name = "tentativas_restantes", nullable = false)
    private int tentativasRestantes;
    @Column(name = "consumida_em", nullable = true, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime consumidaEm;
    @Column(name = "invalidada_em", nullable = true, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime invalidadaEm;
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    protected AutorizacaoRedefinicaoSenha() { }

    public AutorizacaoRedefinicaoSenha(UUID id, Usuario usuario, Usuario autorizadaPor, String digest, MotivoRedefinicao motivo, OffsetDateTime criadaEm, OffsetDateTime expiraEm, int tentativasRestantes, OffsetDateTime consumidaEm, OffsetDateTime invalidadaEm) {
        this.id = id;
        this.usuario = usuario;
        this.autorizadaPor = autorizadaPor;
        this.digest = digest;
        this.motivo = motivo;
        this.criadaEm = criadaEm;
        this.expiraEm = expiraEm;
        this.tentativasRestantes = tentativasRestantes;
        this.consumidaEm = consumidaEm;
        this.invalidadaEm = invalidadaEm;
    }

    public UUID getId() { return id; }
    public void registrarErro(OffsetDateTime instante) {
        if(consumidaEm!=null || invalidadaEm!=null || tentativasRestantes<=0 || !instante.isBefore(expiraEm))
            throw new IllegalStateException("Autorização indisponível");
        tentativasRestantes--;
        if(tentativasRestantes==0) invalidadaEm=instante;
    }
    public void consumir(OffsetDateTime instante) {
        if(consumidaEm!=null || invalidadaEm!=null || tentativasRestantes<=0 || !instante.isBefore(expiraEm))
            throw new IllegalStateException("Autorização indisponível");
        consumidaEm=instante;
    }
    /** Revogação administrativa apenas; não emite nem consome códigos. */
    public void invalidar(OffsetDateTime instante) {
        if(consumidaEm==null && invalidadaEm==null) invalidadaEm=instante;
    }

    public Usuario getUsuario() { return usuario; }

    public Usuario getAutorizadaPor() { return autorizadaPor; }

    public String getDigest() { return digest; }

    public MotivoRedefinicao getMotivo() { return motivo; }

    public OffsetDateTime getCriadaEm() { return criadaEm; }

    public OffsetDateTime getExpiraEm() { return expiraEm; }

    public int getTentativasRestantes() { return tentativasRestantes; }

    public OffsetDateTime getConsumidaEm() { return consumidaEm; }

    public OffsetDateTime getInvalidadaEm() { return invalidadaEm; }

    public Long getVersao() { return versao; }
}
