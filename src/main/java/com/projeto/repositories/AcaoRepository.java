package com.projeto.repositories;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);

    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);
}
