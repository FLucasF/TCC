#!/usr/bin/env bash
# The project's database, measured on an empty PostgreSQL: the service is built and started once against it (it has
# to create its schema when it starts, as the texts ask), the schema is saved with pg_dump --schema-only, and a few
# simple checks run on the catalog: tables without a primary key, money without the right decimal places, foreign
# keys without an index, dates stored as text, what is NOT NULL, CHECK, UNIQUE and EXCLUDE, how the schema is created
# (migrations or code), and SQL put together by concatenation. In maintenance (when ../base exists), also which
# migrations of the base were edited. Writes measures/database.md and measures/database-schema.sql. Judges nothing.
#
# Usage: bash measure_database.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"

DATABASE=review_schema
PORT=18091
project="$(cd "$1" && pwd)"
measures="$(results_folder "$project")"
out="$measures/$(result_name database "$project").md"
trap stop_service EXIT

# psql on the measured database: one row per line, columns separated by |
query() {
    psql -X -q -At -F'|' -d "$DATABASE" -c "$1" 2>&1
}

# the user tables, outside the system schemas and the migration tools' own tables
USER_TABLES="c.relkind IN ('r', 'p') AND n.nspname NOT IN ('pg_catalog', 'information_schema')
    AND n.nspname NOT LIKE 'pg_toast%'
    AND c.relname NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')"

