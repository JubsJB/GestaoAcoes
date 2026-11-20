#!/bin/sh
set -eu
# psql nunca recebe senha em argv; somente arquivo montado. Não habilitar xtrace.
psql -X -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<'SQL'
SET log_statement = 'none';
SET log_min_error_statement = 'panic';
\getenv app_user OP_DB_APP_USER
\getenv db_name POSTGRES_DB
\getenv run_id OP_RUN_ID
\set app_password `cat /run/secrets/app-password`
CREATE ROLE :"app_user" LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD :'app_password';
ALTER SCHEMA public OWNER TO :"app_user";
GRANT CONNECT ON DATABASE :"db_name" TO :"app_user";
CREATE SCHEMA ops_guard;
CREATE TABLE ops_guard.execution (id uuid PRIMARY KEY);
INSERT INTO ops_guard.execution VALUES (:'run_id');
REVOKE ALL ON SCHEMA ops_guard FROM PUBLIC;
SQL
