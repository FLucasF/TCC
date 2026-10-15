"""Reads Semgrep's JSON (one subfolder = one package) and answers the régua's questions for each package.

Usage:  python classify.py <semgrep-output.json> <packages-root> [root-in-json]
Output: CSV on stdout, one line per package (a direct subfolder of the root). [root-in-json] is how the root
appears in the JSON paths when Semgrep ran in a container (e.g. /packages); by default, the root.

HOW TO READ. Semgrep returns a list of marked LINES, each with its rule's name (e.g. "p4-cond-calc" on line 58
of CheckoutService.java). This script groups the marked lines by package and by rule and decides:

  P1 to P4, Strategy (answer_strategy_point(), V5's logic): first it REMOVES what the régua says not to count:
  the refusals (cond-refusal-*), the factories (cond-factory, switch-factory), the registration (the Gold class
  saying it answers "GOLD") and the lists of valid cases (a block citing every case only to check). Then:
    - location: is any line left that names a case outside its home? scattered. Otherwise, isolated.
    - selection: is any left that names AND calculates? conditional-in-calculation. Otherwise, was there a
      factory? single-conditional. Otherwise, lookup.
    - alarm: if the package never cites a name of the point, the agent used other names, and the answer is
      undetermined, never a false "isolated".
  P5, region (answer_region_point(), V5's logic): the form and the proportion (structure, conditional, data).
  P6, Adapter (answer_carrier_point()): does the carrier's vocabulary stay on the carrier's side? isolated or
  leaked. The carrier's side is the files that make the HTTP call, plus the files next to them that do not know
  our cases (its response records, its own enums). Declaring a field with the carrier's name (a response record)
  is not a leak; using it elsewhere is.
  P7, State (answer_status_point()): is every decision about the order status inside the status (the enum, or a
  class per status)? isolated or scattered. Same removals as P1 to P4 (lists of valid statuses, and a label
  that only translates the text to the constant).
  F, Factory (describe_creation(), descriptive, no hypothesis): how the strategy objects are created.

A "marked line" is a text like "com/shop/CheckoutService.java:58". The "evidence" of each package shows only
what COUNTED, with the first marked line of each rule: it is what the human reader checks when they disagree.
"""

import csv
import json
import re
import sys
from collections.abc import Callable
from dataclasses import dataclass, field
from enum import StrEnum
from pathlib import Path
from typing import Any

from points import (
    CARRIER_CODES, CROSS_RULE_PARTNER_HOMES, POINTS, REGION_CLASSES, STATUS, STATUS_CLASSES, STATUS_HOME, Point,
)


class Location(StrEnum):
    ISOLATED = "isolated"
    SCATTERED = "scattered"
    UNDETERMINED = "undetermined"


class Selection(StrEnum):
    LOOKUP = "lookup"
    SINGLE_CONDITIONAL = "single-conditional"
    CONDITIONAL_IN_CALCULATION = "conditional-in-calculation"
    UNDETERMINED = "undetermined"


class Form(StrEnum):
    ENUM_DATA = "enum-data"
    ENUM_BODY = "enum-body"
    CLASSES = "classes"
    MAP = "map"
    SWITCH = "switch"
    OTHER = "other"


class Proportion(StrEnum):
    DATA = "data"
    CONDITIONAL = "conditional"
    STRUCTURE = "structure"
    UNDETERMINED = "undetermined"


class Adapter(StrEnum):
    ISOLATED = "isolated"
    LEAKED = "leaked"
    UNDETERMINED = "undetermined"


# ------------------------------------------------------------------ reading Semgrep's output

@dataclass
class SemgrepFindings:
    """Semgrep's output, grouped by package."""

    # package name -> rule name -> marked lines ("File.java:58")
    marked_lines: dict[str, dict[str, list[str]]] = field(default_factory=dict)
    # "package name|marked line" -> the text of that line
    line_texts: dict[str, str] = field(default_factory=dict)
    # package name -> class name -> the marked lines where an object of it is created for a region
    region_objects: dict[str, dict[str, list[str]]] = field(default_factory=dict)
    # "package name|short file path" -> the file's path inside the package (to read the code around a line)
    source_files: dict[str, str] = field(default_factory=dict)
    warnings: dict[str, list[str]] = field(default_factory=dict)
    broken_packages: set[str] = field(default_factory=set)


