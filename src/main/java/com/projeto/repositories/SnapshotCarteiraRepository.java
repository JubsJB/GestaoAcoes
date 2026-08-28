package com.projeto.repositories;

import com.projeto.entities.SnapshotCarteira;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotCarteiraRepository extends JpaRepository<SnapshotCarteira, Long> {

    boolean existsByCarteiraId(Long carteiraId);
}
