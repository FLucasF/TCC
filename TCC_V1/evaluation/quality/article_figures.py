"""Draws the figures of the article as SVG, in shades of one blue that stay readable when printed in gray. The two
charts read their numbers from the reviews, through rounds_report.py; the diagrams only describe the design.

Usage (from TCC_V1):
    python evaluation/quality/article_figures.py ../docs/figuras
"""

import sys
from html import escape
from pathlib import Path

from rounds_report import ARMS, SCENARIOS, STUDIES, Run, read_runs

WIDTH = 640
FONT = "Arial"
CODE_FONT = "Courier New"
INK = "#1C2733"
MUTED = "#566373"
GRID = "#E3E8EE"
LINE = "#C4D1E0"
SOFT = "#F4F7FB"
ACCENT_SOFT = "#E1ECF9"
ACCENT = "#1F5FAF"
DEEP = "#123F7A"
WHITE = "#FFFFFF"
ARM_COLORS = {"N0": "#A9C8EC", "N1": "#4C8DDB", "N2": "#123F7A"}
ARROW = (
    '<defs><marker id="arrow" markerWidth="8" markerHeight="8" refX="7" refY="3.5" orient="auto">'
    f'<path d="M0,0 L7,3.5 L0,7 z" fill="{MUTED}"/></marker></defs>'
)


def text(
    x: float,
    y: float,
    content: str,
    size: int = 12,
    anchor: str = "start",
    weight: str = "normal",
    font: str = FONT,
    color: str = INK,
) -> str:
    return (
        f'<text x="{x}" y="{y}" font-family="{font}" font-size="{size}" text-anchor="{anchor}" '
        f'font-weight="{weight}" fill="{color}" xml:space="preserve">{escape(content)}</text>'
    )


def card(
    x: float, y: float, width: float, height: float, fill: str = WHITE, stroke: str = LINE, stroke_width: float = 1
) -> str:
    return (
        f'<rect x="{x}" y="{y}" width="{width}" height="{height}" rx="8" fill="{fill}" stroke="{stroke}" '
        f'stroke-width="{stroke_width}"/>'
    )


def band(x: float, y: float, width: float, height: float, fill: str) -> str:
    """A title band: rounded on top and square at the bottom."""
    return (
        f'<path d="M{x},{y + height} V{y + 8} Q{x},{y} {x + 8},{y} H{x + width - 8} '
        f'Q{x + width},{y} {x + width},{y + 8} V{y + height} Z" fill="{fill}"/>'
    )


def arrow(x1: float, y1: float, x2: float, y2: float) -> str:
    return (
        f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{MUTED}" stroke-width="1.5" marker-end="url(#arrow)"/>'
    )


def lines(
    x: float, y: float, content: list[str], size: int = 11, anchor: str = "middle", step: int = 15, color: str = INK
) -> list[str]:
    return [text(x, y + index * step, line, size, anchor, color=color) for index, line in enumerate(content)]


def svg(height: int, parts: list[str]) -> str:
    body = "\n".join(parts)
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {WIDTH} {height}" width="{WIDTH}" height="{height}">'
        f'{ARROW}<rect width="{WIDTH}" height="{height}" fill="{WHITE}"/>\n{body}\n</svg>\n'
    )


