package com.projeto.repositories;

import com.projeto.entities.Carteira;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarteiraRepository extends JpaRepository<Carteira, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select carteira from Carteira carteira where carteira.id = :id")
    Optional<Carteira> findByIdForUpdate(@Param("id") Long id);
}
