"""Analise do lote: junta runs.csv com metricas.csv, calcula medianas e
Mann-Whitney exato entre as condicoes, e emite analise.json.

O JSON alimenta o relatorio. Manter o calculo separado da apresentacao
permite reconferir os numeros sem reabrir o relatorio, e vice-versa.

Uso:  python analise.py --base ~/eval
"""

import argparse
import csv
import io
import json
import os
import statistics
import sys
from itertools import combinations

# Desfechos numericos comparados entre A e B, por modelo.
DESFECHOS = [
    ("num_turns", "turnos"),
    ("output_tokens", "tokens de saida"),
    ("total_input", "total_input"),
    ("thinking_tokens", "tokens de raciocinio"),
    ("tool_total", "chamadas de ferramenta"),
    ("bash_build", "builds rodados"),
    ("arq_main", "arquivos de producao"),
    ("loc_main", "linhas de producao"),
    ("n_metodos_test", "metodos @Test"),
    ("abst_total", "abstracoes"),
    ("abst_especulativa", "abstracoes especulativas"),
    ("duration_ms", "duracao total"),
    # Tempo de API nao sofre contencao de CPU local; so o tempo local sofre,
    # e ele e 3-6% do total. Por isso os tres separados.
    ("duration_api_ms", "tempo de API"),
    ("duration_local_ms", "tempo local (contaminado em modo par)"),
]


def mann_whitney_exato(a, b):
    """U bilateral por enumeracao. Sem dependencia externa e exato para n
    pequeno, que e o caso aqui. Empates recebem posto medio; com empates o
    p exato por permutacao continua valido porque enumeramos as
    combinacoes reais, nao a distribuicao teorica."""
    n1, n2 = len(a), len(b)
    if n1 == 0 or n2 == 0:
        return None
    juntos = sorted(a + b)

    def soma_postos(amostra):
        s = 0.0
        for v in amostra:
            i = juntos.index(v)
            j = i
            while j + 1 < len(juntos) and juntos[j + 1] == v:
                j += 1
            s += (i + j) / 2.0 + 1
        return s

    u_obs = soma_postos(a) - n1 * (n1 + 1) / 2.0
    u_obs = min(u_obs, n1 * n2 - u_obs)

    total = a + b
    indices = range(len(total))
    extremos = 0
    todos = 0
    for combo in combinations(indices, n1):
        grupo = [total[i] for i in combo]
        u = soma_postos(grupo) - n1 * (n1 + 1) / 2.0
        u = min(u, n1 * n2 - u)
        todos += 1
        if u <= u_obs + 1e-9:
            extremos += 1
    return {"U": u_obs, "p": extremos / todos if todos else None,
            "permutacoes": todos}


def resumo(vals):
    if not vals:
        return None
    return {
        "n": len(vals),
        "mediana": statistics.median(vals),
        "media": statistics.mean(vals),
        "min": min(vals),
        "max": max(vals),
        "desvio": statistics.stdev(vals) if len(vals) > 1 else 0.0,
        # Coeficiente de variacao: e a dispersao dentro da celula que decide
        # se o n definitivo precisa subir.
        "cv": (statistics.stdev(vals) / statistics.mean(vals))
              if len(vals) > 1 and statistics.mean(vals) else 0.0,
        "valores": vals,
    }