def harness_layers() -> str:
    parts = [
        card(6, 8, 176, 286, ACCENT_SOFT, ACCENT, 1.2),
        band(6, 8, 176, 44, ACCENT),
        text(94, 27, "Configuração do harness", 12, "middle", "bold", color=WHITE),
        text(94, 43, "a variável estudada", 11, "middle", color=WHITE),
    ]
    for index, (title, detail) in enumerate(
        [
            ("CLAUDE.md", ["orientações carregadas", "a cada sessão"]),
            ("Skill", ["instruções abertas", "durante a tarefa"]),
            ("Processo", ["desenho breve e", "subagente revisor"]),
        ]
    ):
        top = 64 + index * 75
        parts += [
            card(18, top, 152, 66),
            text(94, top + 21, title, 12, "middle", "bold", color=DEEP),
            *lines(94, top + 38, detail, 11, step=14),
        ]
    parts += [
        arrow(183, 151, 209, 151),
        card(212, 8, 248, 286, SOFT),
        band(212, 8, 248, 44, "#3B4A5C"),
        text(336, 27, "Harness (Claude Code)", 12, "middle", "bold", color=WHITE),
        text(336, 43, "o software em volta do modelo", 11, "middle", color=WHITE),
        *lines(336, 74, ["conduz o ciclo do agente, monta a", "entrada e oferece as ferramentas"], 11, step=14),
        card(262, 112, 148, 68, DEEP, DEEP),
        text(336, 142, "Modelo (LLM)", 14, "middle", "bold", color=WHITE),
        text(336, 161, "gera texto e código", 11, "middle", color=WHITE),
        *lines(336, 206, ["Ciclo do agente: raciocinar,", "agir e observar o resultado"], 11, step=14),
        f'<line x1="232" y1="248" x2="440" y2="248" stroke="{LINE}" stroke-width="1"/>',
        *lines(336, 268, ["No nível N0, o harness roda", "sem configuração."], 11, step=14, color=MUTED),
        arrow(461, 140, 489, 140),
        arrow(489, 164, 461, 164),
        card(492, 84, 142, 136),
        band(492, 84, 142, 30, "#7A8797"),
        text(563, 104, "Ambiente", 12, "middle", "bold", color=WHITE),
        *lines(563, 140, ["arquivos do projeto,", "terminal, testes", "e banco de dados"], 11, step=17),
    ]
    return svg(302, parts)


def conditional_versus_strategy() -> str:
    left = [
        "double preco(String plano,",
        "             double diaria, int noites) {",
        '  if (plano.equals("normal"))',
        "    return diaria * noites;",
        '  if (plano.equals("fimDeSemana"))',
        "    return diaria * noites + 30;",
        '  if (plano.equals("pacote"))',
        "    return diaria * (noites - 1);",
        "  throw new IllegalArgumentException();",
        "}",
    ]
    right = [
        "interface PlanoTarifa {",
        "  double preco(double diaria, int noites);",
        "}",
        "",
        "class Normal implements PlanoTarifa {",
        "  public double preco(double d, int n) {",
        "    return d * n;",
        "  }",
        "}",
        "// FimDeSemana e Pacote: uma classe cada",
    ]
    parts = [
        card(6, 8, 310, 226, SOFT),
        band(6, 8, 310, 30, "#7A8797"),
        text(161, 28, "Sem pattern: condicional", 12, "middle", "bold", color=WHITE),
        card(324, 8, 310, 226, ACCENT_SOFT, ACCENT, 1.2),
        band(324, 8, 310, 30, ACCENT),
        text(479, 28, "Com o pattern Strategy", 12, "middle", "bold", color=WHITE),
    ]
    for x, code in ((18, left), (336, right)):
        parts += [text(x, 60 + index * 17, line, 11, font=CODE_FONT) for index, line in enumerate(code)]
    parts += [
        *lines(161, 256, ["Cada plano novo obriga a alterar", "este método."], 12, step=16),
        *lines(479, 256, ["Cada plano novo é uma classe nova, e o", "código existente não muda."], 12, step=16),
    ]
    return svg(282, parts)


