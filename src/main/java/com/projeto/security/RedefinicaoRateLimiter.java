package com.projeto.security;

import com.projeto.entities.autenticacao.*;
import com.projeto.repositories.autenticacao.*;
import com.projeto.services.exceptions.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

/** Fase independente antes dos locks de consumo; contabiliza requests, não só falhas. */
@Service @ConditionalOnWebApplication
public class RedefinicaoRateLimiter {
    private final ContadorAbusoRepository contadores;
    private final ControleSegurancaRepository controle;
    private final Clock clock;
    private final TransactionTemplate transacao;
    public RedefinicaoRateLimiter(ContadorAbusoRepository contadores,ControleSegurancaRepository controle,Clock clock,PlatformTransactionManager manager) {
        this.contadores=contadores;this.controle=controle;this.clock=clock;
        transacao=new TransactionTemplate(manager);transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transacao.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public void registrarIp(String ip) {registrar(TipoContadorAbuso.REDEFINICAO_IP,ip,50);}
    public void registrarIdentidade(String username,String ip) {registrar(TipoContadorAbuso.REDEFINICAO_IDENTIDADE_IP,username.trim().toLowerCase(Locale.ROOT)+"\0"+ip,10);}
    private void registrar(TipoContadorAbuso tipo,String identidade,int limite) {
        try {
            String chave=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identidade.getBytes(StandardCharsets.UTF_8)));
            var erro=transacao.execute(status -> {
                controle.findByIdForUpdate((short)1).orElseThrow(IllegalStateException::new);
                var agora=OffsetDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                var contador=contadores.findFirstByTipoAndChaveHashAndExpiraEmAfterOrderByInicioJanelaDesc(tipo,chave,agora).orElse(null);
                if(contador!=null && contador.getTentativas()>=limite) {
                    var duracao=Duration.between(agora,contador.getExpiraEm());
                    long espera=Math.max(1,duracao.getSeconds()+(duracao.getNano()>0?1:0));
                    return new ApiException(HttpStatus.TOO_MANY_REQUESTS,"LIMITE_REDEFINICAO_EXCEDIDO","Tente novamente mais tarde.",Map.of("retryAfter",espera));
                }
                if(contador==null) contadores.save(new ContadorAbuso(tipo,chave,agora,agora.plusMinutes(15),1));
                else contador.registrarFalha();
                contadores.flush();return null;
            });
            if(erro!=null) throw erro;
        } catch(NoSuchAlgorithmException ex) {throw new IllegalStateException("Verificação indisponível");}
        catch(org.springframework.dao.DataAccessException | TransactionException ex) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"SEGURANCA_INDISPONIVEL","Não foi possível verificar o limite de requisições.");
        }
    }
}
