"""Um teste por saída da árvore de verificação.

Rodam offline, sem Maven, sem npm e sem rede: os comandos sintéticos usam o próprio
Python, que é o único pré-requisito garantido.
"""

from __future__ import annotations

import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import pytest

import runner
from runner import boundary_for, format_for_model, norm, truncate, verify

PY = sys.executable


@pytest.fixture(autouse=True)
def traco_isolado(monkeypatch, tmp_path):
    """Nenhum teste grava no .trace.jsonl real do harness."""
    monkeypatch.setattr(runner, "TRACE", tmp_path / ".trace.jsonl")


def manifest(**boundary) -> dict:
    base = {"id": "b", "paths": ["x/**"], "workingDirectory": "."}
    base.update(boundary)
    return {"boundaries": [base], "unmappedPathPolicy": "report"}


def cmd(code: int, text: str = "") -> dict:
    script = f'import sys; sys.stdout.write({text!r}); sys.exit({code})'
    return {"run": f'"{PY}" -c "{script}"'}


def relogio_fixo() -> datetime:
    return datetime(2026, 9, 6, 12, 0, tzinfo=timezone.utc)


def linhas(path: Path) -> list[dict]:
    return [json.loads(ln) for ln in path.read_text(encoding="utf-8").splitlines()]


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


def test_caminho_fora_da_raiz_do_harness_nao_e_verificado(tmp_path):
    """Regressão: "*.md" casava uma nota em ~/.claude e rodava a suíte do harness
    nela, além de gravar o caminho externo no traço."""
    fora = tmp_path / "nota.md"
    assert norm(str(fora)) is None
    result = verify([str(fora)], manifest=manifest(paths=["*.md"]))
    assert result.boundaries == []
    assert result.unmapped == []


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


# ----------------------------------------------------------------------- o traço

CAMPOS = {"schema", "ts", "boundary", "phase", "command", "outcome",
          "duration_s", "exit_code", "touched"}


def test_traco_grava_um_registro_por_comando_com_os_campos(tmp_path):
    traco = tmp_path / "t.jsonl"
    m = manifest(commands=[cmd(0), cmd(0)])
    verify(["x/a.py"], phase="full", manifest=m, trace=traco, now=relogio_fixo)
    regs = linhas(traco)
    assert len(regs) == 2
    for r in regs:
        assert set(r) == CAMPOS
        assert r["schema"] == "harness.trace.v1"
        assert r["ts"] == "2026-09-06T12:00:00+00:00"
        assert r["boundary"] == "b"
        assert r["phase"] == "full"
        assert r["command"] == cmd(0)["run"]
        assert r["outcome"] == "PASS"
        assert r["exit_code"] == 0
        assert isinstance(r["duration_s"], float) and r["duration_s"] >= 0
        assert r["touched"] == ["x/a.py"]


def test_traco_continua_jsonl_valido_apos_varias_execucoes(tmp_path):
    traco = tmp_path / "t.jsonl"
    for _ in range(3):
        verify(["x/a.py"], phase="full", manifest=manifest(commands=[cmd(0)]),
               trace=traco, now=relogio_fixo)
    dados = traco.read_bytes()
    assert b"\r" not in dados
    bruto = dados.decode("utf-8").split("\n")
    assert bruto[-1] == ""
    assert len(bruto) == 4
    for ln in bruto[:-1]:
        assert isinstance(json.loads(ln), dict)


def test_traco_output_head_so_em_fail(tmp_path):
    traco = tmp_path / "t.jsonl"
    informativo = {**cmd(0, "x/Alvo.java achou algo"), "reportOnly": True}
    m = manifest(commands=[cmd(0), cmd(1, "boom: erro aqui"), informativo])
    verify(["x/Alvo.java"], phase="full", manifest=m, trace=traco, now=relogio_fixo)
    por_desfecho = {r["outcome"]: r for r in linhas(traco)}
    assert set(por_desfecho) == {"PASS", "FAIL", "NOTE"}
    assert "boom" in por_desfecho["FAIL"]["output_head"]
    assert "output_head" not in por_desfecho["PASS"]
    assert "output_head" not in por_desfecho["NOTE"]


def test_traco_unmapped_e_no_checks_gravam_com_command_nulo(tmp_path):
    traco = tmp_path / "t.jsonl"
    verify(["fora/mapa.txt", "x/a.md"], phase="full", manifest=manifest(commands=[]),
           trace=traco, now=relogio_fixo)
    unmapped, no_checks = linhas(traco)
    assert unmapped["outcome"] == "UNMAPPED"
    assert unmapped["boundary"] is None
    assert unmapped["phase"] == "full"
    assert unmapped["touched"] == ["fora/mapa.txt"]
    assert no_checks["outcome"] == "NO_CHECKS"
    assert no_checks["boundary"] == "b"
    assert no_checks["touched"] == ["x/a.md"]
    for r in (unmapped, no_checks):
        assert r["command"] is None
        assert r["duration_s"] is None
        assert r["exit_code"] is None


def test_traco_blocked_nao_tem_duracao_nem_exit_code(tmp_path):
    traco = tmp_path / "t.jsonl"
    spec = {**cmd(0), "prerequisites": ["binario-que-nao-existe-aqui"]}
    verify(["x/a.py"], phase="full", manifest=manifest(commands=[spec]),
           trace=traco, now=relogio_fixo)
    (r,) = linhas(traco)
    assert r["outcome"] == "BLOCKED"
    assert r["command"] == spec["run"]
    assert r["duration_s"] is None
    assert r["exit_code"] is None


def test_traco_teto_descarta_o_mais_antigo_e_preserva_o_recente(tmp_path, monkeypatch):
    monkeypatch.setattr(runner, "MAX_TRACE_RECORDS", 3)
    traco = tmp_path / "t.jsonl"
    for i in range(1, 6):
        result = verify([f"fora/{i}.txt"], manifest=manifest(), trace=traco, now=relogio_fixo)
    assert result.notes == []
    assert [r["touched"] for r in linhas(traco)] == [["fora/3.txt"], ["fora/4.txt"], ["fora/5.txt"]]
    assert list(tmp_path.glob("*.tmp")) == []


def test_falha_ao_gravar_traco_nao_muda_o_resultado_e_emite_nota(tmp_path):
    result = verify(["fora.txt"], manifest=manifest(), trace=tmp_path, now=relogio_fixo)
    assert result.unmapped == ["fora.txt"]
    assert not result.failed
    assert len(result.notes) == 1
    assert result.notes[0].startswith("traço não gravado em")
    assert "traço não gravado" in format_for_model(result)


def test_traco_exige_relogio_com_timezone(tmp_path):
    with pytest.raises(ValueError):
        verify(["fora.txt"], manifest=manifest(), trace=tmp_path / "t.jsonl",
               now=lambda: datetime(2026, 9, 6, 12, 0))


def test_gate_grava_traco_com_touched_vazio(tmp_path):
    traco = tmp_path / "t.jsonl"
    m = manifest(id="backend", commands=[cmd(0)])
    runner.verify_boundaries(["backend"], phase="full", manifest=m, trace=traco, now=relogio_fixo)
    (r,) = linhas(traco)
    assert r["boundary"] == "backend"
    assert r["phase"] == "full"
    assert r["touched"] == []
