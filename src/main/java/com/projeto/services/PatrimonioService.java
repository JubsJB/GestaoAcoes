package com.projeto.services;

import com.projeto.dto.PatrimonioMoedaResponse;
import com.projeto.dto.PatrimonioResponse;
import com.projeto.dto.PosicaoResponse;
import com.projeto.entities.Moeda;
import com.projeto.mappers.PatrimonioMapper;
import com.projeto.services.exceptions.ApiException;
import com.projeto.services.exceptions.ErrorCodes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PatrimonioService {

    private static final int ESCALA_PATRIMONIO = 12;
    private static final int PRECISAO_PATRIMONIO = 38;
    private static final Comparator<Moeda> ORDEM_MOEDA = Comparator.comparing(Enum::name);

    private final PosicaoService posicaoService;
    private final PatrimonioMapper mapper;

    public PatrimonioService(PosicaoService posicaoService, PatrimonioMapper mapper) {
        this.posicaoService = posicaoService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PatrimonioResponse consultar(Long carteiraId) {
        List<PosicaoResponse> posicoes = posicaoService.listarPorCarteira(carteiraId);
        Map<Moeda, BigDecimal> acumulados = somarPorMoeda(posicoes);
        List<PatrimonioMoedaResponse> patrimonios = new ArrayList<>(acumulados.size());

        acumulados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(ORDEM_MOEDA))
                .forEach(entry -> patrimonios.add(mapper.toMoedaResponse(
                        entry.getKey(),
                        normalizar(carteiraId, entry.getKey(), entry.getValue())
                )));

        return mapper.toResponse(carteiraId, patrimonios);
    }

    private Map<Moeda, BigDecimal> somarPorMoeda(List<PosicaoResponse> posicoes) {
        Map<Moeda, BigDecimal> acumulados = new EnumMap<>(Moeda.class);
        for (PosicaoResponse posicao : posicoes) {
            acumulados.merge(posicao.moeda(), posicao.valorAtualPosicao(), BigDecimal::add);
        }
        return acumulados;
    }

    private BigDecimal normalizar(Long carteiraId, Moeda moeda, BigDecimal acumulado) {
        try {
            BigDecimal normalizado = acumulado.setScale(ESCALA_PATRIMONIO, RoundingMode.UNNECESSARY);
            if (normalizado.precision() > PRECISAO_PATRIMONIO) {
                throw new ArithmeticException("Patrimônio excede a precisão máxima 38");
            }
            return normalizado;
        } catch (ArithmeticException exception) {
            Map<String, Object> detalhes = new LinkedHashMap<>();
            detalhes.put("carteiraId", carteiraId);
            detalhes.put("moeda", moeda);
            detalhes.put("motivo", exception.getMessage());
            throw new ApiException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    ErrorCodes.CALCULO_POSICAO_FORA_DA_PRECISAO,
                    "Cálculo do patrimônio excede a precisão aprovada",
                    detalhes
            );
        }
    }
}
