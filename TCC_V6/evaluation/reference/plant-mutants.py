"""Validates the suite itself: plants one known bug at a time in a pilot's service, rebuilds it and runs
the suite. A suite that misses a planted bug has a hole. The mutants below are written for the code of
runs/pilots/OPUS55-N0-03 (each one is a literal text replacement in one file of that service).

Usage (from TCC_V6):  python evaluation/reference/plant-mutants.py runs/pilots/OPUS55-N0-03 [mutant-name ...]
Output: <pilot>/suite-mutants.txt, one block per mutant (caught or MISSED, and the groups that failed).
"""
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

JAVA = "src/main/java/com/loja/orders/"
RES = "src/main/resources/"

# name -> (what the bug is, file, original text, mutated text)
MUTANTS = {
    "deadline-48h-exact-refused": (
        "a payment at exactly 48 hours is refused",
        JAVA + "order/OrderService.java", "compareTo(PAYMENT_DEADLINE) > 0", "compareTo(PAYMENT_DEADLINE) >= 0"),
    "repeated-notice-not-recognized": (
        "the same transactionId again is treated as a new notice",
        JAVA + "order/OrderService.java", "if (previousNotice.isPresent())", "if (false && previousNotice.isPresent())"),
    "stock-one-item-is-enough": (
        "payment goes through if ANY item has stock",
        JAVA + "catalog/InventoryService.java", ".allMatch(item -> stock", ".anyMatch(item -> stock"),
    "cancel-paid-keeps-stock": (
        "cancelling a paid order does not return the stock",
        JAVA + "catalog/StockReturnListener.java", "if (previousStatus.paymentSettled())", "if (false)"),
    "cancel-paid-keeps-credit": (
        "cancelling a paid order does not remove the credit",
        JAVA + "customer/ClubCreditListener.java", "nextPurchaseCredit().negate()", "nextPurchaseCredit().multiply(java.math.BigDecimal.ZERO)"),
    "deliver-straight-from-paid": (
        "a paid order can be delivered without being shipped",
        JAVA + "order/OrderStatus.java", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED))", "NEXT.put(PAID, EnumSet.of(SHIPPED, CANCELLED, DELIVERED))"),
    "no-shipped-notification": (
        "shipping creates no notification",
        JAVA + "notification/CustomerNotificationListener.java",
        "notifications.add(order.cpf(), order.id(), NotificationType.ORDER_SHIPPED);", ""),
    "cancelled-order-counts-as-purchase": (
        "a cancelled order blocks WELCOME10 (counts as a first purchase)",
        JAVA + "order/OrderRepository.java", ".filter(OrderStatus::paymentSettled)", ".filter(s -> s != OrderStatus.AWAITING_PAYMENT)"),
    "restart-resets-stock": (
        "the seed overwrites the stock on every start",
        RES + "data.sql", "-- Catálogo inicial.",
        "UPDATE product SET stock = 50 WHERE sku = 'CAM-001'; UPDATE product SET stock = 10 WHERE sku = 'TEN-001'; "
        "UPDATE product SET stock = 5 WHERE sku = 'FON-001'; UPDATE product SET stock = 3 WHERE sku = 'JAQ-001'; "
        "UPDATE product SET stock = 1 WHERE sku = 'MOC-001';\n-- Catálogo inicial."),
    "restart-loses-orders": (
        "the schema drops the orders on every start",
        RES + "schema.sql", "CREATE TABLE IF NOT EXISTS product (",
        "DROP TABLE IF EXISTS notification, payment_notice, order_item, customer_order, customer CASCADE;\n"
        "CREATE TABLE IF NOT EXISTS product ("),
    "stock-read-without-lock": (
        "the stock is read without FOR UPDATE (check-then-act race)",
        JAVA + "catalog/ProductRepository.java", "ORDER BY sku FOR UPDATE", "ORDER BY sku"),
    "payment-without-order-lock": (
        "the payment does not lock the order row (two notices run side by side)",
        JAVA + "order/OrderService.java",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.lockById(id)",
        "public Order registerPayment(String id, String transactionId, String paidAtText) {\n        Order order = orders.findById(id)"),
}


def run(cmd, cwd):
    cmd = [shutil.which(cmd[0]) or cmd[0], *cmd[1:]]          # mvn is mvn.cmd on Windows
    return subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace")


def main():
    pilot = Path(sys.argv[1]).resolve()
    names = sys.argv[2:] or list(MUTANTS)
    here = Path(__file__).resolve().parent
    out_file = pilot / "suite-mutants.txt"
    lines = []
    for name in names:
        what, file, original, mutated = MUTANTS[name]
        with tempfile.TemporaryDirectory(prefix=f"mutant-{name}-") as tmp:
            workspace = Path(tmp) / "workspace"
            shutil.copytree(pilot / "workspace", workspace, ignore=shutil.ignore_patterns("target"))
            target = workspace / file
            text = target.read_text(encoding="utf-8")
            if text.count(original) != 1:
                lines.append(f"[{name}] NOT PLANTED: the original text is not in {file} exactly once")
                continue
            target.write_text(text.replace(original, mutated), encoding="utf-8", newline="")
            build = run(["mvn", "-B", "-q", "-DskipTests", "package"], workspace)
            if build.returncode != 0:
                lines.append(f"[{name}] DID NOT BUILD:\n{build.stdout[-800:]}")
                continue
            report_dir = Path(tmp) / "suite"
            run(["bash", str(here / "run-suite.sh"), workspace.as_posix(), report_dir.as_posix()], here)
            result_file = report_dir / "suite-result.json"
            if not result_file.exists():
                lines.append(f"[{name}] the suite produced no result (see the run by hand)")
                continue
            result = json.loads(result_file.read_text(encoding="utf-8"))
            failed = {g: v for g, v in result["groups"].items() if v["passed"] < v["total"]}
            verdict = "caught" if failed or not result["serviceStarted"] else "MISSED"
            lines.append(f"[{name}] {verdict}: {what}")
            for group, v in failed.items():
                lines.append(f"    {group:12} {v['passed']} of {v['total']}")
                first = next(c for c in result["checks"] if c["group"] == group and c["differences"])
                lines.append(f"      e.g. {first['name']}: {first['differences'][0]}"[:300])
        print(lines[-1].splitlines()[0], flush=True)
    out_file.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"\nwritten to {out_file}")


if __name__ == "__main__":
    main()
