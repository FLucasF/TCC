"""Generates rules.yml, the Semgrep rules that mark the design of each package.

Usage:  python evaluation/semgrep/rules.py           (writes rules.yml next to this file)
        python evaluation/semgrep/rules.py --check   (exit 1 if rules.yml is not what this generator writes)

HOW TO READ. Each rule looks for ONE way of writing something and marks the line. There are two kinds:
"languages: [java]" (Semgrep understands the code: an if, a comparison, a new) and "languages: [regex]" (a text
search, for what the parser does not see well, like a pattern-matching case). Semgrep reads a copy without
comments (clean_copy.py), so the text rules never read a comment.

What a mark means is decided apart: here we only mark what exists; classify.py decides (what is scattered,
what is a lookup...). Deterministic and without AI.

  P1 to P4 (Strategy), the same rules as V5's, with the names of points.py:
    cond-calc            a comparison that names a case (tier == GOLD, "GOLD".equals(x));
    cond-refusal-throw   the same, inside an if that refuses with throw;
    cond-refusal-return  the same, inside an if that returns an error (no throw);
    cond-factory         the same, inside an if that only returns or stores an object (a factory);
    instanceof           instanceof of a case class, outside equals();
    instanceof-refusal   the same, inside an if that refuses;
    switch-label         a "case GOLD" or "case Gold g" label;
    switch-factory       the label that only returns an object;
    set                  EnumSet/Set/List.of with cases (counts only if it does not cite them all);
    name                 the alarm: does the case's name appear anywhere?
  P5 (region, negative control), V5's rules: enum-body, enum-data, region-class, map, map-function, object
    (with the class captured), config, switch-label, switch-factory, translation, name.
  P6 (Adapter): http (an HTTP call), wire (the carrier's field names and route), code ("STD"/"EXP").
  P7 (State): cond, switch-label and set over the order statuses, and name.
  F (Factory, descriptive): component, bean, new, strategy-class, enum-body and factory-class.

The regexes are written as plain raw strings with <<NAME>> placeholders (fill()), because an f-string would
fight with the regexes' own braces.
"""

import sys
from pathlib import Path

from points import (
    ALL_CASES, ALL_CLASSES, CARRIER_CODES, CARRIER_WIRE_NAMES, CARRIER_WIRE_TEXT, FACTORY_CLASSES, HTTP_CLIENTS,
    POINTS, REGION_CLASSES, REGION_NAMES, REGION_WORDS, STATUS, Point,
)

RULES_FILE = Path(__file__).with_name("rules.yml")

# Value types: "new BigDecimal(...)" is a number, not a strategy object.
VALUE = "BigDecimal|BigInteger|Double|Integer|Long|Float|String|Object"
# A number's name (goldCredit, GOLD_RATE): a switch or an if returning it is doing the math, not choosing.
NUMBER_NAME = ("pct|percent|rate|taxa|credit|credito|discount|desconto|value|valor|fee|aliquota|factor|fator"
               "|shipping|frete|interest|juros|bonus|cashback|gift|brinde|installment|parcela|limit|limite")
# An if that refuses: a throw, or a return of an error (without throw). "return false" is NOT a refusal.
# Semgrep's metavariable-regex matches from the start of the text (re.match): the ".*" lets the error appear
# anywhere in the returned value (Optional.of(new Error(...))).
REFUSAL = (r"(?i).*(erro|error|badrequest|bad_request|recus|refus|reject|invalid|indispon|unavailable"
           r"|unprocessable|status\(\s*4\d\d|HttpStatus\.(BAD|UNPROC|CONFLICT))")
MAIN_ONLY = 'paths: { include: ["**/src/main/**/*.java"] }'


def fill(template: str, **values: str) -> str:
    """Replaces each <<NAME>> of the template by its value."""
    for placeholder, value in values.items():
        template = template.replace(f"<<{placeholder}>>", value)
    assert "<<" not in template, f"a placeholder was left: {template[:80]}"
    return template


OBJECT = fill(r"new\s+(?!(?:<<VALUE>>)\b)[A-Z]\w*\s*\(", VALUE=VALUE)
# Arguments with up to three levels of parentheses: NORTH(BigDecimal.valueOf(25).divide(BigDecimal.valueOf(1000)))
ARGS = r"\((?:[^()]|\((?:[^()]|\((?:[^()]|\([^()]*\))*\))*\))*\)"


