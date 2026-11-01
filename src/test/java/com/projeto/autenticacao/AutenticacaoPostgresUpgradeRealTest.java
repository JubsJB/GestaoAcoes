package com.projeto.autenticacao;

import com.projeto.custodia.postgresql.AutenticacaoPostgresFinanceiro;
import liquibase.*;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AutenticacaoPostgresReal.Ciclo.class)
class AutenticacaoPostgresUpgradeRealTest {
    @Test void upgradeRealPreservaDadosEstruturasEReferenciasFinanceiras()throws Exception {
        try(var run=AutenticacaoPostgresExecucao.nova()) {
            Object antes;
            try(var c=run.datasource().getConnection();var lb=new Liquibase("db/changelog/custodia-ate-008.yaml",new ClassLoaderResourceAccessor(),DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c)))) {
                lb.update(new Contexts(),new LabelExpression());
                AutenticacaoPostgresFinanceiro.inserir(c);antes=AutenticacaoPostgresFinanceiro.retrato(c);
                try(var s=c.createStatement();var r=s.executeQuery("select count(*) from databasechangelog")){assertTrue(r.next());assertEquals(9,r.getInt(1));}
            }
            try(var c=run.datasource().getConnection();var lb=new Liquibase("db/changelog/db.changelog-master.yaml",new ClassLoaderResourceAccessor(),DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(c)))) {
                lb.update(new Contexts(),new LabelExpression());
                assertEquals(antes,AutenticacaoPostgresFinanceiro.retrato(c));
                try(var s=c.createStatement();var r=s.executeQuery("select count(*) from databasechangelog")){assertTrue(r.next());assertEquals(10,r.getInt(1));}
                lb.update(new Contexts(),new LabelExpression());assertEquals(antes,AutenticacaoPostgresFinanceiro.retrato(c));
            }
            run.reiniciarBanco();
            try(var c=run.datasource().getConnection()){assertEquals(antes,AutenticacaoPostgresFinanceiro.retrato(c));}
            AutenticacaoPostgresExecucao.registrar(run.execucao.plano().execucao().toString(),"UPGRADE_PRESERVACAO_IDEMPOTENCIA_REINICIO","PASS");
        }
    }
}
