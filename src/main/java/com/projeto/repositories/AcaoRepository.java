package com.projeto.repositories;

import com.projeto.entities.Acao;
import com.projeto.entities.Mercado;
import com.projeto.entities.ReferenciaTipoCotacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import org.springframework.transaction.annotation.Transactional;

public interface AcaoRepository extends JpaRepository<Acao, Long> {

    /** Projeção escalar: lê valores do banco sem reutilizar Acao gerenciada. */
    interface SnapshotCotacao {
        BigDecimal getPreco();
        ReferenciaTipoCotacao getTipo();
        OffsetDateTime getInstante();
        LocalDate getData();
        OffsetDateTime getObtencao();
        OffsetDateTime getLegado();
    }

    @Transactional(readOnly = true)
    @Query("select a.cotacaoAtual as preco, a.referenciaTipo as tipo, a.referenciaInstante as instante, "
            + "a.referenciaData as data, a.cotacaoObtidaEm as obtencao, a.dataHoraCotacao as legado "
            + "from Acao a where a.id = :id")
    Optional<SnapshotCotacao> lerSnapshotCotacao(@Param("id") Long id);

    boolean existsByTickerAndMercado(String ticker, Mercado mercado);

    Optional<Acao> findByTickerAndMercado(String ticker, Mercado mercado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select acao from Acao acao where acao.id = :id")
    Optional<Acao> findByIdForUpdate(@Param("id") Long id);
}
