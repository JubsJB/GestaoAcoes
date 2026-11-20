package com.projeto.custodia.postgresql;

import java.sql.Connection;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;

/** Transportes simulados; não homologa PostgreSQL. */
class CustodiaPostgresCapacidadeTest {
    private final Imagem pin = new Imagem(IMAGEM, "sha256:" + "a".repeat(64));
    private final Execucao execucao = mock(Execucao.class);
    private final CustodiaPostgresDataSource ds = mock(CustodiaPostgresDataSource.class);
    private final Connection c = mock(Connection.class);
    private final CustodiaPostgresCapacidade.Experimento experimento = mock(CustodiaPostgresCapacidade.Experimento.class);

    private void preparar() throws Exception {
        var p = Plano.novo(pin, 55437);
        when(execucao.plano()).thenReturn(p);
        var m = new CustodiaPostgresManifesto(p, "b".repeat(64), p.volume(), "c".repeat(64));
        when(execucao.manifesto()).thenReturn(m);
        when(execucao.inspecionar(m)).thenReturn(new Estado(
                new Recurso(Tipo.CONTAINER, m.containerId(), p.container(), p.inicio(), false, p.rotulos()),
                new Recurso(Tipo.VOLUME, m.volumeId(), p.volume(), p.inicio(), false, p.rotulos()),
                new Recurso(Tipo.REDE, m.redeId(), p.rede(), p.inicio(), false, p.rotulos()),
                pin, "127.0.0.1", p.porta(), p.banco(), p.credenciais(), true, m.volumeId(),
                "/var/lib/postgresql/data", java.util.Set.of(m.redeId()), false, false, false));
        when(execucao.datasource()).thenReturn(ds); when(ds.getConnection()).thenReturn(c);
        when(experimento.executar(c)).thenReturn(new CustodiaPostgresCapacidade.Resultado(
                170011, 8192, 8192, 2467, 2449, java.util.Set.of(CustodiaPostgresCapacidade.Prova.values())));
    }

