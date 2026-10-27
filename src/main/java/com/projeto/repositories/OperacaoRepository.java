package com.projeto.repositories;

import com.projeto.entities.Operacao;
import com.projeto.entities.TipoOperacao;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OperacaoRepository extends JpaRepository<Operacao, Long> {

    /** Histórico integral do contexto; a cronologia quantitativa é determinada em Java. */
    @Query("""
            select o from Operacao o
            join fetch o.carteira
            join fetch o.acao
            left join fetch o.corretora
            where o.carteira.id = :carteiraId and o.acao.id = :acaoId
            """)
    List<Operacao> listarParaReplayQuantitativo(@Param("carteiraId") Long carteiraId,
            @Param("acaoId") Long acaoId);

    @EntityGraph(attributePaths = {"acao", "corretora"})
    @Query("select o from Operacao o where o.carteira.id = :carteiraId order by o.dataOperacao, o.ordemNoDia, o.id")
    List<Operacao> listarParaDistribuicao(@Param("carteiraId") Long carteiraId);

    @EntityGraph(attributePaths = "acao")
    List<Operacao> findByCarteiraIdOrderByDataOperacaoAscOrdemNoDiaAscIdAsc(Long carteiraId);

    List<Operacao> findByCarteiraIdAndAcaoIdOrderByDataOperacaoAscOrdemNoDiaAsc(
            Long carteiraId,
            Long acaoId
    );

    boolean existsByCarteiraId(Long carteiraId);

    Optional<Operacao> findFirstByCarteiraIdAndAcaoIdAndTipoAndDataOperacaoLessThanEqualOrderByDataOperacaoDescOrdemNoDiaDescIdDesc(
            Long carteiraId,
            Long acaoId,
            TipoOperacao tipo,
            LocalDate dataOperacao
    );

    boolean existsByCarteiraIdAndAcaoIdAndDataOperacaoAndOrdemNoDia(
            Long carteiraId,
            Long acaoId,
            LocalDate dataOperacao,
            Integer ordemNoDia
    );
    @Query("select max(o.ordemNoDia) from Operacao o where o.carteira.id = :carteiraId and o.acao.id = :acaoId and o.dataOperacao = :dataOperacao")
    Integer findMaxOrdemNoDia(@Param("carteiraId") Long carteiraId, @Param("acaoId") Long acaoId,
                              @Param("dataOperacao") LocalDate dataOperacao);
}
