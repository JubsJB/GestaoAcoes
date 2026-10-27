package com.projeto.custodia.postgresql;

import java.util.*;
import java.sql.*;
import liquibase.Liquibase;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import static org.junit.jupiter.api.Assertions.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19.*;

/** Cenários programáticos de Liquibase; nenhum acesso ao banco ao carregar a classe. */
final class CustodiaPostgresMigracoes19 {
    static final String MASTER = "db/changelog/db.changelog-master.yaml";
    static final String ARQUIVO_008 = "db/changelog/changes/008-create-movimentacoes-custodia.yaml";
    static final List<String> IDS = List.of("001-create-corretora","002-create-acao","003-create-carteira",
            "004-create-operacao","005-create-historico-cotacao","006-create-snapshot-carteira",
            "007-add-referencia-temporal-cotacao","008-create-movimentacoes-custodia","008-add-integridade-movimentacoes-custodia");
    record Registro(String id,String autor,String arquivo,String checksum,String tipo,int ordem) { }
    static void validarPlano(liquibase.Liquibase lb) throws Exception {
        var changes = lb.getDatabaseChangeLog().getChangeSets();
        CustodiaPostgresManifesto.exigir(IDS.equals(changes.stream().map(liquibase.changelog.ChangeSet::getId).toList()),
                "Master diferente de 001–008");
        for (var cs : changes.subList(7,9)) {
            CustodiaPostgresManifesto.exigir(ARQUIVO_008.equals(cs.getFilePath()) && cs.hasCustomRollbackChanges()
                    && cs.getRollback().getChanges().size()==1
                    && cs.getRollback().getChanges().get(0) instanceof liquibase.change.core.StopChange stop
                    && stop.getMessage().contains("bloqueado"),"Rollback 008 sem proteção StopChange");
        }
    }
    static void validarHistorico(List<Registro> registros,int quantidade) {
        CustodiaPostgresManifesto.exigir((quantidade==7 || quantidade==9) && registros!=null
                && registros.size()==quantidade,"Histórico incompleto");
        for (int i=0;i<quantidade;i++) {
            var r = registros.get(i);
            String arquivo = i==8 ? ARQUIVO_008 : "db/changelog/changes/"+IDS.get(i)+".yaml";
            CustodiaPostgresManifesto.exigir(r!=null && IDS.get(i).equals(r.id()) && "codex".equals(r.autor())
                    && arquivo.equals(r.arquivo()) && r.checksum()!=null && !r.checksum().isBlank()
                    && "EXECUTED".equals(r.tipo()) && r.ordem()==i+1,"Histórico divergente");
        }
    }
    static final List<String> LEGADO=List.of("corretora","acao","carteira","operacao","historico_cotacao","snapshot_carteira","snapshot_carteira_moeda");
    static final List<String> TODAS=java.util.stream.Stream.concat(LEGADO.stream(),java.util.stream.Stream.of("movimentacao_custodia","estorno_movimentacao_custodia")).toList();
    record Retrato(Map<String,List<List<String>>> dados,Map<String,List<List<String>>> estrutura) { }
    static String preservar(Retrato antes,Retrato depois)throws Exception {
        CustodiaPostgresManifesto.exigir(antes!=null && antes.equals(depois),"Dados ou estruturas alterados");
        return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(antes.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
    static Resultado executar(Cenario cenario,CustodiaPostgresDataSource ds)throws Exception{
        CasoFalha etapa=CasoFalha.MIGRATIONS;
        try {
        var provas=EnumSet.noneOf(Prova.class);
        Retrato financeiro;Retrato completa;
        try(var c=ds.getConnection();var recursos=new ClassLoaderResourceAccessor()){
            assertEquals(List.of(List.of("0")),consulta(c,"SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'"));
            var db=DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));
            assertEquals("postgresql",db.getShortName());db.setDefaultSchemaName("public");
            try(var lb=new Liquibase(MASTER,recursos,db)){
                validarPlano(lb);
                System.out.println("CUSTODIA_1_9 etapa=MIGRATIONS cenario="+cenario);
                List<List<String>> historiaAnterior=null;
                financeiro=null;
                if(cenario==Cenario.UPGRADE){
                    lb.update(7,new Contexts(),new LabelExpression());c.setAutoCommit(true);
                    validarHistorico(historico(c),7);inserirFinanceiro(c);
                    financeiro=retrato(c,LEGADO);historiaAnterior=historiaCompleta(c);
                    assertEquals(16,financeiro.dados().values().stream().mapToInt(List::size).sum());
                }
                lb.update(new Contexts(),new LabelExpression());c.setAutoCommit(true);
                validarHistorico(historico(c),9);provas.add(Prova.MIGRATIONS);
                if(cenario==Cenario.UPGRADE){
                    assertEquals(historiaAnterior,historiaCompleta(c).subList(0,7),"Histórico legado preservado");
                    System.out.println("CUSTODIA_1_9 legadoLinhas=16 sha256="+preservar(financeiro,retrato(c,LEGADO)));provas.add(Prova.LEGADO);
                }else{
                    for(var tabela:TODAS)assertEquals(List.of(List.of("0")),consulta(c,"SELECT count(*) FROM "+tabela),"Instalação limpa sem backfill");
                    inserirFinanceiro(c);financeiro=retrato(c,LEGADO);
                }
                etapa=CasoFalha.SCHEMA;CustodiaPostgresIntegridade19.schema(c);provas.add(Prova.SCHEMA);
                etapa=CasoFalha.IDEMPOTENCIA;
                var antes=retrato(c,TODAS);var historia=historiaCompleta(c);
                lb.update(new Contexts(),new LabelExpression());c.setAutoCommit(true);
                assertTrue(lb.listUnrunChangeSets(new Contexts(),new LabelExpression()).isEmpty());
                assertEquals(historia,historiaCompleta(c));preservar(antes,retrato(c,TODAS));provas.add(Prova.IDEMPOTENCIA);
                completa=antes;
            }
        }
        // A fábrica só é inicializada depois dos nove changeSets e é destruída antes do descarte.
        System.out.println("CUSTODIA_1_9 etapa=HIBERNATE_PERSISTENCIA_REPOSITORIOS_REPLAY");
        etapa=CasoFalha.PERSISTENCIA;
        CustodiaPostgresPersistencia19.executar(ds);
        provas.addAll(Set.of(Prova.HIBERNATE,Prova.PERSISTENCIA,Prova.REPOSITORIOS,Prova.REPLAY));
        System.out.println("CUSTODIA_1_9 etapa=INTEGRIDADE_INDICE");
        etapa=CasoFalha.INTEGRIDADE_INDICE;
        long bytes=CustodiaPostgresIntegridade19.executar(ds);provas.addAll(Set.of(Prova.INTEGRIDADE,Prova.INDICE));
        etapa=CasoFalha.PRESERVACAO;
        try(var c=ds.getConnection()){preservar(financeiro,retrato(c,LEGADO));preservar(completa,retrato(c,TODAS));}
        System.out.println("CUSTODIA_1_9 etapa=ROLLBACK_PROTEGIDO");
        etapa=CasoFalha.ROLLBACK;
        rollbackProtegido(ds);provas.add(Prova.ROLLBACK);
        return new Resultado(provas,9,bytes);
        } catch(Exception | AssertionError erro) { throw falha(etapa,erro); }
    }
    static void rollbackProtegido(CustodiaPostgresDataSource ds)throws Exception{
        Retrato antes;List<List<String>> historia;
        try(var c=ds.getConnection();var recursos=new ClassLoaderResourceAccessor()){
            // Auditoria confirmada para comprovar que a negativa de rollback a preserva.
            CustodiaPostgresIntegridade19.fixture(c);
            CustodiaPostgresIntegridade19.inserir(c,"movimentacao_custodia",CustodiaPostgresIntegridade19.movimento());
            CustodiaPostgresIntegridade19.inserir(c,"estorno_movimentacao_custodia",CustodiaPostgresIntegridade19.estorno(801L));
            antes=retrato(c,TODAS);historia=historiaCompleta(c);
            var db=DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c));db.setDefaultSchemaName("public");
            try(var lb=new Liquibase(MASTER,recursos,db)){
                validarPlano(lb);
                var erro=assertThrows(liquibase.exception.LiquibaseException.class,()->lb.rollback(2,new Contexts(),new LabelExpression()));
                Throwable raiz=erro;for(int i=0;raiz.getCause()!=null && i<100;i++)raiz=raiz.getCause();
                assertInstanceOf(liquibase.change.core.StopChange.StopChangeException.class,raiz);
                assertTrue(raiz.getMessage().contains("Rollback das constraints de custódia bloqueado"));
                // Nenhum SQL nesta conexão após a falha esperada. Fechar e verificar em outra conexão.
            }
        }
        try(var c=ds.getConnection()){
            preservar(antes,retrato(c,TODAS));assertEquals(historia,historiaCompleta(c));
            assertEquals(List.of(List.of("false")),consulta(c,"SELECT locked::text FROM databasechangeloglock ORDER BY id"));
            validarHistorico(historico(c),9);
        }
    }
    static List<Registro> historico(Connection c)throws SQLException{
        var registros=new ArrayList<Registro>();
        try(var s=c.createStatement();var rs=s.executeQuery("SELECT id,author,filename,md5sum,exectype,orderexecuted FROM databasechangelog ORDER BY orderexecuted")){
            while(rs.next())registros.add(new Registro(rs.getString(1),rs.getString(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6)));
        }return List.copyOf(registros);
    }
    static List<List<String>> historiaCompleta(Connection c)throws SQLException{return consulta(c,"SELECT * FROM databasechangelog ORDER BY orderexecuted");}
    static Retrato retrato(Connection c,List<String> tabelas)throws SQLException{
        var dados=new TreeMap<String,List<List<String>>>();var estruturas=new TreeMap<String,List<List<String>>>();
        for(var tabela:tabelas){
            assertTrue(TODAS.contains(tabela),"Tabela fora do escopo fixo");
            dados.put(tabela,consulta(c,"SELECT * FROM "+tabela+" ORDER BY id"));
            estruturas.put(tabela+".colunas",consulta(c,"SELECT column_name,data_type,is_nullable,character_maximum_length,numeric_precision,numeric_scale,datetime_precision,column_default,is_identity "
                    +"FROM information_schema.columns WHERE table_schema='public' AND table_name='"+tabela+"' ORDER BY ordinal_position"));
            estruturas.put(tabela+".constraints",consulta(c,"SELECT conname,contype,convalidated,pg_get_constraintdef(oid) FROM pg_constraint WHERE conrelid='public."+tabela+"'::regclass ORDER BY conname"));
            estruturas.put(tabela+".indices",consulta(c,"SELECT indexname,indexdef FROM pg_indexes WHERE schemaname='public' AND tablename='"+tabela+"' ORDER BY indexname"));
        }return new Retrato(Collections.unmodifiableMap(dados),Collections.unmodifiableMap(estruturas));
    }
    static List<List<String>> consulta(Connection c,String sql)throws SQLException{
        var linhas=new ArrayList<List<String>>();try(var s=c.createStatement();var rs=s.executeQuery(sql)){
            int n=rs.getMetaData().getColumnCount();while(rs.next()){var linha=new ArrayList<String>();
                for(int i=1;i<=n;i++)linha.add(rs.getString(i));linhas.add(Collections.unmodifiableList(linha));}}
        return List.copyOf(linhas);
    }
    static void executarSql(Connection c,String sql)throws SQLException{try(var s=c.createStatement()){assertTrue(s.executeUpdate(sql)>0);}}
    // Fixture financeira sintética equivalente à validada em CustodiaUpgradeRollbackH2Test.
    static void inserirFinanceiro(Connection c) throws SQLException {
        executarSql(c, "INSERT INTO corretora(id,cnpj,razao_social,cep,logradouro,bairro,cidade,uf,situacao_cadastral,data_cadastro) "
                + "VALUES (101,'11111111000111','Origem sintética','00000000','Rua','Bairro','Cidade','SP','ATIVA',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00.123456-03:00'),"
                + "(102,'22222222000122','Destino sintético','00000000','Rua','Bairro','Cidade','SP','ATIVA',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00.654321+05:30')");
        executarSql(c, "INSERT INTO carteira(id,nome,data_criacao) VALUES "
                + "(201,'BR sintética',TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00-03:00'),"
                + "(202,'EUA sintética',TIMESTAMP WITH TIME ZONE '2020-01-01 12:00:00+00:00')");
        executarSql(c, "INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao) VALUES "
                + "(301,'SINTBR','Empresa BR','BRASIL','BRL',12.123456,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.123456-03:00'),"
                + "(302,'SINTUS','Empresa EUA','EUA','USD',123.654321,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00')");
        executarSql(c, "INSERT INTO operacao(id,carteira_id,acao_id,corretora_id,tipo,quantidade,preco_unitario,data_operacao,ordem_no_dia,valor_total) VALUES "
                + "(401,201,301,101,'COMPRA',100,12,DATE '2020-01-02',1,1200),"
                + "(402,201,301,NULL,'VENDA',10,15,DATE '2020-01-03',1,150),"
                + "(403,202,302,102,'COMPRA',1.123456,100,DATE '2020-01-02',1,112.3456),"
                + "(404,202,302,102,'VENDA',0.000001,120,DATE '2020-01-03',1,0.000120)");
        executarSql(c, "INSERT INTO historico_cotacao(id,acao_id,cotacao,data_hora_cotacao,referencia_tipo,referencia_instante,referencia_data,cotacao_obtida_em) VALUES "
                + "(501,301,12.123456,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.123456-03:00','DATA',NULL,DATE '2020-01-02',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:01-03:00'),"
                + "(502,302,123.654321,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00','INSTANTE',"
                + "TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:00.654321+00:00',NULL,TIMESTAMP WITH TIME ZONE '2020-01-02 12:00:01+00:00')");
        executarSql(c, "INSERT INTO snapshot_carteira(id,carteira_id,data_hora_snapshot) VALUES "
                + "(601,201,TIMESTAMP WITH TIME ZONE '2020-01-04 12:00:00.123456-03:00'),"
                + "(602,202,TIMESTAMP WITH TIME ZONE '2020-01-04 12:00:00.654321+00:00')");
        executarSql(c, "INSERT INTO snapshot_carteira_moeda(id,snapshot_carteira_id,moeda,patrimonio_atual) VALUES "
                + "(701,601,'BRL',1091.111040000000),(702,602,'USD',138.925735459055)");
    }
    private CustodiaPostgresMigracoes19() { }
}
