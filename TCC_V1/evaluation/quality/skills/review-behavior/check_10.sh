#!/usr/bin/env bash
# Scenario 10 (control: the guest's name on the occupancy report): the common part with the new column, then the comma turned into a space, and the revenue report unchanged.
#
# Usage: bash check_10.sh <project folder>
set -uo pipefail
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=/dev/null
source "$here/project.sh"
# shellcheck source=/dev/null
source "$here/behavior.sh"

behavior_begin "$1" 10 || exit 0
OCCUPANCY_HEADER="room,type,reservation,guest,check_in,check_out"
OCCUPANCY_GUEST="Carla Dias,"
common_checks

section "Cenário 10: a coluna do hóspede"
restart_fresh || exit 0
create 301 2027-03-10 2027-03-12 2 STANDARD '{"guestName": "Silva,Ana"}'
comma="$RID"
request POST "/reservations/$comma/confirmation"
request GET "/reports/occupancy?date=2027-03-10"
expect_line "o cabeçalho com a coluna depois de reservation" "$OCCUPANCY_HEADER"
expect_line "a vírgula do nome vira um espaço" "301,SUITE,$comma,Silva Ana,2027-03-10,2027-03-12"
expect_line "o total continua" "total,1/5"
request GET "/reports/revenue?month=2027-03"
expect_line "o financeiro continua com o mesmo cabeçalho" "reservation,room,check_in,check_out,status,amount"

restart_fresh || exit 0
concurrency_check
behavior_end
