package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.AuditoriaUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaUsuarioRepository extends JpaRepository<AuditoriaUsuario, Long> {
}
