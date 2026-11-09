#!/usr/bin/env bash
# Scenario 08 (control: the signature on the messages): the common part, then the blank line and the three signature lines at the end of the message, with the text before them unchanged.
#
# Usage: bash check_08.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

behavior_begin "$1" 08 || exit 0
common_checks

section "Cenário 08: a assinatura"
restart_fresh || exit 0
create 101 2027-03-10 2027-03-11 1 STANDARD
request POST "/reservations/$RID/confirmation"
message="$(messages_to carla@example.com)"
body="$(jq -r '.[0].body // ""' <<< "$message" 2>/dev/null)"
expected_end='Até breve!

--
Pousada Maré Alta
(79) 3200-0000 | @pousadamarealta'
# $(...) already dropped the newlines at the end, which is the tolerance the text allows
if [[ "$body" == *"$expected_end" ]]; then
    ok "depois do texto, uma linha em branco e as três linhas da assinatura, no fim"
else
    bad "depois do texto, uma linha em branco e as três linhas da assinatura, no fim" "$expected_end" "$body"
fi
expect_contains "a saudação continua" "Olá, Carla Dias!" "$body"
expect_contains "o comprovante continua" "Total: R\$ 249,90" "$body"

restart_fresh || exit 0
concurrency_check
behavior_end
