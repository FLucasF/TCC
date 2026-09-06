"""Um teste por saída da árvore de verificação.

Rodam offline, sem Maven, sem npm e sem rede: os comandos sintéticos usam o próprio
Python, que é o único pré-requisito garantido.
"""

from __future__ import annotations

import sys

import pytest

import runner
from runner import boundary_for, format_for_model, norm, truncate, verify

PY = sys.executable


def manifest(**boundary) -> dict:
    base = {"id": "b", "paths": ["x/**"], "workingDirectory": "."}
    base.update(boundary)
    return {"boundaries": [base], "unmappedPathPolicy": "report"}


def cmd(code: int, text: str = "") -> dict:
    script = f'import sys; sys.stdout.write({text!r}); sys.exit({code})'
    return {"run": f'"{PY}" -c "{script}"'}


# ---------------------------------------------------------------- as cinco saídas

def test_unmapped_reporta_e_nao_adivinha():
    result = verify(["fora/do/mapa.txt"], manifest=manifest())
    assert result.unmapped == ["fora/do/mapa.txt"]
    assert result.boundaries == []


def test_no_checks_nao_substitui_pela_suite_de_outra():
    result = verify(["x/a.md"], phase="full", manifest=manifest(commands=[]))
    assert [b.outcome for b in result.boundaries] == ["NO_CHECKS"]


def test_blocked_quando_falta_prerequisito():
    spec = {**cmd(0), "prerequisites": ["binario-que-nao-existe-aqui"]}
    result = verify(["x/a.py"], phase="full", manifest=manifest(commands=[spec]))
    assert result.boundaries[0].outcome == "BLOCKED"
    assert result.boundaries[0].commands[0].missing == ["binario-que-nao-existe-aqui"]


def test_blocked_nunca_vira_fail():
    """O critério de pronto da spec: ambiente quebrado não é código quebrado."""
    spec = {**cmd(1), "prerequisites": ["binario-que-nao-existe-aqui"]}
    result = verify(["x/a.py"], phase="full", manifest=manifest(commands=[spec]))
    assert result.boundaries[0].outcome == "BLOCKED"
    assert not result.failed


def test_pass_quando_todos_saem_zero():
    result = verify(["x/a.py"], phase="full", manifest=manifest(commands=[cmd(0), cmd(0)]))
    assert result.boundaries[0].outcome == "PASS"


def test_fail_quando_algum_sai_diferente_de_zero():
    m = manifest(commands=[cmd(0), cmd(1, "boom: erro aqui")])
    result = verify(["x/a.py"], phase="full", manifest=m)
    assert result.boundaries[0].outcome == "FAIL"
    assert result.failed
    assert "boom" in format_for_model(result)


# ------------------------------------------------------------------------- fases

def test_fase_fast_nao_roda_a_suite_completa():
    m = manifest(fast=[cmd(0)], commands=[cmd(1, "a suíte lenta rodou")])
    result = verify(["x/a.py"], phase="fast", manifest=m)
    assert result.boundaries[0].outcome == "PASS"


def test_fase_full_roda_commands():
    m = manifest(fast=[cmd(0)], commands=[cmd(1, "quebrou")])
    result = verify(["x/a.py"], phase="full", manifest=m)
    assert result.boundaries[0].outcome == "FAIL"


# -------------------------------------------------------------------- truncagem

def test_filter_to_touched_reduz_ao_arquivo_editado():
    saida = "outro/Arquivo.java:1-9 ~ z.java:2\nmeu/Alvo.java:10-20 ~ w.java:3"
    spec = {**cmd(1, saida), "filterToTouched": True}
    result = verify(["x/Alvo.java"], phase="full", manifest=manifest(commands=[spec]))
    corpo = result.boundaries[0].commands[0].output
    assert "Alvo.java" in corpo
    assert "outro/Arquivo.java" not in corpo


# ------------------------------------------------- comandos informativos (NOTE)