def require_type(value: Any, expected_type: type, description: str) -> Any:
    """The JSON comes from outside: a missing or mistyped field stops the run with its name, not later."""
    if not isinstance(value, expected_type):
        sys.exit(f"Semgrep's JSON is not what was expected: {description} should be "
                 f"{expected_type.__name__}, got {value!r:.80}")
    return value


def read_semgrep_output(semgrep_output: Any, root_in_json: str) -> SemgrepFindings:
    root_prefix = root_in_json.replace("\\", "/").rstrip("/") + "/"

    def package_name_of(path: str) -> str:
        return path.replace("\\", "/").replace(root_prefix, "", 1).split("/")[0]

    def path_inside_package_of(path: str) -> str:
        return "/".join(path.replace("\\", "/").replace(root_prefix, "", 1).split("/")[1:])

    def short_path_of(path: str) -> str:
        """The path from the Java package on (com/shop/CheckoutService.java)."""
        return re.sub(r"^.*/java/", "", path_inside_package_of(path))

    findings = SemgrepFindings()
    results = require_type(require_type(semgrep_output, dict, "the output").get("results"), list, "results")
    for result in results:
        path = require_type(result.get("path"), str, "results[].path")
        rule_name = require_type(result.get("check_id"), str, "results[].check_id").split(".")[-1]
        line_number = require_type(result.get("start", {}).get("line"), int, "results[].start.line")
        package_name = package_name_of(path)
        marked_line = f"{short_path_of(path)}:{line_number}"
        findings.source_files[f"{package_name}|{short_path_of(path)}"] = path_inside_package_of(path)
        findings.marked_lines.setdefault(package_name, {}).setdefault(rule_name, []).append(marked_line)
        findings.line_texts[f"{package_name}|{marked_line}"] = result.get("extra", {}).get("lines", "")
        if rule_name == "p5-object":
            class_name = result.get("extra", {}).get("metavars", {}).get("$T", {}).get("abstract_content")
            if class_name:
                findings.region_objects.setdefault(package_name, {}).setdefault(class_name, []).append(marked_line)

    # Semgrep's errors, per package. A part it could not read (PartialParsing, e.g. a new Java feature) keeps the
    # answers and warns; any other error leaves the whole package undetermined.
    for error in semgrep_output.get("errors") or []:
        error_type = error.get("type")
        error_type = error_type[0] if isinstance(error_type, list) else error_type
        path = error.get("path") or ""
        package_name = package_name_of(path) if path else "(batch)"
        line_number = (error.get("spans") or [{}])[0].get("start", {}).get("line", "?")
        findings.warnings.setdefault(package_name, []).append(f"{error_type} {short_path_of(path)}:{line_number}")
        if error_type != "PartialParsing":
            findings.broken_packages.add(package_name)
    if "(batch)" in findings.warnings:
        sys.exit(f"Semgrep reported an error without a file: {'; '.join(findings.warnings['(batch)'])}")
    return findings


# ------------------------------------------------------------------ small helpers on marked lines

def file_of(marked_line: str) -> str:
    return marked_line.rsplit(":", 1)[0]


def line_number_of(marked_line: str) -> int:
    return int(marked_line.rsplit(":", 1)[1])


def file_name_of(marked_line: str) -> str:
    return file_of(marked_line).split("/")[-1]


def folder_of(marked_line: str) -> str:
    return file_of(marked_line).rpartition("/")[0]


def mentions(text: str, name: str) -> bool:
    """The name appears as a whole word."""
    return re.search(rf"\b{re.escape(name)}\b", text) is not None


def cites_every_case(text: str, point: Point) -> bool:
    """Cites ALL the cases of the point: each case by any of its names (GOLD or OURO)."""
    return all(
        any(mentions(text, name) for name in point.names if point.case_of(name) == case)
        for case in point.cases
    )


