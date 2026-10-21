#!/usr/bin/env bash
# Scenario 05 (one document per kind of guest): the common part, then the three ways to book (CPF, company,
# foreigner) and the refusals, the five identification fields with null where they do not apply, the invoice (ISS
# 5%) and the booking receipt (BRL 1,234.56, ISO dates, "1 night", "Discount:") in the document, in the e-mail and at
# check-out, and the two reservations at the same time.
#
# Usage: bash check_05.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

COMPANY='{"guestCpf": null, "guestName": "Ana Souza", "guestEmail": "ana@example.com",
          "companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda"}'
FOREIGNER='{"guestCpf": null, "guestName": "John Smith", "guestEmail": "john@example.com",
            "guestPassport": "X1234567", "guestCountry": "United Kingdom"}'

# the five identification fields of the last response, separated by |
identification() {
    field '[.guestCpf, .companyCnpj, .companyName, .guestPassport, .guestCountry] | map(tostring) | join("|")'
}

behavior_begin "$1" 05 || exit 0
common_checks

section "Cenário 05: o documento por tipo de hóspede"
restart_fresh || exit 0

create 101 2027-03-10 2027-03-11 1 STANDARD
expect "com CPF: 201" "201" "$STATUS"
expect "com CPF: os campos que não se aplicam vêm null" "111.222.333-44|null|null|null|null" "$(identification)"
cpf="$RID"
request POST "/reservations/$cpf/confirmation"
request GET "/reservations/$cpf/document"
expect "com CPF: o documento continua o comprovante" "Comprovante de reserva" "$(field '.title')"
expect_contains "com CPF: o texto do documento" "Hóspede: Carla Dias (CPF 111.222.333-44)" "$(field '.text // ""')"

create 201 2027-03-10 2027-03-12 2 STANDARD "$COMPANY"
company="$RID"
expect "empresa (CNPJ e razão social): 201" "201" "$STATUS"
expect "empresa: os campos que não se aplicam vêm null" "null|12.345.678/0001-90|Tecnosul Ltda|null|null" \
    "$(identification)"
request GET "/reservations/$company"
expect "empresa: a consulta da reserva mostra os cinco campos" "null|12.345.678/0001-90|Tecnosul Ltda|null|null" \
    "$(identification)"
request POST "/reservations/$company/confirmation"
request GET "/reservations/$company/document"
invoice="$(field '.text // ""')"
expect "empresa: o documento é a fatura" "Fatura de hospedagem" "$(field '.title')"
expect "empresa: o documento traz os cinco campos" "null|12.345.678/0001-90|Tecnosul Ltda|null|null" "$(identification)"
expect_contains "fatura: a primeira linha" "Fatura de hospedagem nº $company" "$invoice"
expect_contains "fatura: a empresa no lugar do CPF" "Empresa: Tecnosul Ltda (CNPJ 12.345.678/0001-90)" "$invoice"
expect_contains "fatura: o hóspede" "Hóspede: Ana Souza" "$invoice"
expect_contains "fatura: o ISS retido de 5%" "ISS retido (5%): R\$ 38,99" "$invoice"
expect_contains "fatura: o total" "Total: R\$ 779,80" "$invoice"
email="$(jq -r '[.[] | select(.subject == "Reserva confirmada - Pousada Maré Alta")][0].body // ""' \
    <<< "$(messages_to ana@example.com)" 2>/dev/null)"
expect_contains "empresa: o e-mail (em português) leva a fatura" "Fatura de hospedagem nº $company" "$email"
expect_contains "empresa: o e-mail continua em português" "Olá, Ana Souza!" "$email"
request POST "/reservations/$company/check-in"
request POST "/reservations/$company/check-out"
expect "empresa: o check-out devolve a fatura" "200 Fatura de hospedagem" "$STATUS $(field '.title')"

create 202 2027-03-10 2027-03-12 2 STANDARD "$FOREIGNER"
foreigner="$RID"
expect "estrangeiro (passaporte e país): 201" "201" "$STATUS"
expect "estrangeiro: os campos que não se aplicam vêm null" "null|null|null|X1234567|United Kingdom" "$(identification)"
request POST "/reservations/$foreigner/confirmation"
request GET "/reservations/$foreigner/document"
receipt="$(field '.text // ""')"
expect "estrangeiro: o documento é o booking receipt" "Booking receipt" "$(field '.title')"
expect_contains "booking receipt: a primeira linha" "Booking receipt no. $foreigner" "$receipt"
expect_contains "booking receipt: o hóspede" "Guest: John Smith (passport X1234567, United Kingdom)" "$receipt"
expect_contains "booking receipt: as datas no formato 2027-03-10" \
    "Check-in: 2027-03-10 | Check-out: 2027-03-12 | 2 nights" "$receipt"
expect_contains "booking receipt: as diárias em BRL" "Nights: BRL 779.80" "$receipt"
expect_contains "booking receipt: o total em BRL" "Total: BRL 779.80" "$receipt"

create 201 2027-03-20 2027-03-21 1 NON_REFUNDABLE "$FOREIGNER"
request POST "/reservations/$RID/confirmation"
request GET "/reservations/$RID/document"
receipt="$(field '.text // ""')"
expect_contains "booking receipt de uma noite: \"1 night\"" "| 1 night" "$receipt"
expect_contains "booking receipt com desconto: a linha Discount" "Discount: BRL 58.48" "$receipt"
create 301 2027-01-05 2027-01-09 2 STANDARD "$FOREIGNER"
request POST "/reservations/$RID/confirmation"
request GET "/reservations/$RID/document"
expect_contains "booking receipt: milhar com vírgula (BRL 2,783.52)" "Total: BRL 2,783.52" "$(field '.text // ""')"

create 102 2027-04-01 2027-04-02 1 STANDARD '{"guestCpf": null}'
expect_error "sem nenhuma identificação: 400 INVALID_REQUEST" 400 INVALID_REQUEST
create 102 2027-04-01 2027-04-02 1 STANDARD '{"companyCnpj": "12.345.678/0001-90", "companyName": "Tecnosul Ltda"}'
expect_error "CPF e CNPJ juntos: 400 INVALID_REQUEST" 400 INVALID_REQUEST
create 102 2027-04-01 2027-04-02 1 STANDARD '{"guestCpf": null, "companyCnpj": "12.345.678/0001-90"}'
expect_error "CNPJ sem a razão social: 400 INVALID_REQUEST" 400 INVALID_REQUEST
create 102 2027-04-01 2027-04-02 1 STANDARD '{"guestCpf": null, "guestPassport": "X1234567"}'
expect_error "passaporte sem o país: 400 INVALID_REQUEST" 400 INVALID_REQUEST

concurrency_check
behavior_end
