package com.projeto.custodia.postgresql;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresManifesto.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresOperacional.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19.*;

/** Somente fronteira operacional simulada; os adaptadores existentes têm regressões próprias. */
class CustodiaPostgresHomologacao19Test {
    final Imagem pin = new Imagem(IMAGEM,"sha256:"+"a".repeat(64));
    final Execucao e = mock(Execucao.class);
    final CustodiaPostgresDataSource ds = mock(CustodiaPostgresDataSource.class);
    final Roteiro roteiro = mock(Roteiro.class);
    void preparar() throws Exception {
        var p = Plano.novo(pin,55437);
        var m = new CustodiaPostgresManifesto(p,"b".repeat(64),p.volume(),"c".repeat(64));
        when(e.plano()).thenReturn(p); when(e.manifesto()).thenReturn(m); when(e.datasource()).thenReturn(ds);
        when(e.inspecionar(m)).thenReturn(new Estado(
                new Recurso(Tipo.CONTAINER,m.containerId(),p.container(),p.inicio(),false,p.rotulos()),
                new Recurso(Tipo.VOLUME,m.volumeId(),p.volume(),p.inicio(),false,p.rotulos()),
                new Recurso(Tipo.REDE,m.redeId(),p.rede(),p.inicio(),false,p.rotulos()),
                pin,"127.0.0.1",p.porta(),p.banco(),p.credenciais(),true,p.volume(),
                "/var/lib/postgresql/data",Set.of(m.redeId()),false,false,false));
        when(roteiro.executar(any(),eq(ds))).thenReturn(new Resultado(Set.of(Prova.values()),9,65536));
    }
    @Test void somenteEntregaDatasourceAposProvisionarEValidarELimpaAoFinal() throws Exception {
        preparar(); var r = executar(e,pin,Cenario.INSTALACAO,roteiro);
        assertTrue(r.completo()); assertTrue(r.limpo());
        var ordem = inOrder(e,roteiro);
        ordem.verify(e).provisionar(); ordem.verify(e).manifesto(); ordem.verify(e).inspecionar(any());
        ordem.verify(e).datasource(); ordem.verify(roteiro).executar(Cenario.INSTALACAO,ds); ordem.verify(e).limpar();
    }
    @ParameterizedTest @ValueSource(strings={"ausente","provas","changesets","indice","legado"})
    void resultadoParcialNuncaAprovaUpgrade(String defeito) throws Exception {
        preparar();
        var provas = EnumSet.allOf(Prova.class);
        if (defeito.equals("provas")) provas.remove(Prova.HIBERNATE);
        if (defeito.equals("legado")) provas.remove(Prova.LEGADO);
        when(roteiro.executar(any(),any())).thenReturn(defeito.equals("ausente") ? null : new Resultado(provas,
                defeito.equals("changesets") ? 7 : 9,defeito.equals("indice") ? 0 : 65536));
        var erro = assertThrows(Interrompida.class,() -> executar(e,pin,Cenario.UPGRADE,roteiro));
        assertFalse(erro.relatorio.completo()); verify(e).limpar();
    }
    @ParameterizedTest @ValueSource(strings={"manifesto","identidade","provisionamento"})
    void falhaAntesDoRoteiroNaoEntregaDatasource(String defeito)throws Exception{
        preparar();
        if(defeito.equals("manifesto"))when(e.manifesto()).thenReturn(null);
        if(defeito.equals("identidade"))when(e.inspecionar(any())).thenReturn(null);
        if(defeito.equals("provisionamento"))doThrow(new IllegalStateException("senha-sintetica")).when(e).provisionar();
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.INSTALACAO,roteiro));
        verifyNoInteractions(roteiro,ds);verify(e).limpar();assertFalse(erro.toString().contains("senha-sintetica"));
    }
    @Test void falhaDeAssertionDoCenarioTambemLimpaESanitiza()throws Exception{
        preparar();when(roteiro.executar(any(),any())).thenThrow(new AssertionError("password=segredo"));
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.UPGRADE,roteiro));
        assertFalse(erro.relatorio.completo());assertTrue(erro.relatorio.limpo());verify(e).limpar();
        assertNull(erro.getCause());assertEquals(0,erro.getSuppressed().length);assertFalse(erro.toString().contains("segredo"));
    }
    @Test void falhaDeLimpezaNaoAprovaENaoPerdeRecibos()throws Exception{
        preparar();var p=e.plano();when(e.recibos()).thenReturn(Map.of(Tipo.VOLUME,new Recurso(Tipo.VOLUME,p.volume(),p.volume(),p.inicio(),false,p.rotulos())));
        doThrow(new java.sql.SQLException("segredo")).when(e).limpar();
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.UPGRADE,roteiro));
        assertTrue(erro.relatorio.completo());assertFalse(erro.relatorio.limpo());assertEquals(p.volume(),erro.relatorio.pendentes().get(Tipo.VOLUME));
        assertFalse(erro.toString().contains(p.credenciais().senha()));assertFalse(erro.toString().contains(p.credenciais().marcador()));
    }
    @Test void marcadorDeAtivacaoRealEhExclusivoDaTarefa19(){
        var condicao=CustodiaPostgresHomologacao19RealTest.class.getAnnotation(org.junit.jupiter.api.condition.EnabledIfSystemProperty.class);
        assertEquals("homologacao.custodia.real",condicao.named());assertEquals("EXECUTAR_1_9",condicao.matches());
    }
    @Test void diagnosticoDaRejeicaoPreservaCasoEstadoEConstraintSemEncadearSegredos()throws Exception{
        preparar();
        when(roteiro.executar(any(),any())).thenAnswer(invocacao->{
            CustodiaPostgresIntegridade19Test.rejeicao(CustodiaPostgresIntegridade19Test.NATUREZA,
                    CustodiaPostgresIntegridade19Test.sql("23505",CustodiaPostgresIntegridade19Test.DIRECAO));
            return null;
        });
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.INSTALACAO,roteiro));
        String texto=erro.toString();
        assertTrue(texto.contains("caso=NATUREZA_INVALIDA"));
        assertTrue(texto.contains("categoria=SQLSTATE_DIVERGENTE"));
        assertTrue(texto.contains("sqlState=23505"));
        assertTrue(texto.contains("constraint="+CustodiaPostgresIntegridade19Test.DIRECAO));
        assertTrue(texto.contains("cenario=INSTALACAO"));
        assertFalse(texto.contains("segredo"));assertFalse(texto.contains("jdbc:postgresql"));
        assertNull(erro.getCause());assertEquals(0,erro.getSuppressed().length);
        assertTrue(erro.relatorio.limpo());verify(e).limpar();
    }
    @Test void diagnosticoNaoPublicaConstraintDesconhecidaNemSqlStateMalformado()throws Exception{
        preparar();
        when(roteiro.executar(any(),any())).thenThrow(new IllegalStateException("password=segredo",
                CustodiaPostgresIntegridade19Test.sql("segredo","constraint_segredo")));
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.UPGRADE,roteiro));
        assertTrue(erro.toString().contains("categoria=SQL"));
        assertTrue(erro.toString().contains("constraint=NA"));assertEquals("NA",erro.relatorio.sqlState());
        assertFalse(erro.toString().contains("segredo"));assertNull(erro.getCause());verify(e).limpar();
    }
    @Test void falhaComLimpezaInconclusivaPreservaDiagnosticoOriginalERecibo()throws Exception{
        preparar();var p=e.plano();
        when(roteiro.executar(any(),any())).thenAnswer(invocacao->{
            CustodiaPostgresIntegridade19Test.rejeicao(CustodiaPostgresIntegridade19Test.NATUREZA,null);return null;
        });
        when(e.recibos()).thenReturn(Map.of(Tipo.VOLUME,new Recurso(Tipo.VOLUME,p.volume(),p.volume(),p.inicio(),false,p.rotulos())));
        doThrow(new IllegalStateException("password=segredo")).when(e).limpar();
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.INSTALACAO,roteiro));
        assertFalse(erro.relatorio.completo());assertFalse(erro.relatorio.limpo());
        assertEquals(p.volume(),erro.relatorio.pendentes().get(Tipo.VOLUME));
        assertEquals(CasoFalha.NATUREZA_INVALIDA,erro.relatorio.diagnostico().caso());
        assertEquals(Categoria.SQL_AUSENTE,erro.relatorio.diagnostico().categoria());
        assertNull(erro.getCause());assertEquals(0,erro.getSuppressed().length);assertFalse(erro.toString().contains("segredo"));verify(e).limpar();
    }
    @Test void falhaSomenteNaLimpezaTemCategoriaSemExporCausa()throws Exception{
        preparar();doThrow(new java.sql.SQLException("password=segredo","08006")).when(e).limpar();
        var erro=assertThrows(Interrompida.class,()->executar(e,pin,Cenario.UPGRADE,roteiro));
        assertEquals(CasoFalha.LIMPEZA,erro.relatorio.diagnostico().caso());
        assertEquals(Categoria.SQL,erro.relatorio.diagnostico().categoria());assertEquals("08006",erro.relatorio.sqlState());
        assertTrue(erro.relatorio.completo());assertFalse(erro.relatorio.limpo());assertFalse(erro.toString().contains("segredo"));
    }
    @Test void categoriaDeAssertionPermaneceControladaSemMensagem() {
        var d=diagnosticar(CasoFalha.INTEGRIDADE_INDICE,new AssertionError("password=segredo"));
        assertEquals(Categoria.ASSERTIVA,d.categoria());assertEquals("NA",d.sqlState());assertFalse(d.toString().contains("segredo"));
    }
    @Test void causasCiclicasNaoImpedemDiagnosticoSeguro() {
        var a=new IllegalStateException("segredo");var b=new IllegalStateException("segredo",a);a.initCause(b);
        var d=diagnosticar(CasoFalha.ROTEIRO,a);
        assertEquals(Categoria.EXECUCAO,d.categoria());assertFalse(d.toString().contains("segredo"));
    }
}
