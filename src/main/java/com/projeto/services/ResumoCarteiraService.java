package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.dto.ResumoCarteiraResponse;
import com.projeto.dto.ResumoMoedaResponse;
import com.projeto.mappers.ResumoCarteiraMapper;
import com.projeto.services.AgregadorPosicoesPorMoeda.FalhaAgregacaoException;
import com.projeto.services.AgregadorPosicoesPorMoeda.TotaisPorMoeda;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumoCarteiraService {

    private final PosicaoService posicaoService;
    private final AgregadorPosicoesPorMoeda agregador;
    private final CalculadoraRentabilidade calculadoraRentabilidade;
    private final ResumoCarteiraMapper mapper;

    public ResumoCarteiraService(
            PosicaoService posicaoService,
            AgregadorPosicoesPorMoeda agregador,
            CalculadoraRentabilidade calculadoraRentabilidade,
            ResumoCarteiraMapper mapper
    ) {
        this.posicaoService = posicaoService;
        this.agregador = agregador;
        this.calculadoraRentabilidade = calculadoraRentabilidade;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public ResumoCarteiraResponse consultar(Long carteiraId) {
        List<PosicaoResponse> posicoes = posicaoService.listarPorCarteira(carteiraId);
        try {
            List<TotaisPorMoeda> totais = agregador.agregar(posicoes);
            List<ResumoMoedaResponse> resumos = new ArrayList<>(totais.size());
            for (TotaisPorMoeda total : totais) {
                validarCustoTotalPositivo(carteiraId, total);
                BigDecimal rentabilidadePercentual;
                try {
                    rentabilidadePercentual = calculadoraRentabilidade.calcularPercentual(
                            total.resultadoNaoRealizadoTotal(),
                            total.custoTotalPosicoes()
                    );
                } catch (ArithmeticException exception) {
                    throw falhaCalculoRentabilidade(carteiraId, total, exception);
                }
                resumos.add(mapper.toMoedaResponse(total, rentabilidadePercentual));
            }
            return mapper.toResponse(carteiraId, resumos);
        } catch (FalhaAgregacaoException exception) {
            throw falhaCalculo(carteiraId, exception);
        }
    }

    private void validarCustoTotalPositivo(Long carteiraId, TotaisPorMoeda total) {
        if (total.custoTotalPosicoes().signum() > 0) {
            return;
        }
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("carteiraId", carteiraId);
        detalhes.put("moeda", total.moeda());
        detalhes.put("custoTotalPosicoes", total.custoTotalPosicoes());
        detalhes.put("motivo", "Posições abertas possuem custo total não positivo");
        throw new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                "Histórico de Operações inconsistente",
                detalhes
        );
    }

    private ApiException falhaCalculoRentabilidade(
            Long carteiraId,
            TotaisPorMoeda total,
            ArithmeticException exception
    ) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("carteiraId", carteiraId);
        detalhes.put("moeda", total.moeda());
        detalhes.put("indicador", "rentabilidadePercentual");
        detalhes.put("motivo", exception.getMessage());
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,
                "Cálculo do resumo excede a precisão aprovada",
                detalhes
        );
    }

    private ApiException falhaCalculo(Long carteiraId, FalhaAgregacaoException exception) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("carteiraId", carteiraId);
        detalhes.put("moeda", exception.moeda());
        detalhes.put("indicador", exception.indicador());
        detalhes.put("motivo", exception.getMessage());
        return new ApiException(
                HttpStatus.UNPROCESSABLE_CONTENT,
                ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,
                "Cálculo do resumo excede a precisão aprovada",
                detalhes
        );
    }
}
