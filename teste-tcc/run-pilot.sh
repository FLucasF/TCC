#!/usr/bin/env bash
# run-pilot.sh
# Piloto TCC - harness vs default em cenario de padroes de projeto.
#
# Materiais (prompt.txt, regra-b.md) sao lidos do diretorio DESTE script.
# O workspace das runs (--base) e outra coisa: precisa estar fora de
# qualquer arvore que contenha CLAUDE.md, porque o CLI sobe a arvore de
# diretorios concatenando todo CLAUDE.md que encontra.
#
# Uso:
#   ./run-pilot.sh --base ~/eval --reps 5 --pairs
#   ./run-pilot.sh --base ~/eval --reps 3 --shuffle
#   ./run-pilot.sh --base ~/eval --dry-run
#
# Roda em Linux, macOS, WSL e Git Bash. Requer bash, o CLI do Claude Code,
# e jq ou Python para ler o JSON de saida.

set -uo pipefail

# --------------------------------------------------------------------
# Parametros
# --------------------------------------------------------------------

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"

BASE="$HOME/eval"
REPS=3
MODELOS="claude-opus-5 claude-sonnet-5"
SHUFFLE=0
PAIRS=0
DRY_RUN=0
CLAUDE_BIN="${CLAUDE_BIN:-claude}"
TIMEOUT_S=3600
SEED=""
ALLOW_PROVIDER_OVERRIDE=0
SKIP_AUTH_CHECK=0
ISOLAR_MEM=1

uso() {
    sed -n '2,20p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
    exit "${1:-0}"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --base)      BASE="$2"; shift 2 ;;
        --reps)      REPS="$2"; shift 2 ;;
        --models)    MODELOS="$2"; shift 2 ;;
        --claude-bin) CLAUDE_BIN="$2"; shift 2 ;;
        --timeout)   TIMEOUT_S="$2"; shift 2 ;;
        --seed)      SEED="$2"; shift 2 ;;
        --shuffle)   SHUFFLE=1; shift ;;
        # Roda A e B da mesma repeticao ao mesmo tempo. Elimina o
        # confundimento de ordem e horario entre as condicoes; em troca,
        # dois builds Maven concorrentes contaminam a duracao.
        --pairs)     PAIRS=1; shift ;;
        --dry-run)   DRY_RUN=1; shift ;;
        --allow-provider-override) ALLOW_PROVIDER_OVERRIDE=1; shift ;;
        --skip-auth-check) SKIP_AUTH_CHECK=1; shift ;;
        --keep-user-memory) ISOLAR_MEM=0; shift ;;
        -h|--help)   uso 0 ;;
        *) echo "Parametro desconhecido: $1" >&2; uso 1 ;;
    esac
done

vermelho() { printf '\033[31m%s\033[0m\n' "$*" >&2; }
amarelo()  { printf '\033[33m%s\033[0m\n' "$*" >&2; }
verde()    { printf '\033[32m%s\033[0m\n' "$*"; }
ciano()    { printf '\033[36m%s\033[0m\n' "$*"; }
morrer()   { vermelho "ERRO: $*"; exit 1; }

case "$REPS" in ''|*[!0-9]*) echo "--reps precisa ser inteiro positivo: '$REPS'" >&2; exit 1 ;; esac
[ "$REPS" -ge 1 ] || { echo "--reps precisa ser >= 1" >&2; exit 1; }
case "$TIMEOUT_S" in ''|*[!0-9]*) echo "--timeout precisa ser inteiro (segundos): '$TIMEOUT_S'" >&2; exit 1 ;; esac
[ -n "$MODELOS" ] || { echo "--models nao pode ser vazio" >&2; exit 1; }
if [ "$PAIRS" -eq 1 ] && [ "$SHUFFLE" -eq 1 ]; then
    amarelo "--shuffle com --pairs so embaralha a ordem dos pares; dentro do par A e B saem juntos."
fi

PROMPT_FILE="$SCRIPT_DIR/prompt.txt"
REGRA_B="$SCRIPT_DIR/regra-b.md"

