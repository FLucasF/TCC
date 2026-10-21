#!/usr/bin/env bash
# The project's packages and the dependencies between them, measured by jdeps on the compiled classes: how many
# classes and lines each package has, who depends on whom, the cycles, and which libraries (Javalin, JDBC, Jackson,
# Flyway...) each package touches. Writes measures/dependencies.md. Judges nothing.
#
# Usage: bash measure_dependencies.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"

project="$(cd "$1" && pwd)"
out="$(results_folder "$project")/$(result_name dependencies "$project").md"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

{
    echo "# Arquitetura: os pacotes e as dependências (jdeps)"
    echo
    root="$(project_root "$project")"
    if [ -z "$root" ]; then
        echo "Resultado: sem medida (não há pom.xml no projeto)."
        exit 0
    fi
    if ! (cd "$root" && timeout 900 mvn -B -q -DskipTests compile) > "$scratch/compile.log" 2>&1; then
        echo "Resultado: sem medida (o projeto não compila). O fim do log:"
        echo '```'
        tail -20 "$scratch/compile.log"
        echo '```'
        exit 0
    fi
    mapfile -t class_folders < <(find "$root" -path '*/target/classes' -type d -not -path '*/test-classes*')

    # the packages of the project: the folders of target/classes that have a .class
    for folder in "${class_folders[@]}"; do
        find "$folder" -name '*.class' -printf '%h\n' | sed "s|^$folder/\?||; s|/|.|g"
    done | sed 's/^$/(sem pacote)/' | sort | uniq -c | awk '{print $2, $1}' > "$scratch/packages"

    # package -> package lines from jdeps (the source and the target, whatever the archive)
    for folder in "${class_folders[@]}"; do
        jdeps -verbose:package -filter:none --multi-release 21 "$folder" 2>/dev/null
    done | awk '$2 == "->" {print $1, $3}' | sort -u > "$scratch/edges-all"

    awk 'NR == FNR {internal[$1] = 1; next} ($1 in internal) && ($2 in internal) && $1 != $2' \
        "$scratch/packages" "$scratch/edges-all" > "$scratch/edges"
    awk 'NR == FNR {internal[$1] = 1; next} ($1 in internal) && !($2 in internal)' \
        "$scratch/packages" "$scratch/edges-all" > "$scratch/external"

    package_count=$(wc -l < "$scratch/packages")
    edge_count=$(wc -l < "$scratch/edges")
    tsort "$scratch/edges" > /dev/null 2> "$scratch/loops"
    cycle_members=$(grep -vc 'input contains a loop' "$scratch/loops" || true)
    echo "Resultado: medido. $package_count pacotes, $edge_count dependências entre eles, $( \
        [ "$cycle_members" -gt 0 ] && echo "com ciclo" || echo "sem ciclo")."
    echo

    echo "## Os pacotes"
    echo
    echo "| pacote | classes | linhas de Java (src/main) |"
    echo "|---|---|---|"
    while read -r package classes; do
        path="${package//./\/}"
        [ "$package" = "(sem pacote)" ] && path=""
        source_folder=$(find "$root" -type d -path "*/src/main/java${path:+/$path}" 2>/dev/null | head -1)
        lines=0
        if [ -n "$source_folder" ]; then
            lines=$(find "$source_folder" -maxdepth 1 -name '*.java' -exec cat {} + 2>/dev/null | wc -l)
        fi
        echo "| \`$package\` | $classes | $lines |"
    done < "$scratch/packages"
    echo

    echo "## As dependências entre os pacotes do projeto"
    echo
    if [ "$edge_count" -eq 0 ]; then
        echo "Nenhuma (um pacote só, ou pacotes que não se usam)."
    else
        echo '```'
        awk '{printf "%s -> %s\n", $1, $2}' "$scratch/edges"
        echo '```'
        echo
        echo "| pacote | depende de (quantos) | é usado por (quantos) |"
        echo "|---|---|---|"
        while read -r package _; do
            fan_out=$(awk -v p="$package" '$1 == p' "$scratch/edges" | wc -l)
            fan_in=$(awk -v p="$package" '$2 == p' "$scratch/edges" | wc -l)
            echo "| \`$package\` | $fan_out | $fan_in |"
        done < "$scratch/packages"
    fi
    echo

    echo "## Os ciclos"
    echo
    if [ "$cycle_members" -eq 0 ]; then
        echo "Nenhum ciclo entre pacotes."
    else
        echo "O tsort achou ciclos; cada bloco abaixo é um ciclo (os pacotes que o formam):"
        echo '```'
        sed 's/^.*input contains a loop:$/--- um ciclo:/; s/^tsort: //' "$scratch/loops"
        echo '```'
    fi
    echo

    echo "## As bibliotecas que cada pacote usa"
    echo
    echo "Para ver em que camada mora o HTTP (Javalin), o SQL (JDBC), o JSON (Jackson) e as migrações (Flyway)."
    echo
    echo "| pacote | Javalin/Jetty | JDBC | Jackson | Flyway/Liquibase | pool | outras |"
    echo "|---|---|---|---|---|---|---|"
    while read -r package _; do
        targets=$(awk -v p="$package" '$1 == p {print $2}' "$scratch/external")
        has() { echo "$targets" | grep -Eq "$1" && echo "sim" || echo ""; }
        others=$(echo "$targets" | grep -Ev '^(java\.|javax\.sql|io\.javalin|org\.eclipse\.jetty|jakarta\.servlet|com\.fasterxml|org\.flywaydb|liquibase|com\.zaxxer|org\.postgresql|org\.slf4j|kotlin)' \
            | grep -v '^$' | sed 's/\.[^.]*$//' | sort -u | head -5 | paste -sd' ' -)
        echo "| \`$package\` | $(has '^(io\.javalin|org\.eclipse\.jetty|jakarta\.servlet)') | $(has '^(java\.sql|javax\.sql|org\.postgresql)') | $(has '^com\.fasterxml') | $(has '^(org\.flywaydb|liquibase)') | $(has '^com\.zaxxer') | $others |"
    done < "$scratch/packages"
    echo

    echo "## As maiores classes (linhas, src/main)"
    echo
    echo '```'
    find "$root" -path '*/src/main/java/*' -name '*.java' -exec wc -l {} + 2>/dev/null | grep -v ' total$' \
        | sort -rn | head -10 | sed "s|$project/||"
    echo '```'
} > "$out"
echo "dependencies: $out"
