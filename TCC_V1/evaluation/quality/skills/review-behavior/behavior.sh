# What the check_NN.sh scripts share, sourced by them after project.sh: starting the service on an empty database,
# calling the API with curl, comparing with jq, writing each check as "- OK: ..." or "- FALHOU: ... (esperado: ...;
# veio: ...)", and the checks of the common part of the texts (the same in every scenario), the restart and the two
# reservations at the same time. The result goes to measures/behavior.md.

export LC_ALL=C
PORT=18092
DATABASE_NUMBER=0
PASSED=0
FAILED=0
STATUS=""
CONTENT_TYPE=""
RESPONSE=""

# builds and starts the project; returns 1 (with the reason in the result file) when it cannot
behavior_begin() {
    PROJECT="$(cd "$1" && pwd)"
    SCENARIO="$2"
    MEASURES="$(results_folder "$PROJECT")"
    OUT="$MEASURES/behavior.md"
    WORK="$(mktemp -d)"
    BODY_FILE="$WORK/checks.md"
    : > "$BODY_FILE"
    trap 'stop_service; rm -rf "$WORK"' EXIT
    local root
    root="$(project_root "$PROJECT")"
    if [ -z "$root" ]; then
        behavior_cannot_run "não há pom.xml no projeto"
        return 1
    fi
    if ! build_package "$root" "$MEASURES/behavior-build.log"; then
        behavior_cannot_run "o projeto não empacota (veja measures/behavior-build.log)"
        return 1
    fi
    JAR="$(main_jar "$root")"
    if [ -z "$JAR" ]; then
        behavior_cannot_run "nenhum jar com Main-Class em target/"
        return 1
    fi
    restart_fresh || return 1
    return 0
}

behavior_cannot_run() {
    {
        echo "# Comportamento: o roteiro do cenário $SCENARIO"
        echo
        echo "Resultado: não deu para testar ($1)."
    } > "$OUT"
    echo "behavior: $OUT"
}

# (re)starts the service on a new, empty database; extra arguments are variables (NAME=value)
restart_fresh() {
    stop_service
    DATABASE_NUMBER=$((DATABASE_NUMBER + 1))
    DATABASE_URL="$(fresh_database "review_behavior_$DATABASE_NUMBER")"
    start_again "$@"
}

# (re)starts the service on the same database (to see that the data stays)
start_again() {
    stop_service
    BASE_URL="http://localhost:$PORT"
    if ! start_service "$JAR" "$DATABASE_URL" "$PORT" "$MEASURES/behavior-service.log" "$@"; then
        behavior_cannot_run "o serviço não subiu contra um banco vazio; o fim do log: $(tail -5 \
            "$MEASURES/behavior-service.log" | tr '\n' ' ' | cut -c1-600)"
        return 1
    fi
}

section() {
    printf '\n## %s\n\n' "$1" >> "$BODY_FILE"
}

note() {
    echo "- nota: $1" >> "$BODY_FILE"
}

# METHOD PATH [JSON body]: sets STATUS, CONTENT_TYPE and RESPONSE
request() {
    local arguments=(-s -o "$WORK/response" -w '%{http_code} %{content_type}' -X "$1" --max-time 30)
    if [ -n "${3-}" ]; then
        arguments+=(-H 'Content-Type: application/json' --data-binary "$3")
    elif [ "$1" = "POST" ]; then
        arguments+=(--data-binary '')
    fi
    local answer
    answer="$(curl "${arguments[@]}" "$BASE_URL$2")"
    STATUS="${answer%% *}"
    CONTENT_TYPE="${answer#* }"
    RESPONSE="$(cat "$WORK/response" 2>/dev/null)"
}

# a jq filter over the last response ("" when it is not JSON)
field() {
    jq -r "$1" <<< "$RESPONSE" 2>/dev/null
}

# money with 2 places, whatever the JSON had (599.76, 599.760, "599.76"); anything else as it came
money() {
    if [[ "$1" =~ ^-?[0-9]+(\.[0-9]+)?$ ]]; then
        printf '%.2f' "$1"
    else
        printf '%s' "$1"
    fi
}

ok() {
    PASSED=$((PASSED + 1))
    echo "- OK: $1" >> "$BODY_FILE"
}