def object_named(classes: str) -> str:
    """A variable named after the case (gold, goldTier) is an object already created; but not if the name is a
    number's, and not a constant in capitals."""
    return fill(r"(?![A-Z0-9_]+\b)(?!\w*(?i:<<NUMBER>>))\w*(?i:<<CLASSES>>)\w*", NUMBER=NUMBER_NAME, CLASSES=classes)


def comparisons(point: Point) -> str:
    """The comparisons that name a case. The literal written in the rule also finds a constant with another name
    and the same value (VIP = "GOLD"), through Semgrep's constant propagation."""
    case_names = "|".join(point.names)
    rules_text = fill(r"""
          - patterns:
              - pattern-either:
                  - pattern: $X == $C
                  - pattern: $C == $X
                  - pattern: $X != $C
                  - pattern: $C != $X
                  - pattern: $C.equals($X)
                  - pattern: $X.equals($C)
                  - pattern: $X.equalsIgnoreCase($C)
                  - pattern: $C.equalsIgnoreCase($X)
                  - pattern: Objects.equals($X, $C)
                  - pattern: Objects.equals($C, $X)
                  - pattern: $X.compareTo($C)
                  - pattern: $C.compareTo($X)
              - metavariable-regex:
                  metavariable: $C
                  regex: '^"?([A-Za-z_]\w*\.)*(<<ALT>>)"?(\.\w+\(\))?$'""", ALT=case_names)
    for case_name in point.names:
        rules_text += fill("""
          - pattern: $X.equals("<<C>>")
          - pattern: '"<<C>>".equals($X)'
          - pattern: $X.equalsIgnoreCase("<<C>>")
          - pattern: Objects.equals($X, "<<C>>")
          - pattern: Objects.equals("<<C>>", $X)""", C=case_name)
    return rules_text


