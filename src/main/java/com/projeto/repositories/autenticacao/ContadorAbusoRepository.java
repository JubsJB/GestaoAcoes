package com.projeto.repositories.autenticacao;

import com.projeto.entities.autenticacao.ContadorAbuso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContadorAbusoRepository extends JpaRepository<ContadorAbuso, Long> {
    java.util.Optional<ContadorAbuso> findFirstByTipoAndChaveHashAndExpiraEmAfterOrderByInicioJanelaDesc(
            com.projeto.entities.autenticacao.TipoContadorAbuso tipo, String chaveHash, java.time.OffsetDateTime agora);
}