bad() {
    FAILED=$((FAILED + 1))
    echo "- FALHOU: $1 (esperado: $2; veio: $(printf '%s' "$3" | tr '\n' ' ' | cut -c1-240))" >> "$BODY_FILE"
}

# description expected actual
expect() {
    if [ "$2" = "$3" ]; then ok "$1"; else bad "$1" "$2" "$3"; fi
}

# description expected_status expected_error_code (from the last response)
expect_error() {
    local got="$STATUS $(field '.error // empty')"
    expect "$1" "$2 $3" "${got% }"
}

# description line: the last response (a CSV) has this exact line
expect_line() {
    if tr -d '\r' <<< "$RESPONSE" | grep -qxF -- "$2"; then
        ok "$1"
    else
        bad "$1" "a linha '$2'" "$RESPONSE"
    fi
}

# description text: the given text contains this piece
expect_contains() {
    if [[ "$3" == *"$2"* ]]; then
        ok "$1"
    else
        bad "$1" "um texto com '$2'" "$3"
    fi
}

# a reservation's JSON body: room checkIn checkOut guests ratePlan [JSON object merged over it; null removes a field]
reservation_body() {
    local extra="${6-}"
    [ -n "$extra" ] || extra='{}'
    jq -nc --argjson room "$1" --arg in "$2" --arg out "$3" --argjson guests "$4" --arg plan "$5" \
        --argjson extra "$extra" \
        '{roomNumber: $room, guestName: "Carla Dias", guestEmail: "carla@example.com", guestCpf: "111.222.333-44",
          checkIn: $in, checkOut: $out, guests: $guests, ratePlan: $plan} + $extra
         | with_entries(select(.value != null))'
}

# creates a reservation (the arguments of reservation_body); sets RID ("" when it was not created)
create() {
    request POST /reservations "$(reservation_body "$@")"
    RID=""
    if [ "$STATUS" = "201" ]; then
        RID="$(field '.id')"
    fi
}

# the messages addressed to $1 (a JSON array), from GET /messages
messages_to() {
    request GET /messages
    jq -c --arg to "$1" '[.[] | select(.to == $to)]' <<< "$RESPONSE" 2>/dev/null
}

message_count() {
    request GET /messages
    jq 'length' <<< "$RESPONSE" 2>/dev/null
}

