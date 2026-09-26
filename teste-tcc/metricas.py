"""Coletor de metricas do piloto.

Le o que ja esta no disco depois de um lote e emite metricas.csv, uma linha
por run, com tres familias de dados que o runs.csv nao carrega:

  resultado   - campos do _result.json que o run-pilot.sh nao extrai
                (thinking tokens, tempo de API vs relogio, ttft, modelo
                secundario, negacoes de permissao, variaveis de controle)
  transcricao - contagem de chamadas de ferramenta por tipo, lida do JSONL
                de sessao que o CLI grava sob o CLAUDE_CONFIG_DIR
  artefato    - metricas estaticas do codigo produzido, e opcionalmente o
                resultado real do `mvn test`

Nada aqui altera a medicao: e leitura do que o lote deixou. Pode rodar
quantas vezes quiser.

Uso:  python metricas.py --base ~/eval [--build]
"""

import argparse
import csv
import glob
import io
import json
import os
import re
import shutil
import subprocess
import sys

# --------------------------------------------------------------------
# Leitura do _result.json
# --------------------------------------------------------------------


def cava(d, *caminho, padrao=None):
    """Desce por chaves aninhadas sem estourar quando falta uma."""
    atual = d
    for chave in caminho:
        if not isinstance(atual, dict) or chave not in atual:
            return padrao
        atual = atual[chave]
    return atual if atual is not None else padrao


def do_resultado(dir_run):
    caminho = os.path.join(dir_run, "_result.json")
    if not os.path.isfile(caminho):
        return {}
    try:
        with io.open(caminho, encoding="utf-8-sig") as f:
            d = json.load(f)
    except Exception:
        return {"resultado_ilegivel": 1}

    uso = d.get("usage") or {}
    dur = d.get("duration_ms") or 0
    api = d.get("duration_api_ms") or 0

    # Um modelo auxiliar (haiku) aparece no modelUsage mesmo quando --model
    # pede outro: vale separar, senao o custo do principal fica poluido.
    principal_out = secundario_out = 0
    for nome, u in (d.get("modelUsage") or {}).items():
        if "haiku" in nome.lower():
            secundario_out += u.get("outputTokens") or 0
        else:
            principal_out += u.get("outputTokens") or 0

    return {
        "session_id": d.get("session_id") or "",
        "thinking_tokens": cava(uso, "output_tokens_details", "thinking_tokens", padrao=0),
        "duration_api_ms": api,
        # Relogio menos API = tempo gasto rodando ferramenta local (mvn, etc).
        "duration_local_ms": max(dur - api, 0),
        "ttft_ms": d.get("ttft_ms") or 0,
        "cache_1h": cava(uso, "cache_creation", "ephemeral_1h_input_tokens", padrao=0),
        "cache_5m": cava(uso, "cache_creation", "ephemeral_5m_input_tokens", padrao=0),
        "out_modelo_principal": principal_out,
        "out_modelo_auxiliar": secundario_out,
        "permission_denials": len(d.get("permission_denials") or []),
        "subagents": cava(d, "subagent_stats", "spawned", padrao=0),
        "stop_reason": d.get("stop_reason") or "",
        "terminal_reason": d.get("terminal_reason") or "",
        "subtype": d.get("subtype") or "",
        "api_error_status": d.get("api_error_status") or "",
        # Variaveis de controle: nao sao desfecho, servem para provar que
        # nao mudaram entre as condicoes.
        "service_tier": cava(uso, "service_tier", padrao=""),
        "speed": cava(uso, "speed", padrao=""),
        "fast_mode": d.get("fast_mode_state") or "",
    }


# --------------------------------------------------------------------
# Leitura da transcricao de sessao
# --------------------------------------------------------------------

FERRAMENTAS = ["Write", "Edit", "Read", "Bash", "Grep", "Glob", "TodoWrite", "Task"]


