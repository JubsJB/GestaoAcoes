package com.projeto.services;

import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Acao;
import com.projeto.entities.Operacao;
import com.projeto.mappers.PosicaoMapper;
import com.projeto.repositories.CarteiraRepository;
import com.projeto.repositories.OperacaoRepository;
import com.projeto.services.CalculadoraPosicao.FalhaReplay;
import com.projeto.services.CalculadoraPosicao.ResultadoReplay;
import com.projeto.services.CalculadoraPosicao.TipoFalha;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PosicaoService {

    private static final Comparator<PosicaoResponse> ORDEM_APRESENTACAO = Comparator
            .comparing(PosicaoResponse::mercado)
            .thenComparing(PosicaoResponse::ticker)
            .thenComparing(PosicaoResponse::acaoId);

    private final CarteiraRepository carteiraRepository;
    private final OperacaoRepository operacaoRepository;
    private final CalculadoraPosicao calculadora;
    private final PosicaoMapper mapper;

    public PosicaoService(
            CarteiraRepository carteiraRepository,
            OperacaoRepository operacaoRepository,
            CalculadoraPosicao calculadora,
            PosicaoMapper mapper
    ) {
        this.carteiraRepository = carteiraRepository;
        this.operacaoRepository = operacaoRepository;
        this.calculadora = calculadora;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public List<PosicaoResponse> listarPorCarteira(Long carteiraId) {
        carteiraRepository.findById(carteiraId)
                .orElseThrow(() -> new ObjectNotFoundException(
                        "Carteira não encontrada para o id: " + carteiraId
                ));

        List<Operacao> historico = operacaoRepository
                .findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(carteiraId);
        Map<Long, List<Operacao>> operacoesPorAcao = agruparPorAcao(historico);
        List<PosicaoResponse> posicoes = new ArrayList<>(operacoesPorAcao.size());

        for (List<Operacao> operacoes : operacoesPorAcao.values()) {
            ResultadoReplay resultado = calculadora.reproduzir(operacoes);
            if (!resultado.valido()) {
                throw falhaDeReplay(carteiraId, resultado.falha());
            }
            if (resultado.posicao().quantidadeAtual().signum() == 0) {
                continue;
            }

            Acao acao = operacoes.get(0).getAcao();
            posicoes.add(mapper.toResponse(acao, resultado.posicao()));
        }

        posicoes.sort(ORDEM_APRESENTACAO);
        return List.copyOf(posicoes);
    }

    private Map<Long, List<Operacao>> agruparPorAcao(List<Operacao> historico) {
        Map<Long, List<Operacao>> grupos = new LinkedHashMap<>();
        for (Operacao operacao : historico) {
            Long acaoId = operacao == null || operacao.getAcao() == null
                    ? null
                    : operacao.getAcao().getId();
            grupos.computeIfAbsent(acaoId, ignored -> new ArrayList<>()).add(operacao);
        }
        return grupos;
    }

    private ApiException falhaDeReplay(Long carteiraId, FalhaReplay falha) {
        Map<String, Object> detalhes = detalhes(carteiraId, falha);
        if (falha.tipo() == TipoFalha.CALCULO_FORA_DA_PRECISAO) {
            return new ApiException(
                    HttpStatus.valueOf(422),
                    ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,
                    "Cálculo da posição excede a precisão aprovada",
                    detalhes
            );
        }
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCodes.HISTORICO_OPERACOES_INCONSISTENTE,
                "Histórico de Operações inconsistente",
                detalhes
        );
    }

    private Map<String, Object> detalhes(Long carteiraId, FalhaReplay falha) {
        Map<String, Object> detalhes = new LinkedHashMap<>();
        detalhes.put("carteiraId", carteiraId);
        detalhes.put("motivo", falha.motivo());

        Operacao operacao = falha.operacao();
        if (operacao != null) {
            putIfNotNull(detalhes, "operacaoId", operacao.getId());
            putIfNotNull(detalhes, "dataOperacao", operacao.getDataOperacao());
            putIfNotNull(detalhes, "ordemNoDia", operacao.getOrdemNoDia());
            if (operacao.getAcao() != null) {
                putIfNotNull(detalhes, "acaoId", operacao.getAcao().getId());
                putIfNotNull(detalhes, "ticker", operacao.getAcao().getTicker());
            }
        }
        putIfNotNull(detalhes, "quantidadeDisponivel", falha.quantidadeDisponivel());
        putIfNotNull(detalhes, "quantidadeSolicitada", falha.quantidadeSolicitada());
        return detalhes;
    }

    private void putIfNotNull(Map<String, Object> detalhes, String chave, Object valor) {
        if (valor != null) {
            detalhes.put(chave, valor);
        }
    }
}
