#!/usr/bin/env bash
# anonimizar.sh
# Prepara a avaliacao cega. Copia o codigo de cada run para um diretorio
# com ID neutro, remove as pistas da condicao, embaralha a ordem e emite:
#
#   cego/rubrica.csv     -> voce preenche as notas olhando so o codigo
#   cego/como-pontuar.md -> a escala, para nao pontuar de memoria
#   cego/mapa.csv        -> id_cego -> run_id. NAO ABRA antes de pontuar.
#
# Uso:  ./anonimizar.sh --base ~/eval

set -uo pipefail

BASE="$HOME/eval"
SEED=""

while [ $# -gt 0 ]; do
    case "$1" in
        --base) BASE="$2"; shift 2 ;;
        --seed) SEED="$2"; shift 2 ;;
        -h|--help) sed -n '2,12p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Parametro desconhecido: $1" >&2; exit 1 ;;
    esac
done

vermelho() { printf '\033[31m%s\033[0m\n' "$*" >&2; }
amarelo()  { printf '\033[33m%s\033[0m\n' "$*" >&2; }
verde()    { printf '\033[32m%s\033[0m\n' "$*"; }
ciano()    { printf '\033[36m%s\033[0m\n' "$*"; }
morrer()   { vermelho "ERRO: $*"; exit 1; }

RUNS_ROOT="$BASE/runs"
OUT="$BASE/cego"

[ -d "$RUNS_ROOT" ] || morrer "nao achei $RUNS_ROOT"
[ -e "$OUT" ] && morrer "$OUT ja existe. Apague antes de refazer."

# Arquivos que revelam a condicao ou nao fazem parte do codigo avaliado.
EXCLUIR_NOME="CLAUDE.md CLAUDE.local.md _result.json _stderr.txt README.md readme.md .DS_Store"
# Diretorios ignorados em QUALQUER nivel do caminho, nao so na raiz da run.
EXCLUIR_DIR=".claude .git target build out node_modules .mvn .idea .vscode"

mkdir -p "$OUT"

# Ordem cega embaralhada, com semente registrada para poder reproduzir.
[ -n "$SEED" ] || SEED="$(date +%s)"
listar_runs() {
    find "$RUNS_ROOT" -mindepth 1 -maxdepth 1 -type d | sort
}
if command -v shuf >/dev/null 2>&1; then
    RUNS="$(listar_runs | shuf --random-source=<(yes "$SEED"))"
else
    RUNS="$(listar_runs | awk -v seed="$SEED" 'BEGIN{srand(seed)}{printf "%.10f\t%s\n", rand(), $0}' | sort -k1,1n | cut -f2-)"
fi

[ -n "$RUNS" ] || morrer "nenhuma run em $RUNS_ROOT"

MAPA="$OUT/mapa.csv"
RUBRICA="$OUT/rubrica.csv"
printf 'id_cego,run_id\n' > "$MAPA"
printf 'id_cego,observer,strategy,factory,overeng,notas\n' > "$RUBRICA"

i=0
while IFS= read -r run; do
    [ -n "$run" ] || continue
    i=$((i + 1))
    id_cego="$(printf 'C%02d' "$i")"
    destino="$OUT/$id_cego"
    mkdir -p "$destino"

    while IFS= read -r arq; do
        rel="${arq#$run/}"
        base_nome="$(basename "$rel")"

        pular=0
        for n in $EXCLUIR_NOME; do
            [ "$base_nome" = "$n" ] && pular=1
        done
        # Casa o diretorio em qualquer segmento do caminho relativo.
        for d in $EXCLUIR_DIR; do
            case "/$rel/" in
                */"$d"/*) pular=1 ;;
            esac
        done
        [ "$pular" -eq 1 ] && continue

        mkdir -p "$destino/$(dirname "$rel")"
        cp "$arq" "$destino/$rel"
    done <<EOF
$(find "$run" -type f | sort)
EOF

    printf '%s,%s\n' "$id_cego" "$(basename "$run")" >> "$MAPA"
    printf '%s,,,,,\n' "$id_cego" >> "$RUBRICA"
done <<EOF
$RUNS
EOF

cat > "$OUT/como-pontuar.md" <<'MD'
# Como pontuar

Olhe so o codigo. Nao abra `mapa.csv` antes de fechar todas as notas.

Criterio **estrutural, nao nominal**: se o modelo fez inversao de dependencia
com uma lista de handlers e nunca escreveu "Observer", conta como uso.

| Nota | Criterio |
|---|---|
| 0 | Condicional central sobre o tipo, ou chamada direta hardcoded |
| 1 | Abstracao extraida, mas ainda ha ponto de decisao acoplado |
| 2 | Adicionar um caso novo nao toca no codigo existente |

Tres eixos, 0 a 2 cada (total 0-6 por run):

- `observer` — despacho do evento para N interessados
- `strategy` — variacao do texto por plano do cliente
- `factory` — familias de canal com credencial e payload proprios

Metrica separada e negativa, **fora** do total:

- `overeng` — contagem inteira de padroes aplicados sem necessidade.
  Nao entra no 0-6. A regra "sempre que exigir" tende a provocar excesso,
  e isso e resultado, nao erro do experimento.

`notas` — texto livre.
MD

verde "$i runs anonimizadas em $OUT  (seed $SEED)"
echo
ciano "Checando vazamento textual nas copias..."

# Um comentario ou nome de classe pode citar a diretriz e denunciar a
# condicao. Cobre todo arquivo de texto, nao so .java/.xml/.md.
TERMOS='padr(a|ã|ao)o de projeto|design pattern|diretriz|CLAUDE\.md|harness|extens(i|í)vel|extensivel'
VAZ="$(grep -rInEi -- "$TERMOS" "$OUT" --exclude=mapa.csv --exclude=rubrica.csv --exclude=como-pontuar.md 2>/dev/null || true)"

if [ -n "$VAZ" ]; then
    amarelo "Mencoes encontradas - revise antes de pontuar:"
    printf '%s\n' "$VAZ" | cut -d: -f1,2 | sed "s|^$OUT/|  |"
else
    verde "Nenhuma mencao obvia."
fi

echo
amarelo "Limite conhecido: quem pontua e quem formulou a hipotese. A cegagem"
amarelo "esconde a condicao, nao a expectativa. Vale um segundo avaliador em"
amarelo "pelo menos parte das runs, para reportar concordancia."
echo
verde "Preencha $RUBRICA. So depois abra $MAPA."