def strategy_point(point: Point) -> str:
    case_names = "|".join(point.names)
    # what an if returns or stores when it only picks the object: a new object, the case's constant, an object
    # named after the case, or a constructor reference
    only_picks_an_object = fill(
        r"^(new\s+(?!(<<VALUE>>)\b)[A-Z]\w*\s*\(.*|([A-Za-z_]\w*\.)*(<<ALT>>)|(this\.)?<<NAMED>>|\w*(<<CLASSES>>)\w*::new)$",
        VALUE=VALUE, ALT=case_names, NAMED=object_named(point.classes), CLASSES=point.classes)
    return fill(r"""
  - id: <<ID>>-cond-calc
    languages: [java]
    severity: INFO
    message: a condition that names a case (the classifier removes the refusals and the factories)
    <<MAIN>>
    pattern-either:<<COMPARISONS>>
  - id: <<ID>>-cond-refusal-throw
    languages: [java]
    severity: INFO
    message: a condition that names a case only to refuse
    <<MAIN>>
    patterns:
      - pattern-either:
          - pattern-inside: if (...) throw $E;
          - pattern-inside: if (...) { ... throw $E; ... }
      - pattern-either:<<COMPARISONS>>
  - id: <<ID>>-cond-refusal-return
    languages: [java]
    severity: INFO
    message: a condition that names a case only to refuse, returning an error (no throw)
    <<MAIN>>
    patterns:
      - pattern-either:
          - pattern-inside: if (...) return $R;
          - pattern-inside: if (...) { ... return $R; }
      - metavariable-regex: { metavariable: $R, regex: '<<REFUSAL>>' }
      - pattern-either:<<COMPARISONS>>
  - id: <<ID>>-cond-factory
    languages: [java]
    severity: INFO
    message: a condition that names a case only to pick the object (a factory)
    <<MAIN>>
    patterns:
      - pattern-either:
          - pattern-inside: if (...) return $V;
          - pattern-inside: if (...) { return $V; }
          - pattern-inside: if (...) $Y = $V;
          - pattern-inside: if (...) { $Y = $V; }
      - metavariable-regex: { metavariable: $V, regex: '<<FACTORY>>' }
      - pattern-either:<<COMPARISONS>>
  - id: <<ID>>-instanceof
    languages: [java]
    severity: INFO
    message: instanceof of a case class (outside equals)
    <<MAIN>>
    patterns:
      - pattern: $X instanceof $K
      - metavariable-regex: { metavariable: $K, regex: ".*(<<CLASSES>>)" }
      - pattern-not-inside: |
          boolean equals(Object $O) { ... }
  - id: <<ID>>-instanceof-refusal
    languages: [java]
    severity: INFO
    message: instanceof of a case class inside an if that refuses (the classifier counts it as a refusal)
    <<MAIN>>
    patterns:
      - pattern-either:
          - pattern-inside: if (...) throw $E;
          - pattern-inside: if (...) { ... throw $E; ... }
      - pattern: $X instanceof $K
      - metavariable-regex: { metavariable: $K, regex: ".*(<<CLASSES>>)" }
  - id: <<ID>>-switch-label
    languages: [regex]
    severity: INFO
    message: a switch label that names a case (including pattern matching, Java 21)
    <<MAIN>>
    pattern-regex: '\bcase\s+(?:"?(?:\w+\.)?(?:<<ALT>>)"?\s*(?:,|->|:)|\w*(?:<<CLASSES>>)\w*\s*(?:\(|\s+\w+\s*(?:->|:|when\b)))'
  - id: <<ID>>-switch-factory
    languages: [regex]
    severity: INFO
    message: a switch label that only returns an object (a factory, or a translation to the constant)
    <<MAIN>>
    pattern-regex: '\bcase\s+"?(?:\w+\.)?(?:<<ALT>>)"?\s*(?:->|:)\s*(?:return\s+)?(?:<<OBJECT>>|(?:\w+\.)?(?:<<ALT>>)\s*;|(?:this\.)?<<NAMED>>\s*;|\w*(?:<<CLASSES>>)\w*::new)'
  - id: <<ID>>-set
    languages: [regex]
    severity: INFO
    message: a set or list that cites cases (the classifier counts only those that do not cite them all)
    <<MAIN>>
    pattern-regex: '\b(?:EnumSet|Set|List)\.of\s*\([^()]*\b(?:<<ALT>>)\b[^()]*\)'
  - id: <<ID>>-name
    languages: [regex]
    severity: INFO
    message: a case's name appears in the code (alarm - with none, the agent used other names)
    <<MAIN>>
    pattern-regex: '\b(?:<<ALT>>)\b|\b\w*(?:<<CLASSES>>)\w*\b'
""", ID=point.id, MAIN=MAIN_ONLY, COMPARISONS=comparisons(point), REFUSAL=REFUSAL, FACTORY=only_picks_an_object,
                CLASSES=point.classes, ALT=case_names, OBJECT=OBJECT, NAMED=object_named(point.classes))


