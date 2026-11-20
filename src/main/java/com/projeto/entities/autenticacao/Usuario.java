package com.projeto.entities.autenticacao;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;

/** Estado persistente. Não cadastra contas, autentica ou aplica regras administrativas. */
@Entity
@Table(name = "usuario", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_username", columnNames = "username"))
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100, updatable = false)
    private String username;
    @JsonIgnore
    @Column(name = "senha_hash", length = 100)
    private String senhaHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Papel papel;
    @Column(nullable = false)
    private boolean ativo = true;
    @Column(name = "credencial_definida", nullable = false)
    private boolean credencialDefinida;
    @Column(name = "versao_seguranca", nullable = false)
    private long versaoSeguranca;
    @Version @Column(nullable = false)
    private long versao;
    @Column(name = "criada_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime criadaEm;
    @Column(name = "atualizada_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime atualizadaEm;

    protected Usuario() { }

    public Usuario(String username, Papel papel, OffsetDateTime instante) {
        this.username = normalizarUsername(username);
        this.papel = Objects.requireNonNull(papel, "papel");
        this.criadaEm = Objects.requireNonNull(instante, "instante");
        this.atualizadaEm = instante;
    }

    public static String normalizarUsername(String username) {
        String canonico = Objects.requireNonNull(username, "username").trim().toLowerCase(Locale.ROOT);
        if (!canonico.matches("[a-z0-9._-]{3,100}")) throw new IllegalArgumentException("Username inválido");
        return canonico;
    }

    /** Somente credencial inicial já protegida; não implementa redefinição ou administração. */
    public void definirCredencialInicial(String hash, OffsetDateTime instante) {
        if (credencialDefinida || hash == null || !hash.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}"))
            throw new IllegalStateException("Credencial inicial inválida");
        this.senhaHash = hash;
        this.credencialDefinida = true;
        this.atualizadaEm = Objects.requireNonNull(instante);
    }

    public Long getId() { return id; }
    /** Somente após validar autorização temporária sob lock; também atende primeira definição. */
    public void redefinirCredencial(String hash,OffsetDateTime instante) {
        if(!ativo || hash==null || !hash.matches("\\$2[aby]\\$[0-9]{2}\\$[./A-Za-z0-9]{53}")) throw new IllegalStateException("Credencial inválida");
        senhaHash=hash;credencialDefinida=true;versaoSeguranca++;atualizadaEm=Objects.requireNonNull(instante);
    }
    public String getUsername() { return username; }
    public String getSenhaHash() { return senhaHash; }
    public Papel getPapel() { return papel; }
    public boolean isAtivo() { return ativo; }
    public boolean isCredencialDefinida() { return credencialDefinida; }
    public long getVersaoSeguranca() { return versaoSeguranca; }
    public long getVersao() { return versao; }
    public OffsetDateTime getCriadaEm() { return criadaEm; }
    public OffsetDateTime getAtualizadaEm() { return atualizadaEm; }

    /** Mutador de estado persistente; política administrativa e revogação pertencem ao serviço futuro. */
    public void setAtivo(boolean ativo) { this.ativo = ativo; }

    /** Executado pelo serviço administrativo após autorização, versão e invariantes sob lock. */
    public void alterarPerfil(Papel papel, OffsetDateTime instante) {
        if (this.papel != Objects.requireNonNull(papel)) {
            this.papel=papel;
            versaoSeguranca++;
            atualizadaEm=Objects.requireNonNull(instante);
        }
    }
    public void alterarSituacao(boolean ativo, OffsetDateTime instante) {
        if(this.ativo!=ativo) {
            this.ativo=ativo;
            if(!ativo) versaoSeguranca++;
            atualizadaEm=Objects.requireNonNull(instante);
        }
    }
}
