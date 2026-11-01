package com.projeto.autenticacao;

/** Fronteira de preflight, antes de Docker create/JDBC; somente suporte de testes. */
final class AutenticacaoPostgresPreflight {
    static final String DIGEST = "sha256:eee6c6e1b0894fa4fe0075eb054af9fd1cf39a35321c2d4110626cfe506411fb";
    static void validar(String contexto, String endpoint, String os, String arquitetura, String digest) {
        if (!"desktop-linux".equals(contexto) || !"npipe:////./pipe/dockerDesktopLinuxEngine".equals(endpoint)
                || !"linux".equals(os) || !"amd64".equals(arquitetura) || !DIGEST.equals(digest))
            throw new IllegalStateException("Preflight PostgreSQL recusado: contexto/plataforma/pin divergentes");
    }
}