def looks_like_comparison_list(text: str) -> bool:
    """Comparisons joined by && or ||, without else, ternary or lambda."""
    return re.search(r"&&|\|\|", text) is not None and re.search(r"\belse\b|\?|->", text) is None


def looks_like_label_list(text: str) -> bool:
    """Labels: "case A, B, C -> {}" or "case A: case B: case C: break;", with nothing calculated."""
    label_only_line = r"\s*(?:case\s+[^:>-]+(?:->\s*\{\s*\}?|:)\s*)+(?:break\s*;\s*)?"
    return all(re.fullmatch(label_only_line, line) for line in text.split("\n"))


# a type declaration: its indentation, and its name (public static final class FreeShipping ...)
TYPE_DECLARATION = re.compile(r"^(\s*)(?:[\w@]+\s+)*?(?:class|enum|record|interface)\s+(\w+)")


@dataclass
class Package:
    """One package's marked lines, with the questions every point asks about them."""

    name: str
    marked_lines_by_rule: dict[str, list[str]]
    line_texts: dict[str, str]
    region_objects: dict[str, list[str]]
    package_folder: Path
    source_files: dict[str, str]
    _file_lines: dict[str, list[str]] = field(default_factory=dict)

    def enclosing_type_names(self, marked_line: str) -> list[str]:
        """The class, enum, record or interface the line is in, innermost first, with the ones around it. A nested
        class (class FreeShipping inside Coupons.java) is the home of its own code, like a file of its own."""
        file = file_of(marked_line)
        if file not in self._file_lines:
            path_inside_package = self.source_files.get(f"{self.name}|{file}")
            source = self.package_folder / path_inside_package if path_inside_package else None
            self._file_lines[file] = (source.read_text(encoding="utf-8").split("\n")
                                      if source and source.is_file() else [])
        lines = self._file_lines[file]
        names: list[str] = []
        indentation_limit = None
        for line in reversed(lines[:line_number_of(marked_line)]):
            declaration = TYPE_DECLARATION.match(line)
            if not declaration:
                continue
            indentation = len(declaration.group(1))
            if indentation_limit is None or indentation < indentation_limit:
                names.append(declaration.group(2))
                indentation_limit = indentation
        return names

    def home_names(self, marked_line: str) -> list[str]:
        """The names that say whose home the line is in: the file's and the types around the line."""
        return [file_name_of(marked_line).removesuffix(".java"), *self.enclosing_type_names(marked_line)]

    def marked_lines(self, rule_name: str) -> list[str]:
        return self.marked_lines_by_rule.get(rule_name, [])

    def count(self, rule_name: str) -> int:
        return len(self.marked_lines(rule_name))

    def text_of(self, marked_line: str) -> str:
        return self.line_texts.get(f"{self.name}|{marked_line}", "")

    def without_lists_of_valid_cases(self, marked_lines: list[str], point: Point,
                                     looks_like_list: Callable[[str], bool]) -> list[str]:
        """Régua: a name inside a list of valid cases does not count. A list is a block of marked lines in a row
        that cites ALL the cases of the point and passes the shape test."""
        marked_lines_by_file: dict[str, list[str]] = {}
        for marked_line in marked_lines:
            marked_lines_by_file.setdefault(file_of(marked_line), []).append(marked_line)
        inside_a_list: set[str] = set()
        for lines_of_one_file in marked_lines_by_file.values():
            lines_of_one_file.sort(key=line_number_of)
            blocks_in_a_row: list[list[str]] = []
            for marked_line in lines_of_one_file:
                follows_the_previous = (blocks_in_a_row and
                                        line_number_of(marked_line) - line_number_of(blocks_in_a_row[-1][-1]) <= 1)
                if follows_the_previous:
                    blocks_in_a_row[-1].append(marked_line)
                else:
                    blocks_in_a_row.append([marked_line])
            for block in blocks_in_a_row:
                block_text = "\n".join(dict.fromkeys(self.text_of(line) for line in block))
                if cites_every_case(block_text, point) and looks_like_list(block_text):
                    inside_a_list.update(block)
        return [line for line in marked_lines if line not in inside_a_list]

    def is_registration(self, marked_line: str, point: Point) -> bool:
        """The case's unit says which code it answers ('return "GOLD".equals(code);' inside Gold.java or
        GoldTier.java). It is the name used as a key, inside its own home."""
        text = self.text_of(marked_line).strip()
        returns_one_comparison = r"(?:^|[{;]\s*)return\s+[^;&|?{}]*(?:equals|==)[^;&|?{}]*;\s*\}?$"
        if not re.search(returns_one_comparison, text):
            return False
        names_a_case = any(mentions(text, name) for name in point.names)
        inside_the_case_class = any(fragment in name for name in self.home_names(marked_line)
                                    for fragment in point.classes.split("|"))
        return names_a_case and inside_the_case_class

    def files_that_know_our_cases(self) -> set[str]:
        return {file_of(line) for point in POINTS for line in self.marked_lines(f"{point.id}-name")}


