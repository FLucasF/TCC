"""What the agent's database itself guarantees, read from the schema its service created (database-schema.json, saved
by run_suite.py after the suite, before the database is removed).

Usage (from TCC_V6):  python evaluation/measures/database.py <runs-folder> <output.csv>

For each run, it reads <run>/suite/database-schema.json and answers:
  stock_guard               does the database refuse a negative stock (a CHECK on the stock column, >= 0)?
  repeated_notice_guard     does the database refuse the same payment notice twice (a primary key, unique
                            constraint or unique index with the transaction id)?
  foreign_keys              how many foreign keys;
  tables_without_primary_key, check_constraints, tables (the application's, without migration-tool tables).

The two guards are the database's own answer to the prompt's rules "o estoque nunca fica negativo" and "o mesmo aviso
pode chegar mais de uma vez": the suite checks the behaviour; this checks whether the database enforces it too.
"""

import csv
import json
import re
import sys
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Any

# tables of migration tools, not of the application
MIGRATION_TABLES = re.compile(r"^(flyway_schema_history|databasechangelog\w*)$")
# a CHECK on a stock-like column that refuses negatives: CHECK ((stock >= 0)), CHECK ((estoque > '-1'::integer))
STOCK_GUARD = re.compile(r"CHECK\s*\(+\s*\(?\w*(?:stock|estoque|inventory|available|disponivel)\w*\)?\s*"
                         r"(?:>=\s*\(?0|>\s*\(?'?-1)", re.IGNORECASE)
TRANSACTION_COLUMN = re.compile(r"transaction|transacao|transação|\btx\w*", re.IGNORECASE)


@dataclass
class DatabaseResult:
    run: str
    situation: str = ""
    tables: int = 0
    tables_without_primary_key: int = 0
    foreign_keys: int = 0
    check_constraints: int = 0
    stock_guard: str = ""
    repeated_notice_guard: str = ""
    evidence: str = ""


def read_schema(path: Path) -> dict[str, list[Any]]:
    """The schema file, checked: three lists (tables, constraints, uniqueIndexes)."""
    raw = json.loads(path.read_text(encoding="utf-8"))
    schema: dict[str, list[Any]] = {}
    for key in ("tables", "constraints", "uniqueIndexes"):
        value = raw.get(key) if isinstance(raw, dict) else None
        if not isinstance(value, list):
            raise ValueError(f"{path}: '{key}' is not a list (the schema was not read)")
        schema[key] = value
    return schema


def table_name(qualified: str) -> str:
    """public.product -> product (the constraints come with the schema name, the tables without)."""
    return qualified.split(".")[-1].strip('"')


def measure(run_folder: Path) -> DatabaseResult:
    result = DatabaseResult(run_folder.name)
    schema_file = run_folder / "suite" / "database-schema.json"
    if not schema_file.is_file():
        result.situation = "no-schema"           # the suite did not run, or ran before the schema was saved
        return result
    suite_result = run_folder / "suite" / "suite-result.json"
    if suite_result.is_file() and json.loads(suite_result.read_text(encoding="utf-8")).get("serviceStarted") is False:
        result.situation = "service-did-not-start"   # it never got to create its tables
        return result
    schema = read_schema(schema_file)
    tables = [name for name in schema["tables"] if not MIGRATION_TABLES.match(name)]
    constraints = [constraint for constraint in schema["constraints"] if table_name(constraint["table"]) in tables]
    if not tables:
        result.situation = "no-tables"
        return result

    with_primary_key = {table_name(c["table"]) for c in constraints if c["type"] == "p"}
    checks = [c for c in constraints if c["type"] == "c"]
    stock_checks = [c for c in checks if STOCK_GUARD.search(c["definition"])]
    unique_definitions = ([f'{c["table"]}: {c["definition"]}' for c in constraints if c["type"] in ("p", "u")]
                          + [f'{index["table"]}: {index["definition"]}' for index in schema["uniqueIndexes"]])
    notice_guards = [definition for definition in unique_definitions if TRANSACTION_COLUMN.search(definition)]

    result.situation = "ok"
    result.tables = len(tables)
    result.tables_without_primary_key = len([table for table in tables if table not in with_primary_key])
    result.foreign_keys = sum(1 for c in constraints if c["type"] == "f")
    result.check_constraints = len(checks)
    result.stock_guard = "yes" if stock_checks else "no"
    result.repeated_notice_guard = "yes" if notice_guards else "no"
    evidence = [f'{c["table"]}: {c["definition"]}' for c in stock_checks] + notice_guards[:1]
    result.evidence = " | ".join(evidence)
    return result


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: python database.py <runs-folder> <output.csv>")
    runs_folder, output_csv = Path(sys.argv[1]), Path(sys.argv[2])
    if output_csv.exists():
        sys.exit(f"{output_csv} already exists: a result is never written over another")
    results = [measure(folder) for folder in sorted(runs_folder.iterdir()) if (folder / "workspace").is_dir()]
    for result in results:
        print(f"{result.run}: {result.situation}, {result.tables} tables, stock guard {result.stock_guard or '-'}, "
              f"repeated-notice guard {result.repeated_notice_guard or '-'}, {result.foreign_keys} foreign keys, "
              f"{result.tables_without_primary_key} tables without primary key")
    with output_csv.open("w", encoding="utf-8", newline="") as file:
        writer = csv.DictWriter(file, fieldnames=list(DatabaseResult.__dataclass_fields__), lineterminator="\n")
        writer.writeheader()
        writer.writerows(asdict(result) for result in results)
    print(f"{len(results)} runs -> {output_csv}")


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