{
    echo "# Banco: o esquema que o serviço cria e as conferências simples"
    echo
    root="$(project_root "$project")"
    if [ -z "$root" ]; then
        echo "Resultado: sem medida (não há pom.xml no projeto)."
        exit 0
    fi
    url="$(fresh_database "$DATABASE")" || { echo "Resultado: sem medida (o banco não subiu)."; exit 0; }
    if ! build_package "$root" "$measures/database-build.log"; then
        echo "Resultado: sem medida (o projeto não empacota). O fim do log:"
        echo '```'
        tail -20 "$measures/database-build.log"
        echo '```'
        exit 0
    fi
    jar="$(main_jar "$root")"
    if [ -z "$jar" ]; then
        echo "Resultado: sem medida (nenhum jar com Main-Class em target/)."
        exit 0
    fi
    if start_service "$jar" "$url" "$PORT" "$measures/database-service.log"; then
        started="sim"
    else
        started="não"
    fi
    stop_service
    schema_file="$(result_name database-schema "$project").sql"
    pg_dump --schema-only --no-owner --no-privileges -d "$DATABASE" > "$measures/$schema_file" 2>&1
    table_count=$(query "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE $USER_TABLES")

    if [ "$started" = "não" ]; then
        echo "Resultado: o serviço não subiu contra um banco vazio; o esquema abaixo é o que ficou ($table_count" \
            "tabelas). O fim do log do serviço:"
        echo '```'
        tail -15 "$measures/database-service.log"
        echo '```'
    else
        echo "Resultado: medido. O serviço subiu contra um banco vazio e criou $table_count tabelas" \
            "(o esquema inteiro em \`measures/$schema_file\`)."
    fi
    echo

    echo "## Como o esquema é criado"
    echo
    has_flyway=$(query "SELECT count(*) FROM pg_class WHERE relname = 'flyway_schema_history'")
    has_liquibase=$(query "SELECT count(*) FROM pg_class WHERE relname = 'databasechangelog'")
    [ "$has_flyway" != "0" ] && echo "- Flyway: $(query "SELECT count(*) FROM flyway_schema_history WHERE success") migrações aplicadas"
    [ "$has_liquibase" != "0" ] && echo "- Liquibase: $(query "SELECT count(*) FROM databasechangelog") mudanças aplicadas"
    migration_files=$(find "$root" -path '*/src/main/resources/*' \( -name 'V*__*.sql' -o -name '*.sql' -o -name '*changelog*' \) \
        -not -path '*/target/*' 2>/dev/null | sed "s|$project/||" | sort)
    echo "- arquivos de esquema em src/main/resources: $(echo "$migration_files" | grep -c . || true)"
    [ -n "$migration_files" ] && echo "$migration_files" | sed 's/^/  - `/; s/$/`/'
    create_in_code=$(grep -rln 'CREATE TABLE' --include='*.java' "$root" 2>/dev/null | grep '/src/main/' | sed "s|$project/||")
    [ -n "$create_in_code" ] && echo "- \`CREATE TABLE\` dentro do código Java: $(echo "$create_in_code" | paste -sd' ' -)"
    if [ -d "$project/../base" ] && [ "$(basename "$project")" != "base" ]; then
        echo
        echo "Na manutenção, as migrações da base (\`base/\`) comparadas com as do projeto final:"
        base_root="$(project_root "$project/../base")"
        while read -r migration; do
            relative="${migration#"$base_root/"}"
            if [ ! -f "$root/$relative" ]; then
                echo "- \`$relative\`: APAGADA no projeto final"
            elif ! cmp -s "$migration" "$root/$relative"; then
                echo "- \`$relative\`: EDITADA no projeto final (uma migração já aplicada mudou)"
            else
                echo "- \`$relative\`: igual"
            fi
        done < <(find "$base_root/src/main/resources" -name '*.sql' 2>/dev/null | sort)
        find "$root/src/main/resources" -name '*.sql' 2>/dev/null | sort | while read -r migration; do
            relative="${migration#"$root/"}"
            [ -f "$base_root/$relative" ] || echo "- \`$relative\`: nova"
        done
    fi
    echo

    echo "## As tabelas"
    echo
    echo "| tabela | colunas | NOT NULL | chave primária | chaves estrangeiras | CHECK | UNIQUE / EXCLUDE |"
    echo "|---|---|---|---|---|---|---|"
    query "SELECT n.nspname || '.' || c.relname,
            (SELECT count(*) FROM pg_attribute a WHERE a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped),
            (SELECT count(*) FROM pg_attribute a WHERE a.attrelid = c.oid AND a.attnum > 0 AND NOT a.attisdropped
                AND a.attnotnull),
            (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'p'),
            (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'f'),
            (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'c'),
            (SELECT count(*) FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype IN ('u', 'x'))
        FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE $USER_TABLES ORDER BY 1" \
        | awk -F'|' '{printf "| `%s` | %s | %s | %s | %s | %s | %s |\n", $1, $2, $3, ($4 > 0 ? "sim" : "NÃO"), $5, $6, $7}'
    echo

    echo "## As conferências"
    echo
    no_key=$(query "SELECT n.nspname || '.' || c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE $USER_TABLES AND NOT EXISTS (SELECT 1 FROM pg_constraint k WHERE k.conrelid = c.oid AND k.contype = 'p')
        ORDER BY 1")
    echo "**Tabela sem chave primária:** ${no_key:-nenhuma}" | paste -sd' ' -
    echo

    echo "**Dinheiro** (colunas com nome de valor: price, total, subtotal, discount, amount, refund, paid, rate, tax," \
        "fee, valor, preco, desconto; o certo é \`numeric(p, 2)\`):"
    echo
    money=$(query "SELECT c.table_name || '.' || c.column_name, c.data_type,
            coalesce(c.numeric_precision::text, ''), coalesce(c.numeric_scale::text, '')
        FROM information_schema.columns c
        WHERE c.table_schema NOT IN ('pg_catalog', 'information_schema')
          AND c.table_name NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
          AND c.column_name ~* '(price|total|subtotal|discount|amount|refund|paid|rate|tax|fee|valor|preco|desconto|reembolso)'
          AND c.column_name !~* '(plan|_date$|_at$|_count$|^nights?$|percent|code$|type$|_id$|^id$)'
        ORDER BY 1")
    if [ -z "$money" ]; then
        echo "- nenhuma coluna com nome de valor"
    else
        echo "$money" | awk -F'|' '{
            verdict = "ok";
            if ($2 == "numeric" && $4 == "") verdict = "numeric SEM escala definida";
            else if ($2 == "numeric" && $4 != "2") verdict = "numeric com " $4 " casas";
            else if ($2 ~ /double|real/) verdict = "PONTO FLUTUANTE";
            else if ($2 ~ /integer|bigint|smallint/) verdict = "inteiro (centavos?)";
            else if ($2 != "numeric") verdict = "tipo " $2;
            type = $2; if ($3 != "") type = type "(" $3 "," $4 ")";
            printf "- `%s`: %s, %s\n", $1, type, verdict}'
    fi
    echo

    echo "**Chave estrangeira sem índice** (nenhum índice começando pelas colunas da chave):"
    echo
    fk_without_index=$(query "SELECT k.conrelid::regclass || ' (' || array_to_string(ARRAY(
                SELECT a.attname FROM pg_attribute a WHERE a.attrelid = k.conrelid AND a.attnum = ANY (k.conkey)), ', ')
            || ') -> ' || k.confrelid::regclass
        FROM pg_constraint k WHERE k.contype = 'f' AND NOT EXISTS (
            SELECT 1 FROM pg_index i WHERE i.indrelid = k.conrelid
              AND (string_to_array(i.indkey::text, ' ')::int2[])[1:cardinality(k.conkey)] @> k.conkey)
        ORDER BY 1")
    echo "${fk_without_index:-- nenhuma}" | sed '/^- /!s/^/- /'
    echo

    echo "**Datas guardadas como texto** (colunas com nome de data em text ou varchar):"
    echo
    text_dates=$(query "SELECT table_name || '.' || column_name || ' (' || data_type || ')'
        FROM information_schema.columns
        WHERE table_schema NOT IN ('pg_catalog', 'information_schema')
          AND table_name NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
          AND column_name ~* '(date|check_?in|check_?out|_at$|_on$|^data|month|night)'
          AND data_type IN ('text', 'character varying', 'character')
        ORDER BY 1")
    echo "${text_dates:-- nenhuma}" | sed '/^- /!s/^/- /'
    echo

    echo "**As colunas de situação e de tipo** (status, plano, tipo, canal) e se há CHECK sobre elas:"
    echo
    enums=$(query "SELECT c.table_name || '.' || c.column_name || ': ' || c.data_type || CASE WHEN EXISTS (
                SELECT 1 FROM pg_constraint k WHERE k.contype = 'c' AND k.conrelid = (quote_ident(c.table_schema)
                    || '.' || quote_ident(c.table_name))::regclass
                  AND pg_get_constraintdef(k.oid) ILIKE '%' || c.column_name || '%')
            THEN ', com CHECK' ELSE ', sem CHECK' END
        FROM information_schema.columns c
        WHERE c.table_schema NOT IN ('pg_catalog', 'information_schema')
          AND c.column_name ~* '(status|plan|type|channel|kind|situacao|tipo)'
          AND c.table_name NOT IN ('flyway_schema_history', 'databasechangelog', 'databasechangeloglock')
        ORDER BY 1")
    echo "${enums:-- nenhuma}" | sed '/^- /!s/^/- /'
    echo

    echo "**Restrições que protegem uma regra** (UNIQUE, EXCLUDE, CHECK):"
    echo
    rules=$(query "SELECT k.conrelid::regclass || ': ' || pg_get_constraintdef(k.oid) FROM pg_constraint k
        JOIN pg_class c ON c.oid = k.conrelid JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE k.contype IN ('u', 'x', 'c') AND $USER_TABLES ORDER BY 1")
    echo "${rules:-- nenhuma}" | sed '/^- /!s/^/- /; s/^- \(.*\)$/- `\1`/'
    echo

    echo "## O código que fala com o banco"
    echo
    mapfile -t java_files < <(find "$root" -path '*/src/main/java/*' -name '*.java' 2>/dev/null)
    if [ "${#java_files[@]}" -gt 0 ]; then
        echo "- arquivos com JDBC (\`java.sql\`/\`javax.sql\`): $(grep -lE 'import java\.sql|import javax\.sql' "${java_files[@]}" | wc -l)"
        echo "- pool de conexões (HikariCP ou outro): $(grep -lE 'Hikari|ComboPooled|BasicDataSource|PGSimpleDataSource' "${java_files[@]}" | wc -l) arquivos"
        echo "- \`DriverManager.getConnection\`: $(cat "${java_files[@]}" | grep -c 'DriverManager.getConnection') chamadas"
        echo "- transações explícitas (\`setAutoCommit(false)\`): $(cat "${java_files[@]}" | grep -c 'setAutoCommit(false)')"
        echo "- \`FOR UPDATE\` (trava de linha): $(cat "${java_files[@]}" | grep -ci 'for update')"
        echo
        echo "SQL montado com \`+\` (candidatos; confira se o que entra é constante ou dado de fora):"
        echo '```'
        grep -nE '"\s*(SELECT|INSERT|UPDATE|DELETE|WHERE|AND|SET|VALUES|FROM)\b[^"]*"\s*\+\s*[a-z]|\+\s*"\s*(WHERE|AND|OR|SET|VALUES|FROM|ORDER)\b' \
            "${java_files[@]}" 2>/dev/null | sed "s|$project/||" | head -20
        echo '```'
    fi
} > "$out"
echo "database: $out"
