#!/usr/bin/env bash
# Scenario 06 (control: children on the reservation): the common part, then the children field, the capacity with adults and children, the same price, the receipt line, and the data after a restart.
#
# Usage: bash check_06.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

behavior_begin "$1" 06 || exit 0
common_checks

section "Cenário 06: as crianças na reserva"
restart_fresh || exit 0
create 201 2027-03-10 2027-03-11 2 STANDARD
plain="$RID"
plain_total="$(money "$(field '.total')")"
expect "sem o campo, são zero crianças" "201 0" "$STATUS $(field '.children')"
create 202 2027-03-10 2027-03-11 2 STANDARD '{"children": 1}'
with_children="$RID"
expect "dois adultos e uma criança num quarto para três" "201 1 2" "$STATUS $(field '.children') $(field '.guests')"
expect "criança não muda o preço" "$plain_total" "$(money "$(field '.total')")"
request GET "/reservations/$with_children"
expect "a consulta mostra as crianças" "200 1" "$STATUS $(field '.children')"
create 201 2027-04-10 2027-04-11 2 STANDARD '{"children": 2}'
expect_error "adultos e crianças acima da capacidade: 422 CAPACITY_EXCEEDED" 422 CAPACITY_EXCEEDED
create 101 2027-04-10 2027-04-11 2 STANDARD '{"children": 1}'
expect_error "o quarto para dois com dois adultos e uma criança: 422 CAPACITY_EXCEEDED" 422 CAPACITY_EXCEEDED
create 201 2027-04-10 2027-04-11 2 STANDARD '{"children": -1}'
expect_error "número negativo: 400 INVALID_REQUEST" 400 INVALID_REQUEST

section "Cenário 06: o comprovante"
create 301 2027-05-10 2027-05-11 2 STANDARD '{"children": 2}'
two="$RID"
# the receipt's text goes out in the confirmation e-mail
request POST "/reservations/$two/confirmation"
request POST "/reservations/$plain/confirmation"
emails="$(messages_to carla@example.com)"
expect "a linha \"Crianças: 2\" logo depois da linha do quarto" "Crianças: 2" \
    "$(jq -r '.[0].body // ""' <<< "$emails" 2>/dev/null | grep -A1 '^Quarto:' | tail -1)"
second_email="$(jq -r '.[1].body // ""' <<< "$emails" 2>/dev/null)"
if [[ "$second_email" == *"Crianças"* ]]; then
    bad "sem criança, o comprovante fica como está" "sem a linha" "$second_email"
else
    ok "sem criança, o comprovante fica como está"
fi

section "Cenário 06: as crianças continuam depois de reiniciar"
if start_again; then
    request GET "/reservations/$with_children"
    expect "a reserva continua com uma criança" "200 1" "$STATUS $(field '.children')"
else
    bad "o serviço sobe de novo sobre o mesmo banco" "subir" "não subiu"
fi

restart_fresh || exit 0
concurrency_check
behavior_end
