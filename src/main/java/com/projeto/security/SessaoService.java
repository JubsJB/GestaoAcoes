package com.projeto.security;

import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import com.projeto.services.exceptions.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.Map;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@ConditionalOnWebApplication
public class SessaoService {
    private final SessaoAutenticacaoRepository sessoes;
    private final UsuarioRepository usuarios;
    private final CredenciaisService credenciais;
    private final JwtService jwt;
    private final Clock clock;
    private final AuthRuntimeConfig config;
    private final ControleSegurancaRepository controle;
    private final ContadorAbusoRepository contadores;
    private final TransactionTemplate loginTransaction;
    public SessaoService(SessaoAutenticacaoRepository sessoes, UsuarioRepository usuarios, CredenciaisService credenciais,
                         JwtService jwt, Clock clock, AuthRuntimeConfig config, ControleSegurancaRepository controle,
                         ContadorAbusoRepository contadores, PlatformTransactionManager manager) {
        this.sessoes=sessoes; this.usuarios=usuarios; this.credenciais=credenciais; this.jwt=jwt; this.clock=clock; this.config=config;
        this.controle=controle; this.contadores=contadores;
        loginTransaction = new TransactionTemplate(manager);
        loginTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        loginTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public Login login(String username, String senha, String cookieAnterior, String ip) {
        String canonico = CredenciaisService.username(username);
        CredenciaisService.validarSenha(senha);
        Tentativa tentativa = loginTransaction.execute(status -> {
            // Mesmo lock singleton antes do usuário: contadores compartilhados e bootstrap concorrente.
            controle.findByIdForUpdate((short) 1).orElseThrow(() -> new IllegalStateException("Controle de segurança ausente"));
            var agora = OffsetDateTime.now(clock);
            String origem = ip == null ? "desconhecido" : ip;
            var identidade = contador(TipoContadorAbuso.LOGIN_IDENTIDADE_IP, hash(canonico+"\0"+origem), agora);
            var endereco = contador(TipoContadorAbuso.LOGIN_IP, hash(origem), agora);
            if ((identidade != null && identidade.getTentativas() >= 5) || (endereco != null && endereco.getTentativas() >= 50)) {
                var fim = agora;
                if (identidade != null && identidade.getTentativas() >= 5) fim = identidade.getExpiraEm();
                if (endereco != null && endereco.getTentativas() >= 50 && endereco.getExpiraEm().isAfter(fim)) fim = endereco.getExpiraEm();
                Duration restante = Duration.between(agora,fim);
                long segundos = Math.max(1, restante.getSeconds()+(restante.getNano() > 0 ? 1 : 0));
                return new Tentativa(null, new ApiException(HttpStatus.TOO_MANY_REQUESTS,"LIMITE_LOGIN_EXCEDIDO",
                        "Tente novamente mais tarde.",Map.of("retryAfter",segundos)));
            }
            try { return new Tentativa(criarLogin(canonico,senha,cookieAnterior),null); }
            catch (ApiException ex) {
                if (ex.getStatus() != HttpStatus.UNAUTHORIZED) throw ex;
                falha(identidade,TipoContadorAbuso.LOGIN_IDENTIDADE_IP,hash(canonico+"\0"+origem),agora);
                falha(endereco,TipoContadorAbuso.LOGIN_IP,hash(origem),agora);
                contadores.flush();
                return new Tentativa(null,ex);
            }
        });
        // O erro HTTP ocorre depois do commit, preservando tentativas mesmo no caminho 401.
        if (tentativa.erro() != null) throw tentativa.erro();
        return tentativa.login();
    }
    private ContadorAbuso contador(TipoContadorAbuso tipo, String chave, OffsetDateTime agora) {
        return contadores.findFirstByTipoAndChaveHashAndExpiraEmAfterOrderByInicioJanelaDesc(tipo,chave,agora).orElse(null);
    }
    private void falha(ContadorAbuso contador, TipoContadorAbuso tipo, String chave, OffsetDateTime agora) {
        if (contador == null) contadores.save(new ContadorAbuso(tipo,chave,agora,agora.plusMinutes(15),1));
        else contador.registrarFalha();
    }
    private static String hash(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 indisponível"); }
    }
    private record Tentativa(Login login, ApiException erro) { }
    private Login criarLogin(String username, String senha, String cookieAnterior) {
        var identidade = credenciais.autenticar(username, senha);
        var usuario = usuarios.findById(identidade.id()).orElseThrow(CredenciaisService::invalida);
        var agora = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
        var expira = agora.plus(config.duracao()).truncatedTo(ChronoUnit.SECONDS);
        var sessao = new SessaoAutenticacao(UUID.randomUUID(), usuario, identidade.versaoSeguranca(), agora, agora, expira, null, null);
        sessoes.saveAndFlush(sessao);
        if (cookieAnterior != null) {
            try {
                var anterior = jwt.validar(cookieAnterior);
                var registro = sessoes.findByIdForUpdate(anterior.sid()).orElse(null);
                if (registro != null) { validar(registro, anterior); registro.revogar(agora, "NOVO_LOGIN"); }
            } catch (ApiException ex) { if (ex.getStatus().value() != 401) throw ex; }
        }
        return new Login(jwt.emitir(sessao), principal(sessao, agora));
    }
    @Transactional(readOnly = true)
    public Principal autenticar(JwtService.Claims claims) {
        var sessao = sessoes.findComUsuarioBySid(claims.sid()).orElseThrow(CredenciaisService::invalida);
        return validar(sessao, claims);
    }
    @Transactional
    public void logout(JwtService.Claims claims) {
        var sessao = bloquear(claims);
        validar(sessao, claims);
        sessao.revogar(OffsetDateTime.now(clock), "LOGOUT");
        sessoes.flush();
    }
    @Transactional
    public Principal atividade(JwtService.Claims claims) {
        var sessao = bloquear(claims);
        var validada = validar(sessao, claims); // Clock aferido após esperar os locks.
        var instante = validada.horarioServidor().truncatedTo(ChronoUnit.MICROS);
        sessao.registrarAtividade(instante);
        sessoes.flush();
        return principal(sessao, validada.horarioServidor());
    }
    private SessaoAutenticacao bloquear(JwtService.Claims claims) {
        // D7: usuário antes da sessão; aferir prazo somente depois de ambos os locks.
        usuarios.findByIdForUpdate(claims.usuarioId()).orElseThrow(CredenciaisService::invalida);
        return sessoes.findByIdForUpdate(claims.sid()).orElseThrow(CredenciaisService::invalida);
    }
    private Principal validar(SessaoAutenticacao sessao, JwtService.Claims claims) {
        var usuario = sessao.getUsuario();
        var agora = OffsetDateTime.now(clock);
        if (!usuario.isAtivo() || !usuario.isCredencialDefinida() || usuario.getId() != claims.usuarioId()
                || usuario.getVersaoSeguranca() != claims.versao() || sessao.getVersaoEmitida() != claims.versao()
                || usuario.getPapel() != claims.papel() || sessao.getRevogadaEm() != null
                || !sessao.getExpiraEmAbsoluto().toInstant().equals(claims.expiraEm())
                || sessao.getCriadaEm().toEpochSecond() != claims.emitidoEm().getEpochSecond()) throw CredenciaisService.invalida();
        if (!agora.isBefore(sessao.getExpiraEmAbsoluto()) || !agora.isBefore(sessao.getUltimaAtividadeEm().plusMinutes(10)))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSAO_EXPIRADA", "Sessão expirada. Autentique novamente.");
        return principal(sessao, agora);
    }
    private Principal principal(SessaoAutenticacao s, OffsetDateTime agora) {
        return new Principal(s.getSid(), s.getUsuario().getId(), s.getUsuario().getUsername(), s.getUsuario().getPapel(),
                agora, s.getUltimaAtividadeEm(), s.getExpiraEmAbsoluto());
    }
    public record Principal(UUID sid, Long id, String username, Papel papel, OffsetDateTime horarioServidor,
                            OffsetDateTime ultimaAtividadeEm, OffsetDateTime expiraEm) { }
    public static final class Login {
        private final String token;
        private final Principal principal;
        Login(String token, Principal principal) { this.token=token; this.principal=principal; }
        public String token() { return token; }
        public Principal principal() { return principal; }
        @Override public String toString() { return "Login[credencial protegida]"; }
    }
}
