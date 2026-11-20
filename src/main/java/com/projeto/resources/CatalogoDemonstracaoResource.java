package com.projeto.resources;

import com.projeto.config.CatalogoDemonstracao;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("dados-demonstracao")
@RestController
class CatalogoDemonstracaoResource {
    private final CatalogoDemonstracao catalogo;
    private final String runId;

    CatalogoDemonstracaoResource(CatalogoDemonstracao catalogo, Environment env) {
        this.catalogo = catalogo;
        this.runId = env.getRequiredProperty("app.demo-run-id");
    }

    @GetMapping("/apresentacao/catalogo")
    ResponseEntity<?> consultar() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "perfil", "dados-demonstracao", "execucao", runId, "acoes", catalogo.acoes(),
                "corretoras", catalogo.corretoras()));
    }
}