# the common part of the texts, the same in every scenario (dates in 2029, out of the scenarios' way)
common_checks() {
    section "A parte comum do enunciado"

    request GET /rooms
    expect "GET /rooms responde 200" "200" "$STATUS"
    expect "cinco quartos" "5" "$(field 'length')"
    expect "o 201 comporta 3" "3" "$(field '.[] | select(.number == 201) | .capacity')"
    expect "a diária do 201 é 389.90" "389.90" "$(money "$(field '.[] | select(.number == 201) | .nightlyRate')")"

    create 101 2029-07-10 2029-07-12 2 STANDARD
    local first="$RID"
    expect "criar uma reserva responde 201" "201" "$STATUS"
    expect "a reserva nasce PENDING" "PENDING" "$(field '.status')"
    expect "STANDARD de 10 a 12/07 (alta temporada): 2 noites" "2" "$(field '.nights')"
    expect "STANDARD de 10 a 12/07: total 599.76" "599.76" "$(money "$(field '.total')")"
    expect "nada pago antes da confirmação (amountPaid 0.00)" "0.00" "$(money "$(field '.amountPaid')")"
    expect "refund null antes do cancelamento" "null" "$(field '.refund')"
    if grep -qE '"amountPaid"[[:space:]]*:[[:space:]]*0\.00\b' <<< "$RESPONSE"; then
        ok "dinheiro no JSON como número com 2 casas (amountPaid 0.00)"
    else
        bad "dinheiro no JSON como número com 2 casas (amountPaid 0.00)" '"amountPaid":0.00' "$RESPONSE"
    fi
    if [ -n "$first" ] && [ "$first" = "1" ]; then
        ok "as reservas são numeradas a partir de 1"
    else
        bad "as reservas são numeradas a partir de 1" "1" "$first"
    fi

    create 101 2029-03-10 2029-03-11 1 NON_REFUNDABLE
    local non_refundable="$RID"
    expect "NON_REFUNDABLE de uma noite: desconto 37.48 (meio para o par)" "37.48" "$(money "$(field '.discount')")"
    expect "NON_REFUNDABLE de uma noite: total 212.42" "212.42" "$(money "$(field '.total')")"

    create 101 2029-07-11 2029-07-13 2 STANDARD
    expect_error "quarto ocupado numa das noites: 409 ROOM_UNAVAILABLE" 409 ROOM_UNAVAILABLE
    create 101 2029-09-01 2029-09-02 3 STANDARD
    expect_error "mais hóspedes que a capacidade: 422 CAPACITY_EXCEEDED" 422 CAPACITY_EXCEEDED
    create 999 2029-09-01 2029-09-02 1 STANDARD
    expect_error "quarto que não existe: 404 ROOM_NOT_FOUND" 404 ROOM_NOT_FOUND
    create 101 2029-09-01 2029-09-01 1 STANDARD
    expect_error "saída igual à entrada: 400 INVALID_REQUEST" 400 INVALID_REQUEST
    create 101 2029-09-01 2029-09-02 0 STANDARD
    expect_error "nenhum hóspede: 400 INVALID_REQUEST" 400 INVALID_REQUEST
    request POST /reservations 'isto não é json'
    expect_error "corpo que não é JSON: 400 INVALID_REQUEST" 400 INVALID_REQUEST
    request POST /reservations '{"roomNumber": 201}'
    expect_error "faltam campos: 400 INVALID_REQUEST" 400 INVALID_REQUEST
    request GET /reservations/99999
    expect_error "reserva que não existe: 404 RESERVATION_NOT_FOUND" 404 RESERVATION_NOT_FOUND

    request GET "/reservations/$non_refundable/document"
    expect_error "segunda via de reserva pendente: 409 OPERATION_NOT_ALLOWED" 409 OPERATION_NOT_ALLOWED

    request POST "/reservations/$first/confirmation"
    expect "confirmar responde 200 com CONFIRMED" "200 CONFIRMED" "$STATUS $(field '.status')"
    expect "o pago é o total (599.76)" "599.76" "$(money "$(field '.amountPaid')")"
    request POST "/reservations/$first/confirmation"
    expect_error "confirmar de novo: 409 OPERATION_NOT_ALLOWED" 409 OPERATION_NOT_ALLOWED

    local guest_messages
    guest_messages="$(messages_to carla@example.com)"
    expect "o hóspede recebe o e-mail \"Reserva confirmada - Pousada Maré Alta\"" "1" \
        "$(jq '[.[] | select(.subject == "Reserva confirmada - Pousada Maré Alta")] | length' <<< "$guest_messages" 2>/dev/null)"
    expect_contains "o e-mail traz o comprovante (\"Total: R\$ 599,76\")" "Total: R\$ 599,76" \
        "$(jq -r '.[0].body // ""' <<< "$guest_messages" 2>/dev/null)"

    request GET "/reservations/$first/document"
    expect "a segunda via responde 200 com o título \"Comprovante de reserva\"" "200 Comprovante de reserva" \
        "$STATUS $(field '.title')"
    request POST "/reservations/$first/check-in"
    expect "check-in responde 200 com CHECKED_IN" "200 CHECKED_IN" "$STATUS $(field '.status')"
    request POST "/reservations/$first/cancellation"
    expect_error "cancelar depois do check-in: 409 OPERATION_NOT_ALLOWED" 409 OPERATION_NOT_ALLOWED
    request POST "/reservations/$first/check-out"
    expect "check-out responde 200 com o comprovante" "200 Comprovante de reserva $first" \
        "$STATUS $(field '.title') $(field '.reservationId')"
    request GET "/reservations/$first"
    expect "a reserva fica CHECKED_OUT" "CHECKED_OUT" "$(field '.status')"

    create 102 2029-07-20 2029-07-21 1 STANDARD
    local standard="$RID"
    request POST "/reservations/$standard/confirmation"
    request POST "/reservations/$standard/cancellation"
    expect "cancelar STANDARD confirmada devolve o pago (299.88)" "CANCELLED 299.88" \
        "$(field '.status') $(money "$(field '.refund')")"
    request POST "/reservations/$non_refundable/confirmation"
    request POST "/reservations/$non_refundable/cancellation"
    expect "cancelar NON_REFUNDABLE confirmada não devolve nada (0.00)" "0.00" "$(money "$(field '.refund')")"
    create 202 2029-08-01 2029-08-02 1 STANDARD
    request POST "/reservations/$RID/cancellation"
    expect "cancelar reserva pendente não devolve nada (0.00)" "0.00" "$(money "$(field '.refund')")"
    create 102 2029-07-20 2029-07-21 1 STANDARD
    expect "a reserva cancelada libera o quarto" "201" "$STATUS"

    request GET "/reports/occupancy?date=2029-07-10"
    expect "o relatório de ocupação responde 200 em text/csv" "200 text/csv" "$STATUS ${CONTENT_TYPE%%;*}"
    expect_line "ocupação: o cabeçalho" "room,type,reservation,check_in,check_out"
    expect_line "ocupação: a reserva com check-out conta na noite de entrada" "101,STANDARD,$first,2029-07-10,2029-07-12"
    expect_line "ocupação: o total" "total,1/5"
    request GET "/reports/revenue?month=2029-07"
    expect_line "financeiro: o cabeçalho" "reservation,room,check_in,check_out,status,amount"
    expect_line "financeiro: a reserva paga" "$first,101,2029-07-10,2029-07-12,CHECKED_OUT,599.76"
    expect_line "financeiro: o total (a cancelada com devolução não conta)" "total,599.76"
    request GET "/reports/revenue?month=2029-03"
    expect_line "financeiro: a NON_REFUNDABLE cancelada continua contando" \
        "$non_refundable,101,2029-03-10,2029-03-11,CANCELLED,212.42"
    request GET "/reports/revenue?month=julho"
    expect_error "mês fora do formato: 400 INVALID_REQUEST" 400 INVALID_REQUEST

    section "Os dados continuam depois de reiniciar o serviço (o banco)"
    local before
    before="$(message_count)"
    if start_again; then
        request GET "/reservations/$first"
        expect "a reserva continua lá, CHECKED_OUT" "200 CHECKED_OUT" "$STATUS $(field '.status')"
        expect "as mensagens continuam lá" "$before" "$(message_count)"
        create 101 2029-07-11 2029-07-12 1 STANDARD
        expect_error "o quarto continua ocupado depois de reiniciar" 409 ROOM_UNAVAILABLE
    else
        bad "o serviço sobe de novo sobre o mesmo banco" "subir" "não subiu"
        return 1
    fi
}

