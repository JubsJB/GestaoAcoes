package com.projeto.repositories;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);

    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select acao from Acao acao where acao.id = :id")
    Optional<Acao> findByIdForUpdate(@Param("id") Long id);
}
