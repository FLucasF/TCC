#!/usr/bin/env bash
# Scenario 03 (who is told about a confirmation): the common part, then the e-mails to the housekeeping and the
# finance on confirmation and on the cancellation of a confirmed reservation (subjects, dates, guests, amounts), no
# refund e-mail when nothing goes back, nobody told when a pending reservation is cancelled, and the two
# reservations at the same time.
#
# Usage: bash check_03.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

HOUSEKEEPING=governanca@pousadamarealta.com.br
FINANCE=financeiro@pousadamarealta.com.br

# the body of the message to $1 whose subject is $2 ("" when there is none)
body_of() {
    jq -r --arg subject "$2" '[.[] | select(.subject == $subject)][0].body // ""' <<< "$(messages_to "$1")" 2>/dev/null
}

# how many messages to $1 have the subject $2
count_of() {
    jq --arg subject "$2" '[.[] | select(.subject == $subject)] | length' <<< "$(messages_to "$1")" 2>/dev/null
}

behavior_begin "$1" 03 || exit 0
common_checks

section "Cenário 03: os avisos da confirmação e do cancelamento"
restart_fresh || exit 0

create 201 2027-03-10 2027-03-12 2 STANDARD
standard="$RID"
request POST "/reservations/$standard/confirmation"
expect "a confirmação responde 200" "200" "$STATUS"
expect "o hóspede continua recebendo o e-mail de confirmação" "1" \
    "$(count_of carla@example.com "Reserva confirmada - Pousada Maré Alta")"
expect "a governança recebe \"Reserva confirmada - quarto 201\"" "1" \
    "$(count_of "$HOUSEKEEPING" "Reserva confirmada - quarto 201")"
housekeeping="$(body_of "$HOUSEKEEPING" "Reserva confirmada - quarto 201")"
expect_contains "o e-mail da governança traz a entrada (10/03/2027)" "10/03/2027" "$housekeeping"
expect_contains "o e-mail da governança traz a saída (12/03/2027)" "12/03/2027" "$housekeeping"
# the number 2 on its own, once the dates and the room number are out of the text
guests_text="$(sed -E 's#[0-9]{2}/[0-9]{2}/[0-9]{4}##g; s#[0-9]{4}-[0-9]{2}-[0-9]{2}##g; s/201//g' <<< "$housekeeping")"
if grep -qE '(^|[^0-9])2([^0-9]|$)' <<< "$guests_text"; then
    ok "o e-mail da governança traz quantos hóspedes vêm (2)"
else
    bad "o e-mail da governança traz quantos hóspedes vêm (2)" "o número 2" "$housekeeping"
fi
expect "o financeiro recebe \"Pagamento recebido - reserva $standard\"" "1" \
    "$(count_of "$FINANCE" "Pagamento recebido - reserva $standard")"
expect_contains "o e-mail do financeiro traz o valor pago (R\$ 779,80)" "R\$ 779,80" \
    "$(body_of "$FINANCE" "Pagamento recebido - reserva $standard")"

request POST "/reservations/$standard/cancellation"
expect "a governança recebe \"Reserva cancelada - quarto 201\"" "1" \
    "$(count_of "$HOUSEKEEPING" "Reserva cancelada - quarto 201")"
expect_contains "o aviso de cancelamento traz as datas que ficaram livres (10/03/2027)" "10/03/2027" \
    "$(body_of "$HOUSEKEEPING" "Reserva cancelada - quarto 201")"
expect "o financeiro recebe \"Reembolso - reserva $standard\"" "1" \
    "$(count_of "$FINANCE" "Reembolso - reserva $standard")"
expect_contains "o reembolso traz o valor a devolver (R\$ 779,80)" "R\$ 779,80" \
    "$(body_of "$FINANCE" "Reembolso - reserva $standard")"

create 202 2027-03-10 2027-03-11 1 NON_REFUNDABLE
non_refundable="$RID"
request POST "/reservations/$non_refundable/confirmation"
request POST "/reservations/$non_refundable/cancellation"
expect "NON_REFUNDABLE cancelada: a governança é avisada" "1" \
    "$(count_of "$HOUSEKEEPING" "Reserva cancelada - quarto 202")"
expect "NON_REFUNDABLE cancelada: nada a devolver, o financeiro não é avisado" "0" \
    "$(count_of "$FINANCE" "Reembolso - reserva $non_refundable")"

create 301 2027-03-10 2027-03-12 2 STANDARD
before="$(message_count)"
request POST "/reservations/$RID/cancellation"
expect "cancelar uma reserva pendente não manda nada" "$before" "$(message_count)"

concurrency_check
behavior_end