# ------------------------------------------------------------------ P1 to P4: Strategy

# A line that only turns a case into the carrier's service code (case EXPRESS -> "EXP") is the adapter's job: it is
# measured in P6, not as a decision of the shipping method. (Found in OPUS55-N0-03: CarrierService.EXPRESS is the
# carrier's service, a namesake of our EXPRESS.)
ONLY_TRANSLATES_TO_A_CARRIER_CODE = re.compile(rf'(?:->|return)\s*"(?:{"|".join(CARRIER_CODES)})"\s*;')


def answer_strategy_point(package: Package, point: Point) -> tuple[Location, Selection]:
    evidence, point_id = package.marked_lines_by_rule, point.id

    def is_carrier_translation(marked_line: str) -> bool:
        return ONLY_TRANSLATES_TO_A_CARRIER_CODE.search(package.text_of(marked_line)) is not None

    def is_in_a_cross_rule_partner_home(marked_line: str) -> bool:
        """Every case the line names has a cross-rule partner, and the line is in that partner's home
        (STORE_PICKUP refused inside the FreeShipping coupon)."""
        text = package.text_of(marked_line)
        named_cases = {point.case_of(name) for name in point.names if mentions(text, name)}
        if not named_cases or not named_cases <= CROSS_RULE_PARTNER_HOMES.keys():
            return False
        return all(any(re.search(CROSS_RULE_PARTNER_HOMES[case], home) for home in package.home_names(marked_line))
                   for case in named_cases)

    def counts_for_this_point(marked_line: str) -> bool:
        return not is_carrier_translation(marked_line) and not is_in_a_cross_rule_partner_home(marked_line)

    refusals = list(dict.fromkeys(package.marked_lines(f"{point_id}-cond-refusal-throw")
                                  + package.marked_lines(f"{point_id}-cond-refusal-return")))
    not_counted = set(refusals) | set(package.marked_lines(f"{point_id}-cond-factory"))
    conditions_in_calculation = [
        line for line in package.without_lists_of_valid_cases(
            package.marked_lines(f"{point_id}-cond-calc"), point, looks_like_comparison_list)
        if line not in not_counted and not package.is_registration(line, point) and counts_for_this_point(line)
    ]
    conditions_that_refuse = [line for line in package.without_lists_of_valid_cases(
                                  refusals, point, looks_like_comparison_list)
                              if counts_for_this_point(line)]
    factories = list(dict.fromkeys(package.marked_lines(f"{point_id}-switch-factory")
                                   + package.marked_lines(f"{point_id}-cond-factory")))
    switch_labels = package.without_lists_of_valid_cases(
        [line for line in package.marked_lines(f"{point_id}-switch-label")
         if line not in factories and counts_for_this_point(line)],
        point, looks_like_label_list)
    partial_sets = [line for line in package.marked_lines(f"{point_id}-set")
                    if not cites_every_case(package.text_of(line), point) and counts_for_this_point(line)]
    # shown in the evidence, so the reader sees what was let through as a cross rule in its home
    evidence[f"{point_id}-cross-rule-home"] = [
        line for line in dict.fromkeys(package.marked_lines(f"{point_id}-cond-calc") + refusals
                                       + package.marked_lines(f"{point_id}-switch-label"))
        if is_in_a_cross_rule_partner_home(line)]
    # an instanceof inside an if that refuses is a refusal, it does not count as calculation
    instanceof_that_refuses = set(package.marked_lines(f"{point_id}-instanceof-refusal"))
    instanceof_in_calculation = [line for line in package.marked_lines(f"{point_id}-instanceof")
                                 if line not in instanceof_that_refuses]

    # the evidence keeps only what COUNTED
    evidence[f"{point_id}-instanceof"] = instanceof_in_calculation
    evidence[f"{point_id}-instanceof-refusal"] = sorted(instanceof_that_refuses)
    evidence[f"{point_id}-cond-calc"] = conditions_in_calculation
    evidence[f"{point_id}-cond-refusal"] = conditions_that_refuse
    evidence[f"{point_id}-switch-label"] = switch_labels
    evidence[f"{point_id}-set"] = partial_sets
    evidence[f"{point_id}-factory"] = factories
    for replaced_rule in ("switch-factory", "cond-factory", "cond-refusal-throw", "cond-refusal-return"):
        evidence.pop(f"{point_id}-{replaced_rule}", None)

    if package.count(f"{point_id}-name") == 0:
        return Location.UNDETERMINED, Selection.UNDETERMINED
    lines_that_calculate = (len(conditions_in_calculation) + len(instanceof_in_calculation)
                            + len(switch_labels) + len(partial_sets))
    lines_that_refuse = len(conditions_that_refuse) + len(instanceof_that_refuses)
    location = Location.SCATTERED if lines_that_calculate + lines_that_refuse else Location.ISOLATED
    if lines_that_calculate:
        selection = Selection.CONDITIONAL_IN_CALCULATION
    elif factories:
        selection = Selection.SINGLE_CONDITIONAL
    else:
        selection = Selection.LOOKUP
    return location, selection


