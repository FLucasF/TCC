"""The regression: what the base does today and every scenario must keep doing. A scenario that legitimately changes
something (the season's percentage in 02, the report title in 09...) skips or adapts only that part; the rest still
counts. Which parts each scenario changes is decided here, from the scenario texts, never from an agent's output.
"""

from collections.abc import Callable

from checks import (
    Check,
    create_reservation,
    expect,
    expect_equal,
    expect_money,
    expect_status,
    messages_to,
    operate,
    report,
    reservation_body,
)
from hotel_service import HotelApi, HotelLauncher

REPORT_TITLE_PREFIX = "Pousada Maré Alta,"


def rooms(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        response = api.get("/rooms")
        expect_status(response, 200, "GET /rooms")
        listed = response.json()
        expect_equal([room["number"] for room in listed], [101, 102, 201, 202, 301], "room numbers")
        expect_equal([room["type"] for room in listed], ["STANDARD", "STANDARD", "DELUXE", "DELUXE", "SUITE"],
                     "room types")
        expect_equal([room["capacity"] for room in listed], [2, 2, 3, 3, 4], "capacities")
        for room, rate in zip(listed, ["249.90", "249.90", "389.90", "389.90", "579.90"], strict=True):
            expect_money(room["nightlyRate"], rate, f"nightly rate of room {room['number']}")


def low_season_price(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        reservation = create_reservation(api, reservation_body(201, "2027-03-10", "2027-03-12", guests=2))
        expect_equal(reservation["status"], "PENDING", "status")
        expect_equal(reservation["nights"], 2, "nights")
        expect_money(reservation["subtotal"], "779.80", "subtotal")
        expect_money(reservation["discount"], "0.00", "discount")
        expect_money(reservation["total"], "779.80", "total")


def non_refundable_rounds_half_even(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        reservation = create_reservation(api, reservation_body(101, "2027-03-10", "2027-03-11",
                                                               rate_plan="NON_REFUNDABLE"))
        expect_money(reservation["discount"], "37.48", "15% of 249.90 (37.485, half-even)")
        expect_money(reservation["total"], "212.42", "total")


def high_season_price(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        suite_in_january = create_reservation(api, reservation_body(301, "2027-01-05", "2027-01-09", guests=2,
                                                                    rate_plan="NON_REFUNDABLE"))
        expect_money(suite_in_january["subtotal"], "2783.52", "4 high-season nights in the suite")
        expect_money(suite_in_january["discount"], "417.53", "discount")
        expect_money(suite_in_january["total"], "2365.99", "total")
        december_boundary = create_reservation(api, reservation_body(101, "2027-12-14", "2027-12-16"))
        expect_money(december_boundary["total"], "549.78", "December 14 (low) + December 15 (high)")
        leap_day_boundary = create_reservation(api, reservation_body(102, "2028-02-29", "2028-03-02"))
        expect_money(leap_day_boundary["total"], "549.78", "February 29 (high) + March 1 (low)")
        july = create_reservation(api, reservation_body(201, "2027-06-30", "2027-07-02"))
        expect_money(july["total"], "857.78", "June 30 (low) + July 1 (high)")


def refusals(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        same_day = api.post("/reservations", reservation_body(101, "2027-03-10", "2027-03-10"))
        expect_status(same_day, 400, "check-out on the check-in day", "INVALID_REQUEST")
        no_guests = api.post("/reservations", reservation_body(101, "2027-03-10", "2027-03-11", guests=0))
        expect_status(no_guests, 400, "zero guests", "INVALID_REQUEST")
        no_room = api.post("/reservations", reservation_body(999, "2027-03-10", "2027-03-11"))
        expect_status(no_room, 404, "room 999", "ROOM_NOT_FOUND")
        too_many = api.post("/reservations", reservation_body(101, "2027-03-10", "2027-03-11", guests=3))
        expect_status(too_many, 422, "3 guests in a room for 2", "CAPACITY_EXCEEDED")
        without_name = reservation_body(101, "2027-03-10", "2027-03-11")
        del without_name["guestName"]
        expect_status(api.post("/reservations", without_name), 400, "no guest name", "INVALID_REQUEST")
        expect_status(api.post("/reservations", raw_body="not json"), 400, "a body that is not JSON",
                      "INVALID_REQUEST")
        unknown_plan = api.post("/reservations", reservation_body(101, "2027-03-10", "2027-03-11", rate_plan="VIP"))
        expect_status(unknown_plan, 400, "an unknown rate plan", "INVALID_REQUEST")
        expect_status(api.get("/reservations/4242"), 404, "an unknown reservation", "RESERVATION_NOT_FOUND")


def room_availability(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        first = create_reservation(api, reservation_body(201, "2027-03-10", "2027-03-13"))
        overlapping = api.post("/reservations", reservation_body(201, "2027-03-12", "2027-03-14"))
        expect_status(overlapping, 409, "an overlapping stay in the same room", "ROOM_UNAVAILABLE")
        create_reservation(api, reservation_body(201, "2027-03-13", "2027-03-15"))  # the check-out day is free
        create_reservation(api, reservation_body(202, "2027-03-10", "2027-03-13"))  # another room
        operate(api, first["id"], "cancellation")
        create_reservation(api, reservation_body(201, "2027-03-10", "2027-03-13"))  # a cancellation frees the room


def stay_lifecycle(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        reservation = create_reservation(api, reservation_body(202, "2027-03-10", "2027-03-12", guests=3))
        reservation_id = reservation["id"]
        operate(api, reservation_id, "check-in", expected_status=409)
        confirmed = operate(api, reservation_id, "confirmation")
        expect_equal(confirmed["status"], "CONFIRMED", "status after the confirmation")
        expect_money(confirmed["amountPaid"], "779.80", "amount paid")
        operate(api, reservation_id, "confirmation", expected_status=409)
        operate(api, reservation_id, "check-out", expected_status=409)
        expect_equal(operate(api, reservation_id, "check-in")["status"], "CHECKED_IN", "status after the check-in")
        receipt = operate(api, reservation_id, "check-out")
        expect_equal(receipt["title"], "Comprovante de reserva", "check-out receipt title")
        expect_equal(receipt["reservationId"], reservation_id, "check-out receipt reservation")
        expect_money(receipt["total"], "779.80", "check-out receipt total")
        expect_equal(api.get(f"/reservations/{reservation_id}").json()["status"], "CHECKED_OUT", "final status")


def cancellation_refunds(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        pending = create_reservation(api, reservation_body(101, "2027-03-10", "2027-03-11"))
        standard = create_reservation(api, reservation_body(102, "2027-03-10", "2027-03-11"))
        non_refundable = create_reservation(api, reservation_body(201, "2027-03-10", "2027-03-11",
                                                                  rate_plan="NON_REFUNDABLE"))
        checked_in = create_reservation(api, reservation_body(202, "2027-03-10", "2027-03-11"))
        for paid in (standard, non_refundable, checked_in):
            operate(api, paid["id"], "confirmation")
        operate(api, checked_in["id"], "check-in")
        expect_money(operate(api, pending["id"], "cancellation")["refund"], "0.00", "refund of an unpaid reservation")
        cancelled_standard = operate(api, standard["id"], "cancellation")
        expect_equal(cancelled_standard["status"], "CANCELLED", "status after the cancellation")
        expect_money(cancelled_standard["refund"], "249.90", "STANDARD gives everything back")
        expect_money(operate(api, non_refundable["id"], "cancellation")["refund"], "0.00",
                     "NON_REFUNDABLE gives nothing back")
        operate(api, checked_in["id"], "cancellation", expected_status=409)


def receipt_copy(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        reservation = create_reservation(api, reservation_body(101, "2027-03-10", "2027-03-11",
                                                               rate_plan="NON_REFUNDABLE"))
        expect_status(api.get(f"/reservations/{reservation['id']}/document"), 409, "a copy before paying",
                      "OPERATION_NOT_ALLOWED")
        operate(api, reservation["id"], "confirmation")
        response = api.get(f"/reservations/{reservation['id']}/document")
        expect_status(response, 200, "a copy after paying")
        copy = response.json()
        expect_equal(copy["title"], "Comprovante de reserva", "title")
        expect_equal(copy["guestCpf"], "123.456.789-09", "guest CPF")
        expect_money(copy["discount"], "37.48", "discount")
        expect_money(copy["total"], "212.42", "total")


def confirmation_email(scenario: str) -> Callable[[HotelLauncher], None]:
    subject_changes = scenario == "04"

    def run(hotel: HotelLauncher) -> None:
        with hotel.running() as api:
            reservation = create_reservation(api, reservation_body(101, "2027-03-10", "2027-03-11",
                                                                   rate_plan="NON_REFUNDABLE"))
            expect_equal(messages_to(api, "ana@example.com"), [], "messages before the confirmation")
            operate(api, reservation["id"], "confirmation")
            to_guest = messages_to(api, "ana@example.com")
            expect_equal(len(to_guest), 1, "e-mails to the guest after the confirmation")
            email = to_guest[0]
            expect_equal(email.get("channel"), "EMAIL", "channel")
            if not subject_changes:
                expect_equal(email.get("subject"), "Reserva confirmada - Pousada Maré Alta", "subject")
            body = str(email.get("body"))
            for expected_text in ("Olá, Ana Souza!", "Comprovante de reserva nº", "Quarto: 101 (STANDARD)",
                                  "Desconto: R$ 37,48", "Total: R$ 212,42", "Até breve!"):
                expect(expected_text in body, f"the confirmation e-mail should contain {expected_text!r}")

    return run


def strip_report_title(csv: str, scenario: str) -> str:
    """Scenario 09 adds a title line to every report; anywhere else, a title line is a regression."""
    if scenario == "09" and csv.startswith(REPORT_TITLE_PREFIX):
        return csv.split("\n", 1)[1]
    return csv


def book_some_april_stays(api: HotelApi) -> None:
    april = create_reservation(api, reservation_body(201, "2027-04-01", "2027-04-03"))
    late_march = create_reservation(api, reservation_body(101, "2027-03-30", "2027-04-02",
                                                          rate_plan="NON_REFUNDABLE"))
    suite = create_reservation(api, reservation_body(301, "2027-04-02", "2027-04-04"))
    create_reservation(api, reservation_body(102, "2027-04-01", "2027-04-05"))  # never paid
    refunded = create_reservation(api, reservation_body(202, "2027-04-01", "2027-04-02"))
    kept = create_reservation(api, reservation_body(102, "2027-04-20", "2027-04-21", rate_plan="NON_REFUNDABLE"))
    for paid in (april, late_march, suite, refunded, kept):
        operate(api, paid["id"], "confirmation")
    operate(api, suite["id"], "check-in")
    operate(api, refunded["id"], "cancellation")
    operate(api, kept["id"], "cancellation")


def occupancy_report(scenario: str) -> Callable[[HotelLauncher], None]:
    def run(hotel: HotelLauncher) -> None:
        with hotel.running() as api:
            book_some_april_stays(api)
            csv = strip_report_title(report(api, "/reports/occupancy?date=2027-04-01"), scenario)
            expect_equal(csv, "room,type,reservation,check_in,check_out\n"
                              "101,STANDARD,2,2027-03-30,2027-04-02\n"
                              "201,DELUXE,1,2027-04-01,2027-04-03\n"
                              "total,2/5\n", "occupancy on 2027-04-01")
            csv = strip_report_title(report(api, "/reports/occupancy?date=2027-04-03"), scenario)
            expect_equal(csv, "room,type,reservation,check_in,check_out\n"
                              "301,SUITE,3,2027-04-02,2027-04-04\n"
                              "total,1/5\n", "occupancy on 2027-04-03 (the check-out night is free)")

    return run


def revenue_report(scenario: str) -> Callable[[HotelLauncher], None]:
    def run(hotel: HotelLauncher) -> None:
        with hotel.running() as api:
            book_some_april_stays(api)
            csv = strip_report_title(report(api, "/reports/revenue?month=2027-04"), scenario)
            expect_equal(csv, "reservation,room,check_in,check_out,status,amount\n"
                              "1,201,2027-04-01,2027-04-03,CONFIRMED,779.80\n"
                              "3,301,2027-04-02,2027-04-04,CHECKED_IN,1159.80\n"
                              "6,102,2027-04-20,2027-04-21,CANCELLED,212.42\n"
                              "total,2152.02\n", "revenue of April 2027")
            csv = strip_report_title(report(api, "/reports/revenue?month=2027-05"), scenario)
            expect_equal(csv, "reservation,room,check_in,check_out,status,amount\ntotal,0.00\n", "an empty month")

    return run


def report_parameters(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        expect_status(api.get("/reports/occupancy"), 400, "occupancy without a date", "INVALID_REQUEST")
        expect_status(api.get("/reports/occupancy?date=10/04/2027"), 400, "a date in the wrong format",
                      "INVALID_REQUEST")
        expect_status(api.get("/reports/revenue?month=july"), 400, "a month that is not a month", "INVALID_REQUEST")


# The scenarios that change what a check looks at, so the check is left to that scenario's acceptance.
SKIPPED_IN: dict[str, set[str]] = {
    "high_season_price": {"02"},     # the season becomes 25% (and Easter joins it)
    "occupancy_report": {"10"},      # the occupancy report gains the guest column
}


def regression_checks(scenario: str) -> list[Check]:
    all_checks = [
        Check("rooms", rooms),
        Check("low_season_price", low_season_price),
        Check("non_refundable_rounds_half_even", non_refundable_rounds_half_even),
        Check("high_season_price", high_season_price),
        Check("refusals", refusals),
        Check("room_availability", room_availability),
        Check("stay_lifecycle", stay_lifecycle),
        Check("cancellation_refunds", cancellation_refunds),
        Check("receipt_copy", receipt_copy),
        Check("confirmation_email", confirmation_email(scenario)),
        Check("occupancy_report", occupancy_report(scenario)),
        Check("revenue_report", revenue_report(scenario)),
        Check("report_parameters", report_parameters),
    ]
    return [check for check in all_checks if scenario not in SKIPPED_IN.get(check.name, set())]