def region_point() -> str:
    regions = "|".join(REGION_NAMES)
    # a region as text or constant, with an optional prefix ("NORTH", Region.NORTH)
    region_reference = fill(r'"?(?:\w+\.)?(?:<<R>>)"?', R=regions)
    # A map key: in quotes ("NORTH") or qualified (Region.NORTH). The bare name is left out because that is how
    # it appears in a "case SOUTHEAST, SOUTH ->", which is not a map.
    map_key = fill(r'(?:"(?:<<R>>)"|\w+\.(?:<<R>>)\b)', R=regions)
    return fill(r"""
  - id: p5-name
    languages: [regex]
    severity: INFO
    message: a region's name appears in the code (alarm)
    <<MAIN>>
    pattern-regex: '\b(?:<<R>>)\b|\b\w*(?:<<RCLASSES>>)\w*\b'
  - id: p5-enum-body
    languages: [regex]
    severity: INFO
    message: a region constant with its own body
    <<MAIN>>
    pattern-regex: '(?m)^\s*(?:<<R>>)\s*(?:<<ARGS>>)?\s*\{'
  - id: p5-enum-data
    languages: [regex]
    severity: INFO
    message: a region constant that only carries values
    <<MAIN>>
    pattern-regex: '(?m)^\s*(?:<<R>>)\s*<<ARGS>>\s*[,;]'
  - id: p5-region-class
    languages: [regex]
    severity: INFO
    message: a class per region
    <<MAIN>>
    pattern-regex: '\b(?:class|record)\s+\w*(?:<<RCLASSES>>)\w*[^{]*\b(?:implements|extends)\b'
  - id: p5-map
    languages: [regex]
    severity: INFO
    message: a map from region to value
    <<MAIN>>
    # the \s* stays INSIDE the lookahead: outside, it gives the space back and the lookahead looks at
    # " new Insurance()", which does not start with "new" (a V5 defect)
    pattern-regex: '(?:Map\.(?:of|entry)\s*\(\s*|\bentry\s*\(\s*|\bput\s*\(\s*)<<REG>>\s*,(?!\s*(?:<<OBJECT>>|(?:\([^()]*\)|\w+)\s*->|[\w.]+::\w+))'
  - id: p5-map-function
    languages: [regex]
    severity: INFO
    message: a map from region to a function or an anonymous class (one per region)
    <<MAIN>>
    pattern-regex: '<<KEY>>\s*,\s*(?:\([^()]*\)|\w+)\s*->|<<KEY>>\s*,\s*[\w.]+::\w+|<<KEY>>\s*,\s*new\s+[A-Z]\w*\s*\([^()]*\)\s*\{'
  - id: p5-config
    languages: [regex]
    severity: INFO
    message: a value per region in a configuration file (data)
    paths: { include: ["**/src/main/resources/**"] }
    pattern-regex: '(?im)^\s*[\w.\[\]-]*\b(?:<<WORDS>>)\b\s*[:=]\s*[0-9]'
  - id: p5-object
    languages: [java]
    severity: INFO
    message: an object per region (the classifier tells one class for all from one class per region)
    <<MAIN>>
    patterns:
      - pattern-either:
          - pattern: new $T($R, ...)
          - pattern: Map.of(..., $R, new $T(...), ...)
          - pattern: Map.entry($R, new $T(...))
          - pattern: entry($R, new $T(...))
          - pattern: $M.put($R, new $T(...))
          - pattern: $TT $R = new $T(...);
      - metavariable-regex: { metavariable: $R, regex: '^"?([A-Za-z_]\w*\.)*(<<R>>)"?$' }
      - metavariable-regex: { metavariable: $T, regex: '^(?!(<<VALUE>>)$)\w+$' }
  - id: p5-switch-label
    languages: [regex]
    severity: INFO
    message: a switch label or a condition that names a region
    <<MAIN>>
    pattern-regex: '\bcase\s+<<REG>>\s*(?:,|->|:)|"(?:<<R>>)"\s*\.equals|\.equals(?:IgnoreCase)?\(\s*<<REG>>\s*\)|[=!]=\s*(?:\w+\.)?(?:<<R>>)\b'
  - id: p5-switch-factory
    languages: [regex]
    severity: INFO
    message: a region switch that returns an object
    <<MAIN>>
    pattern-regex: '\bcase\s+<<REG>>\s*(?:->|:)\s*(?:return\s+)?<<OBJECT>>'
  - id: p5-translation
    languages: [regex]
    severity: INFO
    message: a switch or if that only translates the text to the region constant
    <<MAIN>>
    pattern-regex: '(?:\bcase\s+<<REG>>\s*(?:->|:)|\.equals(?:IgnoreCase)?\(\s*<<REG>>\s*\)\s*\)\s*\{?)\s*(?:return\s+)?(?:\w+\.)?(?:<<R>>)\s*;'
""", MAIN=MAIN_ONLY, R=regions, RCLASSES=REGION_CLASSES, ARGS=ARGS, REG=region_reference, KEY=map_key, OBJECT=OBJECT,
                WORDS=REGION_WORDS, VALUE=VALUE)


def carrier_point() -> str:
    return fill(r"""
  - id: p6-http
    languages: [regex]
    severity: INFO
    message: an HTTP call (or an HTTP client type), outside the imports
    <<MAIN>>
    pattern-regex: '(?m)^(?!\s*import\b).*\b(?:<<HTTP>>)\b'
  - id: p6-wire
    languages: [regex]
    severity: INFO
    message: the carrier's own vocabulary (its field names, its error code, its route)
    <<MAIN>>
    pattern-regex: '"[^"\n]*(?:<<WIRE>>)[^"\n]*"|\b(?:<<WIRE_NAMES>>)\b'
  - id: p6-code
    languages: [regex]
    severity: INFO
    message: the carrier's service codes
    <<MAIN>>
    pattern-regex: '"(?:<<CODES>>)"'
""", MAIN=MAIN_ONLY, HTTP=HTTP_CLIENTS, WIRE="|".join(CARRIER_WIRE_TEXT),
                WIRE_NAMES="|".join(CARRIER_WIRE_NAMES), CODES="|".join(CARRIER_CODES))


