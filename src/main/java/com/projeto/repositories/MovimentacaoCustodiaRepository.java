package com.projeto.repositories;

import com.projeto.entities.MovimentacaoCustodia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** Consultas completas; o futuro serviço controla o snapshot e a ordenação do replay. */
public interface MovimentacaoCustodiaRepository extends JpaRepository<MovimentacaoCustodia, Long> {

    boolean existsByCarteira_Id(Long carteiraId);

    @Query("""
            select m from MovimentacaoCustodia m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId
            """)
    List<MovimentacaoCustodia> listarPorCarteira(@Param("carteiraId") Long carteiraId);

    @Query("""
            select m from MovimentacaoCustodia m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId and m.acao.id = :acaoId
            """)
    List<MovimentacaoCustodia> listarPorCarteiraEAcao(@Param("carteiraId") Long carteiraId,
            @Param("acaoId") Long acaoId);

    @Query("""
            select m from MovimentacaoCustodia m
            join fetch m.carteira
            join fetch m.acao
            join fetch m.corretoraDestino
            left join fetch m.corretoraOrigem
            where m.carteira.id = :carteiraId and m.id = :movimentacaoId
            """)
    Optional<MovimentacaoCustodia> buscarPorCarteiraEId(@Param("carteiraId") Long carteiraId,
            @Param("movimentacaoId") Long movimentacaoId);
}