[ -f "$PROMPT_FILE" ] || morrer "nao achei $PROMPT_FILE"
[ -f "$REGRA_B" ]     || morrer "nao achei $REGRA_B"

# O molde da condicao B NAO pode se chamar CLAUDE.md dentro da arvore de
# trabalho: se chamasse, seria carregado como memoria de projeto e vazaria
# para as duas condicoes. Por isso regra-b.md, copiado com renome na hora.
if [ -f "$SCRIPT_DIR/CLAUDE.md" ]; then
    amarelo "Existe CLAUDE.md no diretorio dos materiais ($SCRIPT_DIR)."
    amarelo "Ele nao afeta as runs (que vivem em --base), mas confunde: renomeie para regra-b.md."
fi

# --------------------------------------------------------------------
# Preflight: o CLI
# --------------------------------------------------------------------

BIN_PATH="$(command -v "$CLAUDE_BIN" 2>/dev/null || true)"
if [ -z "$BIN_PATH" ]; then
    vermelho "Nao achei o executavel '$CLAUDE_BIN' no PATH."
    vermelho "O app desktop embute o proprio CLI e nao expoe o binario headless."
    vermelho "Instale-o antes do lote:  npm i -g @anthropic-ai/claude-code"
    vermelho "Ou aponte o caminho:      --claude-bin /caminho/para/claude"
    exit 1
fi

# Atalhos que reescrevem o ambiente: claude-max imprime banner em stdout
# (corrompe o --output-format json) e claude-free redireciona para outro
# provedor. Nenhum dos dois pode ser o binario medido.
case "$(basename "$BIN_PATH")" in
    claude-free*|claude-max*)
        morrer "'$BIN_PATH' e um wrapper que injeta flags, imprime banner e/ou troca de provedor. Use o binario 'claude' direto."
        ;;
esac

# --------------------------------------------------------------------
# Preflight: provedor
# --------------------------------------------------------------------

problemas=""
if [ -n "${ANTHROPIC_BASE_URL:-}" ] && \
   ! printf '%s' "$ANTHROPIC_BASE_URL" | grep -qi '^https://api\.anthropic\.com/*$'; then
    problemas="$problemas\n  ANTHROPIC_BASE_URL=$ANTHROPIC_BASE_URL"
fi
[ -n "${ANTHROPIC_MODEL:-}" ]      && problemas="$problemas\n  ANTHROPIC_MODEL=$ANTHROPIC_MODEL (sobrepoe o --model)"
[ -n "${ANTHROPIC_AUTH_TOKEN:-}" ] && problemas="$problemas\n  ANTHROPIC_AUTH_TOKEN definido"
[ -n "${ANTHROPIC_API_KEY:-}" ]    && problemas="$problemas\n  ANTHROPIC_API_KEY definido (cobranca por API, nao assinatura)"

if [ -n "$problemas" ]; then
    if [ "$ALLOW_PROVIDER_OVERRIDE" -eq 1 ]; then
        amarelo "Ambiente de provedor alterado (aceito por --allow-provider-override):"
        printf '%b\n' "$problemas" >&2
    else
        vermelho "Ambiente de provedor alterado - o lote mediria outra coisa:"
        printf '%b\n' "$problemas" >&2
        morrer "limpe essas variaveis, ou passe --allow-provider-override se for deliberado."
    fi
fi

# --------------------------------------------------------------------
# Preflight: leitor de JSON
# --------------------------------------------------------------------

PARSER=""
if command -v jq >/dev/null 2>&1; then
    PARSER="jq"
else
    # python3 no Windows costuma ser o stub da Microsoft Store: resolve no
    # PATH e nao executa. Detecta rodando, nao localizando.
    for cand in python python3 py; do
        if command -v "$cand" >/dev/null 2>&1 && "$cand" -c 'import json' >/dev/null 2>&1; then
            PARSER="$cand"; break
        fi
    done
fi
[ -n "$PARSER" ] || morrer "preciso de jq ou de um Python funcional para ler o JSON das runs."

