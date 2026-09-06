"""O cap de regras do CLAUDE.md, como invariante em vez de intenção.

Uma regra é um parágrafo com um imperativo independente. Conta-se todo parágrafo
abaixo do primeiro título `##`, exceto blockquotes, que explicam mecanismo e não
mandam nada.
"""

from __future__ import annotations

from pathlib import Path

RULES_FILE = Path(__file__).resolve().parent.parent / "CLAUDE.md"
RULE_CAP = 10


def rules(text: str) -> list[str]:
    counted: list[str] = []
    current: list[str] = []
    started = False
    for line in text.splitlines():
        if line.startswith("## "):
            started = True
            line = ""
        if not started:
            continue
        if line.strip():
            current.append(line)
        elif current:
            counted.append("\n".join(current))
            current = []
    if current:
        counted.append("\n".join(current))
    return [p for p in counted if not p.startswith(">")]


AMOSTRA = """# Título

Introdução, não é regra.

## A

Regra um.

Regra dois, em
duas linhas.

## B

> Mecanismo explicado, não é regra.

Regra três.
"""


def test_conta_um_paragrafo_por_regra_abaixo_dos_titulos():
    assert rules(AMOSTRA) == ["Regra um.", "Regra dois, em\nduas linhas.", "Regra três."]


def test_um_paragrafo_a_mais_conta_uma_regra_a_mais():
    assert len(rules(AMOSTRA + "\nNunca faça X.\n")) == len(rules(AMOSTRA)) + 1


def test_claude_md_respeita_o_cap():
    texto = RULES_FILE.read_text(encoding="utf-8")
    assert len(rules(texto)) <= RULE_CAP, rules(texto)
