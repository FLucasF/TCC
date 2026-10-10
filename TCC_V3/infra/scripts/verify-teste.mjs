// A prova de que o verify.mjs acusa (PLANO-IMPLEMENTACAO.md, Parte 3): um verificador
// que nunca acusa nada parece igual a um que funciona.
//
// Uso:  node infra/scripts/verify-teste.mjs
//
// Monta um lote SINTETICO numa pasta temporaria (2 replicas x 2 modelos x 4 niveis, com
// meta.json, acceptance.txt, enunciado, gabarito, resultados.csv gerado pelo
// aggregate.mjs, mapa, amostra, planilha e CSV do Semgrep), confere que ele sai com 0, e
// depois corrompe uma copia por caso: cada uma tem de sair com 1 acusando a checagem
// certa. Nada aqui toca os dados reais. Sai com 1 se algum caso nao se comportar.

import { mkdtempSync, mkdirSync, writeFileSync, readFileSync, rmSync, cpSync, existsSync, appendFileSync } from "node:fs";
import { join } from "node:path";
import { tmpdir } from "node:os";
import { createHash } from "node:crypto";
import { execFileSync, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const AQUI = fileURLToPath(new URL(".", import.meta.url));
const VERIFY = join(AQUI, "verify.mjs");
const AGGREGATE = join(AQUI, "aggregate.mjs");
const sha = (s) => createHash("sha256").update(s).digest("hex");

const PREFIXO = "SINT";
const REPLICAS = 2;
const MODELOS = { FRACO: "claude-haiku-4-5", FORTE: "claude-opus-5" };
const OBSERVADO = { FRACO: "claude-haiku-4-5-20251001", FORTE: "claude-opus-5" };
const HARNESS = { N0: null, N1: "560577922737dbb9", N2: "27987df0bd1febe2", N3: "5f4c492bf3d12f68" };
const IMAGEM = "sha256:" + "a".repeat(64);
const VERSAO = "2.1.269";
const ENUNCIADO = "Enunciado sintetico do teste do verify.\n";
const SUITE = "// suite sintetica\n";
const id = (r, m, n) => `${PREFIXO}-${String(r).padStart(2, "0")}-${m}-${n}`;

function montar(raiz) {
  writeFileSync(join(raiz, "enunciado.md"), ENUNCIADO);
  writeFileSync(join(raiz, "suite.mjs"), SUITE);
  writeFileSync(join(raiz, "gabarito.md"),
    `---\npadrao: sintetico\nenunciado: enunciado.md\nenunciado_hash: ${sha(ENUNCIADO)}\nlotes: ${PREFIXO}\n---\n\n# Gabarito sintetico\n`);
  writeFileSync(join(raiz, "desenho.json"), JSON.stringify({
    prefixo: PREFIXO, replicas: REPLICAS,
    niveis: Object.fromEntries(Object.entries(HARNESS).map(([n, h]) => [n, { condition: h ? "HARNESS" : "CONTROL", harness_hash: h }])),
    modelos: MODELOS, simultaneos_minutos: 5, effort: "medium", image_id: IMAGEM, claude_code_version: VERSAO,
    suite: "suite.mjs", gabarito: "gabarito.md", resultados: "analysis/resultados.csv",
  }, null, 2));

  const runs = [];
  for (let r = 1; r <= REPLICAS; r++)
    for (const [m, modelo] of Object.entries(MODELOS)) {
      const inicio = new Date(Date.UTC(2026, 9, 10 + r, m === "FRACO" ? 12 : 18, 0, 0));
      for (const [n, h] of Object.entries(HARNESS)) {
        const run = id(r, m, n);
        const dir = join(raiz, "runs", run);
        mkdirSync(dir, { recursive: true });
        writeFileSync(join(dir, "meta.json"), JSON.stringify({
          run_id: run, valid: null, model_requested: modelo, model_init: modelo,
          models_observed: { messages: [OBSERVADO[m]] },
          condition: h ? "HARNESS" : "CONTROL", replicate: r,
          environment: {
            image_id: IMAGEM, claude_code_version: VERSAO, prompt_hash: sha(ENUNCIADO),
            harness_hash: h ? h + "0".repeat(48) : null, api_key_source: "none",
          },
          parameters: { effort: "medium" },
          timing: { start: new Date(inicio.getTime() + 1000 * Object.keys(HARNESS).indexOf(n)).toISOString(), duration_s: 300 },
          outcome: { termination: "completed", terminal_reason: "completed", final_message: "pronto", build_ok: true, turns: 30, tool_calls: 40 },
          tokens: { source: "result", input_total: 1000, output: 500, cost_estimated_usd: 0.5 },
        }, null, 2));
        writeFileSync(join(dir, "acceptance.txt"),
          `run_id: ${run}\nsuite: suite.mjs ${sha(SUITE).slice(0, 16)}\nprompt: ${sha(ENUNCIADO).slice(0, 16)} confere\nimage: ${IMAGEM}\nstatus: all_passed\n`);
        runs.push({ run, m, n });
      }
    }
  execFileSync(process.execPath, [AGGREGATE, "--prefix", `${PREFIXO}-`, "--out", "analysis/resultados.csv"], { cwd: raiz, stdio: "pipe" });

  // Codigos C01..C16 na ordem dos runs; a amostra pega a replica 1 de cada modelo x nivel.
  const cod = (i) => `C${String(i + 1).padStart(2, "0")}`;
  writeFileSync(join(raiz, "mapa.csv"), "blind_code,run_id,files,condition_leaks,seed\n" +
    runs.map((x, i) => `${cod(i)},${x.run},10,0,1`).join("\n") + "\n");
  const amostra = runs.map((x, i) => ({ ...x, c: cod(i) })).filter((x) => x.run.startsWith(`${PREFIXO}-01-`));
  mkdirSync(join(raiz, "leitura"));
  writeFileSync(join(raiz, "leitura", "amostra.csv"), "blind_code\n" + amostra.map((x) => x.c).join("\n") + "\n");
  writeFileSync(join(raiz, "leitura", "leitura-lucas.csv"),
    "leitor,blind_code,P4_localizacao,P4_selecao,P5_forma,P5_proporcao,evidencia,duvida,achei_que_sabia_nivel,observacao\n" +
    amostra.map((x) => `lucas,${x.c},isolado,consulta,enum-dados,dados,"A.java:1; B.java:2; C.java:3; C.java:3",,,`).join("\n") + "\n");
  writeFileSync(join(raiz, "semgrep.csv"), "pacote,P4_localizacao\n" + runs.map((_, i) => `${cod(i)},isolado`).join("\n") + "\n");
}

const ARGS = ["desenho.json", "--mapa", "mapa.csv", "--amostra", "leitura", "--semgrep", "semgrep.csv"];
const rodar = (raiz) => {
  const r = spawnSync(process.execPath, [VERIFY, ...ARGS, "--raiz", raiz], { encoding: "utf8" });
  return { codigo: r.status, saida: (r.stdout ?? "") + (r.stderr ?? "") };
};
const json = (raiz, run, f) => {
  const arq = join(raiz, "runs", run, "meta.json");
  const m = JSON.parse(readFileSync(arq, "utf8")); f(m); writeFileSync(arq, JSON.stringify(m, null, 2));
};
// Depois de mudar um meta.json num caso que tem de passar, o CSV e gerado de novo, como
// na bancada (o aggregate.mjs roda depois de tudo).
const regerar = (raiz) => {
  rmSync(join(raiz, "analysis", "resultados.csv"));
  execFileSync(process.execPath, [AGGREGATE, "--prefix", `${PREFIXO}-`, "--out", "analysis/resultados.csv"], { cwd: raiz, stdio: "pipe" });
};
const trocar = (arq, de, para) => {
  const t = readFileSync(arq, "utf8");
  if (!t.includes(de)) throw new Error(`o caso nao achou "${de}" em ${arq}`);
  writeFileSync(arq, t.replace(de, para));
};

// [checagem esperada, descricao, corrupcao, trecho esperado na linha de erro (opcional,
// para o caso nao passar por um erro vizinho)]. A corrupcao recebe a raiz da copia.
const CASOS = [
  ["desenho", "falta uma replica", (r) => rmSync(join(r, "runs", id(2, "FORTE", "N3")), { recursive: true })],
  ["desenho", "sobra uma execucao do lote fora do desenho", (r) => cpSync(join(r, "runs", id(1, "FRACO", "N0")), join(r, "runs", `${PREFIXO}-03-FRACO-N0`), { recursive: true })],
  ["desenho", "modelo pedido errado", (r) => json(r, id(1, "FRACO", "N1"), (m) => { m.model_requested = "claude-sonnet-5"; })],
  ["desenho", "respondeu outro modelo (opus-5-5 no lugar de opus-5)", (r) => json(r, id(1, "FORTE", "N2"), (m) => { m.models_observed.messages = ["claude-opus-5-5"]; })],
  ["desenho", "nivel com o harness de outro", (r) => json(r, id(2, "FRACO", "N2"), (m) => { m.environment.harness_hash = HARNESS.N3 + "0".repeat(48); })],
  ["desenho", "N0 com harness", (r) => json(r, id(1, "FORTE", "N0"), (m) => { m.environment.harness_hash = HARNESS.N1 + "0".repeat(48); m.condition = "HARNESS"; }), "harness_hash"],
  ["desenho", "outra imagem", (r) => json(r, id(2, "FORTE", "N1"), (m) => { m.environment.image_id = "sha256:" + "b".repeat(64); })],
  ["desenho", "outra versao do Claude Code", (r) => json(r, id(1, "FRACO", "N3"), (m) => { m.environment.claude_code_version = "2.1.270"; })],
  ["desenho", "outro effort", (r) => json(r, id(1, "FRACO", "N0"), (m) => { m.parameters.effort = "high"; })],
  ["desenho", "interrompida pela cota", (r) => json(r, id(2, "FORTE", "N3"), (m) => { Object.assign(m.outcome, { termination: "error", terminal_reason: "api_error", final_message: "You've hit your session limit" }); })],
  ["desenho", "um nivel refeito sozinho, 2 h depois", (r) => json(r, id(1, "FORTE", "N2"), (m) => { m.timing.start = new Date(Date.parse(m.timing.start) + 2 * 3600e3).toISOString(); })],
  ["desenho", "medida com uma suite antiga", (r) => trocar(join(r, "runs", id(1, "FRACO", "N0"), "acceptance.txt"), sha(SUITE).slice(0, 16), "f77d14886c5c9e0a")],
  ["desenho", "suite nao rodou", (r) => rmSync(join(r, "runs", id(2, "FRACO", "N1"), "acceptance.txt"))],
  ["desenho", "suite trocada depois da medicao", (r) => appendFileSync(join(r, "suite.mjs"), "// mudou\n")],
  ["gabarito", "enunciado_hash errado no cabecalho", (r) => trocar(join(r, "gabarito.md"), sha(ENUNCIADO), sha("outro"))],
  ["gabarito", "enunciado mudou depois do gabarito", (r) => appendFileSync(join(r, "enunciado.md"), "uma linha a mais\n")],
  ["gabarito", "lote fora dos lotes do gabarito", (r) => trocar(join(r, "gabarito.md"), `lotes: ${PREFIXO}`, "lotes: EXT")],
  ["gabarito", "execucao rodou com outro enunciado", (r) => json(r, id(2, "FRACO", "N3"), (m) => { m.environment.prompt_hash = sha("outro"); })],
  ["gabarito", "suite mediu com o enunciado errado", (r) => trocar(join(r, "runs", id(1, "FORTE", "N1"), "acceptance.txt"), " confere", " DIFERE")],
  ["csv", "linha faltando", (r) => { const a = join(r, "analysis", "resultados.csv"); writeFileSync(a, readFileSync(a, "utf8").split("\n").filter((l) => !l.startsWith(id(1, "FORTE", "N0") + ",")).join("\n")); }],
  ["csv", "linha duplicada", (r) => { const a = join(r, "analysis", "resultados.csv"); const l = readFileSync(a, "utf8").split("\n").find((x) => x.startsWith(id(2, "FRACO", "N2") + ",")); appendFileSync(a, l + "\n"); }],
  ["csv", "custo editado a mao", (r) => { const a = join(r, "analysis", "resultados.csv"); writeFileSync(a, readFileSync(a, "utf8").split("\n").map((l) => l.startsWith(id(1, "FRACO", "N1") + ",") ? l.replace(",0.5,", ",0.05,") : l).join("\n")); }],
  ["csv", "meta.json mudou depois do CSV", (r) => json(r, id(2, "FORTE", "N0"), (m) => { m.outcome.build_ok = false; })],
  ["csv", "linha de uma execucao que nao e do desenho", (r) => { const a = join(r, "analysis", "resultados.csv"); const l = readFileSync(a, "utf8").split("\n").find((x) => x.startsWith(id(1, "FRACO", "N0") + ",")); appendFileSync(a, l.replace(id(1, "FRACO", "N0"), `${PREFIXO}-09-FRACO-N0`) + "\n"); }],
  ["csv", "colunas de outra versao do aggregate", (r) => trocar(join(r, "analysis", "resultados.csv"), "run_id,replicate", "run_id,replica")],
  ["leitura", "codigo trocado no mapa (dois runs no mesmo codigo)", (r) => trocar(join(r, "mapa.csv"), "C02,", "C01,"), "aparece duas vezes no mapa"],
  ["leitura", "execucao sem codigo no mapa", (r) => { const a = join(r, "mapa.csv"); writeFileSync(a, readFileSync(a, "utf8").split("\n").filter((l) => !l.startsWith("C16,")).join("\n")); }],
  ["leitura", "amostra com dois pacotes do mesmo grupo", (r) => { trocar(join(r, "leitura", "amostra.csv"), "C02\n", "C09\n"); trocar(join(r, "leitura", "leitura-lucas.csv"), "lucas,C02,", "lucas,C09,"); }, "2 pacote(s) de FRACO N0"],
  ["leitura", "planilha com codigo fora da amostra", (r) => trocar(join(r, "leitura", "leitura-lucas.csv"), "lucas,C03,", "lucas,C11,")],
  ["leitura", "planilha sem um codigo da amostra", (r) => { const a = join(r, "leitura", "leitura-lucas.csv"); writeFileSync(a, readFileSync(a, "utf8").split("\n").filter((l) => !l.startsWith("lucas,C04,")).join("\n")); }],
  ["leitura", "valor fora da regua (isolada)", (r) => trocar(join(r, "leitura", "leitura-lucas.csv"), "lucas,C01,isolado", "lucas,C01,isolada")],
  ["leitura", "Semgrep sem um pacote", (r) => { const a = join(r, "semgrep.csv"); writeFileSync(a, readFileSync(a, "utf8").split("\n").filter((l) => !l.startsWith("C07,")).join("\n")); }],
  // O ensaio de 10/10: o projeto numa subpasta, nenhum arquivo lido, tudo "indeterminado".
  ["leitura", "Semgrep nao achou codigo num pacote", (r) => {
    const a = join(r, "semgrep.csv");
    const linhas = readFileSync(a, "utf8").trim().split("\n");
    writeFileSync(a, [linhas[0] + ",avisos", ...linhas.slice(1).map((l) => (l.startsWith("C05,") ? 'C05,indeterminado,"sem-codigo: nenhum .java em src/main"' : l + ',""'))].join("\n") + "\n");
  }, "C05: nenhum codigo lido"],
  ["leitura", "releitura com codigo fora da amostra", (r) => { writeFileSync(join(r, "leitura", "releitura.csv"), "blind_code\nC01\n"); writeFileSync(join(r, "leitura", "releitura-lucas.csv"), "leitor,blind_code,P4_localizacao,P4_selecao,P5_forma,P5_proporcao\nlucas,C12,isolado,consulta,mapa,dados\n"); }],
];

// Casos que tem de PASSAR (sair 0): o que e dado do modelo, e nao defeito da bancada.
const LIMPOS = [
  ["o lote limpo", () => {}],
  ["o agente parou por conta propria (so aviso)", (r) => { json(r, id(1, "FRACO", "N2"), (m) => { Object.assign(m.outcome, { termination: "max_turns", terminal_reason: "max_turns", final_message: "parei" }); }); regerar(r); }],
  ["planilha ainda em branco (so aviso)", (r) => { const a = join(r, "leitura", "leitura-lucas.csv"); writeFileSync(a, readFileSync(a, "utf8").replace(/isolado,consulta,enum-dados,dados/g, ",,,")); }],
  ["indeterminado e valor da lista", (r) => trocar(join(r, "leitura", "leitura-lucas.csv"), "lucas,C01,isolado", "lucas,C01,indeterminado")],
  // [descricao, mudanca, trecho que tem de aparecer na saida (opcional)]
  ["aviso do Semgrep num pacote (so aviso, listado)", (r) => {
    const a = join(r, "semgrep.csv");
    const linhas = readFileSync(a, "utf8").trim().split("\n");
    writeFileSync(a, [linhas[0] + ",avisos", ...linhas.slice(1).map((l) => l + (l.startsWith("C03,") ? ',"sem chamada: X.java:9 (metodo velho)"' : ',""'))].join("\n") + "\n");
  }, "Semgrep, C03: sem chamada"],
];

const base = mkdtempSync(join(tmpdir(), "verify-teste-"));
let falhas = 0;
try {
  const modelo = join(base, "modelo");
  mkdirSync(modelo);
  montar(modelo);
  const copia = (i) => { const r = join(base, `caso-${i}`); cpSync(modelo, r, { recursive: true }); return r; };

  console.log("tem de passar (sair 0):");
  LIMPOS.forEach(([desc, f, esperado], i) => {
    const r = copia(`limpo-${i}`); f(r);
    const { codigo, saida } = rodar(r);
    const ok = codigo === 0 && (!esperado || saida.includes(esperado));
    if (!ok) falhas++;
    console.log(`  ${ok ? "ok   " : "FALHA"} ${desc}` + (ok ? "" : `\n${saida}`));
  });

  console.log("\ntem de acusar (sair 1, na checagem certa):");
  CASOS.forEach(([chk, desc, f, esperado], i) => {
    const r = copia(i); f(r);
    const { codigo, saida } = rodar(r);
    const linha = saida.split("\n").find((l) => l.includes(`ERRO  [${chk}]`) && (!esperado || l.includes(esperado)));
    const ok = codigo === 1 && !!linha;
    if (!ok) falhas++;
    console.log(`  ${ok ? "ok   " : "FALHA"} [${chk}] ${desc}` + (ok ? `\n          ${linha.trim().replace(/^ERRO\s+/, "")}` : `\n  saiu ${codigo}:\n${saida}`));
  });
} finally {
  rmSync(base, { recursive: true, force: true });
}
console.log(`\n${LIMPOS.length + CASOS.length - falhas} de ${LIMPOS.length + CASOS.length} casos como esperado.`);
process.exit(falhas ? 1 : 0);
