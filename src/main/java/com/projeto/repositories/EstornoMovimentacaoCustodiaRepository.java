package com.projeto.repositories;

import com.projeto.entities.EstornoMovimentacaoCustodia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/** Vínculos em lote; o futuro serviço controla o snapshot e confirma a completude da consulta. */
public interface EstornoMovimentacaoCustodiaRepository extends JpaRepository<EstornoMovimentacaoCustodia, Long> {

    boolean existsByMovimentacaoOriginal_Carteira_Id(Long carteiraId);

    @Query("""
            select e from EstornoMovimentacaoCustodia e
            join fetch e.movimentacaoOriginal m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId
            """)
    List<EstornoMovimentacaoCustodia> listarPorCarteira(@Param("carteiraId") Long carteiraId);

    @Query("""
            select e from EstornoMovimentacaoCustodia e
            join fetch e.movimentacaoOriginal m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId and m.acao.id = :acaoId
            """)
    List<EstornoMovimentacaoCustodia> listarPorCarteiraEAcao(@Param("carteiraId") Long carteiraId,
            @Param("acaoId") Long acaoId);

    /** O chamador deve retornar coleção vazia sem executar esta consulta quando não houver IDs. */
    @Query("""
            select e from EstornoMovimentacaoCustodia e
            join fetch e.movimentacaoOriginal m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId and m.id in :movimentacaoOriginalIds
            """)
    List<EstornoMovimentacaoCustodia> listarPorCarteiraEOriginais(@Param("carteiraId") Long carteiraId,
            @Param("movimentacaoOriginalIds") Collection<Long> movimentacaoOriginalIds);
}