    @Test void resultadoAusenteNaoPodeAprovar() throws Exception {
        preparar(); when(experimento.executar(c)).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresCapacidade.executar(execucao, pin, experimento));
        verify(execucao).limpar();
    }

    @Test void falhaEvidenciaEtapaESeparadamenteLimpezaPendente() throws Exception {
        preparar(); doThrow(new IllegalStateException("segredo" )).when(execucao).limpar();
        var erro = assertThrows(IllegalStateException.class,
                () -> CustodiaPostgresCapacidade.executar(execucao, pin, experimento));
        assertTrue(erro.getMessage().contains("experimentoCompleto=true"));
        assertTrue(erro.getMessage().contains("limpezaCompleta=false"));
        assertFalse(erro.getMessage().contains("segredo"));
    }

    @Test void relatorioDeFalhaMantemSqlstateEReciboSemTextoSensivel() throws Exception {
        preparar();
        when(experimento.executar(c)).thenThrow(new java.sql.SQLException("senha=segredo","54000"));
        doThrow(new IllegalStateException("senha=segredo")).when(execucao).limpar();
        var p = execucao.plano();
        when(execucao.recibos()).thenReturn(Map.of(Tipo.CONTAINER,
                new Recurso(Tipo.CONTAINER,"b".repeat(64),p.container(),p.inicio(),false,p.rotulos())));
        var erro = assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        assertTrue(erro.toString().contains("54000")); assertTrue(erro.toString().contains("b".repeat(64)));
        assertFalse(erro.toString().contains("segredo")); assertNull(erro.getCause());
    }

    @Test void naoAprovaEvidenciasSemTodasAsProvas() throws Exception {
        preparar(); when(experimento.executar(c)).thenReturn(new CustodiaPostgresCapacidade.Resultado(
                170011,8192,8192,2467,2449,java.util.Set.of()));
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        verify(execucao).limpar();
    }

    @Test void contagemParcialNaoPodeSerApresentadaComoRoteiroCompleto() throws Exception {
        preparar(); when(experimento.executar(c)).thenReturn(new CustodiaPostgresCapacidade.Resultado(
                170011,8192,8192,2459,2449,Set.of(CustodiaPostgresCapacidade.Prova.values())));
        assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
    }

    @Test void manifestoAusenteInterrompeAntesDeQualquerSql() throws Exception {
        preparar(); when(execucao.manifesto()).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        verifyNoInteractions(ds,c,experimento); verify(execucao).limpar();
    }

    @Test void identidadeDivergenteInterrompeAntesDeQualquerSql() throws Exception {
        preparar(); when(execucao.inspecionar(any())).thenReturn(null);
        assertThrows(IllegalStateException.class, () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        verifyNoInteractions(ds,c,experimento); verify(execucao).limpar();
    }

    @Test void provisionaValidaExperimentaFechaELimpaNestaOrdem() throws Exception {
        preparar();
        CustodiaPostgresCapacidade.executar(execucao, pin, experimento);
        var ordem = inOrder(execucao, ds, c, experimento);
        ordem.verify(execucao).provisionar(); ordem.verify(execucao).manifesto();
        ordem.verify(execucao).inspecionar(any()); ordem.verify(execucao).datasource();
        ordem.verify(ds).getConnection(); ordem.verify(experimento).executar(c);
        ordem.verify(c).close(); ordem.verify(execucao).limpar();
    }

    @Test void falhaNaoExpoeMensagemJdbcNemCredenciaisEMantemLimpeza() throws Exception {
        preparar();
        doThrow(new java.sql.SQLException("password=segredo marcador=secreto"))
                .when(experimento).executar(c);
        var erro = assertThrows(Exception.class,
                () -> CustodiaPostgresCapacidade.executar(execucao, pin, experimento));
        assertFalse(erro.toString().contains("segredo"));
        assertFalse(erro.toString().contains("secreto"));
        assertNull(erro.getCause()); assertEquals(0, erro.getSuppressed().length);
        verify(c).close(); verify(execucao).limpar();
    }

    @Test void falhaDeProvisionamentoNaoAbreSqlEAindaSolicitaLimpezaCondicionada() throws Exception {
        preparar(); doThrow(new IllegalStateException("credencial-sintetica")).when(execucao).provisionar();
        var erro = assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        assertEquals(CustodiaPostgresCapacidade.Etapa.PROVISIONAMENTO,erro.relatorio().etapa());
        assertFalse(erro.relatorio().experimentoCompleto());
        verifyNoInteractions(ds,c,experimento); verify(execucao).limpar();
    }

    @Test void fechamentoJdbcFalhoImpedeAprovacaoMesmoComResultadosCompletos() throws Exception {
        preparar(); doThrow(new java.sql.SQLException("senha-sintetica")).when(c).close();
        var erro = assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(execucao,pin,experimento));
        assertFalse(erro.relatorio().experimentoCompleto()); assertNull(erro.relatorio().resultado());
        assertFalse(erro.toString().contains("senha-sintetica")); verify(execucao).limpar();
    }

    @Test void executorIntegraOrquestradorEGuardasReaisComTransportesSimulados() throws Exception {
        var s = new Simulacao();
        var r = CustodiaPostgresCapacidade.executar(s.e,pin,conexao -> {
            assertSame(s.conexao,conexao); return completo();
        });
        assertTrue(r.experimentoCompleto()); assertTrue(r.limpezaCompleta()); assertTrue(s.recursos.isEmpty());
        var ordem = inOrder(s.docker);
        ordem.verify(s.docker).remover(argThat(recurso -> recurso.tipo()==Tipo.CONTAINER));
        ordem.verify(s.docker).remover(argThat(recurso -> recurso.tipo()==Tipo.VOLUME));
        ordem.verify(s.docker).remover(argThat(recurso -> recurso.tipo()==Tipo.REDE));
        String texto = r.toString();
        assertFalse(texto.contains(s.e.plano().credenciais().senha()));
        assertFalse(texto.contains(s.e.plano().credenciais().marcador()));
    }

    @Test void recursoPreexistenteETransportesReaisSimuladosNaoSaoRemovidos() throws Exception {
        var s = new Simulacao(); when(s.docker.existe(eq(Tipo.VOLUME),anyString())).thenReturn(true);
        assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(s.e,pin,conexao -> { fail("SQL proibido"); return completo(); }));
        verify(s.docker,never()).remover(any()); assertTrue(s.recursos.isEmpty());
    }

    @Test void perdaDePropriedadeDuranteExperimentoPreservaTodosOsRecursos() throws Exception {
        var s = new Simulacao();
        var erro = assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(s.e,pin,conexao -> {
                    var volume = s.recursos.get(Tipo.VOLUME);
                    s.recursos.put(Tipo.VOLUME,new Recurso(Tipo.VOLUME,volume.id(),volume.nome(),volume.criadoEm(),false,Map.of()));
                    return completo();
                }));
        assertFalse(erro.relatorio().limpezaCompleta()); assertEquals(3,s.recursos.size());
        verify(s.docker,never()).remover(any());
    }

    @Test void identidadeSqlErradaInterrompeAntesDoExperimento() throws Exception {
        var s = new Simulacao(); s.marcadorInvalido = true;
        var erro = assertThrows(CustodiaPostgresCapacidade.Interrompida.class,
                () -> CustodiaPostgresCapacidade.executar(s.e,pin,conexao -> { fail("SQL experimental proibido"); return completo(); }));
        assertFalse(erro.relatorio().experimentoCompleto()); assertTrue(s.recursos.isEmpty());
    }

    private static CustodiaPostgresCapacidade.Resultado completo() {
        return new CustodiaPostgresCapacidade.Resultado(170011,8192,65536,2467,2449,
                Set.of(CustodiaPostgresCapacidade.Prova.values()));
    }

    /** Orquestrador/datasource/identidade/limpeza reais; somente Docker, bootstrap e JDBC simulados. */
    private final class Simulacao {
        final Docker docker = mock(Docker.class);
        final Map<Tipo,Recurso> recursos = new EnumMap<>(Tipo.class);
        final Connection conexao = mock(Connection.class);
        final Execucao e;
        boolean marcadorInvalido;
        Simulacao() throws Exception {
            var env = new org.springframework.mock.env.MockEnvironment(); env.setActiveProfiles(CustodiaPostgresSeguranca.PERFIL);
            e = CustodiaPostgresOperacional.preparar(new PinPlataforma(pin,"amd64","linux"),55437,env,docker,
                    porta -> true,mock(Bootstrap.class),(url,usuario,senha) -> conexao);
            var p = e.plano();
            when(docker.imagem(pin)).thenReturn(new ImagemLocal(pin,"sha256:"+"d".repeat(64),"amd64","linux","17.11"));
            when(docker.criarVolume(p)).thenAnswer(i -> criar(Tipo.VOLUME,p.volume(),p.volume()));
            when(docker.criarRede(p)).thenAnswer(i -> criar(Tipo.REDE,"c".repeat(64),p.rede()));
            when(docker.criarContainer(eq(p),any(),anyString(),anyString(),any()))
                    .thenAnswer(i -> criar(Tipo.CONTAINER,"b".repeat(64),p.container()));
            when(docker.inspecionarRecurso(any(),anyString())).thenAnswer(i -> recursos.get((Tipo)i.getArgument(0)));
            when(docker.inspecionar(any())).thenAnswer(i -> new Estado(recursos.get(Tipo.CONTAINER),recursos.get(Tipo.VOLUME),
                    recursos.get(Tipo.REDE),pin,"127.0.0.1",p.porta(),p.banco(),p.credenciais(),true,p.volume(),
                    "/var/lib/postgresql/data",Set.of("c".repeat(64)),false,false,false));
            doAnswer(i -> { recursos.remove(((Recurso)i.getArgument(0)).tipo()); return null; }).when(docker).remover(any());
            when(conexao.prepareStatement(anyString())).thenAnswer(i -> {
                var statement = mock(java.sql.PreparedStatement.class); var rs = mock(java.sql.ResultSet.class);
                when(statement.executeQuery()).thenReturn(rs); when(rs.next()).thenReturn(true,false);
                when(rs.getString(1)).thenReturn(p.banco()); when(rs.getString(2)).thenReturn(p.credenciais().usuario());
                when(rs.getInt(3)).thenReturn(170011);
                when(rs.getString(4)).thenAnswer(a -> marcadorInvalido ? "outro" : p.credenciais().marcador());
                return statement;
            });
        }
        private String criar(Tipo tipo,String id,String nome) {
            recursos.put(tipo,new Recurso(tipo,id,nome,e.plano().inicio(),false,e.plano().rotulos())); return id;
        }
    }
}
