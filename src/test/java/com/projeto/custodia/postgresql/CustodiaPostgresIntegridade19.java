package com.projeto.custodia.postgresql;

import java.sql.*;
import java.util.*;
import com.projeto.entities.PosicaoRacionalCustodia;
import static org.junit.jupiter.api.Assertions.*;
import static com.projeto.custodia.postgresql.CustodiaPostgresHomologacao19.*;

/** Assertions PostgreSQL sobre as tabelas reais de 008, sempre pelo datasource guardado. */
final class CustodiaPostgresIntegridade19 {
    static final String MOV="movimentacao_custodia", EST="estorno_movimentacao_custodia";
    static final List<String> CHAVE=List.of("carteira_id","acao_id","data_efetiva","ordem_financeira_ancora","posicao_numerador","posicao_denominador");
    static final Set<String> CONSTRAINTS=Set.of("pk_movimentacao_custodia","pk_estorno_movimentacao_custodia",
            "uk_movimentacao_custodia_posicao","uk_estorno_movimentacao_custodia_original",
            "fk_movimentacao_custodia_carteira","fk_movimentacao_custodia_acao","fk_movimentacao_custodia_corretora_origem",
            "fk_movimentacao_custodia_corretora_destino","fk_estorno_movimentacao_custodia_original",
            "ck_movimentacao_custodia_natureza","ck_movimentacao_custodia_quantidade_positiva","ck_movimentacao_custodia_ancora_nao_negativa",
            "ck_movimentacao_custodia_direcao","ck_movimentacao_custodia_numerador_formato","ck_movimentacao_custodia_denominador_formato");
    static void schema(Connection c) throws Exception {
        var encontradas=new HashSet<String>();
        try(var s=c.createStatement();var rs=s.executeQuery("SELECT conname,contype,confdeltype,convalidated FROM pg_constraint "
                +"WHERE conrelid IN ('public.movimentacao_custodia'::regclass,'public.estorno_movimentacao_custodia'::regclass)")) {
            while(rs.next()) { String nome=rs.getString(1); encontradas.add(nome); assertTrue(rs.getBoolean(4),"Constraint não validada");
                String tipo=nome.startsWith("fk_")?"f":nome.startsWith("pk_")?"p":nome.startsWith("uk_")?"u":"c";
                assertEquals(tipo,rs.getString(2));
                if(nome.startsWith("fk_")) assertEquals("r",rs.getString(3));
            }
        }
        assertEquals(CONSTRAINTS,encontradas,"Constraints completas de 008");
        var nulabilidade=new TreeMap<String,String>();
        for(var linha:CustodiaPostgresMigracoes19.consulta(c,"SELECT table_name,column_name,is_nullable FROM information_schema.columns "
                +"WHERE table_schema='public' AND table_name IN ('movimentacao_custodia','estorno_movimentacao_custodia')"))
            nulabilidade.put(linha.get(0)+"."+linha.get(1),linha.get(2));
        var esperado=new TreeMap<String,String>();
        for(String coluna:List.of("id","carteira_id","acao_id","natureza","corretora_origem_id","corretora_destino_id","quantidade",
                "data_efetiva","data_registro","ordem_financeira_ancora","posicao_numerador","posicao_denominador"))
            esperado.put(MOV+"."+coluna,coluna.equals("corretora_origem_id")?"YES":"NO");
        for(String coluna:List.of("id","movimentacao_original_id","data_registro","motivo"))
            esperado.put(EST+"."+coluna,coluna.equals("motivo")?"YES":"NO");
        assertEquals(esperado,nulabilidade,"Colunas completas e nulabilidade de 008");
        assertEquals(List.of(List.of("character varying","512","NO"),List.of("character varying","512","NO")),
                CustodiaPostgresMigracoes19.consulta(c,"SELECT data_type,character_maximum_length::text,is_nullable "
                        +"FROM information_schema.columns WHERE table_schema='public' AND table_name='movimentacao_custodia' "
                        +"AND column_name IN ('posicao_numerador','posicao_denominador') ORDER BY column_name"));
        assertEquals(List.of(List.of("19","6","NO")),CustodiaPostgresMigracoes19.consulta(c,
                "SELECT numeric_precision::text,numeric_scale::text,is_nullable FROM information_schema.columns "
                        +"WHERE table_schema='public' AND table_name='movimentacao_custodia' AND column_name='quantidade'"));
        assertEquals(List.of(List.of("estorno_movimentacao_custodia","6"),List.of("movimentacao_custodia","6")),
                CustodiaPostgresMigracoes19.consulta(c,"SELECT table_name,datetime_precision::text FROM information_schema.columns "
                        +"WHERE table_schema='public' AND table_name IN ('movimentacao_custodia','estorno_movimentacao_custodia') "
                        +"AND column_name='data_registro' AND data_type='timestamp with time zone' ORDER BY table_name"));
        indice(c,"uk_movimentacao_custodia_posicao",CHAVE);
        indice(c,"uk_estorno_movimentacao_custodia_original",List.of("movimentacao_original_id"));
        indice(c,"idx_movimentacao_custodia_carteira_registro_id",List.of("carteira_id","data_registro","id"));
        indice(c,"idx_movimentacao_custodia_carteira_acao_cronologia",List.of("carteira_id","acao_id","data_efetiva","ordem_financeira_ancora"));
    }
    private static void indice(Connection c,String nome,List<String> colunas) throws SQLException {
        var encontradas=new ArrayList<String>();
        try(var s=c.prepareStatement("SELECT a.attname FROM pg_index i JOIN pg_class idx ON idx.oid=i.indexrelid "
                +"JOIN pg_namespace ns ON ns.oid=idx.relnamespace CROSS JOIN LATERAL unnest(i.indkey) WITH ORDINALITY k(attnum,ord) "
                +"JOIN pg_attribute a ON a.attrelid=i.indrelid AND a.attnum=k.attnum "
                +"WHERE ns.nspname='public' AND idx.relname=? AND i.indisvalid AND i.indisready ORDER BY k.ord")) {
            s.setString(1,nome);try(var rs=s.executeQuery()){while(rs.next()) encontradas.add(rs.getString(1));}
        }
        assertEquals(colunas,encontradas,"Índice completo e válido: "+nome);
    }
    @FunctionalInterface interface Caso { void executar(Connection c) throws Exception; }
    static void transacao(CustodiaPostgresDataSource ds,Caso caso) throws Exception {
        try(var c=ds.getConnection()){c.setAutoCommit(false);try{fixture(c);caso.executar(c);}finally{c.rollback();}}
    }
    static void rejeitar(CustodiaPostgresDataSource ds,String estado,String identificador,Caso caso) throws Exception {
        rejeitar(ds,estado,identificador,identificador!=null && identificador.equals("ck_movimentacao_custodia_natureza")
                ? CasoFalha.NATUREZA_INVALIDA : CasoFalha.REJEICAO_SQL,caso);
    }
    private static void rejeitar(CustodiaPostgresDataSource ds,String estado,String identificador,CasoFalha nome,Caso caso) throws Exception {
        try { transacao(ds,caso); }
        catch(SQLException sql) { validarRejeicao(nome,estado,identificador,sql);return; }
        catch(Exception | AssertionError falha) { throw falha(nome,falha); }
        throw new FalhaCaso(new Diagnostico(nome,Categoria.SQL_AUSENTE,"NA","NA"));
    }
    private static void validarRejeicao(CasoFalha nome,String estado,String identificador,SQLException erro) {
        if(!estado.equals(erro.getSQLState()))
            throw new FalhaCaso(new Diagnostico(nome,Categoria.SQLSTATE_DIVERGENTE,estadoSeguro(erro),constraintSegura(erro)));
        if(identificador!=null) {
            var aceitos=nome==CasoFalha.NATUREZA_INVALIDA && identificador.equals("ck_movimentacao_custodia_natureza")
                    ? Set.of(identificador,"ck_movimentacao_custodia_direcao") : Set.of(identificador);
            var detalhes=erro instanceof org.postgresql.util.PSQLException pg ? pg.getServerErrorMessage() : null;
            String observado=detalhes==null ? null : detalhes.getConstraint();
            if(observado==null && detalhes!=null) observado=detalhes.getColumn();
            if(!aceitos.contains(observado==null ? "" : observado))
                throw new FalhaCaso(new Diagnostico(nome,Categoria.IDENTIFICADOR_DIVERGENTE,estadoSeguro(erro),constraintSegura(erro)));
        }
    }
    static long executar(CustodiaPostgresDataSource ds) throws Exception {
        // As cinco FKs e referências NOT NULL; cada falha encerra a transação antes de outra consulta.
        for(String coluna:List.of("carteira_id","acao_id","corretora_origem_id","corretora_destino_id")) {
            rejeitar(ds,"23503","fk_movimentacao_custodia_"+coluna.replace("_id",""),c->{var m=movimento();m.put(coluna,Long.MAX_VALUE);inserir(c,MOV,m);});
        }
        rejeitar(ds,"23503","fk_estorno_movimentacao_custodia_original",c->inserir(c,EST,estorno(Long.MAX_VALUE)));
        for(String coluna:List.of("carteira_id","acao_id","corretora_destino_id"))
            rejeitar(ds,"23502",coluna,c->{var m=movimento();m.put(coluna,null);inserir(c,MOV,m);});
        rejeitar(ds,"23502","movimentacao_original_id",c->inserir(c,EST,estorno(null)));
        for(String tabela:List.of("carteira","acao","corretora_origem","corretora_destino","original"))
            rejeitar(ds,"23503",tabela.equals("original") ? "fk_estorno_movimentacao_custodia_original" : "fk_movimentacao_custodia_"+tabela,c->{
                inserir(c,MOV,movimento());inserir(c,EST,estorno(801L));
                String alvo=switch(tabela){case "carteira"->"carteira WHERE id=9201";case "acao"->"acao WHERE id=9301";
                    case "corretora_origem"->"corretora WHERE id=9101";case "corretora_destino"->"corretora WHERE id=9102";default->MOV+" WHERE id=801";};
                CustodiaPostgresMigracoes19.executarSql(c,"DELETE FROM "+alvo);
            });
        rejeitar(ds,"23505","uk_movimentacao_custodia_posicao",c->{inserir(c,MOV,movimento());var m=movimento();m.put("id",802L);m.put("quantidade",20);inserir(c,MOV,m);});
        rejeitar(ds,"23505","uk_estorno_movimentacao_custodia_original",c->{inserir(c,MOV,movimento());inserir(c,EST,estorno(801L));var e=estorno(801L);e.put("id",902L);inserir(c,EST,e);});
        for(String caso:List.of("natureza","quantidade_zero","quantidade_negativa","ancora","sem_origem","mesma_corretora","atribuicao_com_origem")) {
            String constraint=switch(caso){case "natureza"->"natureza";case "ancora"->"ancora_nao_negativa";
                case "quantidade_zero","quantidade_negativa"->"quantidade_positiva";default->"direcao";};
            var nome=caso.equals("natureza") ? CasoFalha.NATUREZA_INVALIDA : CasoFalha.valueOf(caso.toUpperCase(Locale.ROOT));
            rejeitar(ds,"23514","ck_movimentacao_custodia_"+constraint,nome,c->{var m=movimento();switch(caso){
                case "natureza"->m.put("natureza","INVALIDA");case "quantidade_zero"->m.put("quantidade",0);
                case "quantidade_negativa"->m.put("quantidade",-1);case "ancora"->m.put("ordem_financeira_ancora",-1);
                case "sem_origem"->m.put("corretora_origem_id",null);case "mesma_corretora"->m.put("corretora_origem_id",9102L);
                default->m.put("natureza","ATRIBUICAO");}inserir(c,MOV,m);});
        }
        for(String coluna:List.of("posicao_numerador","posicao_denominador")) {
            for(String texto:List.of("","0","01","+1","-1","1.0","1e2"," 1","1 ","1\n","1\r","1\t","١","１","1a"))
                rejeitar(ds,"23514","ck_movimentacao_custodia_"+(coluna.endsWith("numerador")?"numerador_formato":"denominador_formato"),
                        c->{var m=movimento();m.put(coluna,texto);inserir(c,MOV,m);});
            rejeitar(ds,"22001",null,c->{var m=movimento();m.put(coluna,"1".repeat(513));inserir(c,MOV,m);});
        }
        for(String coluna:CHAVE) transacao(ds,c->{inserir(c,MOV,movimento());var m=movimento();m.put("id",802L);
            m.put(coluna,switch(coluna){case "carteira_id"->9202L;case "acao_id"->9302L;case "data_efetiva"->java.time.LocalDate.of(2001,2,4);
                case "ordem_financeira_ancora"->2;case "posicao_numerador"->"2";default->"5";});inserir(c,MOV,m);});
        transacao(ds,c->{var m=movimento();m.put("natureza","ATRIBUICAO");m.put("corretora_origem_id",null);inserir(c,MOV,m);});
        for(String fracao:List.of("2/4","1/1","3/2")) transacao(ds,c->{var m=movimento();var par=fracao.split("/");
            m.put("posicao_numerador",par[0]);m.put("posicao_denominador",par[1]);inserir(c,MOV,m);
            assertThrows(IllegalArgumentException.class,()->PosicaoRacionalCustodia.reconstruirPersistida(par[0],par[1]));});
        transacao(ds,c->{var a=movimento();a.put("posicao_denominador","2");inserir(c,MOV,a);var b=movimento();b.put("id",802L);
            b.put("posicao_numerador","2");b.put("posicao_denominador","4");inserir(c,MOV,b);});
        long[] tamanho={0};
        transacao(ds,c->{long id=801;
            // Reutiliza as oito amostras de 1.10, evitando depender de componentes muito compressíveis.
            for(var amostra:CustodiaPostgresExperimentoCapacidade.limites()){
                var m=movimento();m.put("id",id);m.put("posicao_numerador",amostra.n());m.put("posicao_denominador",amostra.d());inserir(c,MOV,m);
                assertEquals(List.of(List.of(amostra.n(),amostra.d())),CustodiaPostgresMigracoes19.consulta(c,
                        "SELECT posicao_numerador,posicao_denominador FROM "+MOV+" WHERE id="+id));id++;
            }
            tamanho[0]=Long.parseLong(CustodiaPostgresMigracoes19.consulta(c,"SELECT pg_relation_size('public.uk_movimentacao_custodia_posicao'::regclass)").get(0).get(0));
            assertTrue(tamanho[0]>0,"Tamanho físico do índice integrado");});
        return tamanho[0];
    }
    static Map<String,Object> movimento(){var m=new LinkedHashMap<String,Object>();m.put("id",801L);m.put("carteira_id",9201L);m.put("acao_id",9301L);
        m.put("natureza","TRANSFERENCIA");m.put("corretora_origem_id",9101L);m.put("corretora_destino_id",9102L);m.put("quantidade",new java.math.BigDecimal("10.000000"));
        m.put("data_efetiva",java.time.LocalDate.of(2001,2,3));m.put("data_registro",java.time.OffsetDateTime.parse("2026-10-14T12:34:56.123456-03:00"));
        m.put("ordem_financeira_ancora",1);m.put("posicao_numerador","1");m.put("posicao_denominador","3");return m;}
    static Map<String,Object> estorno(Long id){var e=new LinkedHashMap<String,Object>();e.put("id",901L);e.put("movimentacao_original_id",id);
        e.put("data_registro",java.time.OffsetDateTime.parse("2026-10-15T12:34:56.654321+05:30"));e.put("motivo",null);return e;}
    static void inserir(Connection c,String tabela,Map<String,Object> valores)throws SQLException{
        try(var s=c.prepareStatement("INSERT INTO "+tabela+" ("+String.join(",",valores.keySet())+") VALUES ("
                +String.join(",",Collections.nCopies(valores.size(),"?"))+")")){int i=1;for(var valor:valores.values())s.setObject(i++,valor);assertEquals(1,s.executeUpdate());}}
    static void fixture(Connection c)throws SQLException{
        CustodiaPostgresMigracoes19.executarSql(c,"INSERT INTO carteira(id,nome,data_criacao) VALUES (9201,'Integridade',CURRENT_TIMESTAMP),(9202,'Outra',CURRENT_TIMESTAMP)");
        CustodiaPostgresMigracoes19.executarSql(c,"INSERT INTO acao(id,ticker,nome_empresa,mercado,moeda,cotacao_atual,data_hora_cotacao) VALUES "
                +"(9301,'INTEG1','Sintética','BRASIL','BRL',10,CURRENT_TIMESTAMP),(9302,'INTEG2','Sintética','BRASIL','BRL',10,CURRENT_TIMESTAMP)");
        CustodiaPostgresMigracoes19.executarSql(c,"INSERT INTO corretora(id,cnpj,razao_social,cep,logradouro,bairro,cidade,uf,situacao_cadastral,data_cadastro) VALUES "
                +"(9101,'33333333000133','Origem','00000000','Rua','Bairro','Cidade','SP','ATIVA',CURRENT_TIMESTAMP),"
                +"(9102,'44444444000144','Destino','00000000','Rua','Bairro','Cidade','SP','ATIVA',CURRENT_TIMESTAMP)");
    }
    private CustodiaPostgresIntegridade19() { }
}
