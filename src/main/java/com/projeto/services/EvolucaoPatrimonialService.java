package com.projeto.services;

import com.projeto.dto.EvolucaoPatrimonialMoedaResponse;
import com.projeto.dto.EvolucaoPatrimonialPontoResponse;
import com.projeto.dto.EvolucaoPatrimonialResponse;
import com.projeto.repositories.SnapshotCarteiraEvolucaoProjection;
import com.projeto.repositories.SnapshotCarteiraRepository;
import com.projeto.services.exceptions.ObjectNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvolucaoPatrimonialService {

    private final SnapshotCarteiraRepository snapshotCarteiraRepository;

    public EvolucaoPatrimonialService(SnapshotCarteiraRepository snapshotCarteiraRepository) {
        this.snapshotCarteiraRepository = snapshotCarteiraRepository;
    }

    @Transactional(readOnly = true)
    public EvolucaoPatrimonialResponse consultar(Long carteiraId) {
        List<SnapshotCarteiraEvolucaoProjection> linhas =
                snapshotCarteiraRepository.consultarEvolucaoPatrimonial(carteiraId);
        if (linhas.isEmpty()) {
            throw new ObjectNotFoundException("Carteira não encontrada para o id: " + carteiraId);
        }

        Map<Long, PontoEmConstrucao> pontos = new LinkedHashMap<>();
        for (SnapshotCarteiraEvolucaoProjection linha : linhas) {
            if (linha.getSnapshotId() == null) {
                continue;
            }
            PontoEmConstrucao ponto = pontos.computeIfAbsent(
                    linha.getSnapshotId(),
                    snapshotId -> new PontoEmConstrucao(snapshotId, linha.getDataHoraSnapshot())
            );
            if (linha.getMoeda() != null) {
                ponto.patrimonios().add(new EvolucaoPatrimonialMoedaResponse(
                        linha.getMoeda(),
                        linha.getPatrimonioAtual()
                ));
            }
        }

        List<EvolucaoPatrimonialPontoResponse> respostas = pontos.values().stream()
                .map(PontoEmConstrucao::toResponse)
                .toList();
        return new EvolucaoPatrimonialResponse(carteiraId, respostas);
    }

    private record PontoEmConstrucao(
            Long snapshotId,
            OffsetDateTime dataHoraSnapshot,
            List<EvolucaoPatrimonialMoedaResponse> patrimonios
    ) {

        private PontoEmConstrucao(Long snapshotId, OffsetDateTime dataHoraSnapshot) {
            this(snapshotId, dataHoraSnapshot, new ArrayList<>());
        }

        private EvolucaoPatrimonialPontoResponse toResponse() {
            return new EvolucaoPatrimonialPontoResponse(snapshotId, dataHoraSnapshot, patrimonios);
        }
    }
}
