package com.projeto.repositories;

import com.projeto.entities.Operacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    List<Operacao> findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(
            Long carteiraId,
            Long acaoId
    );

    boolean existsByCarteiraId(Long carteiraId);

    boolean existsByCarteiraIdAndAcaoIdAndDataOperacaoAndOrdemNoDia(
            Long carteiraId,
            Long acaoId,
            LocalDate dataOperacao,
            Integer ordemNoDia
    );
}