def num(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default=os.path.expanduser("~/eval"))
    ap.add_argument("--max-rep", type=int, default=0,
                    help="usa so as repeticoes ate N (0 = todas)")
    args = ap.parse_args()
    base = os.path.abspath(os.path.expanduser(args.base))

    runs = {}
    with io.open(os.path.join(base, "runs.csv"), encoding="utf-8") as f:
        for r in csv.DictReader(f):
            runs[r["run_id"]] = dict(r)

    caminho_m = os.path.join(base, "metricas.csv")
    if os.path.isfile(caminho_m):
        with io.open(caminho_m, encoding="utf-8") as f:
            for r in csv.DictReader(f):
                if r["run_id"] in runs:
                    runs[r["run_id"]].update(r)

    if not runs:
        sys.exit("nenhuma run em " + base)

    # Run com is_error nao e dado: a de 429 parou no meio, com codigo
    # incompleto em disco e contadores truncados. Entra na contagem de
    # descartes, nunca nas medianas.
    descartadas = []
    mantidas = {}
    for rid, r in runs.items():
        motivo = None
        if r.get("is_error") == "true":
            motivo = "is_error ({})".format(r.get("api_error_status") or "sem status")
        elif args.max_rep and int(r.get("rep") or 0) > args.max_rep:
            motivo = "rep > {}".format(args.max_rep)
        if motivo:
            descartadas.append({"run_id": rid, "motivo": motivo})
        else:
            mantidas[rid] = r
    runs = mantidas
    if not runs:
        sys.exit("todas as runs foram descartadas")

    modelos = sorted({r["modelo"] for r in runs.values()})
    saida = {
        "base": base,
        "total_runs": len(runs),
        "descartadas": descartadas,
        "max_rep": args.max_rep or None,
        "modelos": modelos,
        "por_modelo": {},
        "agregado": {},
    }

    for modelo in modelos:
        bloco = {"desfechos": {}}
        for campo, rotulo in DESFECHOS:
            grupos = {}
            for cond in ("A", "B"):
                vals = [num(r.get(campo)) for r in runs.values()
                        if r["modelo"] == modelo and r["condicao"] == cond]
                vals = [v for v in vals if v is not None]
                grupos[cond] = resumo(vals)
            if not grupos["A"] or not grupos["B"]:
                continue
            teste = mann_whitney_exato(grupos["A"]["valores"], grupos["B"]["valores"])
            bloco["desfechos"][campo] = {
                "rotulo": rotulo, "A": grupos["A"], "B": grupos["B"], "teste": teste,
            }
        # custo total por condicao, para a fatia de pizza
        for cond in ("A", "B"):
            custos = [num(r.get("cost_usd")) or 0 for r in runs.values()
                      if r["modelo"] == modelo and r["condicao"] == cond]
            bloco.setdefault("custo", {})[cond] = sum(custos)
        saida["por_modelo"][modelo] = bloco

    # Composicoes para os graficos de pizza, somando o lote inteiro.
    for cond in ("A", "B"):
        sel = [r for r in runs.values() if r["condicao"] == cond]
        def soma(campo):
            return sum(num(r.get(campo)) or 0 for r in sel)
        saida["agregado"][cond] = {
            "runs": len(sel),
            "ferramentas": {
                "Write": soma("tool_write"), "Edit": soma("tool_edit"),
                "Read": soma("tool_read"), "Bash": soma("tool_bash"),
                "outras": soma("tool_outras"),
            },
            "bash": {"build": soma("bash_build"), "shell": soma("bash_shell")},
            "linhas": {"producao": soma("loc_main"), "teste": soma("loc_test")},
            "abstracoes": {
                "2+ implementacoes": soma("abst_2mais_prod"),
                "1 em producao": soma("abst_1_impl_prod"),
            },
            "especulativas": soma("abst_especulativa"),
            "custo": soma("cost_usd"),
            "tokens_saida": soma("output_tokens"),
            "turnos": soma("num_turns"),
            "testes": soma("n_metodos_test"),
        }

    sufixo = "-rep{}".format(args.max_rep) if args.max_rep else ""
    destino = os.path.join(base, "analise{}.json".format(sufixo))
    with io.open(destino, "w", encoding="utf-8") as f:
        json.dump(saida, f, ensure_ascii=False, indent=2)
    print("{} runs analisadas, {} descartadas -> {}".format(
        len(runs), len(descartadas), destino))
    for d in descartadas:
        print("   descartada: {} ({})".format(d["run_id"], d["motivo"]))

    for modelo, bloco in saida["por_modelo"].items():
        print("\n=== " + modelo + " ===")
        print("  {:<26} {:>10} {:>10} {:>9}".format("desfecho", "med A", "med B", "p"))
        for campo, d in bloco["desfechos"].items():
            p = d["teste"]["p"] if d["teste"] else None
            print("  {:<26} {:>10.1f} {:>10.1f} {:>9}".format(
                d["rotulo"][:26], d["A"]["mediana"], d["B"]["mediana"],
                "{:.4f}".format(p) if p is not None else "-"))


if __name__ == "__main__":
    main()
