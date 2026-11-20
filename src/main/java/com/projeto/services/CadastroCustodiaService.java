package com.projeto.services;

import com.projeto.dto.RegistroCustodiaResponse;
import com.projeto.dto.DetalheCustodiaResponse;
import com.projeto.validation.TickerNormalizer;
import java.time.Clock;
import org.springframework.stereotype.Service;

/** A validação precede o proxy; a tradução inclui o encerramento da transação própria. */
@Service
public class CadastroCustodiaService {
    private final CadastroCustodiaResponsePersistenceService persistence;
    private final TickerNormalizer tickers;
    private final Clock clock;
    public CadastroCustodiaService(CadastroCustodiaResponsePersistenceService persistence,TickerNormalizer tickers,Clock clock){
        this.persistence=persistence;this.tickers=tickers;this.clock=clock;
    }
    public RegistroCustodiaResponse transferir(TransferenciaCustodiaCommand entrada){
        if(entrada==null)throw ValidacaoEntradaCustodia.invalida("request","Transferência obrigatória");
        var validada=entrada.validada(tickers,clock);
        try{return persistence.transferir(validada);}
        catch(RuntimeException falha){throw FalhasEscritaOperacao.traduzir(falha);}
    }
    public RegistroCustodiaResponse atribuir(AtribuicaoCustodiaCommand entrada){
        if(entrada==null)throw ValidacaoEntradaCustodia.invalida("request","Atribuição obrigatória");
        var validada=entrada.validada(tickers,clock);
        try{return persistence.atribuir(validada);}
        catch(RuntimeException falha){throw FalhasEscritaOperacao.traduzir(falha);}
    }
    public DetalheCustodiaResponse estornar(EstornoCustodiaCommand entrada){
        if(entrada==null)throw ValidacaoEntradaCustodia.invalida("request","Estorno obrigatório");
        var validada=entrada.validada();
        try{return persistence.estornar(validada);}
        catch(RuntimeException falha){throw FalhasEscritaOperacao.traduzir(falha);}
    }
}
