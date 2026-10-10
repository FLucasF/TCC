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
#
# COMO LER. Os arquivos desta pasta, na ordem em que entram:
#   pontos.mjs        os nomes dos casos de cada ponto (OURO, NORTE...), a unica lista;
#   gerar-regras.mjs  escreve o regras.yml a partir dela (nunca se edita o .yml a mao);
#   copia-limpa.mjs   faz a copia sem comentarios que o Semgrep le;
#   regras.yml        o que o Semgrep procura: cada regra marca uma LINHA de codigo
#                     (ex.: "um if que nomeia OURO", "um case NORTE ->");
#   classificar.mjs   le as linhas marcadas e responde as perguntas da regua por pacote.
# Este script so encadeia: confere as regras, faz a copia limpa, roda o Semgrep num
# container travado por hash, classifica e grava o CSV com os hashes no cabecalho.
# Cada linha do CSV tem as respostas, a evidencia (a primeira linha que contou de cada
# regra) e os avisos (trechos que o Semgrep nao conseguiu ler).

set -uo pipefail
morrer() { printf '\033[31mERRO: %s\033[0m\n' "$*" >&2; exit 1; }

[ $# -eq 2 ] || morrer "uso: $0 <raiz-dos-pacotes> <saida.csv>"
PACOTES="$1"; SAIDA="$2"
[ -d "$PACOTES" ] || morrer "pasta nao encontrada: $PACOTES"
[ -e "$SAIDA" ] && morrer "$SAIDA ja existe"

AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
# A versao travada. Trocar a imagem e um instrumento novo.
IMAGEM="semgrep/semgrep@sha256:30e6afa99ebd8e7b4115d4904898108eb4bf77025819e9263f09cf14e6f6e549"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
# Sem MSYS_NO_PATHCONV, o Git Bash troca "/pacotes" por "C:/Program Files/Git/pacotes";
# com ele, os outros caminhos vao convertidos a mao (cygpath -m), quando houver cygpath.
win() { cygpath -m "$1" 2>/dev/null || echo "$1"; }

# As regras em uso tem de ser exatamente as que o gerador produz. O gerador falhar e
# outro erro: com MSYS_NO_PATHCONV=1 no terminal, o node recebia /j/... e nao achava o
# arquivo, e a mensagem dizia "regras diferentes" (ensaio de 10/10).
node "$(win "$AQUI/gerar-regras.mjs")" > "$TMP/regras-geradas.yml" || morrer "o gerar-regras.mjs falhou"
cmp -s "$TMP/regras-geradas.yml" "$AQUI/regras.yml" \
    || morrer "regras.yml difere do gerar-regras.mjs: gere de novo antes de rodar"
ABS_PACOTES="$(cd "$PACOTES" && pwd -P)"

# O Semgrep le uma copia sem comentarios, com as linhas no mesmo lugar (09/10): as
# regras de texto nao enxergam comentario, e o arquivo:linha continua o do original.
LIMPA="$TMP/limpa"
node "$(win "$AQUI/copia-limpa.mjs")" "$(win "$ABS_PACOTES")" "$(win "$LIMPA")" || morrer "a copia sem comentarios falhou"

MSYS_NO_PATHCONV=1 docker run --rm \
    --mount "type=bind,source=$(cygpath -w "$LIMPA" 2>/dev/null || echo "$LIMPA"),target=/pacotes,readonly" \
    --mount "type=bind,source=$(cygpath -w "$AQUI" 2>/dev/null || echo "$AQUI"),target=/regras,readonly" \
    "$IMAGEM" semgrep --config /regras/regras.yml --json --metrics=off --quiet --no-git-ignore /pacotes \
    > "$TMP/saida.json" || morrer "o Semgrep falhou"

MSYS_NO_PATHCONV=1 node "$(win "$AQUI/classificar.mjs")" "$(win "$TMP/saida.json")" "$(win "$LIMPA")" /pacotes \
    > "$TMP/saida.csv" || morrer "classificacao falhou"
mkdir -p "$(dirname "$SAIDA")"
{
    echo "# regras $(sha256sum "$AQUI/regras.yml" | cut -c1-16)  classificador $(sha256sum "$AQUI/classificar.mjs" | cut -c1-16)  limpeza $(sha256sum "$AQUI/../sem-comentarios.mjs" | cut -c1-16)  imagem ${IMAGEM##*:}"
    cat "$TMP/saida.csv"
} > "$SAIDA"
echo "$(($(wc -l < "$SAIDA") - 2)) pacotes -> $SAIDA"
