#!/usr/bin/env bash
# Confere, com uma chamada mínima, se o token da assinatura autentica dentro do
# container com HOME limpo. Gasta muito pouco da cota. Rode antes de cada lote.
#
# Uso: scripts/sonda-auth.sh [modelo]   (padrão: claude-haiku-4-5)

set -uo pipefail
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"
IMAGEM="${IMAGEM:-experimento-harness:v1}"
MODELO="${1:-claude-haiku-4-5}"
[ -s "$RAIZ/.env" ] || { echo "falta $RAIZ/.env ou ele está vazio" >&2; exit 1; }
grep -q '^CLAUDE_CODE_OAUTH_TOKEN=.\+' "$RAIZ/.env" || { echo ".env sem CLAUDE_CODE_OAUTH_TOKEN=..." >&2; exit 1; }
ENV_WIN="$(cygpath -w "$RAIZ/.env")"
export MSYS_NO_PATHCONV=1

docker run --rm --env-file "$ENV_WIN" "$IMAGEM" bash -c '
    cd /tmp && echo "responda exatamente: OK" | claude -p --model "$0" --output-format json \
        | jq "{is_error, result, modelo: (.modelUsage // {} | keys), num_turns}"
' "$MODELO"
