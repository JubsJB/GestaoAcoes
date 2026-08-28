package com.projeto.repositories;

import com.projeto.entities.Moeda;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface SnapshotCarteiraEvolucaoProjection {

    Long getSnapshotId();

    OffsetDateTime getDataHoraSnapshot();

    Moeda getMoeda();

    BigDecimal getPatrimonioAtual();
}