# ------------------------------------------------------------------ P5: region (negative control)

def answer_region_point(package: Package) -> tuple[Form, Proportion]:
    if package.count("p5-name") + package.count("p5-config") == 0:
        return Form.OTHER, Proportion.UNDETERMINED
    # Objects per region: one class for all (a value object, a table) is data; one class per region (or named
    # after the region) is structure.
    region_object_classes = package.region_objects
    some_class_named_after_a_region = any(re.search(REGION_CLASSES, name) for name in region_object_classes)
    objects_are_structure = len(region_object_classes) >= 2 or some_class_named_after_a_region
    objects_are_data = len(region_object_classes) == 1 and not some_class_named_after_a_region
    objects_are_in_a_map = any(re.search(r"Map\.|\bput\s*\(|\bentry\s*\(", package.text_of(line))
                               for lines in region_object_classes.values() for line in lines)
    translations = set(package.marked_lines("p5-translation"))
    switch_labels = [line for line in package.marked_lines("p5-switch-label") if line not in translations]
    package.marked_lines_by_rule["p5-switch-label"] = switch_labels
    package.marked_lines_by_rule["p5-object"] = [f"{name}@{lines[0]}" for name, lines in region_object_classes.items()]

    structure = (package.count("p5-enum-body") + package.count("p5-region-class")
                 + package.count("p5-switch-factory") + package.count("p5-map-function") + int(objects_are_structure))
    conditional = len(switch_labels) - package.count("p5-switch-factory")
    data = (package.count("p5-enum-data") + package.count("p5-map") + package.count("p5-config")
            + int(objects_are_data))

    if package.count("p5-enum-body"):
        form = Form.ENUM_BODY
    elif package.count("p5-region-class"):
        form = Form.CLASSES
    elif package.count("p5-map") + package.count("p5-map-function") + int(objects_are_in_a_map):
        form = Form.MAP
    elif conditional + package.count("p5-switch-factory") > 0:
        form = Form.SWITCH
    elif package.count("p5-enum-data"):
        form = Form.ENUM_DATA
    else:
        form = Form.OTHER

    if structure > 0:
        proportion = Proportion.STRUCTURE
    elif conditional > 0:
        proportion = Proportion.CONDITIONAL
    elif data > 0:
        proportion = Proportion.DATA
    else:
        proportion = Proportion.UNDETERMINED
    return form, proportion