def acha_transcricao(config_dir, session_id):
    # A guarda olhava o config dir compartilhado, que deixou de existir
    # quando cada run passou a ter o seu: com ela, a busca desistia antes
    # de comecar e toda contagem de ferramenta saia zerada.
    raiz_busca = os.path.dirname(config_dir)
    if not session_id or not os.path.isdir(raiz_busca):
        return None
    # O config dir agora e um por run, entao a transcricao pode estar sob
    # _cfg/<run_id>/projects/... ou, em lotes antigos, sob _claude-config/.
    achados = glob.glob(os.path.join(raiz_busca, "**", session_id + ".jsonl"),
                        recursive=True)
    return achados[0] if achados else None


# `tool_bash` sozinho engana: metade das chamadas costuma ser mkdir, cd, rm.
# O que interessa como retrabalho e quantas vezes o agente rodou o build.
BUILD = re.compile(r"\b(mvn|mvnw|gradle|gradlew)\b")


def da_transcricao(caminho):
    vazio = {"tool_" + f.lower(): 0 for f in FERRAMENTAS}
    vazio.update({
        "tool_total": 0, "tool_outras": 0, "msgs_assistant": 0,
        "bash_build": 0, "bash_shell": 0,
        "write_alvos": 0, "write_reescritas": 0,
    })
    if not caminho or not os.path.isfile(caminho):
        return vazio

    contagem = dict(vazio)
    alvos = {}
    with io.open(caminho, encoding="utf-8", errors="replace") as f:
        for linha in f:
            try:
                o = json.loads(linha)
            except Exception:
                continue
            # Uma sessao de subagente aparece marcada como sidechain; contar
            # junto misturaria trabalho do principal com o do delegado.
            if o.get("isSidechain"):
                continue
            if o.get("type") == "assistant":
                contagem["msgs_assistant"] += 1
            conteudo = (o.get("message") or {}).get("content")
            if not isinstance(conteudo, list):
                continue
            for c in conteudo:
                if not isinstance(c, dict) or c.get("type") != "tool_use":
                    continue
                nome = c.get("name") or ""
                entrada = c.get("input") or {}
                contagem["tool_total"] += 1
                if nome in FERRAMENTAS:
                    contagem["tool_" + nome.lower()] += 1
                else:
                    contagem["tool_outras"] += 1
                if nome == "Bash":
                    cmd = entrada.get("command") or ""
                    if BUILD.search(cmd):
                        contagem["bash_build"] += 1
                    else:
                        contagem["bash_shell"] += 1
                if nome == "Write":
                    alvo = os.path.normcase(entrada.get("file_path") or "")
                    alvos[alvo] = alvos.get(alvo, 0) + 1

    contagem["write_alvos"] = len(alvos)
    contagem["write_reescritas"] = sum(v - 1 for v in alvos.values() if v > 1)
    return contagem


# --------------------------------------------------------------------
# Metricas estaticas do codigo produzido
# --------------------------------------------------------------------

IGNORAR_DIR = {".claude", ".git", "target", "build", "node_modules", ".mvn"}


def javas(raiz):
    for atual, dirs, arquivos in os.walk(raiz):
        dirs[:] = [d for d in dirs if d not in IGNORAR_DIR]
        for a in arquivos:
            if a.endswith(".java"):
                yield os.path.join(atual, a)


DECLARACAO = re.compile(
    r"^\s*(?:public\s+)?(?:final\s+)?(interface|abstract class|class|record|enum)\s+(\w+)", re.M)
HERANCA = re.compile(
    r"\b(?:class|record|enum)\s+\w+[^{]*?\b(?:implements|extends)\s+([\w,\s<>]+?)\s*\{", re.S)


