"""Validates the suite itself: plants one known bug at a time in a pilot's service, rebuilds it and runs the suite.
A suite that misses a planted bug has a hole. The mutants below are written for the code of runs/pilots/OPUS55-N0-03
(each one is a literal text replacement in one file of that service).

Usage (from TCC_V6):  python evaluation/reference/plant_mutants.py runs/pilots/OPUS55-N0-03 [mutant-name ...]
Output: <pilot>/suite-mutants.txt, one block per mutant (caught or MISSED, and the groups that failed).
"""

import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
from typing import NamedTuple

from run_suite import run_suite


class Mutant(NamedTuple):
    bug: str              # what is wrong in the service after the replacement
    file: str             # the file of the service, inside the workspace
    original_text: str    # must appear exactly once in the file
    mutated_text: str


JAVA_SOURCES = "src/main/java/com/loja/orders/"
RESOURCES = "src/main/resources/"

MUTANTS = {
    "deadline-48h-exact-refused": Mutant(
        "a payment at exactly 48 hours is refused",
        JAVA_SOURCES + "order/OrderService.java", "compareTo(PAYMENT_DEADLINE) > 0", "compareTo(PAYMENT_DEADLINE) >= 0"),
    "repeated-notice-not-recognized": Mutant(
        "the same transactionId again is treated as a new notice",
        JAVA_SOURCES + "order/OrderService.java", "if (previousNotice.isPresent())", "if (false && previousNotice.isPresent())"),
    "stock-one-item-is-enough": Mutant(
        "payment goes through if ANY item has stock",
        JAVA_SOURCES + "catalog/InventoryService.java", ".allMatch(item -> stock", ".anyMatch(item -> stock"),
    "cancel-paid-keeps-stock": Mutant(
        "cancelling a paid order does not return the stock",
        JAVA_SOURCES + "catalog/StockReturnListener.java", "if (previousStatus.paymentSettled())", "if (false)"),
    "cancel-paid-keeps-credit": Mutant(
        "cancelling a paid order does not remove the credit",
        JAVA_SOURCES + "customer/ClubCreditListener.java", "nextPurchaseCredit().negate()", "nextPurchaseCredit().multiply(java.math.BigDecimal.ZERO)"),
    "deliver-straight-from-paid": Mutant(
        "a paid order can be delivered without being shipped",
        JAVA_SOURCES + "order/OrderStatus.java", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED))", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED, DELIVERED))"),
    "no-shipped-notification": Mutant(
        "shipping creates no notification",
        JAVA_SOURCES + "notification/CustomerNotificationListener.java",
        "notifications.add(order.cpf(), order.id(), NotificationType.ORDER_SHIPPED);", ""),
    "cancelled-order-counts-as-purchase": Mutant(
        "a cancelled order blocks WELCOME10 (counts as a first purchase)",
        JAVA_SOURCES + "order/OrderRepository.java", ".filter(OrderStatus::paymentSettled)", ".filter(s -> s != OrderStatus.AWAITING_PAYMENT)"),
    "restart-resets-stock": Mutant(
        "the seed overwrites the stock on every start",
        RESOURCES + "data.sql", "-- Catálogo inicial.",
        "UPDATE product SET stock = 50 WHERE sku = 'CAM-001'; UPDATE product SET stock = 10 WHERE sku = 'TEN-001'; "
        "UPDATE product SET stock = 5 WHERE sku = 'FON-001'; UPDATE product SET stock = 3 WHERE sku = 'JAQ-001'; "
        "UPDATE product SET stock = 1 WHERE sku = 'MOC-001';\n-- Catálogo inicial."),
    "restart-loses-orders": Mutant(
        "the schema drops the orders on every start",
        RESOURCES + "schema.sql", "CREATE TABLE IF NOT EXISTS product (",
        "DROP TABLE IF EXISTS notification, payment_notice, order_item, customer_order, customer CASCADE;\n"
        "CREATE TABLE IF NOT EXISTS product ("),
    "stock-read-without-lock": Mutant(
        "the stock is read without FOR UPDATE (check-then-act race)",
        JAVA_SOURCES + "catalog/ProductRepository.java", "ORDER BY sku FOR UPDATE", "ORDER BY sku"),
    "payment-without-order-lock": Mutant(
        "the payment does not lock the order row (two notices run side by side)",
        JAVA_SOURCES + "order/OrderService.java",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.lockById(id)",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.findById(id)"),
}


def build_service(workspace: Path) -> subprocess.CompletedProcess[str]:
    maven = shutil.which("mvn") or "mvn"            # mvn is mvn.cmd on Windows
    return subprocess.run([maven, "-B", "-q", "-DskipTests", "package"], cwd=workspace, capture_output=True,
                          text=True, encoding="utf-8", errors="replace", check=False)


def try_mutant(pilot: Path, name: str, mutant: Mutant) -> list[str]:
    """Plants one mutant in a copy of the pilot's workspace, rebuilds and runs the suite; returns the report lines."""
    with tempfile.TemporaryDirectory(prefix=f"mutant-{name}-") as temporary:
        workspace = Path(temporary) / "workspace"
        shutil.copytree(pilot / "workspace", workspace, ignore=shutil.ignore_patterns("target"))
        mutated_file = workspace / mutant.file
        source = mutated_file.read_text(encoding="utf-8")
        if source.count(mutant.original_text) != 1:
            return [f"[{name}] NOT PLANTED: the original text is not in {mutant.file} exactly once"]
        mutated_file.write_text(source.replace(mutant.original_text, mutant.mutated_text), encoding="utf-8", newline="")
        build = build_service(workspace)
        if build.returncode != 0:
            return [f"[{name}] DID NOT BUILD:\n{build.stdout[-800:]}"]

        suite_folder = Path(temporary) / "suite"
        run_suite(workspace, suite_folder, echo=False)
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
        sys.exit("usage: python plant_mutants.py <pilot-folder> [mutant-name ...]")
    pilot = Path(sys.argv[1]).resolve()
    chosen = sys.argv[2:] or list(MUTANTS)
    report: list[str] = []
    for name in chosen:
        lines = try_mutant(pilot, name, MUTANTS[name])
        print(lines[0], flush=True)
        report += lines
    report_file = pilot / "suite-mutants.txt"
    report_file.write_text("\n".join(report) + "\n", encoding="utf-8", newline="\n")
    print(f"\nwritten to {report_file}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
