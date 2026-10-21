#!/usr/bin/env bash
# The project's own tests, measured from the command line without touching its pom: mvn verify with the JaCoCo agent
# (prepare-agent before, report after), against an empty database of its own. Counts the tests that ran, failed and
# were skipped, the test methods written in the code, the kinds of test (through the API, against the database,
# with mocks), and the coverage of lines and branches, in total and per package. Writes measures/tests.md. Judges
# nothing.
#
# Usage: bash measure_tests.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"

JACOCO="org.jacoco:jacoco-maven-plugin:0.8.14"
project="$(cd "$1" && pwd)"
measures="$(results_folder "$project")"
out="$measures/$(result_name tests "$project").md"

# sums one attribute (tests, failures, errors, skipped) over the JUnit XML reports given as arguments
sum_attribute() {
    local attribute="$1"
    shift
    [ "$#" -eq 0 ] && { echo 0; return; }
    grep -ho "<testsuite [^>]*" "$@" | grep -o " $attribute=\"[0-9]*\"" | grep -o '[0-9]*' \
        | awk '{total += $1} END {print total + 0}'
}

{
    echo "# Testes: o que o projeto testa e quanto cobre (Maven + JaCoCo)"
    echo
    root="$(project_root "$project")"
    if [ -z "$root" ]; then
        echo "Resultado: sem medida (não há pom.xml no projeto)."
        exit 0
    fi
    url="$(fresh_database review_tests)" || { echo "Resultado: sem medida (o banco não subiu)."; exit 0; }
    # the failures do not stop the build: the report and the counts still come out
    (cd "$root" && DB_URL="$url" DB_USER="$PGUSER" DB_PASSWORD="$PGPASSWORD" timeout 1500 mvn -B \
        "$JACOCO:prepare-agent" verify "$JACOCO:report" -Dmaven.test.failure.ignore=true \
        -Dfailsafe.failIfNoSpecifiedTests=false) > "$measures/$(result_name tests-maven "$project").log" 2>&1
    maven_exit=$?

    mapfile -t reports < <(find "$root" \( -path '*/target/surefire-reports/TEST-*.xml' \
        -o -path '*/target/failsafe-reports/TEST-*.xml' \) 2>/dev/null)
    ran=$(sum_attribute tests "${reports[@]}")
    failures=$(sum_attribute failures "${reports[@]}")
    errors=$(sum_attribute errors "${reports[@]}")
    skipped=$(sum_attribute skipped "${reports[@]}")
    mapfile -t test_folders < <(find "$root" -type d -path '*/src/test' 2>/dev/null)
    written=0
    if [ "${#test_folders[@]}" -gt 0 ]; then
        written=$(grep -rE '^\s*@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b' --include='*.java' \
            "${test_folders[@]}" 2>/dev/null | wc -l)
    fi
    mapfile -t csvs < <(find "$root" -path '*/target/site/jacoco/jacoco.csv' 2>/dev/null)

    if [ "$maven_exit" -ne 0 ] && [ "$ran" -eq 0 ]; then
        echo "Resultado: o build falhou antes dos testes (Maven saiu com $maven_exit). O fim do log:"
    else
        echo "Resultado: medido. $ran testes rodaram ($failures falhas, $errors erros, $skipped pulados);" \
            "$written métodos de teste escritos no código."
    fi
    echo
    if [ "$maven_exit" -ne 0 ]; then
        echo "O Maven saiu com $maven_exit. Os erros do log:"
        echo '```'
        grep -E '^\[ERROR\]' "$measures/$(result_name tests-maven "$project").log" | head -30
        echo '```'
        echo
    fi

    echo "## A cobertura (JaCoCo)"
    echo
    if [ "${#csvs[@]}" -eq 0 ]; then
        echo "Sem relatório do JaCoCo: o agente do JaCoCo não entrou nos testes (por exemplo, um \`argLine\` no" \
            "surefire sem \`@{argLine}\`), ou nenhum teste rodou."
    else
        awk -F, 'FNR > 1 {lm += $8; lc += $9; bm += $6; bc += $7}
            END {printf "- linhas: %d de %d (%.1f%%)\n- ramos (if/switch): %d de %d (%.1f%%)\n",
                 lc, lm + lc, (lm + lc) ? 100 * lc / (lm + lc) : 0, bc, bm + bc, (bm + bc) ? 100 * bc / (bm + bc) : 0}' \
            "${csvs[@]}"
        echo
        echo "| pacote | linhas cobertas | ramos cobertos |"
        echo "|---|---|---|"
        awk -F, 'FNR > 1 {lm[$2] += $8; lc[$2] += $9; bm[$2] += $6; bc[$2] += $7}
            END {for (p in lm) printf "| `%s` | %d de %d | %d de %d |\n", p, lc[p], lm[p] + lc[p], bc[p], bm[p] + bc[p]}' \
            "${csvs[@]}" | sort
    fi
    echo

    echo "## Os testes por classe"
    echo
    if [ "${#reports[@]}" -eq 0 ]; then
        echo "Nenhum relatório de teste (nenhum teste rodou)."
    else
        echo "| classe de teste | testes | falhas + erros | pulados |"
        echo "|---|---|---|---|"
        for report in "${reports[@]}"; do
            name=$(grep -o '<testsuite [^>]*' "$report" | grep -o ' name="[^"]*"' | head -1 | cut -d'"' -f2)
            echo "| \`$name\` | $(sum_attribute tests "$report") | $(( $(sum_attribute failures "$report") \
                + $(sum_attribute errors "$report") )) | $(sum_attribute skipped "$report") |"
        done | sort
    fi
    echo

    failed_tests=$(grep -E '^\[ERROR\]   [A-Za-z_]' "$measures/$(result_name tests-maven "$project").log" | head -20)
    if [ -n "$failed_tests" ]; then
        echo "## Os testes que falharam"
        echo
        echo '```'
        echo "$failed_tests"
        echo '```'
        echo
    fi

    echo "## Os tipos de teste (pelo código dos testes)"
    echo
    mapfile -t test_files < <(find "$root" -path '*/src/test/*' -name '*.java' 2>/dev/null)
    count_files() {
        [ "${#test_files[@]}" -eq 0 ] && { echo 0; return; }
        grep -lE "$1" "${test_files[@]}" 2>/dev/null | wc -l
    }
    echo "- arquivos de teste: ${#test_files[@]}"
    echo "- pela API (HttpClient, JavalinTest, RestAssured, HttpURLConnection): $(count_files 'HttpClient|JavalinTest|RestAssured|HttpURLConnection|okhttp')"
    echo "- contra o banco (DataSource, JDBC, Testcontainers): $(count_files 'DataSource|DriverManager|java\.sql|Testcontainers|PostgreSQLContainer|DB_URL|Database\b')"
    echo "- com H2 ou outro banco em memória: $(count_files 'jdbc:h2|H2Database|hsqldb|derby')"
    echo "- com mocks (Mockito, mock(...)): $(count_files 'org\.mockito|\bmock\(')"
    echo "- com Thread.sleep: $(count_files 'Thread\.sleep')"
    echo
    echo "Os arquivos:"
    echo '```'
    for file in "${test_files[@]}"; do
        echo "$(grep -cE '^\s*@(Test|ParameterizedTest|RepeatedTest|TestFactory)\b' "$file") testes  ${file#"$project/"}"
    done | sort -rn
    echo '```'
} > "$out"
echo "tests: $out"
