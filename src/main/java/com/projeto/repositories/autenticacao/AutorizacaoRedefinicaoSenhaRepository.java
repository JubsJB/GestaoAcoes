package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.AutorizacaoRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AutorizacaoRedefinicaoSenhaRepository extends JpaRepository<AutorizacaoRedefinicaoSenha, UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AutorizacaoRedefinicaoSenha a where a.usuario.id=:usuarioId and a.consumidaEm is null and a.invalidadaEm is null order by a.id")
    java.util.List<AutorizacaoRedefinicaoSenha> findPendentesForUpdate(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select registro from AutorizacaoRedefinicaoSenha registro where registro.id = :id")
    java.util.Optional<AutorizacaoRedefinicaoSenha> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

}