def status_point() -> str:
    return fill(r"""
  - id: p7-cond
    languages: [java]
    severity: INFO
    message: a condition that names an order status
    <<MAIN>>
    pattern-either:<<COMPARISONS>>
  - id: p7-switch-label
    languages: [regex]
    severity: INFO
    message: a switch label that names an order status
    <<MAIN>>
    pattern-regex: '\bcase\s+"?(?:\w+\.)?(?:<<ALT>>)"?\s*(?:,|->|:)'
  - id: p7-set
    languages: [regex]
    severity: INFO
    message: a set or list that cites order statuses (the classifier counts only those that do not cite them all)
    <<MAIN>>
    pattern-regex: '\b(?:EnumSet|Set|List)\.of\s*\([^()]*\b(?:<<ALT>>)\b[^()]*\)'
  - id: p7-name
    languages: [regex]
    severity: INFO
    message: an order status's name appears in the code (alarm)
    <<MAIN>>
    pattern-regex: '\b(?:<<ALT>>)\b'
""", MAIN=MAIN_ONLY, COMPARISONS=comparisons(STATUS), ALT="|".join(STATUS.names))


def factory() -> str:
    return fill(r"""
  - id: f-component
    languages: [regex]
    severity: INFO
    message: a strategy class created by the Spring container
    <<MAIN>>
    pattern-regex: '@(?:Component|Service)\b[^{;]*?\bclass\s+\w*(?:<<CLASSES>>)\w*'
  - id: f-bean
    languages: [regex]
    severity: INFO
    message: a strategy object created by a @Bean method
    <<MAIN>>
    pattern-regex: '@Bean\b[^{;]*?\b\w+\s+\w*(?i:<<CLASSES>>)\w*\s*\('
  - id: f-new
    languages: [regex]
    severity: INFO
    message: a new of a class named after a case (the classifier keeps only the strategy classes)
    <<MAIN>>
    pattern-regex: '\bnew\s+(\w*(?:<<CLASSES>>)\w*)\s*\('
  - id: f-strategy-class
    languages: [regex]
    severity: INFO
    message: a class named after a case that implements or extends something (a strategy class)
    <<MAIN>>
    pattern-regex: '\b(?:class|record)\s+(\w*(?:<<CLASSES>>)\w*)[^{]*\b(?:implements|extends)\b'
  - id: f-enum-body
    languages: [regex]
    severity: INFO
    message: a case constant with its own body (the strategy is an enum constant)
    <<MAIN>>
    pattern-regex: '(?m)^\s*(?:<<CASES>>)\s*(?:<<ARGS>>)?\s*\{'
  - id: f-factory-class
    languages: [regex]
    severity: INFO
    message: a class named factory or builder
    <<MAIN>>
    pattern-regex: '\b(?:class|interface|record)\s+\w*(?:<<FACTORIES>>)\w*'
""", MAIN=MAIN_ONLY, CLASSES=ALL_CLASSES, CASES=ALL_CASES, ARGS=ARGS, FACTORIES=FACTORY_CLASSES)


def render() -> str:
    return ("# GENERATED by rules.py. Do not edit by hand: change the generator and generate again.\nrules:"
            + "".join(strategy_point(point) for point in POINTS) + region_point() + carrier_point() + status_point() + factory())


def main() -> None:
    rules = render()
    if sys.argv[1:] == ["--check"]:
        same = RULES_FILE.exists() and RULES_FILE.read_text(encoding="utf-8") == rules
        print("rules.yml is what rules.py writes" if same else "rules.yml DIFFERS from rules.py: generate it again")
        sys.exit(0 if same else 1)
    RULES_FILE.write_text(rules, encoding="utf-8", newline="\n")
    print(f"{rules.count('  - id: ')} rules -> {RULES_FILE}")


if __name__ == "__main__":
    main()
