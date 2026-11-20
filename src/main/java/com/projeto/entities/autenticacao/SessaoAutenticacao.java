package com.projeto.entities.autenticacao;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Estado persistente; não executa fluxos de autenticação ou administração. */
@Entity
@Table(name = "sessao_autenticacao")
public class SessaoAutenticacao {
    @Id
    @JsonIgnore
    @Column(name = "sid", nullable = false)
    private UUID sid;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(name = "versao_emitida", nullable = false)
    private long versaoEmitida;
    @Column(name = "criada_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime criadaEm;
    @Column(name = "ultima_atividade_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime ultimaAtividadeEm;
    @Column(name = "expira_em_absoluto", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime expiraEmAbsoluto;
    @Column(name = "revogada_em", nullable = true, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime revogadaEm;
    @Column(name = "motivo_revogacao", nullable = true, length = 30)
    private String motivoRevogacao;
    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;

    protected SessaoAutenticacao() { }

    public SessaoAutenticacao(UUID sid, Usuario usuario, long versaoEmitida, OffsetDateTime criadaEm, OffsetDateTime ultimaAtividadeEm, OffsetDateTime expiraEmAbsoluto, OffsetDateTime revogadaEm, String motivoRevogacao) {
        this.sid = sid;
        this.usuario = usuario;
        this.versaoEmitida = versaoEmitida;
        this.criadaEm = criadaEm;
        this.ultimaAtividadeEm = ultimaAtividadeEm;
        this.expiraEmAbsoluto = expiraEmAbsoluto;
        this.revogadaEm = revogadaEm;
        this.motivoRevogacao = motivoRevogacao;
    }

    public UUID getSid() { return sid; }

    public void revogar(OffsetDateTime instante, String motivo) {
        if (revogadaEm == null) { revogadaEm = instante; motivoRevogacao = motivo; }
    }
    /** O serviço valida prazo, revogação e versão sob lock antes de persistir atividade. */
    public void registrarAtividade(OffsetDateTime instante) { ultimaAtividadeEm = instante; }

    public Usuario getUsuario() { return usuario; }

    public long getVersaoEmitida() { return versaoEmitida; }

    public OffsetDateTime getCriadaEm() { return criadaEm; }

    public OffsetDateTime getUltimaAtividadeEm() { return ultimaAtividadeEm; }

    public OffsetDateTime getExpiraEmAbsoluto() { return expiraEmAbsoluto; }

    public OffsetDateTime getRevogadaEm() { return revogadaEm; }

    public String getMotivoRevogacao() { return motivoRevogacao; }

    public Long getVersao() { return versao; }
}
