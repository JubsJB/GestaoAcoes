package com.projeto.autenticacao;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutenticacaoPostgresPreflightTest {
    @Test void recusaContextoDivergenteAntesDeProvisionar() {
        assertThrows(IllegalStateException.class, () -> AutenticacaoPostgresPreflight.validar(
                "remoto", "npipe:////./pipe/dockerDesktopLinuxEngine", "linux", "amd64", AutenticacaoPostgresPreflight.DIGEST));
    }
    @Test void aceitaSomenteEndpointPlataformaEDigestFixados() {
        String endpoint="npipe:////./pipe/dockerDesktopLinuxEngine", digest=AutenticacaoPostgresPreflight.DIGEST;
        assertDoesNotThrow(()->AutenticacaoPostgresPreflight.validar("desktop-linux",endpoint,"linux","amd64",digest));
        assertThrows(IllegalStateException.class,()->AutenticacaoPostgresPreflight.validar("desktop-linux","tcp://remoto:2375","linux","amd64",digest));
        assertThrows(IllegalStateException.class,()->AutenticacaoPostgresPreflight.validar("desktop-linux",endpoint,"windows","amd64",digest));
        assertThrows(IllegalStateException.class,()->AutenticacaoPostgresPreflight.validar("desktop-linux",endpoint,"linux","arm64",digest));
        assertThrows(IllegalStateException.class,()->AutenticacaoPostgresPreflight.validar("desktop-linux",endpoint,"linux","amd64","sha256:"+"0".repeat(64)));
    }
}
