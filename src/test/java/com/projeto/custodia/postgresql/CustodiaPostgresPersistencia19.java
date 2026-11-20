package com.projeto.custodia.postgresql;

import java.math.*;
import java.time.*;
import java.util.*;
import jakarta.persistence.*;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import com.projeto.entities.*;
import com.projeto.repositories.*;
import com.projeto.services.*;
import static com.projeto.services.EventoQuantitativo.Grupo;
import static org.junit.jupiter.api.Assertions.*;

/** Hibernate/repositórios reais em transações descartadas, sem serviços ou contexto Boot. */
final class CustodiaPostgresPersistencia19 {
    static final OffsetDateTime REGISTRO=OffsetDateTime.parse("2026-10-14T12:34:56.123456-03:00");
    static final LocalDate DIA=LocalDate.of(2001,2,3);
    static void executar(CustodiaPostgresDataSource ds)throws Exception{
        var factory=new CustodiaPostgresConfiguracao.SomenteDatasource().prepararHibernate(ds);
        try {
            factory.afterPropertiesSet();var emf=Objects.requireNonNull(factory.getObject());
            assertTrue(emf.isOpen());assertEquals("validate",emf.getProperties().get("hibernate.hbm2ddl.auto"));
            for(var mercado:Mercado.values()) {
                cenario(emf,mercado);
                for(int nanos:List.of(123456000,123456789)) precisao(emf,mercado,nanos);
                limiteQuantidade(emf,mercado);
                System.out.println("CUSTODIA_1_9 persistencia="+mercado+" componentes=512 precisaoTemporal=6 consultasPonte=3 sqlAdaptacaoReplay=0");
            }
            for(var fracao:List.of("2/4","1/1","3/2")) hidratacaoInvalida(emf,fracao);
        } finally { factory.destroy(); }
    }
    @FunctionalInterface interface CenarioJpa { void executar(EntityManager em); }
    static void transacao(EntityManagerFactory emf,CenarioJpa cenario){
        try(var em=emf.createEntityManager()){
            em.getTransaction().begin();try{
                em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ").executeUpdate();
                cenario.executar(em);
            }finally{if(em.getTransaction().isActive())em.getTransaction().rollback();}
        }
    }
    private static void cenario(EntityManagerFactory emf,Mercado mercado){transacao(emf,em->{
        var f=fixture(em,mercado);var unidade=mercado==Mercado.BRASIL?BigDecimal.ONE:new BigDecimal("0.000001");
        var operacao1=new Operacao(f.c(),f.a(),f.origem(),TipoOperacao.COMPRA,unidade.multiply(new BigDecimal("10")),BigDecimal.TEN,DIA,1,unidade.multiply(new BigDecimal("100")));
        var operacao2=new Operacao(f.c(),f.a(),null,TipoOperacao.COMPRA,unidade.multiply(new BigDecimal("5")),BigDecimal.TEN,DIA,2,unidade.multiply(new BigDecimal("50")));
        em.persist(operacao1);em.persist(operacao2);
        BigInteger n=BigInteger.TEN.pow(511);var posicao=PosicaoRacionalCustodia.criar(n,n.add(BigInteger.ONE));
        var transferencia=mov(f,NaturezaMovimentacaoCustodia.TRANSFERENCIA,unidade.multiply(new BigDecimal("3")),1,posicao,REGISTRO);
        var atribuicao=mov(f,NaturezaMovimentacaoCustodia.ATRIBUICAO,unidade.multiply(new BigDecimal("2")),2,PosicaoRacionalCustodia.deTexto("1","2"),REGISTRO);
        var neutralizada=mov(f,NaturezaMovimentacaoCustodia.TRANSFERENCIA,unidade,1,PosicaoRacionalCustodia.deTexto("1","3"),REGISTRO);
        em.persist(transferencia);em.persist(atribuicao);em.persist(neutralizada);
        var estorno=new EstornoMovimentacaoCustodia(null,neutralizada,"  Motivo sintético  ",relogio(REGISTRO.plusDays(1)));
        em.persist(estorno);em.flush();em.clear();
        var repos=new JpaRepositoryFactory(em);
        var movimentos=repos.getRepository(MovimentacaoCustodiaRepository.class);
        var estornos=repos.getRepository(EstornoMovimentacaoCustodiaRepository.class);
        var operacoes=repos.getRepository(OperacaoRepository.class);
        var lida=movimentos.buscarPorCarteiraEId(f.c().getId(),transferencia.getId()).orElseThrow();
        assertEquals(posicao,lida.getPosicaoRacional());assertEquals(512,lida.getPosicaoRacional().getNumeradorTexto().length());
        assertEquals(unidade.multiply(new BigDecimal("3")).setScale(6),lida.getQuantidade());
        assertEquals(DIA,lida.getDataEfetiva());assertEquals(REGISTRO.toInstant(),lida.getDataRegistro().toInstant());
        assertEquals(mercado,lida.getMercado());lida.validarEstruturaHistorica();
        var atr=movimentos.buscarPorCarteiraEId(f.c().getId(),atribuicao.getId()).orElseThrow();assertNull(atr.getCorretoraOrigemId());
        assertEquals(SituacaoMovimentacaoCustodia.ATIVA,lida.getSituacao(VinculoEstornoConsultado.ausenciaVerificada(lida.getId())));
        assertTrue(movimentos.buscarPorCarteiraEId(f.outraCarteira().getId(),lida.getId()).isEmpty());
        assertEquals(3,movimentos.listarPorCarteira(f.c().getId()).size());
        assertTrue(movimentos.listarPorCarteiraEAcao(f.c().getId(),f.outraAcao().getId()).isEmpty());
        assertTrue(estornos.listarPorCarteira(f.outraCarteira().getId()).isEmpty());
        assertEquals(1,estornos.listarPorCarteiraEAcao(f.c().getId(),f.a().getId()).size());
        var stats=emf.unwrap(SessionFactory.class).getStatistics();stats.setStatisticsEnabled(true);em.clear();stats.clear();
        var fs=operacoes.listarParaReplayQuantitativo(f.c().getId(),f.a().getId());em.clear();
        var cs=movimentos.listarPorCarteiraEAcao(f.c().getId(),f.a().getId());em.clear();
        var es=estornos.listarPorCarteiraEOriginais(f.c().getId(),cs.stream().map(MovimentacaoCustodia::getId).toList());em.clear();
        assertEquals(3L,stats.getPrepareStatementCount(),"Três consultas para carregar fontes completas");
        assertEquals(2,fs.size());assertEquals(3,cs.size());assertEquals(1,es.size());
        for(var m:cs){assertTrue(Hibernate.isInitialized(m.getCarteira()));assertTrue(Hibernate.isInitialized(m.getAcao()));
            assertTrue(Hibernate.isInitialized(m.getCorretoraDestino()));m.validarEstruturaHistorica();}
        var e=es.get(0);e.validarEstruturaHistorica();
        assertEquals(estorno.getId(),e.getId());assertEquals("  Motivo sintético  ",e.getMotivo());
        assertEquals(REGISTRO.plusDays(1).toInstant(),e.getDataRegistro().toInstant());assertEquals(DIA,e.getDataEfetiva());
        assertEquals(SituacaoMovimentacaoCustodia.ESTORNADA,e.getMovimentacaoOriginal().getSituacao(VinculoEstornoConsultado.vinculoPresente(neutralizada.getId(),e)));
        var vinculos=cs.stream().map(m->m.getId().equals(e.getMovimentacaoOriginalId())
                ?VinculoEstornoConsultado.vinculoPresente(m.getId(),e):VinculoEstornoConsultado.ausenciaVerificada(m.getId())).toList();
        var adaptador=new AdaptadorEventosQuantitativos();var normalizador=new NormalizadorEventosQuantitativos();
        var fontes=adaptador.adaptar(f.c().getId(),f.a().getId(),fs,cs,vinculos);
        var eventos=normalizador.normalizar(f.c().getId(),f.a().getId(),fontes.financeiros(),fontes.custodias());
        var replay=new MotorReplayQuantitativo().reproduzir(eventos);var estado=replay.estadoFinal();
        decimal(unidade.multiply(new BigDecimal("15")),estado.quantidadeConsolidada());
        decimal(unidade.multiply(new BigDecimal("7")),estado.saldo(Grupo.corretora(f.origem().getId())));
        decimal(unidade.multiply(new BigDecimal("5")),estado.saldo(Grupo.corretora(f.destino().getId())));
        decimal(unidade.multiply(new BigDecimal("3")),estado.saldo(Grupo.semCorretora()));assertTrue(estado.reconciliada());
        assertEquals(eventos.size(),replay.checkpoints().size());
        var invertidas=new ArrayList<>(cs);Collections.reverse(invertidas);var financeiros=new ArrayList<>(fs);Collections.reverse(financeiros);
        var reversas=adaptador.adaptar(f.c().getId(),f.a().getId(),financeiros,invertidas,vinculos);
        assertEquals(eventos,normalizador.normalizar(f.c().getId(),f.a().getId(),reversas.financeiros(),reversas.custodias()));
        assertEquals(3L,stats.getPrepareStatementCount(),"Associações e replay sem SQL adicional");
        assertTrue(movimentos.listarPorCarteira(f.outraCarteira().getId()).isEmpty());
        assertTrue(estornos.listarPorCarteiraEAcao(f.c().getId(),f.outraAcao().getId()).isEmpty());
        assertTrue(estornos.listarPorCarteiraEOriginais(f.c().getId(),List.of(transferencia.getId())).isEmpty());
        assertTrue(estornos.listarPorCarteiraEOriginais(f.outraCarteira().getId(),cs.stream().map(MovimentacaoCustodia::getId).toList()).isEmpty());
        assertTrue(operacoes.listarParaReplayQuantitativo(f.outraCarteira().getId(),f.a().getId()).isEmpty());
        assertTrue(operacoes.listarParaReplayQuantitativo(f.c().getId(),f.outraAcao().getId()).isEmpty());
        em.flush();em.clear();assertEquals(posicao,movimentos.buscarPorCarteiraEId(f.c().getId(),transferencia.getId()).orElseThrow().getPosicaoRacional());
    });}
    private static void precisao(EntityManagerFactory emf,Mercado mercado,int nanos){transacao(emf,em->{
        var f=fixture(em,mercado);var entrada=REGISTRO.withNano(nanos).withOffsetSameInstant(ZoneOffset.ofHoursMinutes(5,30));
        var m=mov(f,NaturezaMovimentacaoCustodia.ATRIBUICAO,BigDecimal.ONE,0,PosicaoRacionalCustodia.deTexto("1","2"),entrada);em.persist(m);
        var e=new EstornoMovimentacaoCustodia(null,m,null,relogio(entrada.plusDays(1)));em.persist(e);em.flush();em.clear();
        int esperado=nanos==123456789?123457000:123456000;
        assertEquals(entrada.withNano(esperado).toInstant(),em.find(MovimentacaoCustodia.class,m.getId()).getDataRegistro().toInstant());
        var est=em.find(EstornoMovimentacaoCustodia.class,e.getId());assertNull(est.getMotivo());
        assertEquals(entrada.plusDays(1).withNano(esperado).toInstant(),est.getDataRegistro().toInstant());
    });}
    private static void limiteQuantidade(EntityManagerFactory emf,Mercado mercado){transacao(emf,em->{
        var f=fixture(em,mercado);var quantidade=new BigDecimal(mercado==Mercado.BRASIL?"9999999999999.000000":"9999999999999.999999");
        var m=mov(f,NaturezaMovimentacaoCustodia.ATRIBUICAO,quantidade,0,PosicaoRacionalCustodia.deTexto("1","2"),REGISTRO);
        em.persist(m);em.flush();em.clear();assertEquals(quantidade,em.find(MovimentacaoCustodia.class,m.getId()).getQuantidade());
    });}
    private static void hidratacaoInvalida(EntityManagerFactory emf,String fracao){transacao(emf,em->{
        var f=fixture(em,Mercado.BRASIL);var m=mov(f,NaturezaMovimentacaoCustodia.TRANSFERENCIA,BigDecimal.ONE,0,PosicaoRacionalCustodia.deTexto("1","2"),REGISTRO);
        em.persist(m);em.flush();var par=fracao.split("/");em.createNativeQuery("UPDATE movimentacao_custodia SET posicao_numerador=:n,posicao_denominador=:d WHERE id=:id")
                .setParameter("n",par[0]).setParameter("d",par[1]).setParameter("id",m.getId()).executeUpdate();em.clear();
        assertThrows(RuntimeException.class,()->em.find(MovimentacaoCustodia.class,m.getId()));
    });}
    private static MovimentacaoCustodia mov(Fixture f,NaturezaMovimentacaoCustodia natureza,BigDecimal q,int ancora,PosicaoRacionalCustodia p,OffsetDateTime t){
        return new MovimentacaoCustodia(null,f.c(),f.a(),natureza,natureza==NaturezaMovimentacaoCustodia.TRANSFERENCIA?f.origem():null,f.destino(),q,DIA,ancora,p,relogio(t));}
    private static Clock relogio(OffsetDateTime t){return Clock.fixed(t.toInstant(),t.getOffset());}
    private static void decimal(BigDecimal esperado,BigDecimal atual){assertEquals(0,esperado.compareTo(atual));}
    private static Fixture fixture(EntityManager em,Mercado mercado){
        var c=new Carteira("Roundtrip",REGISTRO);var outra=new Carteira("Outro contexto",REGISTRO);
        var a=new Acao("PGTEST1","Sintética",mercado,mercado==Mercado.BRASIL?Moeda.BRL:Moeda.USD,BigDecimal.TEN,REGISTRO);
        var outraAcao=new Acao("PGTEST2","Sintética",mercado,mercado==Mercado.BRASIL?Moeda.BRL:Moeda.USD,BigDecimal.TEN,REGISTRO);
        var origem=corretora("55555555000155");var destino=corretora("66666666000166");
        for(var o:List.of(c,outra,a,outraAcao,origem,destino))em.persist(o);em.flush();return new Fixture(c,a,origem,destino,outra,outraAcao);
    }
    private static Corretora corretora(String cnpj){return new Corretora(cnpj,"Sintética",null,null,null,"00000000","Rua","1",null,"Bairro","Cidade","SP","ATIVA",REGISTRO);}
    private record Fixture(Carteira c,Acao a,Corretora origem,Corretora destino,Carteira outraCarteira,Acao outraAcao){ }
    private CustodiaPostgresPersistencia19(){ }
}
