package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    long countByPapelAndAtivoTrueAndCredencialDefinidaTrue(com.projeto.entities.autenticacao.Papel papel);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.username = :username")
    Optional<Usuario> findByUsernameForUpdate(@org.springframework.data.repository.query.Param("username") String username);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
