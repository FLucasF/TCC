#!/usr/bin/env bash
# Scenario 01 (the rate plans): the common part, then the three new plans with the values the text gives (Tecnosul,
# the deluxe weekend package and the long stay), the other two companies, the plans' refusals and refunds, the
# companyCode ignored outside the corporate plan, and the two reservations at the same time.
#
# Usage: bash check_01.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

behavior_begin "$1" 01 || exit 0
common_checks

section "Cenário 01: os planos de tarifa novos"
restart_fresh || exit 0

create 201 2027-03-10 2027-03-12 2 CORPORATE '{"companyCode": "TECNOSUL"}'
tecnosul="$RID"
expect "CORPORATE TECNOSUL, deluxe de 10 a 12/03: 201" "201" "$STATUS"
expect "TECNOSUL: diárias 779.80, desconto 93.58, total 686.22" "779.80 93.58 686.22" \
    "$(money "$(field '.subtotal')") $(money "$(field '.discount')") $(money "$(field '.total')")"
create 202 2027-03-10 2027-03-12 2 CORPORATE '{"companyCode": "AGROVALE"}'
expect "AGROVALE (18%): desconto 140.36, total 639.44" "140.36 639.44" \
    "$(money "$(field '.discount')") $(money "$(field '.total')")"
create 201 2027-03-20 2027-03-22 2 CORPORATE '{"companyCode": "FARMAVIDA"}'
expect "FARMAVIDA (25%): desconto 194.95, total 584.85" "194.95 584.85" \
    "$(money "$(field '.discount')") $(money "$(field '.total')")"
create 102 2027-03-10 2027-03-12 1 CORPORATE
expect_error "CORPORATE sem o código da empresa: 400 INVALID_REQUEST" 400 INVALID_REQUEST
create 102 2027-03-10 2027-03-12 1 CORPORATE '{"companyCode": "ACME"}'
expect_error "CORPORATE com um código que não existe: 400 INVALID_REQUEST" 400 INVALID_REQUEST
request POST "/reservations/$tecnosul/confirmation"
request POST "/reservations/$tecnosul/cancellation"
expect "CORPORATE cancelada depois de paga devolve tudo (686.22)" "686.22" "$(money "$(field '.refund')")"

create 202 2027-03-12 2027-03-14 2 WEEKEND_PACKAGE
weekend="$RID"
expect "WEEKEND_PACKAGE deluxe, sexta 12/03 a domingo 14/03: 201" "201" "$STATUS"
expect "pacote deluxe: diárias 820.00, desconto 0.00, total 820.00" "820.00 0.00 820.00" \
    "$(money "$(field '.subtotal')") $(money "$(field '.discount')") $(money "$(field '.total')")"
request POST "/reservations/$weekend/confirmation"
request POST "/reservations/$weekend/cancellation"
expect "pacote pago e cancelado devolve a metade (410.00)" "410.00" "$(money "$(field '.refund')")"
create 101 2027-03-19 2027-03-21 2 WEEKEND_PACKAGE
expect "pacote standard: total 520.00" "201 520.00" "$STATUS $(money "$(field '.total')")"
create 301 2027-03-19 2027-03-21 2 WEEKEND_PACKAGE
expect "pacote na suíte: total 1200.00" "201 1200.00" "$STATUS $(money "$(field '.total')")"
create 201 2027-07-09 2027-07-11 2 WEEKEND_PACKAGE
expect "pacote em julho (alta temporada) continua 820.00" "201 820.00" "$STATUS $(money "$(field '.total')")"
create 102 2027-03-13 2027-03-15 2 WEEKEND_PACKAGE
expect_error "pacote de sábado a segunda: 400 INVALID_REQUEST" 400 INVALID_REQUEST
create 102 2027-03-26 2027-03-29 2 WEEKEND_PACKAGE
expect_error "pacote de sexta a segunda (3 noites): 400 INVALID_REQUEST" 400 INVALID_REQUEST

create 101 2027-03-01 2027-03-08 2 LONG_STAY
long="$RID"
expect "LONG_STAY standard de 01 a 08/03: diárias 1749.30, desconto 174.93, total 1574.37" "1749.30 174.93 1574.37" \
    "$(money "$(field '.subtotal')") $(money "$(field '.discount')") $(money "$(field '.total')")"
create 101 2027-06-28 2027-07-05 2 LONG_STAY
expect "LONG_STAY com noites de temporada (28/06 a 05/07): total 1754.30" "201 1754.30" \
    "$STATUS $(money "$(field '.total')")"
create 102 2027-04-01 2027-04-07 2 LONG_STAY
expect_error "LONG_STAY com 6 noites: 400 INVALID_REQUEST" 400 INVALID_REQUEST
request POST "/reservations/$long/confirmation"
request POST "/reservations/$long/cancellation"
expect "LONG_STAY cancelada depois de paga devolve tudo (1574.37)" "1574.37" "$(money "$(field '.refund')")"

create 102 2027-05-10 2027-05-12 2 STANDARD '{"companyCode": "TECNOSUL"}'
expect "STANDARD com companyCode: o código é ignorado (201, desconto 0.00)" "201 0.00" \
    "$STATUS $(money "$(field '.discount')")"
create 102 2027-05-20 2027-05-21 1 NON_REFUNDABLE
expect "NON_REFUNDABLE continua com 15% (desconto 37.48)" "37.48" "$(money "$(field '.discount')")"

concurrency_check
behavior_end
