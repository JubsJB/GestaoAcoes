package com.projeto.repositories;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);
}