# ------------------------------------------------------------------ P6: Adapter

# Declaring a field with the carrier's name is the response record itself, not a leak: a record component, a
# field, a @JsonProperty.
CARRIER_FIELD_DECLARATION = re.compile(
    r"@JsonProperty|^\s*(?:(?:private|protected|public|final)\s+)*[\w<>]+\s+"
    r"(?:priceCents|etaHours|price_cents|eta_hours)\s*[,);=]|\brecord\s+\w+\s*\(")


def answer_carrier_point(package: Package) -> Adapter:
    files_with_http_calls = {file_of(line) for line in package.marked_lines("p6-http")}
    if not files_with_http_calls:
        return Adapter.UNDETERMINED        # no HTTP call found: the agent did it some other way
    folders_with_http_calls = {file.rpartition("/")[0] for file in files_with_http_calls}
    files_that_know_our_cases = package.files_that_know_our_cases()

    def on_the_carrier_side(marked_line: str) -> bool:
        """The files with the HTTP call, and the files next to them that do not know our cases."""
        file = file_of(marked_line)
        next_to_the_call = folder_of(marked_line) in folders_with_http_calls and file not in files_that_know_our_cases
        return file in files_with_http_calls or next_to_the_call

    carrier_vocabulary = package.marked_lines("p6-wire") + package.marked_lines("p6-code")
    leaks = [line for line in carrier_vocabulary
             if not on_the_carrier_side(line) and not CARRIER_FIELD_DECLARATION.search(package.text_of(line))]
    evidence = package.marked_lines_by_rule
    evidence["p6-leak"] = leaks
    evidence["p6-http"] = [next(line for line in package.marked_lines("p6-http") if file_of(line) == file)
                           for file in sorted(files_with_http_calls)]
    evidence.pop("p6-wire", None)
    evidence.pop("p6-code", None)
    return Adapter.LEAKED if leaks else Adapter.ISOLATED


# ------------------------------------------------------------------ P7: State

NOT_A_STATUS_HOME = re.compile(r"Service|Controller|Repository|Handler|Listener|Mapper|Dto|Request|Response"
                               r"|Entity|Converter")
ONLY_TRANSLATES_TO_THE_CONSTANT = re.compile(
    rf"case\s+\"?\w+\"?\s*(?:->|:)\s*(?:return\s+)?(?:\w+\.)?(?:{'|'.join(STATUS.names)})\s*;")


def is_inside_the_status(package: Package, marked_line: str) -> bool:
    """The status's own home: the type the line is in is OrderStatus, OrderState, or one class per status
    (PaidState), also when it is nested (an enum Status inside Order.java)."""
    innermost_type = (package.enclosing_type_names(marked_line) or [file_name_of(marked_line).removesuffix(".java")])[0]
    if NOT_A_STATUS_HOME.search(innermost_type):
        return False
    return bool(re.search(STATUS_HOME, innermost_type) or re.match(f"(?:{STATUS_CLASSES})", innermost_type))


def answer_status_point(package: Package) -> Location:
    if package.count("p7-name") == 0:
        return Location.UNDETERMINED
    conditions = package.without_lists_of_valid_cases(
        package.marked_lines("p7-cond"), STATUS, looks_like_comparison_list)
    switch_labels = [line for line in package.without_lists_of_valid_cases(
                         package.marked_lines("p7-switch-label"), STATUS, looks_like_label_list)
                     if not ONLY_TRANSLATES_TO_THE_CONSTANT.search(package.text_of(line))]
    partial_sets = [line for line in package.marked_lines("p7-set")
                    if not cites_every_case(package.text_of(line), STATUS)]
    decisions = list(dict.fromkeys(conditions + switch_labels + partial_sets))
    decisions_outside = [line for line in decisions if not is_inside_the_status(package, line)]

    evidence = package.marked_lines_by_rule
    evidence["p7-outside"] = decisions_outside
    evidence["p7-inside"] = [line for line in decisions if line not in decisions_outside]
    for replaced_rule in ("p7-cond", "p7-switch-label", "p7-set"):
        evidence.pop(replaced_rule, None)
    return Location.SCATTERED if decisions_outside else Location.ISOLATED


