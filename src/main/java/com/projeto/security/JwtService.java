package com.projeto.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.projeto.entities.autenticacao.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.resource.authentication.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Bibliotecas Nimbus/Spring, algoritmo fixo; nenhum transporte Bearer HTTP é habilitado. */
public final class JwtService {
    private final JwtEncoder encoder;
    private final JwtAuthenticationProvider provider;
    public JwtService(AuthRuntimeConfig config, Clock clock) {
        encoder = new NimbusJwtEncoder(new ImmutableSecret<>(config.key()));
        var decoder = NimbusJwtDecoder.withSecretKey(config.key()).macAlgorithm(MacAlgorithm.HS256).build();
        var tempo = new JwtTimestampValidator(Duration.ZERO); tempo.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(tempo, new JwtIssuerValidator("gestaoacoes"), jwt -> {
            boolean valido = jwt.getAudience().contains("gestaoacoes-api") && jwt.getIssuedAt() != null
                    && jwt.getExpiresAt() != null && !jwt.getIssuedAt().isAfter(clock.instant())
                    && jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    && Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).compareTo(Duration.ofHours(4)) <= 0;
            return valido ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        }));
        provider = new JwtAuthenticationProvider(decoder);
    }
    public String emitir(SessaoAutenticacao sessao) {
        var usuario = sessao.getUsuario();
        var claims = JwtClaimsSet.builder().issuer("gestaoacoes").audience(List.of("gestaoacoes-api"))
                .subject(usuario.getId().toString()).issuedAt(sessao.getCriadaEm().toInstant())
                .expiresAt(sessao.getExpiraEmAbsoluto().toInstant()).claim("sid", sessao.getSid().toString())
                .claim("versaoSeguranca", sessao.getVersaoEmitida())
                .claim("authorities", List.of("ROLE_"+usuario.getPapel())).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
    public Claims validar(String token) {
        try {
            Jwt jwt = ((JwtAuthenticationToken) provider.authenticate(new BearerTokenAuthenticationToken(token))).getToken();
            List<String> authorities = jwt.getClaimAsStringList("authorities");
            if (authorities == null || authorities.size() != 1 || !authorities.get(0).startsWith("ROLE_")) throw new IllegalArgumentException();
            Papel papel = Papel.valueOf(authorities.get(0).substring(5));
            Object versao = jwt.getClaim("versaoSeguranca");
            if (!(versao instanceof Number)) throw new IllegalArgumentException();
            long numero = new java.math.BigDecimal(versao.toString()).longValueExact();
            long usuario = Long.parseLong(jwt.getSubject());
            if (usuario <= 0 || numero < 0) throw new IllegalArgumentException();
            return new Claims(usuario, UUID.fromString(jwt.getClaimAsString("sid")), numero,
                    jwt.getIssuedAt(), jwt.getExpiresAt(), papel);
        } catch (RuntimeException ex) {
            throw CredenciaisService.invalida();
        }
    }
    public record Claims(long usuarioId, UUID sid, long versao, Instant emitidoEm, Instant expiraEm, Papel papel) { }
}