def phases() -> str:
    stages = [
        (
            "Bancadas",
            "construction-v2 e v3",
            ["Claude Code com e sem orientação;", "ajustes do enunciado e da medição."],
            False,
        ),
        (
            "Lote descartado",
            "construction-v4",
            ["Lote pré-registrado, descartado antes da análise", "confirmatória; serviu de ensaio da análise."],
            False,
        ),
        (
            "Estudo principal",
            "construction-v5",
            ["Pré-registro; Strategy, checkout, 11 modelos,", "N0 a N3 e 220 execuções."],
            True,
        ),
        (
            "Fase intermediária",
            "construction-v7 e maintenance-v1",
            ["Cinco patterns, pousada e Javalin; construção", "e manutenção, uma execução por combinação."],
            False,
        ),
        (
            "Extensão",
            "TCC_V1",
            [
                "Cinco patterns, com PostgreSQL; construção e",
                "manutenção, 3 modelos, N0 a N2, 5 rodadas,",
                "450 execuções.",
            ],
            True,
        ),
    ]
    parts: list[str] = []
    top = 6
    centers: list[float] = []
    cards: list[str] = []
    for index, (name, folder, detail, is_result) in enumerate(stages):
        height = 70 if len(detail) > 2 else 60
        center = top + height / 2
        centers.append(center)
        cards += [
            card(
                52,
                top,
                582,
                height,
                ACCENT_SOFT if is_result else WHITE,
                ACCENT if is_result else LINE,
                1.4 if is_result else 1,
            ),
            text(68, center - 3, name, 13, weight="bold", color=DEEP if is_result else INK),
            text(68, center + 14, folder, 10, color=MUTED),
            *lines(292, center + 4 - (len(detail) - 1) * 8, detail, 12, "start", step=16),
            f'<circle cx="24" cy="{center}" r="14" fill="{ACCENT if is_result else WHITE}" '
            f'stroke="{ACCENT if is_result else MUTED}" stroke-width="1.5"/>',
            text(24, center + 5, str(index + 1), 13, "middle", "bold", color=WHITE if is_result else MUTED),
        ]
        top += height + 12
    parts.append(f'<line x1="24" y1="{centers[0]}" x2="24" y2="{centers[-1]}" stroke="{LINE}" stroke-width="2"/>')
    parts += cards
    parts += [
        f'<circle cx="20" cy="{top + 10}" r="7" fill="{ACCENT}"/>',
        text(33, top + 14, "traz os resultados do artigo", 11, color=MUTED),
        f'<circle cx="222" cy="{top + 10}" r="7" fill="{WHITE}" stroke="{MUTED}" stroke-width="1.5"/>',
        text(235, top + 14, "caminho até o desenho", 11, color=MUTED),
    ]
    return svg(top + 24, parts)


def experiment_design() -> str:
    rows = [
        (
            "Estudo principal",
            "com pré-registro",
            [
                ["Enunciado", "checkout, pasta", "vazia, sem nomear", "o pattern"],
                ["Configuração", "N0, N1, N2 e N3"],
                ["Agente", "11 modelos Claude,", "5 réplicas"],
                ["Avaliação", "suíte de aceitação,", "Semgrep, SonarQube", "e CK"],
            ],
        ),
        (
            "Extensão",
            "exploratória",
            [
                ["Enunciado", "pousada: construir", "ou manter, sem", "nomear o pattern"],
                ["Configuração", "N0, N1 e N2"],
                ["Agente", "3 modelos Claude,", "5 rodadas"],
                ["Avaliação", "verificador de IA,", "às cegas, e CK"],
            ],
        ),
    ]
    parts: list[str] = []
    width, gap = 136, 22
    top = 6
    for name, kind, steps in rows:
        parts += [
            card(6, top, 628, 136, SOFT),
            text(18, top + 22, name, 13, weight="bold", color=DEEP),
            text(18 + len(name) * 7.3 + 8, top + 22, kind, 11, color=MUTED),
        ]
        for index, step in enumerate(steps):
            x = 18 + index * (width + gap)
            varies = index == 1
            parts += [
                card(x, top + 34, width, 90, DEEP if varies else WHITE, DEEP if varies else LINE),
                text(x + width / 2, top + 55, step[0], 12, "middle", "bold", color=WHITE if varies else DEEP),
                *lines(x + width / 2, top + 74, step[1:], 11, step=15, color=WHITE if varies else INK),
            ]
            if index < len(steps) - 1:
                parts.append(arrow(x + width + 1, top + 79, x + width + gap - 2, top + 79))
        top += 148
    parts.append(text(8, top + 8, "Em destaque, o que varia em cada estudo.", 11, color=MUTED))
    return svg(top + 16, parts)


def legend(x: float, y: float) -> list[str]:
    parts: list[str] = []
    for index, arm in enumerate(ARMS):
        left = x + index * 54
        parts += [
            f'<rect x="{left}" y="{y - 10}" width="12" height="12" rx="3" fill="{ARM_COLORS[arm]}"/>',
            text(left + 17, y, arm, 12, color=MUTED),
        ]
    return parts


