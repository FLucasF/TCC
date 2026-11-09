#!/usr/bin/env bash
# Scenario 04 (control: the confirmation e-mail): the common part with the new subject, then the three new lines in place of the old one, and the rest of the text as it was.
#
# Usage: bash check_04.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

behavior_begin "$1" 04 || exit 0
CONFIRMATION_SUBJECT="Sua reserva na Pousada Maré Alta está confirmada!"
common_checks

section "Cenário 04: o texto novo"
restart_fresh || exit 0
create 101 2027-03-10 2027-03-11 1 STANDARD
request POST "/reservations/$RID/confirmation"
message="$(messages_to carla@example.com)"
expect "uma mensagem para o hóspede" "1" "$(jq 'length' <<< "$message" 2>/dev/null)"
expect "o assunto novo" "$CONFIRMATION_SUBJECT" "$(jq -r '.[0].subject // ""' <<< "$message" 2>/dev/null)"
body="$(jq -r '.[0].body // ""' <<< "$message" 2>/dev/null)"
expect_contains "a saudação continua" "Olá, Carla Dias!" "$body"
expect_contains "o comprovante continua" "Total: R\$ 249,90" "$body"
new_lines='Entrada a partir das 14h e saída até as 12h.
Café da manhã das 7h às 10h, incluído na diária.
Qualquer dúvida, fale com a recepção: (79) 3200-0000.'
expect_contains "as três linhas novas, em ordem" "$new_lines" "$body"
expect "o \"Até breve!\" continua no fim" "Até breve!" "$(tail -1 <<< "$body")"
if [[ "$body" == *"A entrada é a partir das 14h"* ]]; then
    bad "a linha antiga saiu" "sem a linha antiga" "$body"
else
    ok "a linha antiga saiu"
fi

restart_fresh || exit 0
concurrency_check
behavior_end
