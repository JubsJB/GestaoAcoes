package com.projeto.repositories;

import com.projeto.entities.SnapshotCarteira;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SnapshotCarteiraRepository extends JpaRepository<SnapshotCarteira, Long> {

    boolean existsByCarteiraId(Long carteiraId);

    @Query("""
            select snapshot.id as snapshotId,
                   snapshot.dataHoraSnapshot as dataHoraSnapshot,
                   componente.moeda as moeda,
                   componente.patrimonioAtual as patrimonioAtual
              from Carteira carteira
              left join SnapshotCarteira snapshot on snapshot.carteira = carteira
              left join SnapshotCarteiraMoeda componente on componente.snapshotCarteira = snapshot
             where carteira.id = :carteiraId
             order by snapshot.dataHoraSnapshot asc, snapshot.id asc, componente.moeda asc
            """)
    List<SnapshotCarteiraEvolucaoProjection> consultarEvolucaoPatrimonial(
            @Param("carteiraId") Long carteiraId
    );
}