def rounded_bar(x: float, bottom: float, width: float, height: float, color: str) -> str:
    radius = min(4, width / 2, height)
    top = bottom - height
    return (
        f'<path d="M{x},{bottom} V{top + radius} Q{x},{top} {x + radius},{top} H{x + width - radius} '
        f'Q{x + width},{top} {x + width},{top + radius} V{bottom} Z" fill="{color}"/>'
    )


def bars(
    left: float,
    width: float,
    top: float,
    height: float,
    groups: list[tuple[str, list[int]]],
    maximum: int,
    ticks: list[int],
    label_size: int,
) -> list[str]:
    parts: list[str] = []
    bottom = top + height
    for tick in ticks:
        y = bottom - tick / maximum * height
        parts += [
            f'<line x1="{left}" y1="{y}" x2="{left + width}" y2="{y}" stroke="{GRID}" stroke-width="1"/>',
            text(left - 7, y + 4, str(tick), 10, "end", color=MUTED),
        ]
    group_width = width / len(groups)
    bar_width = group_width * 0.7 / len(ARMS)
    for index, (name, values) in enumerate(groups):
        start = left + index * group_width + group_width * 0.15
        for position, (arm, value) in enumerate(zip(ARMS, values, strict=True)):
            x = start + position * bar_width
            bar_height = value / maximum * height
            parts += [
                rounded_bar(x + 1, bottom, bar_width - 2, bar_height, ARM_COLORS[arm]),
                text(x + bar_width / 2, bottom - bar_height - 4, str(value), label_size, "middle", color=INK),
            ]
        parts.append(text(left + index * group_width + group_width / 2, bottom + 17, name, 11, "middle"))
    parts.append(
        f'<line x1="{left}" y1="{bottom}" x2="{left + width}" y2="{bottom}" stroke="{MUTED}" stroke-width="1"/>'
    )
    return parts


def right_count(runs: list[Run], study: str, arm: str, scenario: str | None = None) -> int:
    return sum(run.right for run in runs if (run.study, run.arm) == (study, arm) and scenario in (None, run.scenario))


def right_pattern_total(runs: list[Run]) -> str:
    groups = [(name, [right_count(runs, study, arm) for arm in ARMS]) for study, name in STUDIES.items()]
    parts = [
        text(8, 16, "Execuções com o pattern certo (de 75)", 12, weight="bold", color=DEEP),
        *legend(466, 16),
        *bars(36, 596, 36, 226, groups, 75, [0, 25, 50, 75], 13),
    ]
    return svg(286, parts)


def right_pattern_by_combination(runs: list[Run]) -> str:
    parts = [text(8, 16, "Execuções com o pattern certo (de 15)", 12, weight="bold", color=DEEP), *legend(466, 16)]
    for index, (study, name) in enumerate(STUDIES.items()):
        top = 32 + index * 198
        groups = [
            (scenario.split(" ", 1)[1], [right_count(runs, study, arm, key) for arm in ARMS])
            for key, scenario in SCENARIOS.items()
        ]
        parts += [
            card(6, top, 628, 188, SOFT),
            text(18, top + 20, name, 12, weight="bold", color=DEEP),
            *bars(38, 586, top + 30, 130, groups, 15, [0, 5, 10, 15], 10),
        ]
    return svg(428, parts)


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit(__doc__)
    target = Path(sys.argv[1])
    target.mkdir(parents=True, exist_ok=True)
    runs = read_runs()
    figures = {
        "fig-harness": harness_layers(),
        "fig-strategy": conditional_versus_strategy(),
        "fig-fases": phases(),
        "fig-desenho": experiment_design(),
        "fig-pattern-total": right_pattern_total(runs),
        "fig-pattern-combinacao": right_pattern_by_combination(runs),
    }
    for name, content in figures.items():
        (target / f"{name}.svg").write_text(content, encoding="utf-8")
        print(name)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8")  # type: ignore[union-attr]
    main()
