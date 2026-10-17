"""Validates the suite itself: plants one known bug at a time in a pilot's service, rebuilds it and runs the suite.
A suite that misses a planted bug has a hole. The mutants below are written for the code of runs/pilots/OPUS55-N0-03
(each one is a literal text replacement in one file of that service).

Usage (from TCC_V6):  python evaluation/reference/check_suite_catches_planted_bugs.py runs/pilots/OPUS55-N0-03 [mutant-name ...] [--machines 2]
Output: <pilot>/suite-mutants.txt, one block per mutant (caught or MISSED, and the groups that failed).
"""

import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import NamedTuple

from run_suite_against_service import run_suite


class Change(NamedTuple):
    file: str             # the file of the service, inside the workspace
    original_text: str    # must appear exactly once in the file
    mutated_text: str


class Mutant(NamedTuple):
    bug: str              # what is wrong in the service after the changes
    changes: tuple[Change, ...]


def one_change(bug: str, file: str, original_text: str, mutated_text: str) -> Mutant:
    return Mutant(bug, (Change(file, original_text, mutated_text),))


JAVA_SOURCES = "src/main/java/com/loja/orders/"
RESOURCES = "src/main/resources/"

MUTANTS = {
    "deadline-48h-exact-refused": one_change(
        "a payment at exactly 48 hours is refused",
        JAVA_SOURCES + "order/OrderService.java", "compareTo(PAYMENT_DEADLINE) > 0", "compareTo(PAYMENT_DEADLINE) >= 0"),
    "repeated-notice-not-recognized": one_change(
        "the same transactionId again is treated as a new notice",
        JAVA_SOURCES + "order/OrderService.java", "if (previousNotice.isPresent())", "if (false && previousNotice.isPresent())"),
    "stock-one-item-is-enough": one_change(
        "payment goes through if ANY item has stock",
        JAVA_SOURCES + "catalog/InventoryService.java", ".allMatch(item -> stock", ".anyMatch(item -> stock"),
    "cancel-paid-keeps-stock": one_change(
        "cancelling a paid order does not return the stock",
        JAVA_SOURCES + "catalog/StockReturnListener.java", "if (previousStatus.paymentSettled())", "if (false)"),
    "cancel-paid-keeps-credit": one_change(
        "cancelling a paid order does not remove the credit",
        JAVA_SOURCES + "customer/ClubCreditListener.java", "nextPurchaseCredit().negate()", "nextPurchaseCredit().multiply(java.math.BigDecimal.ZERO)"),
    "deliver-straight-from-paid": one_change(
        "a paid order can be delivered without being shipped",
        JAVA_SOURCES + "order/OrderStatus.java", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED))", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED, DELIVERED))"),
    "no-shipped-notification": one_change(
        "shipping creates no notification",
        JAVA_SOURCES + "notification/CustomerNotificationListener.java",
        "notifications.add(order.cpf(), order.id(), NotificationType.ORDER_SHIPPED);", ""),
    "cancelled-order-counts-as-purchase": one_change(
        "a cancelled order blocks WELCOME10 (counts as a first purchase)",
        JAVA_SOURCES + "order/OrderRepository.java", ".filter(OrderStatus::paymentSettled)", ".filter(s -> s != OrderStatus.AWAITING_PAYMENT)"),
    "restart-resets-stock": one_change(
        "the seed overwrites the stock on every start",
        RESOURCES + "data.sql", "-- Catálogo inicial.",
        "UPDATE product SET stock = 50 WHERE sku = 'CAM-001'; UPDATE product SET stock = 10 WHERE sku = 'TEN-001'; "
        "UPDATE product SET stock = 5 WHERE sku = 'FON-001'; UPDATE product SET stock = 3 WHERE sku = 'JAQ-001'; "
        "UPDATE product SET stock = 1 WHERE sku = 'MOC-001';\n-- Catálogo inicial."),
    "restart-loses-orders": one_change(
        "the schema drops the orders on every start",
        RESOURCES + "schema.sql", "CREATE TABLE IF NOT EXISTS product (",
        "DROP TABLE IF EXISTS notification, payment_notice, order_item, customer_order, customer CASCADE;\n"
        "CREATE TABLE IF NOT EXISTS product ("),
    "stock-read-without-lock": one_change(
        "the stock is read without FOR UPDATE (check-then-act race)",
        JAVA_SOURCES + "catalog/ProductRepository.java", "ORDER BY sku FOR UPDATE", "ORDER BY sku"),
    "payment-without-order-lock": one_change(
        "the payment does not lock the order row (two notices run side by side)",
        JAVA_SOURCES + "order/OrderService.java",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.lockById(id)",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.findById(id)"),
    # The two machines (decided on 2026-10-14): no lock in the database, one lock in memory around the payment. On
    # one machine it is correct (the payment, commit included, runs one at a time); on two, the races go through.
    "lock-in-memory": Mutant(
        "the payment is locked in memory (synchronized), not in the database",
        (Change(JAVA_SOURCES + "order/OrderController.java", "public OrderResponse pay(", "public synchronized OrderResponse pay("),
         Change(JAVA_SOURCES + "order/OrderRepository.java", "WHERE id = :id FOR UPDATE", "WHERE id = :id"),
         Change(JAVA_SOURCES + "catalog/ProductRepository.java", "ORDER BY sku FOR UPDATE", "ORDER BY sku"))),
}


