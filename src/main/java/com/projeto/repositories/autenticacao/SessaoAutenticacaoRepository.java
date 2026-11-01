package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.SessaoAutenticacao;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SessaoAutenticacaoRepository extends JpaRepository<SessaoAutenticacao, UUID> {
    @org.springframework.data.jpa.repository.Query("select s from SessaoAutenticacao s join fetch s.usuario where s.sid = :sid")
    java.util.Optional<SessaoAutenticacao> findComUsuarioBySid(@org.springframework.data.repository.query.Param("sid") UUID sid);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select registro from SessaoAutenticacao registro where registro.sid = :id")
    java.util.Optional<SessaoAutenticacao> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

}
