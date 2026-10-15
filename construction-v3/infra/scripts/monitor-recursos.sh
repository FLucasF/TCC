#!/usr/bin/env bash
# Anota o consumo de CPU e de memoria enquanto as rodadas andam: de cada container do
# experimento e da maquina inteira. Medida DESCRITIVA, acrescentada em 10/10/2026 a
# pedido do Lucas, depois do congelamento do V5: nenhuma hipotese a usa, e ela nao
# toca em nada do que roda (so le o `docker stats` e o Windows).
#
# Uso (Git Bash, deixado rodando em segundo plano durante as rodadas):
#   infra/scripts/monitor-recursos.sh <pasta-de-saida> [intervalo-em-segundos]
#   infra/scripts/monitor-recursos.sh /j/TCC/TCC_V5/runs/recursos 15
#
# Para parar: criar o arquivo <pasta-de-saida>/PARAR (ou matar o processo).
#
# COMO LER. A cada intervalo, duas linhas de medida:
#   1. containers.csv: uma linha por container da imagem do experimento em execucao.
#      O container do agente se chama exp-<run_id> (o run-one.sh da esse nome); o do
#      build final nao tem nome, e e reconhecido pela pasta montada
#      (runs/<run_id>/workspace). cpu_pct e o do `docker stats`: 100 = um nucleo
#      inteiro (com 20 nucleos, o maximo e 2000). mem_mib e a memoria usada pelo
#      container, e pids, os processos dentro dele (o Maven e o Java aparecem aqui).
#   2. maquina.csv: a CPU total da maquina (0 a 100, todos os nucleos), a RAM
#      DISPONIVEL (livre + o cache que o Windows devolve quando precisa: a que diz se vai
#      faltar memoria) e a total, e a memoria comprometida (%). Inclui tudo o que roda no
#      PC (um jogo, o navegador): e o contexto para ler um tempo de relogio mais lento,
#      nao uma medida do agente.
# Os tempos sao em UTC (ISO 8601), como o timing.start do meta.json.

set -uo pipefail

SAIDA="${1:?uso: monitor-recursos.sh <pasta-de-saida> [intervalo-em-segundos]}"
INTERVALO="${2:-15}"
IMAGEM="experimento-harness:v5"
mkdir -p "$SAIDA"
CONT="$SAIDA/containers.csv"
MAQ="$SAIDA/maquina.csv"
[ -f "$CONT" ] || echo "momento_utc,run_id,tipo,cpu_pct,mem_mib,pids" > "$CONT"
[ -f "$MAQ" ] || echo "momento_utc,cpu_total_pct,ram_disponivel_gb,ram_total_gb,memoria_comprometida_pct" > "$MAQ"

# O comando do PowerShell entre aspas simples: o bash nao mexe em nada dentro dele.
PS_MAQUINA='$p = Get-CimInstance Win32_PerfFormattedData_PerfOS_Memory
$o = Get-CimInstance Win32_OperatingSystem
$c = (Get-CimInstance Win32_Processor | Measure-Object -Property LoadPercentage -Average).Average
[string]::Format([Globalization.CultureInfo]::InvariantCulture, "{0},{1:0.00},{2:0.00},{3}", [int]$c, ($p.AvailableMBytes / 1024), ($o.TotalVisibleMemorySize / 1MB), $p.PercentCommittedBytesInUse)'

# Converte o "123.4MiB" / "1.2GiB" / "512kB" do docker stats para MiB.
para_mib() {
    awk -v v="$1" 'BEGIN {
        n = v + 0; u = v; sub(/^[0-9.]+/, "", u)
        if (u ~ /^GiB/) n *= 1024; else if (u ~ /^kB|^KiB/) n /= 1024; else if (u ~ /^B$/) n /= 1048576
        else if (u ~ /^GB/) n *= 953.674; else if (u ~ /^MB/) n *= 0.953674
        printf "%.1f", n }'
}

# O run_id de um container: pelo nome exp-<run_id>, ou pela pasta runs/<run_id>/workspace montada.
quem() {
    local nome="$1" id="$2" fonte run
    case "$nome" in exp-*) echo "${nome#exp-},agente"; return ;; esac
    fonte="$(docker inspect --format '{{range .Mounts}}{{.Source}} {{end}}' "$id" 2>/dev/null)"
    run="$(printf '%s' "$fonte" | tr '\\' '/' | grep -oE 'runs/[^/ ]+/workspace' | head -1 | cut -d/ -f2)"
    [ -n "$run" ] && echo "$run,build" || echo ","
}

while [ ! -f "$SAIDA/PARAR" ]; do
    agora="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    # Os containers da imagem do experimento que estao rodando agora.
    ids="$(docker ps --filter "ancestor=$IMAGEM" --format '{{.ID}}' 2>/dev/null)"
    if [ -n "$ids" ]; then
        # shellcheck disable=SC2086
        docker stats --no-stream --format '{{.ID}} {{.Name}} {{.CPUPerc}} {{.MemUsage}} {{.PIDs}}' $ids 2>/dev/null |
        while read -r id nome cpu mem _barra _limite pids; do
            dono="$(quem "$nome" "$id")"
            [ "$dono" = "," ] && continue
            echo "$agora,$dono,${cpu%\%},$(para_mib "$mem"),$pids" >> "$CONT"
        done
    fi
    # A maquina inteira (o Windows).
    maq="$(powershell.exe -NoProfile -Command "$PS_MAQUINA" 2>/dev/null | tr -d '\r ')"
    [ -n "$maq" ] && echo "$agora,$maq" >> "$MAQ"
    sleep "$INTERVALO"
done
echo "monitor parado (achou $SAIDA/PARAR)"
