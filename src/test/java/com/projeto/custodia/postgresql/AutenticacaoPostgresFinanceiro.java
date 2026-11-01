package com.projeto.custodia.postgresql;

import java.sql.Connection;

/** Reuso da fixture/retrato financeiro aprovado de 001–008, sem alterar suas regras. */
public final class AutenticacaoPostgresFinanceiro {
    public static void inserir(Connection c)throws Exception { CustodiaPostgresMigracoes19.inserirFinanceiro(c); }
    public static Object retrato(Connection c)throws Exception { return CustodiaPostgresMigracoes19.retrato(c,CustodiaPostgresMigracoes19.TODAS); }
    private AutenticacaoPostgresFinanceiro() { }
}
