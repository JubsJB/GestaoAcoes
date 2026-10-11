-- PLANO NAO EXECUTADO. Somente SELECT; nao executar sem aprovacao.
-- __SCHEMA__ e um identificador quoted (quote_ident do schema confirmado).
-- __SCHEMA_LITERAL__ e um literal SQL independente (quote_literal do mesmo nome).
-- Nao fazer replace textual global de nomes crus; nao presumir public/search_path.
-- Executar em sessao/transaction read-only, curta, com timeouts aprovados.

-- Q1: confirmar colunas e schema; pode retornar mais de um candidato.
SELECT table_schema, table_name, column_name, data_type, udt_schema, udt_name,
       domain_schema, domain_name, is_nullable
FROM information_schema.columns
WHERE table_name IN ('acao', 'historico_cotacao')
  AND column_name IN ('referencia_data', 'referencia_tipo')
ORDER BY table_schema, table_name, column_name;

-- Q2: conferir protecoes da sessao; abortar se read-only estiver off.
-- Nao altera parametros e nao retorna credenciais.
SELECT current_database() AS database_name,
       current_schema() AS current_schema_name,
       current_setting('DateStyle') AS date_style,
       current_setting('transaction_read_only') AS transaction_read_only,
       current_setting('transaction_isolation') AS transaction_isolation,
       current_setting('statement_timeout') AS statement_timeout,
       current_setting('lock_timeout') AS lock_timeout;

-- Q3: estimativas de volume e indices, nao contagens exatas.
SELECT n.nspname AS schema_name, c.relname AS table_name,
       c.reltuples AS estimated_rows, c.relpages AS estimated_pages,
       c.relrowsecurity AS row_security_enabled,
       c.relforcerowsecurity AS row_security_forced,
       i.relname AS index_name, pg_get_indexdef(i.oid) AS index_definition
FROM pg_catalog.pg_class c
JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
LEFT JOIN pg_catalog.pg_index x ON x.indrelid = c.oid
LEFT JOIN pg_catalog.pg_class i ON i.oid = x.indexrelid
WHERE n.nspname = __SCHEMA_LITERAL__
  AND c.relname IN ('acao', 'historico_cotacao')
ORDER BY c.relname, i.relname;

-- Q3b: raizes e descendentes; confirmar escopo, SELECT e RLS de cada relacao.
-- SELECT das raizes inclui descendentes, pois nao usa ONLY.
WITH RECURSIVE alcance AS (
    SELECT c.oid AS raiz, c.oid AS relacao
    FROM pg_catalog.pg_class c
    JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = __SCHEMA_LITERAL__
      AND c.relname IN ('acao', 'historico_cotacao')
    UNION
    SELECT a.raiz, h.inhrelid
    FROM alcance a
    JOIN pg_catalog.pg_inherits h ON h.inhparent = a.relacao
)
SELECT r.relname AS raiz, n.nspname AS schema_name, c.relname AS relation_name,
       c.relkind, c.relispartition, c.relrowsecurity, c.relforcerowsecurity,
       row_security_active(c.oid) AS rls_active_for_current_role,
       has_table_privilege(c.oid, 'SELECT') AS table_select,
       (SELECT count(*) FROM pg_catalog.pg_policy p WHERE p.polrelid = c.oid) AS policy_count,
       (SELECT count(*) FROM pg_catalog.pg_inherits h WHERE h.inhrelid = c.oid) AS parent_count
FROM alcance a
JOIN pg_catalog.pg_class r ON r.oid = a.raiz
JOIN pg_catalog.pg_class c ON c.oid = a.relacao
JOIN pg_catalog.pg_namespace n ON n.oid = c.relnamespace
ORDER BY r.relname, n.nspname, c.relname;

-- Q4: inventario exato, ATE duas linhas, um snapshot de statement.
-- Tabela vazia nao emite grupo; ambas vazias produzem zero linhas. Usar Q5.
-- Comparacoes nativas DATE: sem JDBC LocalDate/java.sql.Date/timezone.
-- MIN/MAX retornam texto SQL nativo para preservar inclusive era BC.
-- Faixas se sobrepoem intencionalmente; nao somar suas contagens.
WITH referencias AS (
    SELECT 'acao'::text AS tabela, referencia_data, referencia_tipo
    FROM __SCHEMA__.acao
    UNION ALL
    SELECT 'historico_cotacao'::text, referencia_data, referencia_tipo
    FROM __SCHEMA__.historico_cotacao
)
SELECT tabela,
       count(*) AS total,
       count(*) FILTER (WHERE referencia_data IS NULL) AS nulas,
       count(referencia_data) AS preenchidas,
       min(referencia_data)::text AS menor_data_sql,
       max(referencia_data)::text AS maior_data_sql,
       count(*) FILTER (WHERE referencia_data < DATE '1970-01-01') AS anteriores_1970,
       count(*) FILTER (WHERE referencia_data < DATE '1582-01-01') AS anteriores_1582,
       count(*) FILTER (WHERE isfinite(referencia_data)
                           AND referencia_data < DATE '0001-01-01') AS bc,
       count(*) FILTER (WHERE referencia_data >= DATE '1582-10-01'
                           AND referencia_data < DATE '1582-11-01') AS outubro_1582,
       count(*) FILTER (WHERE referencia_data >= DATE '1582-10-05'
                           AND referencia_data < DATE '1582-10-15') AS intervalo_ambiguo_1582,
       count(*) FILTER (WHERE isfinite(referencia_data)
                           AND referencia_data >= DATE '1970-01-01') AS contemporaneas_desde_1970,
       count(*) FILTER (WHERE isfinite(referencia_data)
                           AND referencia_data >= DATE '2000-01-01') AS contemporaneas_desde_2000,
       count(*) FILTER (WHERE isfinite(referencia_data)
                           AND referencia_data > DATE '9999-12-31') AS ad_acima_9999,
       count(*) FILTER (WHERE referencia_data IS NOT NULL
                           AND NOT isfinite(referencia_data)) AS infinitas,
       count(*) FILTER (WHERE referencia_tipo = 'DATA'
                           AND referencia_data IS NULL) AS tipo_data_sem_data,
       count(*) FILTER (WHERE referencia_data IS NOT NULL
                           AND referencia_tipo IS DISTINCT FROM 'DATA') AS data_com_tipo_incoerente
FROM referencias
GROUP BY tabela
ORDER BY tabela;

-- Q5: opcional apenas se tabela vazia nao produzir linha em Q4.
-- As duas linhas sempre existem, sem expor dados individuais.
SELECT 'acao'::text AS tabela, count(*) AS total FROM __SCHEMA__.acao
UNION ALL
SELECT 'historico_cotacao'::text, count(*) FROM __SCHEMA__.historico_cotacao;