def abstracoes(dir_run):
    """Conta abstracoes por numero de implementacoes.

    Uma abstracao com implementacao unica e candidata a indirecao
    especulativa. Mas contar so o codigo de producao INVERTE o resultado:
    no primeiro par, 5 das 7 abstracoes de implementacao unica da condicao A
    tinham um dublê nos testes, ou seja, pagavam. Por isso duas contagens
    separadas, e a que interessa e `abst_especulativa`, que conta producao e
    teste juntos. Ainda assim e sinal, nao veredito: a pergunta "o padrao se
    pagou" so se responde com teste de extensao.
    """
    tipos, impl_prod, impl_todos = {}, {}, {}
    for caminho in javas(dir_run):
        eh_teste = (os.sep + "test" + os.sep) in caminho
        try:
            texto = io.open(caminho, encoding="utf-8", errors="replace").read()
        except Exception:
            continue
        if not eh_teste:
            for m in DECLARACAO.finditer(texto):
                tipos[m.group(2)] = m.group(1)
        for m in HERANCA.finditer(texto):
            for sup in m.group(1).split(","):
                sup = re.sub(r"<.*", "", sup).strip()
                if not sup:
                    continue
                impl_todos.setdefault(sup, set()).add(caminho)
                if not eh_teste:
                    impl_prod.setdefault(sup, set()).add(caminho)

    m = {"abst_total": 0, "abst_1_impl_prod": 0, "abst_2mais_prod": 0,
         "abst_especulativa": 0}
    for nome, kind in tipos.items():
        if kind not in ("interface", "abstract class"):
            continue
        m["abst_total"] += 1
        if len(impl_prod.get(nome, ())) >= 2:
            m["abst_2mais_prod"] += 1
        elif len(impl_prod.get(nome, ())) == 1:
            m["abst_1_impl_prod"] += 1
        # Especulativa = nao tem segunda implementacao em lugar nenhum,
        # nem em producao nem como dublê de teste.
        if len(impl_todos.get(nome, ())) < 2:
            m["abst_especulativa"] += 1
    return m


def do_artefato(dir_run):
    m = {
        "arq_main": 0, "arq_test": 0, "loc_main": 0, "loc_test": 0,
        "n_interface": 0, "n_abstract": 0, "n_enum": 0, "n_record": 0,
        "n_switch": 0, "n_instanceof": 0, "n_else_if": 0, "n_metodos_test": 0,
    }
    for caminho in javas(dir_run):
        try:
            texto = io.open(caminho, encoding="utf-8", errors="replace").read()
        except Exception:
            continue
        linhas = sum(1 for l in texto.splitlines() if l.strip())
        eh_teste = (os.sep + "test" + os.sep) in caminho
        if eh_teste:
            m["arq_test"] += 1
            m["loc_test"] += linhas
            m["n_metodos_test"] += len(re.findall(r"@Test\b", texto))
            continue
        m["arq_main"] += 1
        m["loc_main"] += linhas
        # Sinais de estrutura. Contagem bruta: nao substitui a rubrica
        # estrutural, e falso positivo e esperado (um switch sobre plano e
        # um switch sobre tipo de evento contam igual aqui).
        m["n_interface"] += len(re.findall(r"^\s*(?:public\s+)?interface\s+\w", texto, re.M))
        m["n_abstract"] += len(re.findall(r"^\s*(?:public\s+)?abstract\s+class\s+\w", texto, re.M))
        m["n_enum"] += len(re.findall(r"^\s*(?:public\s+)?enum\s+\w", texto, re.M))
        m["n_record"] += len(re.findall(r"^\s*(?:public\s+)?record\s+\w", texto, re.M))
        m["n_switch"] += len(re.findall(r"\bswitch\s*\(", texto))
        m["n_instanceof"] += len(re.findall(r"\binstanceof\b", texto))
        m["n_else_if"] += len(re.findall(r"\belse\s+if\b", texto))
    return m


# --------------------------------------------------------------------
# Build opcional
# --------------------------------------------------------------------

RESUMO_TESTES = re.compile(r"Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\s*$")


