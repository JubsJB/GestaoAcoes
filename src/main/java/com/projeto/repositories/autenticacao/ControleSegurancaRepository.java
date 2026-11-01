package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.ControleSeguranca;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ControleSegurancaRepository extends JpaRepository<ControleSeguranca, Short> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select registro from ControleSeguranca registro where registro.id = :id")
    java.util.Optional<ControleSeguranca> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Short id);

}
