#!/usr/bin/env bash
# Le o desenho de cada pacote com o Semgrep e responde as perguntas da regua
# enxuta (P1 a P4: localizacao e selecao; P5: forma e proporcao).
#
# Uso (Git Bash, da raiz do TCC_V3):
#   evaluation/tools/semgrep/detect.sh <raiz-dos-pacotes> <saida.csv>
#   evaluation/tools/semgrep/detect.sh evaluation/strategy/packages analysis/semgrep-EXT.csv
#
# <raiz-dos-pacotes> tem uma subpasta por pacote, cada uma com src/main/java.
# Deterministico e sem IA: o mesmo pacote da sempre a mesma resposta. Recusa
# rodar se as regras nao forem as geradas pelo gerar-regras.mjs atual, ou se a
# saida ja existir (um resultado nunca e refeito por cima de outro).

set -uo pipefail
morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

[ $# -eq 2 ] || morrer "uso: $0 <raiz-dos-pacotes> <saida.csv>"
PACOTES="$1"; SAIDA="$2"
[ -d "$PACOTES" ] || morrer "pasta nao encontrada: $PACOTES"
[ -e "$SAIDA" ] && morrer "$SAIDA ja existe"

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
# A versao travada. Trocar a imagem e um instrumento novo.
IMAGEM="semgrep/semgrep@sha256:30e6afa99ebd8e7b4115d4904898108eb4bf77025819e9263f09cf14e6f6e549"

# As regras em uso tem de ser exatamente as que o gerador produz.
node "$AQUI/gerar-regras.mjs" | cmp -s - "$AQUI/regras.yml" \
    || morrer "regras.yml difere do gerar-regras.mjs: gere de novo antes de rodar"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
ABS_PACOTES="$(cd "$PACOTES" && pwd -P)"
MSYS_NO_PATHCONV=1 docker run --rm \
    --mount "type=bind,source=$(cygpath -w "$ABS_PACOTES" 2>/dev/null || echo "$ABS_PACOTES"),target=/pacotes,readonly" \
    --mount "type=bind,source=$(cygpath -w "$AQUI" 2>/dev/null || echo "$AQUI"),target=/regras,readonly" \
    "$IMAGEM" semgrep --config /regras/regras.yml --json --metrics=off --quiet --no-git-ignore /pacotes \
    > "$TMP/saida.json" || morrer "o Semgrep falhou"

# Sem MSYS_NO_PATHCONV, o Git Bash troca "/pacotes" por "C:/Program Files/Git/pacotes";
# com ele, os outros caminhos vao convertidos a mao (cygpath -m), quando houver cygpath.
win() { cygpath -m "$1" 2>/dev/null || echo "$1"; }
MSYS_NO_PATHCONV=1 node "$(win "$AQUI/classificar.mjs")" "$(win "$TMP/saida.json")" "$(win "$ABS_PACOTES")" /pacotes \
    > "$TMP/saida.csv" || morrer "classificacao falhou"
mkdir -p "$(dirname "$SAIDA")"
{
    echo "# regras $(sha256sum "$AQUI/regras.yml" | cut -c1-16)  classificador $(sha256sum "$AQUI/classificar.mjs" | cut -c1-16)  imagem ${IMAGEM##*:}"
    cat "$TMP/saida.csv"
} > "$SAIDA"
echo "$(($(wc -l < "$SAIDA") - 2)) pacotes -> $SAIDA"