# Emite: input|cache_creation|cache_read|total_input|output|turns|duration_ms|cost|is_error
parse_result() {
    local arq="$1"
    if [ "$PARSER" = "jq" ]; then
        jq -r '
          (.usage.input_tokens // 0) as $i
          | (.usage.cache_creation_input_tokens // 0) as $cc
          | (.usage.cache_read_input_tokens // 0) as $cr
          | [$i, $cc, $cr, ($i + $cc + $cr),
             (.usage.output_tokens // 0), (.num_turns // 0),
             (.duration_ms // 0), (.total_cost_usd // ""),
             (.is_error // false)]
          | map(tostring) | join("|")' "$arq"
    else
        "$PARSER" - "$arq" <<'PY'
import json, sys
try:
    with open(sys.argv[1], encoding="utf-8-sig") as f:
        d = json.load(f)
except Exception:
    sys.exit(1)
u = d.get("usage") or {}
def n(x):
    try:
        return int(x or 0)
    except (TypeError, ValueError):
        return 0
i, cc, cr = n(u.get("input_tokens")), n(u.get("cache_creation_input_tokens")), n(u.get("cache_read_input_tokens"))
campos = [i, cc, cr, i + cc + cr, n(u.get("output_tokens")),
          n(d.get("num_turns")), n(d.get("duration_ms")),
          d.get("total_cost_usd", ""), str(bool(d.get("is_error", False))).lower()]
print("|".join(str(c) for c in campos))
PY
    fi
}

# --------------------------------------------------------------------
# Preflight: isolamento de contexto
#
# Diretorio vazio nao e contexto vazio. O CLI sobe a arvore concatenando
# todo CLAUDE.md, le ~/.claude/CLAUDE.md em qualquer projeto, e grava
# auto memory por projeto. Nada disso aparece na listagem de arquivos:
# confirme com /context dentro de um diretorio de run.
# --------------------------------------------------------------------

RUNS_ROOT="$BASE/runs"
CSV="$BASE/runs.csv"
META="$BASE/runs-meta.txt"

mkdir -p "$RUNS_ROOT" || morrer "nao consegui criar $RUNS_ROOT"
RUNS_ROOT="$(cd "$RUNS_ROOT" && pwd -P)" || morrer "nao consegui entrar em $RUNS_ROOT"
BASE="$(cd "$BASE" && pwd -P)" || morrer "nao consegui entrar em $BASE"
# Sem isso, um BASE vazio transformaria os rm -rf abaixo em caminhos na raiz.
[ -n "$BASE" ] && [ -n "$RUNS_ROOT" ] || morrer "BASE ou RUNS_ROOT vazio apos resolver o caminho"
CFG_ROOT="$BASE/_cfg"
mkdir -p "$CFG_ROOT"

achados=""
probe="$RUNS_ROOT"
while : ; do
    for nome in CLAUDE.md CLAUDE.local.md; do
        if [ -f "$probe/$nome" ]; then
            achados="$achados\n  $probe/$nome"
        fi
    done
    pai="$(dirname "$probe")"
    if [ "$pai" = "$probe" ]; then break; fi   # inclui a raiz do sistema
    probe="$pai"
done

if [ -n "$achados" ]; then
    vermelho "CLAUDE.md em diretorio ancestral - vazaria nas DUAS condicoes:"
    printf '%b\n' "$achados" >&2
    morrer "aponte --base para uma arvore limpa (ex: ~/eval) e rode de novo."
fi

MEM_USUARIO="$HOME/.claude/CLAUDE.md"
MEM_GUARDADA=""
MEM_PRESENTE="nao"
FILHOS=""

# Restauracao garantida: a memoria volta ao lugar em saida normal, erro,
# Ctrl-C ou kill, e as runs em andamento sao encerradas junto.
limpar() {
    for pid in $FILHOS; do
        kill "$pid" 2>/dev/null
    done
    if [ -n "$MEM_GUARDADA" ] && [ -f "$MEM_GUARDADA" ]; then
        mv "$MEM_GUARDADA" "$MEM_USUARIO" && MEM_GUARDADA=""
        amarelo "Memoria de usuario restaurada em $MEM_USUARIO"
    fi
}
trap limpar EXIT INT TERM

if [ -f "$MEM_USUARIO" ]; then
    MEM_PRESENTE="SIM"
    if [ "$ISOLAR_MEM" -eq 1 ]; then
        MEM_GUARDADA="$MEM_USUARIO.piloto-off"
        [ -e "$MEM_GUARDADA" ] && morrer "ja existe $MEM_GUARDADA (lote anterior interrompido?). Resolva a mao antes."
        mv "$MEM_USUARIO" "$MEM_GUARDADA" || morrer "nao consegui mover $MEM_USUARIO"
        MEM_PRESENTE="SIM (isolada durante o lote)"
        amarelo "Memoria de usuario movida para $MEM_GUARDADA durante o lote."
    else
        amarelo "Existe memoria de usuario em $MEM_USUARIO e --keep-user-memory esta ativo."
        amarelo "Ela entra nas DUAS condicoes e some com parte do efeito medido."
    fi
fi

# Auto memory. O nome correto e CLAUDE_CODE_DISABLE_AUTO_MEMORY: verificado
# no binario 2.1.269, onde "DISABLE_AUTO_MEMORY" so aparece como sufixo dele.
# Um script que exporte a forma curta nao desliga nada e nao avisa.
export CLAUDE_CODE_DISABLE_AUTO_MEMORY=1

# Cada run recebe um CLAUDE_CONFIG_DIR proprio e NOVO. Isola settings,
# skills, agents, hooks, plugins e MCP de usuario - e, alem disso, garante
# que nenhuma run herde sessao, historico ou estado de outra.
prepara_config() {
    local destino="$1"
    rm -rf "$destino"
    mkdir -p "$destino"
    # A credencial mora sob o config dir: sem copiar, a run nao autentica.
    for cred in .credentials.json .credentials; do
        if [ -f "$HOME/.claude/$cred" ]; then
            cp "$HOME/.claude/$cred" "$destino/$cred"
        fi
    done
    return 0
}

# --------------------------------------------------------------------
# Preflight: autenticacao
#
# O CLI standalone nao herda a sessao do app desktop, e a credencial mora
# sob o config dir - que este script troca por uma pasta descartavel. Sem
# checar, o lote inteiro sai com is_error e "Not logged in".
# --------------------------------------------------------------------

if [ "$SKIP_AUTH_CHECK" -eq 0 ]; then
    MODELO_SONDA="$(printf '%s' "$MODELOS" | awk '{print $1}')"
    AUTH_DIR="$BASE/_authcheck"
    rm -rf "$AUTH_DIR"; mkdir -p "$AUTH_DIR"
    prepara_config "$CFG_ROOT/_authcheck"
    (
        cd "$AUTH_DIR" || exit 1
        export CLAUDE_CONFIG_DIR="$CFG_ROOT/_authcheck"
        echo "responda exatamente: OK" \
            | "$BIN_PATH" -p \
                --model "$MODELO_SONDA" \
                --output-format json \
                --dangerously-skip-permissions > _r.json 2> _e.txt
    ) >/dev/null 2>&1 || true

    AUTH_CAMPOS="$(parse_result "$AUTH_DIR/_r.json" 2>/dev/null || true)"
    AUTH_ERR="$(printf '%s' "$AUTH_CAMPOS" | awk -F'|' '{print $9}')"
    if [ -z "$AUTH_CAMPOS" ] || [ "$AUTH_ERR" = "true" ]; then
        vermelho "Sonda de autenticacao falhou - o lote sairia todo com is_error."
        if [ -f "$AUTH_DIR/_r.json" ]; then
            if [ "$PARSER" = "jq" ]; then
                MOTIVO="$(jq -r '.result // .error // "sem detalhe"' "$AUTH_DIR/_r.json" 2>/dev/null)"
            else
                MOTIVO="$("$PARSER" -c 'import json,sys;d=json.load(open(sys.argv[1],encoding="utf-8-sig"));print(d.get("result") or d.get("error") or "sem detalhe")' "$AUTH_DIR/_r.json" 2>/dev/null)"
            fi
            vermelho "Resposta do CLI: ${MOTIVO:-sem detalhe}"
        fi
        vermelho "Rode 'claude' interativo e faca /login, depois tente de novo."
        exit 1
    fi
    rm -rf "$AUTH_DIR" "$CFG_ROOT/_authcheck"
    verde "auth      : ok"
fi

# --------------------------------------------------------------------
# Plano de execucao
# --------------------------------------------------------------------

# Em modo par, a unidade e (modelo, rep) e as duas condicoes saem juntas.
plano=""
if [ "$PAIRS" -eq 1 ]; then
    for m in $MODELOS; do
        rep=1
        while [ "$rep" -le "$REPS" ]; do
            plano="$plano$m PAR $rep"$'\n'
            rep=$((rep + 1))
        done
    done
else
    for m in $MODELOS; do
        for c in A B; do
            rep=1
            while [ "$rep" -le "$REPS" ]; do
                plano="$plano$m $c $rep"$'\n'
                rep=$((rep + 1))
            done
        done
    done
fi
plano="$(printf '%s' "$plano" | grep -v '^$' || true)"

if [ "$SHUFFLE" -eq 1 ]; then
    # Semente registrada: embaralhar sem poder reproduzir a ordem
    # transforma um controle em ruido nao documentado.
    [ -n "$SEED" ] || SEED="$(date +%s)"
    if command -v shuf >/dev/null 2>&1; then
        plano="$(printf '%s\n' "$plano" | shuf --random-source=<(yes "$SEED"))"
    else
        plano="$(printf '%s\n' "$plano" | awk -v seed="$SEED" 'BEGIN{srand(seed)}{printf "%.10f\t%s\n", rand(), $0}' | sort -k1,1n | cut -f2-)"
    fi
fi

CLI_VER="$("$BIN_PATH" --version 2>/dev/null | head -1 || echo "desconhecida")"
UNIDADES="$(printf '%s\n' "$plano" | grep -c . || true)"
if [ "$PAIRS" -eq 1 ]; then TOTAL=$((UNIDADES * 2)); else TOTAL="$UNIDADES"; fi

ciano "base      : $BASE"
ciano "materiais : $SCRIPT_DIR"
ciano "cli       : $BIN_PATH  ($CLI_VER)"
ciano "parser    : $PARSER"
ciano "modelos   : $MODELOS"
ciano "runs      : $TOTAL (${REPS} por celula)"
if [ "$PAIRS" -eq 1 ]; then ciano "modo      : pares A/B simultaneos ($UNIDADES pares)"; fi
if [ -n "$SEED" ]; then ciano "seed      : $SEED"; fi

if [ "$DRY_RUN" -eq 1 ]; then
    echo
    ciano "--- ordem planejada (dry-run, nada executado) ---"
    printf '%s\n' "$plano" | while read -r m c r; do
        if [ "$c" = "PAR" ]; then echo "  $m-A-$r  ||  $m-B-$r"; else echo "  $m-$c-$r"; fi
    done
    exit 0
fi

# --------------------------------------------------------------------
# Execucao
# --------------------------------------------------------------------

HEADER="run_id,timestamp,modelo,condicao,rep,input_tokens,cache_creation,cache_read,total_input,output_tokens,num_turns,duration_ms,cost_usd,is_error,dir"
[ -f "$CSV" ] || printf '%s\n' "$HEADER" > "$CSV"

{
    echo "=== lote iniciado em $(date -Iseconds) ==="
    echo "cli            : $BIN_PATH ($CLI_VER)"
    echo "modelos        : $MODELOS"
    echo "reps           : $REPS"
    if [ "$PAIRS" -eq 1 ]; then echo "modo           : pares A/B simultaneos"; else echo "modo           : sequencial"; fi
    echo "seed           : ${SEED:-sem shuffle}"
    echo "memoria usuario: $MEM_PRESENTE ($MEM_USUARIO)"
    echo "config dir     : um por run, sob $CFG_ROOT"
    echo "base url       : ${ANTHROPIC_BASE_URL:-<vazio>}"
} >> "$META"

TIMEOUT_BIN=""
if command -v timeout >/dev/null 2>&1; then TIMEOUT_BIN="timeout"; fi

# Executa uma run inteira. Escreve a linha de CSV num arquivo proprio, em
# vez de no CSV compartilhado: com duas runs concorrentes, dois appends
# simultaneos poderiam se entrelacar.
executa_run() {
    local modelo="$1" cond="$2" rep="$3"
    local run_id="$modelo-$cond-$rep"
    local dir="$RUNS_ROOT/$run_id"
    local cfg="$CFG_ROOT/$run_id"

    mkdir -p "$dir"
    prepara_config "$cfg"

    # Condicao A = diretorio vazio (full default).
    # Condicao B = mesmo diretorio + a regra unica no CLAUDE.md.
    if [ "$cond" = "B" ]; then cp "$REGRA_B" "$dir/CLAUDE.md"; fi

    local rc=0
    (
        cd "$dir" || exit 1
        export CLAUDE_CONFIG_DIR="$cfg"
        if [ -n "$TIMEOUT_BIN" ]; then
            $TIMEOUT_BIN "$TIMEOUT_S" "$BIN_PATH" -p \
                --model "$modelo" \
                --output-format json \
                --dangerously-skip-permissions \
                < "$PROMPT_FILE" > _result.json 2> _stderr.txt
        else
            "$BIN_PATH" -p \
                --model "$modelo" \
                --output-format json \
                --dangerously-skip-permissions \
                < "$PROMPT_FILE" > _result.json 2> _stderr.txt
        fi
    ) || rc=$?

    if [ "$rc" -ne 0 ]; then
        amarelo "$run_id : CLI saiu com codigo $rc. Ver $dir/_stderr.txt"
    fi

    local campos
    campos="$(parse_result "$dir/_result.json" 2>/dev/null || true)"
    if [ -z "$campos" ]; then
        amarelo "$run_id : JSON invalido ou vazio, sem linha no CSV. Ver $dir/_result.json"
        return 1
    fi

    local i cc cr total out turns dur cost is_err
    IFS='|' read -r i cc cr total out turns dur cost is_err <<EOF
$campos
EOF

    printf '%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,"%s"\n' \
        "$run_id" "$(date -Iseconds)" "$modelo" "$cond" "$rep" \
        "$i" "$cc" "$cr" "$total" "$out" "$turns" "$dur" "$cost" "$is_err" "$dir" \
        > "$dir/_linha.csv"

    if [ "$is_err" = "true" ]; then
        amarelo "$run_id : is_error=true"
        return 2
    fi
    verde "$run_id  ok  $((dur / 1000))s  in=$total  out=$out  turns=$turns"
    return 0
}

# Ja registrada no CSV = completa. Diretorio sem linha no CSV = run
# interrompida: nao conta e nao e sobrescrita em silencio.
pular() {
    local run_id="$1"
    if grep -q "^$run_id," "$CSV" 2>/dev/null; then
        echo "  $run_id ja registrado, pulando."
        return 0
    fi
    if [ -d "$RUNS_ROOT/$run_id" ]; then
        amarelo "$run_id : diretorio existe sem linha no CSV (run incompleta). Apague para refazer."
        return 0
    fi
    return 1
}

# O stdout do CLI vai para _result.json e o --output-format json so emite
# no fim, entao a run inteira roda sem imprimir nada. Sem sinal de vida o
# terminal parece travado por minutos.
pulso() {
    local inicio="$1"; shift
    local vivo pid
    while : ; do
        vivo=0
        for pid in "$@"; do
            if kill -0 "$pid" 2>/dev/null; then vivo=1; fi
        done
        if [ "$vivo" -eq 0 ]; then break; fi
        sleep 30
        vivo=0
        for pid in "$@"; do
            if kill -0 "$pid" 2>/dev/null; then vivo=1; fi
        done
        if [ "$vivo" -eq 0 ]; then break; fi
        printf '\r  rodando... %sm' "$((($(date +%s) - inicio) / 60))"
    done
    printf '\r%*s\r' 30 ''
}

FALHAS_SEGUIDAS=0
FEITAS=0

while read -r modelo cond rep; do
    if [ -z "${modelo:-}" ]; then continue; fi

    if [ "$cond" = "PAR" ]; then
        pendentes=""
        for c in A B; do
            if ! pular "$modelo-$c-$rep"; then pendentes="$pendentes $c"; fi
        done
        if [ -z "$pendentes" ]; then continue; fi

        echo
        ciano "=== $modelo rep $rep : A || B ==="
        inicio=$(date +%s); FILHOS=""
        for c in $pendentes; do
            executa_run "$modelo" "$c" "$rep" &
            FILHOS="$FILHOS $!"
        done
        pulso "$inicio" $FILHOS
        erros=0
        for pid in $FILHOS; do
            if ! wait "$pid"; then erros=$((erros + 1)); fi
        done
        FILHOS=""
        for c in $pendentes; do
            linha="$RUNS_ROOT/$modelo-$c-$rep/_linha.csv"
            if [ -f "$linha" ]; then cat "$linha" >> "$CSV"; FEITAS=$((FEITAS + 1)); fi
        done
        if [ "$erros" -ge 2 ]; then
            FALHAS_SEGUIDAS=$((FALHAS_SEGUIDAS + 2))
        else
            FALHAS_SEGUIDAS="$erros"
        fi
    else
        run_id="$modelo-$cond-$rep"
        if pular "$run_id"; then continue; fi
        echo
        ciano "=== $run_id ==="
        inicio=$(date +%s); FILHOS=""
        executa_run "$modelo" "$cond" "$rep" &
        FILHOS="$!"
        pulso "$inicio" $FILHOS
        rc=0; wait $FILHOS || rc=$?
        FILHOS=""
        if [ -f "$RUNS_ROOT/$run_id/_linha.csv" ]; then
            cat "$RUNS_ROOT/$run_id/_linha.csv" >> "$CSV"; FEITAS=$((FEITAS + 1))
        fi
        if [ "$rc" -ne 0 ]; then FALHAS_SEGUIDAS=$((FALHAS_SEGUIDAS + 1)); else FALHAS_SEGUIDAS=0; fi
    fi

    # Duas falhas seguidas quase nunca sao azar: e cota estourada, sessao
    # expirada ou provedor fora do ar. Continuar so enche o CSV de linhas
    # invalidas e queima o desenho balanceado.
    if [ "$FALHAS_SEGUIDAS" -ge 2 ]; then
        vermelho "Duas runs seguidas com falha - lote interrompido."
        vermelho "Investigue antes de continuar; ao retomar, o que ja esta no CSV e pulado."
        break
    fi
done <<PLANO
$plano
PLANO

echo
{ echo "runs concluidas: $FEITAS"; echo "=== lote encerrado em $(date -Iseconds) ==="; echo; } >> "$META"

verde "$FEITAS runs concluidas"
verde "CSV : $CSV"
verde "Meta: $META"
echo
if [ "$REPS" -lt 4 ]; then
    amarelo "Lembrete: com $REPS repeticoes por celula o Mann-Whitney nao alcanca"
    amarelo "p<0,05 - com n=3 contra n=3 o menor p bilateral possivel e 0,10."
    amarelo "O piso para ter chance e 4 por celula."
    echo
fi
if [ "$PAIRS" -eq 1 ]; then
    amarelo "Modo par: A e B rodaram simultaneos, entao a duracao carrega contencao"
    amarelo "de CPU entre os dois. Use turnos, tokens e rubrica como desfecho."
    echo
fi
verde "Proximo passo: ./metricas.sh --base \"$BASE\"  e  ./anonimizar.sh --base \"$BASE\""
