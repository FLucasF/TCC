"""What every check is made of: the Check itself, the ways a check fails, and the calls to the hotel that many
checks repeat (create a reservation, confirm it, read the messages).

A check gets the launcher and starts as many fresh services as it needs (`with hotel.running() as api:`).
"""

from collections.abc import Callable
from dataclasses import dataclass
from decimal import Decimal
from typing import Any

from hotel_service import HotelApi, HotelLauncher, HttpResponse


class CheckFailed(Exception):
    pass


@dataclass(frozen=True)
class Check:
    name: str
    run: Callable[[HotelLauncher], None]


def expect(condition: bool, message: str) -> None:
    if not condition:
        raise CheckFailed(message)


def expect_equal(actual: object, expected: object, what: str) -> None:
    if actual != expected:
        raise CheckFailed(f"{what}: expected {expected!r}, got {actual!r}")


def money(value: Any) -> Decimal:
    """Money as the service wrote it (a JSON number, or a string), as an exact Decimal."""
    if isinstance(value, bool) or value is None:
        raise CheckFailed(f"expected an amount of money, got {value!r}")
    try:
        return Decimal(str(value))
    except ArithmeticError as not_a_number:
        raise CheckFailed(f"expected an amount of money, got {value!r}") from not_a_number


def expect_money(actual: Any, expected: str, what: str) -> None:
    expect_equal(money(actual), Decimal(expected), what)


def expect_status(response: HttpResponse, status: int, what: str, error_code: str | None = None) -> None:
    expect(response.status == status, f"{what}: expected HTTP {status}, got {response.status} ({response.body[:200]})")
    if error_code is not None:
        actual_code = response.json().get("error") if response.body.startswith("{") else None
        expect_equal(actual_code, error_code, f"{what}: error code")


# ---- the calls many checks repeat ------------------------------------------------------------------------------

DEFAULT_GUEST = {"guestName": "Ana Souza", "guestEmail": "ana@example.com", "guestCpf": "123.456.789-09"}


def reservation_body(room_number: int, check_in: str, check_out: str, guests: int = 1,
                     rate_plan: str = "STANDARD", **extra_fields: object) -> dict[str, object]:
    body: dict[str, object] = {"roomNumber": room_number, **DEFAULT_GUEST, "checkIn": check_in,
                               "checkOut": check_out, "guests": guests, "ratePlan": rate_plan}
    body.update(extra_fields)
    return body


def create_reservation(api: HotelApi, body: dict[str, object]) -> Any:
    response = api.post("/reservations", body)
    expect_status(response, 201, f"creating {body}")
    return response.json()


def operate(api: HotelApi, reservation_id: object, operation: str, expected_status: int = 200) -> Any:
    """POST /reservations/{id}/<operation>: confirmation, cancellation, check-in or check-out."""
    response = api.post(f"/reservations/{reservation_id}/{operation}")
    expect_status(response, expected_status, f"{operation} of reservation {reservation_id}")
    return response.json()


def sent_messages(api: HotelApi) -> list[Any]:
    response = api.get("/messages")
    expect_status(response, 200, "GET /messages")
    messages = response.json()
    expect(isinstance(messages, list), f"GET /messages should be a list, got {messages!r}")
    return list(messages)


def messages_to(api: HotelApi, address: str) -> list[Any]:
    return [message for message in sent_messages(api) if message.get("to") == address]


def report(api: HotelApi, path: str) -> str:
    response = api.get(path)
    expect_status(response, 200, f"GET {path}")
    expect(response.content_type.startswith("text/csv"), f"GET {path}: content type {response.content_type!r}")
    return response.body
