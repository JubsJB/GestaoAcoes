package com.projeto.mappers;

import com.projeto.dto.SnapshotCarteiraMoedaResponse;
import com.projeto.dto.SnapshotCarteiraResponse;
import com.projeto.entities.SnapshotCarteira;
import com.projeto.entities.SnapshotCarteiraMoeda;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class SnapshotCarteiraMapper {

    public SnapshotCarteiraResponse toResponse(
            SnapshotCarteira snapshot,
            List<SnapshotCarteiraMoeda> componentes
    ) {
        List<SnapshotCarteiraMoedaResponse> patrimonios = componentes.stream()
                .sorted(Comparator.comparing(componente -> componente.getMoeda().name()))
                .map(componente -> new SnapshotCarteiraMoedaResponse(
                        componente.getMoeda(),
                        componente.getPatrimonioAtual()
                ))
                .toList();
        return new SnapshotCarteiraResponse(
                snapshot.getId(),
                snapshot.getCarteira().getId(),
                snapshot.getDataHoraSnapshot(),
                patrimonios
        );
    }
}
