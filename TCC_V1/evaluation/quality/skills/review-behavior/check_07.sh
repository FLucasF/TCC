#!/usr/bin/env bash
# Scenario 07 (the sending rules): the common part, then, in the normal mode, the status SENT/BLOCKED, the archive
# copy right after the original, the invalid addresses blocked without a copy; and, with HOTEL_TEST_MODE=true, the
# message going to the test address with "[TESTE] " and the original recipient in the first line, without the copy,
# and the invalid address still blocked with the original address. And the two reservations at the same time.
#
# Usage: bash check_07.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

SUBJECT="Reserva confirmada - Pousada Maré Alta"
ARCHIVE=arquivo@pousadamarealta.com.br
TESTS=testes@pousadamarealta.com.br

# creates and confirms a reservation of the guest e-mail $1 on room $2, night $3
confirm_for() {
    local next
    next="$(date -d "$3 + 1 day" +%F)"
    create "$2" "$3" "$next" 1 STANDARD "$(jq -nc --arg email "$1" '{guestEmail: $email}')"
    request POST "/reservations/$RID/confirmation"
}

# the messages created since the count $1 (a JSON array, in the list's order)
messages_since() {
    request GET /messages
    jq -c --argjson skip "$1" '.[$skip:]' <<< "$RESPONSE" 2>/dev/null
}

behavior_begin "$1" 07 || exit 0
common_checks

section "Cenário 07: as regras de envio, no modo normal"
restart_fresh || exit 0

before="$(message_count)"
confirm_for ana@example.com 101 2027-03-10
new="$(messages_since "$before")"
expect "endereço válido: a mensagem sai para o hóspede com status SENT" "ana@example.com SENT" \
    "$(jq -r '.[0] | "\(.to) \(.status)"' <<< "$new" 2>/dev/null)"
expect "a cópia para o arquivo vem logo depois da original" "$ARCHIVE" "$(jq -r '.[1].to // ""' <<< "$new" 2>/dev/null)"
expect "a cópia tem o assunto com \"[Cópia] \"" "[Cópia] $SUBJECT" "$(jq -r '.[1].subject // ""' <<< "$new" 2>/dev/null)"
expect "a cópia tem o mesmo texto" "true" "$(jq '.[0].body == .[1].body' <<< "$new" 2>/dev/null)"
expect "a cópia sai com status SENT" "SENT" "$(jq -r '.[1].status // ""' <<< "$new" 2>/dev/null)"

day=1
for address in "ana@gmail" "ana.gmail.com" "ana@@gmail.com" "@gmail.com"; do
    day=$((day + 2))
    before="$(message_count)"
    confirm_for "$address" 102 "2027-04-$(printf '%02d' "$day")"
    new="$(messages_since "$before")"
    expect "endereço inválido \"$address\": uma mensagem só, BLOCKED, com o endereço original, sem cópia" \
        "1 $address BLOCKED" "$(jq -r '"\(length) \(.[0].to) \(.[0].status)"' <<< "$new" 2>/dev/null)"
done

section "Cenário 07: as regras de envio, no modo de teste (HOTEL_TEST_MODE=true)"
restart_fresh HOTEL_TEST_MODE=true || exit 0
before="$(message_count)"
confirm_for ana@example.com 201 2027-03-10
new="$(messages_since "$before")"
expect "modo de teste: uma mensagem só (sem a cópia para o arquivo)" "1" "$(jq 'length' <<< "$new" 2>/dev/null)"
expect "modo de teste: vai para o endereço de testes, SENT" "$TESTS SENT" \
    "$(jq -r '.[0] | "\(.to) \(.status)"' <<< "$new" 2>/dev/null)"
expect "modo de teste: o assunto começa com \"[TESTE] \"" "[TESTE] $SUBJECT" "$(jq -r '.[0].subject // ""' <<< "$new" 2>/dev/null)"
expect "modo de teste: a primeira linha diz o destinatário original" "Destinatário original: ana@example.com" \
    "$(jq -r '.[0].body // ""' <<< "$new" 2>/dev/null | head -1)"
before="$(message_count)"
confirm_for "ana@gmail" 202 2027-03-10
new="$(messages_since "$before")"
expect "modo de teste: endereço inválido continua BLOCKED, com o endereço original" "1 ana@gmail BLOCKED" \
    "$(jq -r '"\(length) \(.[0].to) \(.[0].status)"' <<< "$new" 2>/dev/null)"

restart_fresh || exit 0
concurrency_check
behavior_end
