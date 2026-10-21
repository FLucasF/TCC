#!/usr/bin/env bash
# Scenario 09 (the reports): the common part, then a known month (July 2027, six reservations: paid, checked in,
# cancelled with and without refund, pending, and one starting in June) and the four reports line by line: the title
# line first, the header, the rows in the right order and the total; the tourism tax (R$ 4.50 per guest per night)
# and the rate plans (STANDARD then NON_REFUNDABLE, even with nothing). And the two reservations at the same time.
#
# Usage: bash check_09.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

# the first line of the last response
first_line() {
    tr -d '\r' <<< "$RESPONSE" | head -1
}

behavior_begin "$1" 09 || exit 0
common_checks

section "Cenário 09: os relatórios"
restart_fresh || exit 0
# ids 1 to 6, in this order, on an empty database
create 101 2027-07-10 2027-07-13 2 STANDARD          # 1: confirmed, 3 nights x 299.88 = 899.64, tax 27.00
request POST /reservations/1/confirmation
create 201 2027-07-05 2027-07-07 3 NON_REFUNDABLE    # 2: confirmed and cancelled, keeps 795.40, no tax
request POST /reservations/2/confirmation
request POST /reservations/2/cancellation
create 301 2027-07-20 2027-07-21 4 STANDARD          # 3: checked in, 695.88, tax 18.00
request POST /reservations/3/confirmation
request POST /reservations/3/check-in
create 102 2027-07-15 2027-07-17 2 STANDARD          # 4: pending, nothing
create 202 2027-07-01 2027-07-03 1 STANDARD          # 5: confirmed and cancelled with refund, nothing
request POST /reservations/5/confirmation
request POST /reservations/5/cancellation
create 102 2027-06-29 2027-07-02 1 STANDARD          # 6: confirmed, starts in June: 249.90 x 2 + 299.88 = 799.68
request POST /reservations/6/confirmation
if [ "$RID" != "6" ]; then
    note "as reservas não vieram numeradas de 1 a 6 (a última foi '$RID'): as linhas abaixo esperam essa numeração"
fi

request GET "/reports/occupancy?date=2027-07-10"
expect "ocupação: a linha de título vem primeiro" "Pousada Maré Alta,occupancy,2027-07-10" "$(first_line)"
expect_line "ocupação: o cabeçalho continua" "room,type,reservation,check_in,check_out"
expect_line "ocupação: a reserva da noite" "101,STANDARD,1,2027-07-10,2027-07-13"
expect_line "ocupação: o total" "total,1/5"

request GET "/reports/revenue?month=2027-07"
expect "financeiro: a linha de título vem primeiro" "Pousada Maré Alta,revenue,2027-07" "$(first_line)"
expect "financeiro: título, cabeçalho, linhas em ordem e total" \
    "Pousada Maré Alta,revenue,2027-07|reservation,room,check_in,check_out,status,amount|2,201,2027-07-05,2027-07-07,CANCELLED,795.40|1,101,2027-07-10,2027-07-13,CONFIRMED,899.64|3,301,2027-07-20,2027-07-21,CHECKED_IN,695.88|total,2390.92" \
    "$(tr -d '\r' <<< "$RESPONSE" | sed '/^$/d' | paste -sd'|' -)"

request GET "/reports/tourism-tax?month=2027-07"
expect "taxa de turismo: responde 200 em text/csv" "200 text/csv" "$STATUS ${CONTENT_TYPE%%;*}"
expect "taxa de turismo: título, cabeçalho, as pagas que começam no mês e o total" \
    "Pousada Maré Alta,tourism-tax,2027-07|reservation,room,check_in,check_out,guests,nights,tax|1,101,2027-07-10,2027-07-13,2,3,27.00|3,301,2027-07-20,2027-07-21,4,1,18.00|total,45.00" \
    "$(tr -d '\r' <<< "$RESPONSE" | sed '/^$/d' | paste -sd'|' -)"
request GET "/reports/tourism-tax?month=2027-08"
expect "taxa de turismo de um mês vazio: título, cabeçalho e total 0.00" \
    "Pousada Maré Alta,tourism-tax,2027-08|reservation,room,check_in,check_out,guests,nights,tax|total,0.00" \
    "$(tr -d '\r' <<< "$RESPONSE" | sed '/^$/d' | paste -sd'|' -)"

request GET "/reports/rate-plans?month=2027-07"
expect "planos: responde 200 em text/csv" "200 text/csv" "$STATUS ${CONTENT_TYPE%%;*}"
expect "planos: título, cabeçalho, STANDARD, NON_REFUNDABLE e o total" \
    "Pousada Maré Alta,rate-plans,2027-07|rate_plan,reservations,amount|STANDARD,2,1595.52|NON_REFUNDABLE,1,795.40|total,3,2390.92" \
    "$(tr -d '\r' <<< "$RESPONSE" | sed '/^$/d' | paste -sd'|' -)"
request GET "/reports/rate-plans?month=2027-06"
expect "planos: um plano sem reserva no mês aparece com 0" \
    "Pousada Maré Alta,rate-plans,2027-06|rate_plan,reservations,amount|STANDARD,1,799.68|NON_REFUNDABLE,0,0.00|total,1,799.68" \
    "$(tr -d '\r' <<< "$RESPONSE" | sed '/^$/d' | paste -sd'|' -)"
request GET "/reports/tourism-tax?month=julho"
expect_error "taxa de turismo com mês fora do formato: 400 INVALID_REQUEST" 400 INVALID_REQUEST

concurrency_check
behavior_end
