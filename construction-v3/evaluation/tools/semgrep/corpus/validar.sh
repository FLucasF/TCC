#!/usr/bin/env bash
# Refaz a validacao do Semgrep nos quatro corpora (09/10). Cada corpus e um gerador que
# escreve pacotes pequenos, cada um com UMA forma de escrever o padrao, e o
# esperado.csv com a resposta da regua, decidida antes de rodar as regras.
#
# Uso (Git Bash, da raiz do TCC_V3, com o Docker aberto):
#   evaluation/tools/semgrep/corpus/validar.sh
#
# Sai com 1 se alguma resposta divergir do esperado. Os pacotes gerados ficam numa
# pasta temporaria e somem no fim; o repositorio guarda so os geradores.
#
#   corpus 1 (desenvolvimento)  40 pacotes que guiaram a versao 2 (a versao 1 acertou 41 de 80)
#   corpus 2 (controle)         22 formas novas, escritas depois do corpus 1, nunca vistas
#   corpus 3 (desenvolvimento)  a revisao do codigo das regras e a regressao nos pacotes reais
#   corpus 4 (desenvolvimento)  os nomes em ingles e o compareTo

set -uo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd -P)"
DETECT="$AQUI/../detect.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
win() { cygpath -m "$1" 2>/dev/null || echo "$1"; }

FALHAS=0
for gerador in "$AQUI"/corpus-*.mjs; do
    nome="$(basename "$gerador" .mjs)"
    node "$(win "$gerador")" "$(win "$TMP/$nome")" >/dev/null || { echo "$nome: o gerador falhou"; FALHAS=1; continue; }
    "$DETECT" "$TMP/$nome/pacotes" "$TMP/$nome/saida.csv" >/dev/null || { echo "$nome: o detect.sh falhou"; FALHAS=1; continue; }
    resultado="$(node "$(win "$AQUI/comparar.mjs")" "$(win "$TMP/$nome/esperado.csv")" "$(win "$TMP/$nome/saida.csv")")"
    printf '%-30s %s\n' "$nome" "$(printf '%s\n' "$resultado" | tail -1)"
    if printf '%s\n' "$resultado" | grep -q '^ERRO'; then
        printf '%s\n' "$resultado" | grep -A3 '^ERRO'; FALHAS=1
    fi
done
exit $FALHAS
