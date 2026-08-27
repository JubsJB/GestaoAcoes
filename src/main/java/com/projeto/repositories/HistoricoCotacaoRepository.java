package com.projeto.repositories;

import com.projeto.entities.HistoricoCotacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoCotacaoRepository extends JpaRepository<HistoricoCotacao, Long> {

    List<HistoricoCotacao> findByAcaoIdOrderByDataHoraCotacaoAsc(Long acaoId);
}