# ------------------------------------------------------------------ F: Factory (descriptive)

def describe_creation(package: Package) -> str:
    strategy_class_names = {
        match.group(1) for line in package.marked_lines("f-strategy-class")
        if (match := re.search(r"\b(?:class|record)\s+(\w+)", package.text_of(line)))
    }
    news_of_strategy_classes = [
        line for line in package.marked_lines("f-new")
        if any(name in strategy_class_names for name in re.findall(r"\bnew\s+(\w+)\s*\(", package.text_of(line)))
    ]
    factories_in_the_points = [line for point in POINTS for line in package.marked_lines(f"{point.id}-factory")]
    package.marked_lines_by_rule["f-new"] = news_of_strategy_classes
    package.marked_lines_by_rule.pop("f-strategy-class", None)
    ways_of_creating = {
        "container": package.count("f-component"),
        "configuration": package.count("f-bean"),
        "factory": len(factories_in_the_points),
        "new": len(news_of_strategy_classes),
        "enum": package.count("f-enum-body"),
    }
    return "+".join(way for way, times in ways_of_creating.items() if times) or "none"


# ------------------------------------------------------------------ the CSV

HEADER = ["package", *(f"{point.id.upper()}_{question}" for point in POINTS for question in ("location", "selection")),
          "P5_form", "P5_proportion", "P6_adapter", "P7_state", "F_creation", "evidence", "warnings"]
P5_FORM_COLUMN = HEADER.index("P5_form") - 1       # the position among the answers (without "package")


def classify(semgrep_output: Any, packages_root: Path, root_in_json: str) -> list[list[str]]:
    """One row per package (a direct subfolder of the root), in the HEADER's order."""
    findings = read_semgrep_output(semgrep_output, root_in_json)
    rows = []
    for package_folder in sorted(folder for folder in packages_root.iterdir() if folder.is_dir()):
        name = package_folder.name
        # A package without any .java in the clean copy was not measured: its answers come out undetermined, and
        # the "no-code" warning makes the batch be refused, instead of the gap becoming a zero.
        if not any(package_folder.rglob("*.java")):
            findings.warnings.setdefault(name, []).append("no-code: no .java in src/main")
        package = Package(name, findings.marked_lines.get(name, {}), findings.line_texts,
                          findings.region_objects.get(name, {}), package_folder, findings.source_files)
        answers: list[str] = [answer for point in POINTS for answer in answer_strategy_point(package, point)]
        answers += [*answer_region_point(package), answer_carrier_point(package), answer_status_point(package)]
        creation = describe_creation(package)
        if name in findings.broken_packages:
            answers = [Form.OTHER if column == P5_FORM_COLUMN else "undetermined" for column in range(len(answers))]
        # after the answers: each point left in the package only the marked lines that counted
        evidence = " ".join(f"{rule}={len(lines)}({lines[0]})"
                            for rule, lines in package.marked_lines_by_rule.items()
                            if lines and not rule.endswith("-name") and rule != "p5-translation")
        rows.append([name, *map(str, answers), creation, evidence, "; ".join(findings.warnings.get(name, []))])
    return rows


def main() -> None:
    if len(sys.argv) not in (3, 4):
        sys.exit("usage: python classify.py <output.json> <packages-root> [root-in-json]")
    json_path, packages_root = Path(sys.argv[1]), Path(sys.argv[2])
    root_in_json = sys.argv[3] if len(sys.argv) == 4 else str(packages_root)
    semgrep_output = json.loads(json_path.read_text(encoding="utf-8"))
    writer = csv.writer(sys.stdout, lineterminator="\n")
    writer.writerow(HEADER)
    writer.writerows(classify(semgrep_output, packages_root, root_in_json))


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")  # type: ignore[union-attr]
    main()
