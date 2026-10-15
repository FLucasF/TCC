"""Scenario 01, the new rate plans: CORPORATE (a discount per company code), WEEKEND_PACKAGE (Friday to Sunday, a
fixed price per room type) and LONG_STAY (7 nights or more, 10% off). The amounts are the scenario's own examples and
a few more computed the same way (half-even to cents). Only one refusal per call: the text does not order them.
"""

from checks import Check, create_reservation, expect_money, expect_status, operate, reservation_body
from hotel_service import HotelLauncher


def corporate_discounts(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        # two deluxe nights in March (2 × 389.90) for each company
        for room, check_in, check_out, company, discount, total in (
                (201, "2027-03-10", "2027-03-12", "TECNOSUL", "93.58", "686.22"),
                (202, "2027-03-10", "2027-03-12", "AGROVALE", "140.36", "639.44"),
                (201, "2027-03-20", "2027-03-22", "FARMAVIDA", "194.95", "584.85")):
            reservation = create_reservation(api, reservation_body(room, check_in, check_out, rate_plan="CORPORATE",
                                                                   companyCode=company))
            expect_money(reservation["subtotal"], "779.80", f"{company}: subtotal")
            expect_money(reservation["discount"], discount, f"{company}: discount")
            expect_money(reservation["total"], total, f"{company}: total")


def corporate_needs_a_known_company(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        # first a valid one, so a service that refuses every CORPORATE reservation does not pass
        create_reservation(api, reservation_body(202, "2027-03-10", "2027-03-12", rate_plan="CORPORATE",
                                                 companyCode="TECNOSUL"))
        without_code = api.post("/reservations", reservation_body(201, "2027-03-10", "2027-03-12",
                                                                  rate_plan="CORPORATE"))
        expect_status(without_code, 400, "CORPORATE without a company code", "INVALID_REQUEST")
        unknown = api.post("/reservations", reservation_body(201, "2027-03-10", "2027-03-12", rate_plan="CORPORATE",
                                                             companyCode="ACME"))
        expect_status(unknown, 400, "CORPORATE with an unknown company", "INVALID_REQUEST")


def weekend_package_prices(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        for room, check_in, check_out, price in ((201, "2027-03-12", "2027-03-14", "820.00"),
                                                 (101, "2027-03-12", "2027-03-14", "520.00"),
                                                 (301, "2027-03-12", "2027-03-14", "1200.00"),
                                                 (202, "2027-07-09", "2027-07-11", "820.00")):
            reservation = create_reservation(api, reservation_body(room, check_in, check_out,
                                                                   rate_plan="WEEKEND_PACKAGE"))
            what = f"weekend package in room {room} from {check_in}"
            expect_money(reservation["subtotal"], price, f"{what}: subtotal")
            expect_money(reservation["discount"], "0.00", f"{what}: discount")
            expect_money(reservation["total"], price, f"{what}: total (the same in high season)")


def weekend_package_only_friday_to_sunday(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        # first a valid one, so a service that refuses every package does not pass
        create_reservation(api, reservation_body(202, "2027-03-12", "2027-03-14", rate_plan="WEEKEND_PACKAGE"))
        for check_in, check_out, why in (("2027-03-13", "2027-03-15", "Saturday to Monday"),
                                         ("2027-03-12", "2027-03-13", "Friday to Saturday (one night)"),
                                         ("2027-03-12", "2027-03-15", "Friday to Monday (three nights)"),
                                         ("2027-03-11", "2027-03-13", "Thursday to Saturday")):
            response = api.post("/reservations", reservation_body(201, check_in, check_out,
                                                                  rate_plan="WEEKEND_PACKAGE"))
            expect_status(response, 400, f"weekend package {why}", "INVALID_REQUEST")


def long_stay_prices(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        march = create_reservation(api, reservation_body(101, "2027-03-01", "2027-03-08", rate_plan="LONG_STAY"))
        expect_money(march["subtotal"], "1749.30", "long stay in March: subtotal")
        expect_money(march["discount"], "174.93", "long stay in March: discount")
        expect_money(march["total"], "1574.37", "long stay in March: total")
        july = create_reservation(api, reservation_body(101, "2027-07-01", "2027-07-08", rate_plan="LONG_STAY"))
        expect_money(july["subtotal"], "2099.16", "long stay in July (high season): subtotal")
        expect_money(july["discount"], "209.92", "long stay in July: discount (209.916)")
        expect_money(july["total"], "1889.24", "long stay in July: total")
        six_nights = api.post("/reservations", reservation_body(102, "2027-03-01", "2027-03-07",
                                                                rate_plan="LONG_STAY"))
        expect_status(six_nights, 400, "long stay with 6 nights", "INVALID_REQUEST")


def refunds_of_the_new_plans(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        for body, refund, plan in (
                (reservation_body(201, "2027-03-10", "2027-03-12", rate_plan="CORPORATE", companyCode="TECNOSUL"),
                 "686.22", "CORPORATE gives everything back"),
                (reservation_body(202, "2027-03-12", "2027-03-14", rate_plan="WEEKEND_PACKAGE"),
                 "410.00", "WEEKEND_PACKAGE gives half back"),
                (reservation_body(101, "2027-03-01", "2027-03-08", rate_plan="LONG_STAY"),
                 "1574.37", "LONG_STAY gives everything back")):
            reservation = create_reservation(api, body)
            operate(api, reservation["id"], "confirmation")
            expect_money(operate(api, reservation["id"], "cancellation")["refund"], refund, plan)


def company_code_ignored_by_other_plans(hotel: HotelLauncher) -> None:
    with hotel.running() as api:
        reservation = create_reservation(api, reservation_body(201, "2027-03-10", "2027-03-12",
                                                               companyCode="ACME"))
        expect_money(reservation["total"], "779.80", "STANDARD with a company code")


CHECKS = [
    Check("corporate_discounts", corporate_discounts),
    Check("corporate_needs_a_known_company", corporate_needs_a_known_company),
    Check("weekend_package_prices", weekend_package_prices),
    Check("weekend_package_only_friday_to_sunday", weekend_package_only_friday_to_sunday),
    Check("long_stay_prices", long_stay_prices),
    Check("refunds_of_the_new_plans", refunds_of_the_new_plans),
    Check("company_code_ignored_by_other_plans", company_code_ignored_by_other_plans),
]
