package com.projeto.autenticacao;

import com.projeto.entities.autenticacao.Papel;
import com.projeto.entities.autenticacao.Usuario;
import com.projeto.entities.autenticacao.AutorizacaoRedefinicaoSenha;
import com.projeto.entities.autenticacao.MotivoRedefinicao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Caracterização das fronteiras de entrada do modelo persistente, sem serviço funcional. */
class UsuarioModeloTest {
    @Test
    void identificadorDaAutorizacaoExisteAntesDoInsertParaComporVerificador() {
        Usuario usuario = new Usuario("titular", Papel.USER, AutenticacaoPersistenceTest.AGORA);
        var id = java.util.UUID.randomUUID();
        var registro = new AutorizacaoRedefinicaoSenha(id, usuario, usuario, "a".repeat(64),
                MotivoRedefinicao.INICIAL, AutenticacaoPersistenceTest.AGORA,
                AutenticacaoPersistenceTest.AGORA.plusMinutes(15), 5, null, null);
        assertEquals(id, registro.getId());
    }
    @ParameterizedTest
    @ValueSource(strings = {"ab", "nome com espaco", "usuario@dominio", "ábc", "usuario/externo"})
    void rejeitaUsernameForaDaPolitica(String username) {
        assertThrows(IllegalArgumentException.class,
                () -> new Usuario(username, Papel.USER, AutenticacaoPersistenceTest.AGORA));
    }

    @Test
    void respeitaLimiteDeCemCaracteres() {
        assertEquals(100, new Usuario("a".repeat(100), Papel.USER,
                AutenticacaoPersistenceTest.AGORA).getUsername().length());
        assertThrows(IllegalArgumentException.class,
                () -> new Usuario("a".repeat(101), Papel.USER, AutenticacaoPersistenceTest.AGORA));
    }

    @Test
    void normalizaUsernameSemAlterarIdentidadeComLocaleDoAmbiente() {
        assertEquals("investidor.01", new Usuario("  INVESTIDOR.01  ", Papel.USER,
                AutenticacaoPersistenceTest.AGORA).getUsername());
    }

    @Test
    void exigeCamposObrigatoriosSemDefaultsDeCredenciais() {
        assertThrows(NullPointerException.class,
                () -> new Usuario(null, Papel.USER, AutenticacaoPersistenceTest.AGORA));
        assertThrows(NullPointerException.class,
                () -> new Usuario("usuario", null, AutenticacaoPersistenceTest.AGORA));
        assertThrows(NullPointerException.class, () -> new Usuario("usuario", Papel.USER, null));
    }
}