# two reservations of the same room and nights, sent at the same moment, ten times: only one may be created
concurrency_check() {
    section "Duas reservas do mesmo quarto, ao mesmo tempo"
    local round double=0 none=0 statuses
    for round in 1 2 3 4 5 6 7 8 9 10; do
        local day check_in check_out
        day=$((round * 2))
        check_in="$(printf '2030-02-%02d' "$day")"
        check_out="$(printf '2030-02-%02d' $((day + 1)))"
        local body
        body="$(reservation_body 301 "$check_in" "$check_out" 2 STANDARD)"
        local racers=()
        for request_number in 1 2; do
            curl -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' \
                --data-binary "$body" --max-time 30 "$BASE_URL/reservations" > "$WORK/race-$request_number" &
            racers+=($!)
        done
        # only the two calls: a bare `wait` would also wait for the service, which runs in the background too
        wait "${racers[@]}"
        statuses="$(cat "$WORK/race-1" "$WORK/race-2" | sort | paste -sd' ' -)"
        case "$statuses" in
            "201 201") double=$((double + 1)) ;;
            "201 409") ;;
            *) none=$((none + 1)); note "rodada $round: $statuses" ;;
        esac
    done
    expect "em 10 rodadas, nenhuma com as duas reservas criadas" "0" "$double"
    expect "em 10 rodadas, todas com uma criada e a outra recusada com 409" "0" "$none"
}

behavior_end() {
    stop_service
    {
        echo "# Comportamento: o roteiro do cenário $SCENARIO"
        echo
        echo "Resultado: $PASSED de $((PASSED + FAILED)) conferências passaram ($FAILED falharam)."
        awk '/^## / {if (name) printf "- %s: %d de %d\n", name, passed, passed + failed; name = substr($0, 4);
                     passed = 0; failed = 0}
             /^- OK:/ {passed++} /^- FALHOU:/ {failed++}
             END {if (name) printf "- %s: %d de %d\n", name, passed, passed + failed}' "$BODY_FILE"
        cat "$BODY_FILE"
    } > "$OUT"
    echo "behavior: $OUT"
}