def acha_pom(dir_run):
    """O pom nem sempre esta na raiz da run: parte dos agentes cria um
    subdiretorio de projeto. Procurar so na raiz marcava essas runs como
    'sem_pom' e escondia builds que funcionavam. Vale o pom mais raso."""
    melhor, prof_melhor = None, 10 ** 6
    for atual, dirs, arquivos in os.walk(dir_run):
        dirs[:] = [d for d in dirs if d not in IGNORAR_DIR]
        if "pom.xml" in arquivos:
            prof = atual[len(dir_run):].count(os.sep)
            if prof < prof_melhor:
                melhor, prof_melhor = atual, prof
    return melhor


def do_build(dir_run):
    m = {"build_ok": "", "testes_rodados": "", "testes_falhos": "",
         "projeto_aninhado": 0}
    dir_pom = acha_pom(dir_run)
    if not dir_pom:
        m["build_ok"] = "sem_pom"
        return m
    m["projeto_aninhado"] = 0 if os.path.normpath(dir_pom) == os.path.normpath(dir_run) else 1
    dir_run = dir_pom
    # No Windows o executavel e mvn.cmd: shutil.which resolve pelo PATHEXT,
    # coisa que subprocess sozinho nao faz.
    mvn = shutil.which("mvn")
    if not mvn:
        m["build_ok"] = "mvn_ausente"
        return m
    try:
        p = subprocess.run([mvn, "-B", "test"], cwd=dir_run, capture_output=True,
                           text=True, errors="replace", timeout=1800)
    except OSError:
        m["build_ok"] = "mvn_falhou_ao_iniciar"
        return m
    except subprocess.TimeoutExpired:
        m["build_ok"] = "timeout"
        return m

    m["build_ok"] = "sim" if p.returncode == 0 else "nao"
    # A ultima linha de resumo (sem "-- in") e o total do modulo.
    for linha in p.stdout.splitlines():
        achado = RESUMO_TESTES.search(linha.strip())
        if achado and "-- in" not in linha:
            m["testes_rodados"] = int(achado.group(1))
            m["testes_falhos"] = int(achado.group(2)) + int(achado.group(3))
    return m


# --------------------------------------------------------------------

def main():
    ap = argparse.ArgumentParser(description="Coleta metricas detalhadas de um lote ja executado.")
    ap.add_argument("--base", default=os.path.expanduser("~/eval"))
    ap.add_argument("--build", action="store_true",
                    help="roda 'mvn test' em cada run (lento) e registra o resultado real")
    args = ap.parse_args()

    base = os.path.abspath(os.path.expanduser(args.base))
    runs_root = os.path.join(base, "runs")
    config_dir = os.path.join(base, "_claude-config")
    if not os.path.isdir(runs_root):
        sys.exit("nao achei " + runs_root)

    linhas = []
    for nome in sorted(os.listdir(runs_root)):
        dir_run = os.path.join(runs_root, nome)
        if not os.path.isdir(dir_run):
            continue

        # run_id no formato <modelo>-<condicao>-<rep>
        partes = nome.rsplit("-", 2)
        modelo, cond, rep = (partes + ["", "", ""])[:3] if len(partes) == 3 else (nome, "", "")

        linha = {"run_id": nome, "modelo": modelo, "condicao": cond, "rep": rep}
        res = do_resultado(dir_run)
        linha.update(res)
        linha.update(da_transcricao(acha_transcricao(config_dir, res.get("session_id", ""))))
        linha.update(do_artefato(dir_run))
        linha.update(abstracoes(dir_run))
        if args.build:
            linha.update(do_build(dir_run))
        linhas.append(linha)
        print("  " + nome, flush=True)

    if not linhas:
        sys.exit("nenhuma run em " + runs_root)

    colunas = []
    for l in linhas:
        for k in l:
            if k not in colunas:
                colunas.append(k)

    saida = os.path.join(base, "metricas.csv")
    with io.open(saida, "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=colunas)
        w.writeheader()
        for l in linhas:
            w.writerow(l)

    print("\n{} runs -> {}".format(len(linhas), saida))
    print("{} colunas".format(len(colunas)))


if __name__ == "__main__":
    main()
