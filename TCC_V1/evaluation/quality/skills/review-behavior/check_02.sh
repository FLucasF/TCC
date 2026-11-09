#!/usr/bin/env bash
# Scenario 02 (control: the high season changed): the common part with the new surcharge, then the 25% on the three room types, the Easter weeks of 2027 and 2028, the old periods, and the discount over the new value.
#
# Usage: bash check_02.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

# one night of room $1 starting on $2; prints the total
night_total() {
    create "$1" "$2" "$(date -d "$2 + 1 day" +%F)" 1 "${3:-STANDARD}"
    money "$(field '.total')"
}

behavior_begin "$1" 02 || exit 0
TWO_HIGH_NIGHTS="624.76"
ONE_HIGH_NIGHT="312.38"
common_checks

section "Cenário 02: o acréscimo novo"
restart_fresh || exit 0
create 101 2027-07-05 2027-07-06 1 STANDARD
expect "standard numa noite de julho: 312.38" "312.38" "$(money "$(field '.total')")"
create 201 2027-07-05 2027-07-06 1 STANDARD
expect "deluxe numa noite de julho: 487.38" "487.38" "$(money "$(field '.total')")"
create 301 2027-07-05 2027-07-06 1 STANDARD
expect "suíte numa noite de julho: 724.88" "724.88" "$(money "$(field '.total')")"
create 102 2027-07-05 2027-07-06 1 NON_REFUNDABLE
expect "não reembolsável em alta: desconto 46.86 sobre o valor novo" "46.86" "$(money "$(field '.discount')")"
expect "não reembolsável em alta: total 265.52" "265.52" "$(money "$(field '.total')")"

check_night() {
    create 101 "$1" "$(date -d "$1 + 1 day" +%F)" 1 STANDARD
    expect "a noite de $1 custa $2" "$2" "$(money "$(field '.total')")"
}

section "Cenário 02: a semana da Páscoa"
for pair in 2027-03-20:249.90 2027-03-21:312.38 2027-03-24:312.38 2027-03-28:312.38 2027-03-29:249.90 \
            2028-04-08:249.90 2028-04-09:312.38 2028-04-16:312.38 2028-04-17:249.90; do
    check_night "${pair%%:*}" "${pair##*:}"
done
create 102 2027-03-20 2027-03-22 1 STANDARD
expect "de 20 a 22/03/2027, uma noite fora e uma dentro: 562.28" "562.28" "$(money "$(field '.total')")"

section "Cenário 02: o resto dos períodos continua"
for pair in 2027-12-14:249.90 2027-12-15:312.38 2027-02-28:312.38 2027-03-01:249.90 2027-07-31:312.38 \
            2027-08-01:249.90; do
    check_night "${pair%%:*}" "${pair##*:}"
done

restart_fresh || exit 0
concurrency_check
behavior_end