def build_service(workspace: Path) -> subprocess.CompletedProcess[str]:
    maven = shutil.which("mvn") or "mvn"            # mvn is mvn.cmd on Windows
    return subprocess.run([maven, "-B", "-q", "-DskipTests", "package"], cwd=workspace, capture_output=True,
                          text=True, encoding="utf-8", errors="replace", check=False)


def try_mutant(pilot: Path, name: str, mutant: Mutant, machines: int = 2) -> list[str]:
    """Plants one mutant in a copy of the pilot's workspace, rebuilds and runs the suite; returns the report lines."""
    with tempfile.TemporaryDirectory(prefix=f"mutant-{name}-") as temporary:
        workspace = Path(temporary) / "workspace"
        shutil.copytree(pilot / "workspace", workspace, ignore=shutil.ignore_patterns("target"))
        for change in mutant.changes:
            mutated_file = workspace / change.file
            source = mutated_file.read_text(encoding="utf-8")
            if source.count(change.original_text) != 1:
                return [f"[{name}] NOT PLANTED: the original text is not in {change.file} exactly once"]
            mutated_file.write_text(source.replace(change.original_text, change.mutated_text), encoding="utf-8", newline="")
        build = build_service(workspace)
        if build.returncode != 0:
            return [f"[{name}] DID NOT BUILD:\n{build.stdout[-800:]}"]

        suite_folder = Path(temporary) / "suite"
        run_suite(workspace, suite_folder, echo=False, machines=machines)
        result_file = suite_folder / "suite-result.json"
        if not result_file.exists():
            return [f"[{name}] the suite produced no result (see the run by hand)"]
        result = json.loads(result_file.read_text(encoding="utf-8"))
        failed_groups = {group: totals for group, totals in result["groups"].items() if totals["passed"] < totals["total"]}
        verdict = "caught" if failed_groups or not result["serviceStarted"] else "MISSED"
        report = [f"[{name}] {verdict}: {mutant.bug}"]
        for group, totals in failed_groups.items():
            report.append(f"    {group:12} {totals['passed']} of {totals['total']}")
            first_failed = next(check for check in result["checks"] if check["group"] == group and check["differences"])
            report.append(f"      e.g. {first_failed['name']}: {first_failed['differences'][0]}"[:300])
        return report


def main() -> None:
    if len(sys.argv) < 2:
        sys.exit("usage: python check_suite_catches_planted_bugs.py <pilot-folder> [mutant-name ...] [--machines 2]")
    arguments = sys.argv[1:]
    machines = 2
    if "--machines" in arguments:
        position = arguments.index("--machines")
        machines = int(arguments[position + 1])
        del arguments[position:position + 2]
    pilot = Path(arguments[0]).resolve()
    chosen = arguments[1:] or list(MUTANTS)
    report: list[str] = []
    for name in chosen:
        lines = try_mutant(pilot, name, MUTANTS[name], machines)
        print(lines[0], flush=True)
        report += lines
    report_file = pilot / "suite-mutants.txt"
    report_file.write_text("\n".join(report) + "\n", encoding="utf-8", newline="\n")
    print(f"\nwritten to {report_file}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