def test_report_only_com_achado_vira_note_e_nao_reprova():
    """jscpd sai 0 mesmo achando clones; com --threshold sai 1 sempre. O código de
    saída não é veredito, então a saída dele é nota."""
    spec = {**cmd(0, "meu/Alvo.java:10-20 ~ w.java:3"),
            "filterToTouched": True, "reportOnly": True}
    result = verify(["x/Alvo.java"], phase="full", manifest=manifest(commands=[spec]))
    assert result.boundaries[0].commands[0].outcome == "NOTE"
    assert result.boundaries[0].outcome == "PASS"
    assert not result.failed


def test_report_only_sem_nada_do_arquivo_tocado_fica_calado():
    spec = {**cmd(1, "nada/aqui.java:1 ~ outro.java:2"),
            "filterToTouched": True, "reportOnly": True}
    result = verify(["x/Alvo.java"], phase="full", manifest=manifest(commands=[spec]))
    assert result.boundaries[0].commands[0].outcome == "PASS"
    assert result.boundaries[0].commands[0].output == ""


def test_note_nao_esconde_falha_de_veredito_na_mesma_boundary():
    informativo = {**cmd(0, "x/Alvo.java achou algo"), "reportOnly": True}
    veredito = cmd(1, "o teste quebrou")
    m = manifest(commands=[informativo, veredito])
    result = verify(["x/Alvo.java"], phase="full", manifest=m)
    assert result.boundaries[0].outcome == "FAIL"


def test_truncagem_respeita_o_teto():
    texto = "\n".join(f"linha {i}" for i in range(200))
    assert len(truncate(texto).splitlines()) <= runner.MAX_OUTPUT_LINES + 1


# ------------------------------------------------------------- caminhos Windows

@pytest.mark.parametrize("raw", ["x\\sub\\a.py", "x/sub/a.py", "./x/sub/a.py"])
def test_caminho_com_contrabarra_casa_igual(raw):
    assert boundary_for(norm(raw), manifest()) is not None


def test_norm_devolve_barra_normal():
    assert "\\" not in norm("x\\sub\\a.py")


# --------------------------------------------------- o manifesto real é válido

def test_manifesto_do_projeto_carrega_e_tem_as_boundaries_esperadas():
    real = runner.load_manifest()
    ids = {b["id"] for b in real["boundaries"]}
    assert {"backend", "frontend", "harness"} <= ids
    for b in real["boundaries"]:
        assert "paths" in b and b["paths"]
        for spec in [*b.get("fast", []), *b.get("commands", [])]:
            assert "run" in spec


def test_arquivo_do_harness_cai_na_boundary_harness():
    real = runner.load_manifest()
    assert boundary_for(norm("verify/runner.py"), real)["id"] == "harness"


def test_arquivo_do_backend_cai_na_boundary_backend():
    real = runner.load_manifest()
    caminho = norm("apps/backend/src/main/java/Foo.java")
    assert boundary_for(caminho, real)["id"] == "backend"


# ------------------------------------------------------- o portão de conclusão

def test_gate_roda_a_boundary_inteira_por_id():
    """Regressão: o gate sintetizava um caminho a partir do glob, e o caminho não
    casava com o próprio padrão — passava sem rodar nada, em silêncio."""
    m = manifest(id="backend", commands=[cmd(1, "quebrou")])
    result = runner.verify_boundaries(["backend"], phase="full", manifest=m)
    assert [b.outcome for b in result.boundaries] == ["FAIL"]
    assert result.failed


def test_gate_ignora_boundary_que_nao_existe():
    m = manifest(id="backend", commands=[cmd(0)])
    result = runner.verify_boundaries(["inexistente"], phase="full", manifest=m)
    assert result.boundaries == []


def test_gate_com_boundary_verde_nao_reprova():
    m = manifest(id="backend", commands=[cmd(0)])
    result = runner.verify_boundaries(["backend"], phase="full", manifest=m)
    assert not result.failed
