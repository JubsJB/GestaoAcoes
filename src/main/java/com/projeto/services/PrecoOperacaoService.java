package com.projeto.services;

import com.projeto.dto.PreviaPrecoCompraResponse;
import com.projeto.dto.SugestaoPrecoVendaResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.TipoOperacao;
import com.projeto.repositories.AcaoRepository;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import com.projeto.validation.TickerNormalizer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class PrecoOperacaoService {

    private static final ZoneId BRAZIL = ZoneId.of("America/Sao_Paulo");
    private static final ZoneId USA = ZoneId.of("America/New_York");

    private final CarteiraRepository carteiras;
    private final AcaoRepository acoes;
    private final OperacaoRepository operacoes;
    private final TickerNormalizer tickers;
    private final FechamentoHistoricoService fechamentoHistorico;
    private final Clock clock;

    public PrecoOperacaoService(
            CarteiraRepository carteiras,
            AcaoRepository acoes,
            OperacaoRepository operacoes,
            TickerNormalizer tickers,
            FechamentoHistoricoService fechamentoHistorico,
            Clock clock
    ) {
        this.carteiras = carteiras;
        this.acoes = acoes;
        this.operacoes = operacoes;
        this.tickers = tickers;
        this.fechamentoHistorico = fechamentoHistorico;
        this.clock = clock;
    }

    public PreviaPrecoCompraResponse consultarPreviaCompra(String ticker, Mercado mercado, LocalDate dataOperacao) {
        Consulta consulta = validarAcao(ticker, mercado, dataOperacao);
        var preco = fechamentoHistorico.consultar(consulta.ticker(), mercado, dataOperacao);
        return new PreviaPrecoCompraResponse(
                consulta.ticker(),
                mercado,
                consulta.acao().getMoeda(),
                dataOperacao,
                preco
        );
    }

    @Transactional(readOnly = true)
    public SugestaoPrecoVendaResponse consultarSugestaoVenda(
            Long carteiraId,
            String ticker,
            Mercado mercado,
            LocalDate dataOperacao
    ) {
        if (carteiraId == null) {
            throw invalid("carteiraId", "Carteira é obrigatória");
        }
        carteiras.findById(carteiraId)
                .orElseThrow(() -> new ObjectNotFoundException("Carteira não encontrada para o id: " + carteiraId));
        Consulta consulta = validarAcao(ticker, mercado, dataOperacao);

        var preco = operacoes
                .findFirstByCarteiraIdAndAcaoIdAndTipoAndDataOperacaoLessThanEqualOrderByDataOperacaoDescOrdemNoDiaDescIdDesc(
                        carteiraId,
                        consulta.acao().getId(),
                        TipoOperacao.COMPRA,
                        dataOperacao
                )
                .map(operacao -> operacao.getPrecoUnitario())
                .orElse(null);
        return new SugestaoPrecoVendaResponse(preco);
    }

    private Consulta validarAcao(String ticker, Mercado mercado, LocalDate dataOperacao) {
        if (mercado == null) {
            throw invalid("mercado", "Mercado é obrigatório");
        }
        String tickerNormalizado = tickers.normalizeAndValidate(ticker);
        validarData(dataOperacao, mercado);
        Acao acao = acoes.findByTickerAndMercado(tickerNormalizado, mercado)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Ação não encontrada para ticker " + tickerNormalizado + " no mercado " + mercado
                ));
        return new Consulta(tickerNormalizado, acao);
    }

    private void validarData(LocalDate data, Mercado mercado) {
        if (data == null) {
            throw invalid("dataOperacao", "Data da operação é obrigatória");
        }
        ZoneId zone = mercado == Mercado.BRASIL ? BRAZIL : USA;
        if (data.isAfter(LocalDate.now(clock.withZone(zone)))) {
            throw invalid("dataOperacao", "Data da operação não pode ser futura");
        }
    }

    private ApiException invalid(String field, String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCodes.REQUEST_INVALIDO,
                "Dados da requisição inválidos",
                java.util.Map.of(field, message)
        );
    }

    private record Consulta(String ticker, Acao acao) {
    }
}
