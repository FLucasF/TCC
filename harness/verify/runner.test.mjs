// @ts-check
/**
 * One test per outcome of the verification tree.
 *
 * They run offline, without Maven, without npm and without network: the synthetic
 * commands invoke Node itself, which is the one prerequisite the executor already
 * guarantees. Output is passed through base64 so that no quoting rule of cmd.exe or
 * sh can change what the child prints.
 */

import { test, beforeEach, after } from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import process from "node:process";

import {
  MAX_OUTPUT_LINES,
  asManifest,
  boundaryFor,
  config,
  failed,
  formatForModel,
  loadManifest,
  norm,
  pathsFromHook,
  truncate,
  verify,
  verifyBoundaries,
  which,
} from "./runner.mjs";

/** @type {string[]} */
const scratch = [];

function tmpdir() {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), "harness-test-"));
  scratch.push(dir);
  return dir;
}

after(() => {
  for (const dir of scratch) fs.rmSync(dir, { recursive: true, force: true });
});

/** No test writes to the harness's real .trace.jsonl. */
beforeEach(() => {
  config.trace = path.join(tmpdir(), ".trace.jsonl");
  config.maxTraceRecords = 2000;
});

/**
 * @param {Partial<import("./runner.mjs").Boundary>} [boundary]
 * @returns {import("./runner.mjs").Manifest}
 */
function manifest(boundary = {}) {
  return {
    boundaries: [{
      id: "b", paths: ["x/**"], workingDirectory: ".", ...boundary,
    }],
    unmappedPathPolicy: "report",
  };
}

/**
 * @param {number} code
 * @param {string} [text]
 * @returns {import("./runner.mjs").CommandSpec}
 */
function cmd(code, text = "") {
  const b64 = Buffer.from(text, "utf-8").toString("base64");
  const script = `process.stdout.write(Buffer.from('${b64}','base64')`
    + `.toString('utf-8'));process.exit(${code})`;
  return { run: `"${process.execPath}" -e "${script}"` };
}

const relogioFixo = () => new Date("2026-09-06T12:00:00.000Z");
const TS_FIXO = "2026-09-06T12:00:00.000Z";

/**
 * @param {string} file
 * @returns {Record<string, unknown>[]}
 */
function linhas(file) {
  return fs.readFileSync(file, "utf-8")
    .split("\n").filter(Boolean).map((ln) => JSON.parse(ln));
}

// ---------------------------------------------------------------- as cinco saídas

test("unmapped reporta e não adivinha", () => {
  const result = verify(["fora/do/mapa.txt"], { manifest: manifest() });
  assert.deepEqual(result.unmapped, ["fora/do/mapa.txt"]);
  assert.deepEqual(result.boundaries, []);
});

test("no_checks não substitui pela suíte de outra", () => {
  const result = verify(["x/a.md"], { phase: "full", manifest: manifest({ commands: [] }) });
  assert.deepEqual(result.boundaries.map((b) => b.outcome), ["NO_CHECKS"]);
});

test("blocked quando falta pré-requisito", () => {
  const spec = { ...cmd(0), prerequisites: ["binario-que-nao-existe-aqui"] };
  const result = verify(["x/a.py"], { phase: "full", manifest: manifest({ commands: [spec] }) });
  assert.equal(result.boundaries[0].outcome, "BLOCKED");
  assert.deepEqual(result.boundaries[0].commands[0].missing, ["binario-que-nao-existe-aqui"]);
});

test("blocked nunca vira fail: ambiente quebrado não é código quebrado", () => {
  const spec = { ...cmd(1), prerequisites: ["binario-que-nao-existe-aqui"] };
  const result = verify(["x/a.py"], { phase: "full", manifest: manifest({ commands: [spec] }) });
  assert.equal(result.boundaries[0].outcome, "BLOCKED");
  assert.equal(failed(result), false);
});

test("pass quando todos saem zero", () => {
  const result = verify(["x/a.py"], {
    phase: "full", manifest: manifest({ commands: [cmd(0), cmd(0)] }),
  });
  assert.equal(result.boundaries[0].outcome, "PASS");
});

test("fail quando algum sai diferente de zero", () => {
  const m = manifest({ commands: [cmd(0), cmd(1, "boom: erro aqui")] });
  const result = verify(["x/a.py"], { phase: "full", manifest: m });
  assert.equal(result.boundaries[0].outcome, "FAIL");
  assert.equal(failed(result), true);
  assert.match(formatForModel(result), /boom/);
});

// ------------------------------------------------------------------------- fases

test("fase fast não roda a suíte completa", () => {
  const m = manifest({ fast: [cmd(0)], commands: [cmd(1, "a suíte lenta rodou")] });
  const result = verify(["x/a.py"], { phase: "fast", manifest: m });
  assert.equal(result.boundaries[0].outcome, "PASS");
});

test("fase full roda commands", () => {
  const m = manifest({ fast: [cmd(0)], commands: [cmd(1, "quebrou")] });
  const result = verify(["x/a.py"], { phase: "full", manifest: m });
  assert.equal(result.boundaries[0].outcome, "FAIL");
});

// -------------------------------------------------------------------- truncagem

test("filterToTouched reduz ao arquivo editado", () => {
  const saida = "outro/Arquivo.java:1-9 ~ z.java:2\nmeu/Alvo.java:10-20 ~ w.java:3";
  const spec = { ...cmd(1, saida), filterToTouched: true };
  const result = verify(["x/Alvo.java"], {
    phase: "full", manifest: manifest({ commands: [spec] }),
  });
  const corpo = result.boundaries[0].commands[0].output;
  assert.match(corpo, /Alvo\.java/);
  assert.doesNotMatch(corpo, /outro\/Arquivo\.java/);
});

test("truncagem respeita o teto", () => {
  const texto = Array.from({ length: 200 }, (_, i) => `linha ${i}`).join("\n");
  assert.ok(truncate(texto).split("\n").length <= MAX_OUTPUT_LINES + 1);
});

// ------------------------------------------------- comandos informativos (NOTE)

test("reportOnly com achado vira note e não reprova", () => {
  // jscpd sai 0 mesmo achando clones; com --threshold sai 1 sempre. O código de
  // saída não é veredito, então a saída dele é nota.
  const spec = {
    ...cmd(0, "meu/Alvo.java:10-20 ~ w.java:3"),
    filterToTouched: true,
    reportOnly: true,
  };
  const result = verify(["x/Alvo.java"], {
    phase: "full", manifest: manifest({ commands: [spec] }),
  });
  assert.equal(result.boundaries[0].commands[0].outcome, "NOTE");
  assert.equal(result.boundaries[0].outcome, "PASS");
  assert.equal(failed(result), false);
});

test("reportOnly sem nada do arquivo tocado fica calado", () => {
  const spec = {
    ...cmd(1, "nada/aqui.java:1 ~ outro.java:2"),
    filterToTouched: true,
    reportOnly: true,
  };
  const result = verify(["x/Alvo.java"], {
    phase: "full", manifest: manifest({ commands: [spec] }),
  });
  assert.equal(result.boundaries[0].commands[0].outcome, "PASS");
  assert.equal(result.boundaries[0].commands[0].output, "");
});

test("note não esconde falha de veredito na mesma boundary", () => {
  const informativo = { ...cmd(0, "x/Alvo.java achou algo"), reportOnly: true };
  const veredito = cmd(1, "o teste quebrou");
  const m = manifest({ commands: [informativo, veredito] });
  const result = verify(["x/Alvo.java"], { phase: "full", manifest: m });
  assert.equal(result.boundaries[0].outcome, "FAIL");
});

// ------------------------------------------------------------- caminhos Windows

for (const raw of ["x\\sub\\a.py", "x/sub/a.py", "./x/sub/a.py"]) {
  test(`caminho ${JSON.stringify(raw)} casa igual`, () => {
    assert.notEqual(boundaryFor(norm(raw), manifest()), null);
  });
}

test("norm devolve barra normal", () => {
  assert.doesNotMatch(String(norm("x\\sub\\a.py")), /\\/);
});

test("caminho fora da raiz do harness não é verificado", () => {
  // Regressão: "*.md" casava uma nota em ~/.claude e rodava a suíte do harness
  // nela, além de gravar o caminho externo no traço.
  const fora = path.join(tmpdir(), "nota.md");
  assert.equal(norm(fora), null);
  const result = verify([fora], { manifest: manifest({ paths: ["*.md"] }) });
  assert.deepEqual(result.boundaries, []);
  assert.deepEqual(result.unmapped, []);
});

// --------------------------------------------------- o manifesto real é válido

test("manifesto do projeto carrega e tem as boundaries esperadas", () => {
  const real = loadManifest();
  const ids = new Set(real.boundaries.map((b) => b.id));
  for (const esperado of ["backend", "frontend", "harness"]) {
    assert.ok(ids.has(esperado), `falta a boundary ${esperado}`);
  }
  for (const b of real.boundaries) {
    assert.ok(Array.isArray(b.paths) && b.paths.length);
    for (const spec of [...(b.fast || []), ...(b.commands || [])]) {
      assert.equal(typeof spec.run, "string");
    }
  }
});

test("arquivo do harness cai na boundary harness", () => {
  const real = loadManifest();
  assert.equal(boundaryFor(norm("verify/runner.mjs"), real)?.id, "harness");
});

test("arquivo do backend cai na boundary backend", () => {
  const real = loadManifest();
  const caminho = norm("apps/backend/src/main/java/Foo.java");
  assert.equal(boundaryFor(caminho, real)?.id, "backend");
});

test("md da raiz é do harness; md de apps/docs é de docs", () => {
  // Regressão: "*.md" mandava a documentação do NutriPlan para a boundary do
  // harness, que respondia rodando a suíte do runner.
  const real = loadManifest();
  assert.equal(boundaryFor(norm("README.md"), real)?.id, "harness");
  assert.equal(boundaryFor(norm("apps/docs/02-arquitetura.md"), real)?.id, "docs");
  assert.equal(boundaryFor(norm("apps/dados/taco-4ed.xlsx"), real)?.id, "dados");
});

// ------------------------------------------------------- o portão de conclusão

test("gate roda a boundary inteira por id", () => {
  // Regressão: o gate sintetizava um caminho a partir do glob, e o caminho não
  // casava com o próprio padrão — passava sem rodar nada, em silêncio.
  const m = manifest({ id: "backend", commands: [cmd(1, "quebrou")] });
  const result = verifyBoundaries(["backend"], { phase: "full", manifest: m });
  assert.deepEqual(result.boundaries.map((b) => b.outcome), ["FAIL"]);
  assert.equal(failed(result), true);
});

test("gate ignora boundary que não existe", () => {
  const m = manifest({ id: "backend", commands: [cmd(0)] });
  const result = verifyBoundaries(["inexistente"], { phase: "full", manifest: m });
  assert.deepEqual(result.boundaries, []);
});

test("gate com boundary verde não reprova", () => {
  const m = manifest({ id: "backend", commands: [cmd(0)] });
  const result = verifyBoundaries(["backend"], { phase: "full", manifest: m });
  assert.equal(failed(result), false);
});

// ----------------------------------------------------------------------- o traço

const CAMPOS = ["schema", "ts", "boundary", "phase", "command", "outcome",
  "duration_s", "exit_code", "touched"];

test("traço grava um registro por comando com os campos", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  const m = manifest({ commands: [cmd(0), cmd(0)] });
  verify(["x/a.py"], { phase: "full", manifest: m, trace: traco, now: relogioFixo });
  const regs = linhas(traco);
  assert.equal(regs.length, 2);
  for (const r of regs) {
    assert.deepEqual(Object.keys(r).sort(), [...CAMPOS].sort());
    assert.equal(r.schema, "harness.trace.v1");
    assert.equal(r.ts, TS_FIXO);
    assert.equal(r.boundary, "b");
    assert.equal(r.phase, "full");
    assert.equal(r.command, cmd(0).run);
    assert.equal(r.outcome, "PASS");
    assert.equal(r.exit_code, 0);
    assert.equal(typeof r.duration_s, "number");
    assert.ok(Number(r.duration_s) >= 0);
    assert.deepEqual(r.touched, ["x/a.py"]);
  }
});

test("traço continua jsonl válido após várias execuções", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  for (let i = 0; i < 3; i += 1) {
    verify(["x/a.py"], {
      phase: "full", manifest: manifest({ commands: [cmd(0)] }),
      trace: traco, now: relogioFixo,
    });
  }
  const dados = fs.readFileSync(traco);
  assert.equal(dados.includes(0x0d), false, "o traço não deve conter CR");
  const bruto = dados.toString("utf-8").split("\n");
  assert.equal(bruto[bruto.length - 1], "");
  assert.equal(bruto.length, 4);
  for (const ln of bruto.slice(0, -1)) {
    assert.equal(typeof JSON.parse(ln), "object");
  }
});

test("traço grava output_head só em fail", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  const informativo = { ...cmd(0, "x/Alvo.java achou algo"), reportOnly: true };
  const m = manifest({ commands: [cmd(0), cmd(1, "boom: erro aqui"), informativo] });
  verify(["x/Alvo.java"], { phase: "full", manifest: m, trace: traco, now: relogioFixo });
  const porDesfecho = Object.fromEntries(linhas(traco).map((r) => [r.outcome, r]));
  assert.deepEqual(Object.keys(porDesfecho).sort(), ["FAIL", "NOTE", "PASS"]);
  assert.match(String(porDesfecho.FAIL.output_head), /boom/);
  assert.equal("output_head" in porDesfecho.PASS, false);
  assert.equal("output_head" in porDesfecho.NOTE, false);
});

test("traço grava unmapped e no_checks com command nulo", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  verify(["fora/mapa.txt", "x/a.md"], {
    phase: "full", manifest: manifest({ commands: [] }), trace: traco, now: relogioFixo,
  });
  const [unmapped, noChecks] = linhas(traco);
  assert.equal(unmapped.outcome, "UNMAPPED");
  assert.equal(unmapped.boundary, null);
  assert.equal(unmapped.phase, "full");
  assert.deepEqual(unmapped.touched, ["fora/mapa.txt"]);
  assert.equal(noChecks.outcome, "NO_CHECKS");
  assert.equal(noChecks.boundary, "b");
  assert.deepEqual(noChecks.touched, ["x/a.md"]);
  for (const r of [unmapped, noChecks]) {
    assert.equal(r.command, null);
    assert.equal(r.duration_s, null);
    assert.equal(r.exit_code, null);
  }
});

test("traço de blocked não tem duração nem exit code", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  const spec = { ...cmd(0), prerequisites: ["binario-que-nao-existe-aqui"] };
  verify(["x/a.py"], {
    phase: "full", manifest: manifest({ commands: [spec] }),
    trace: traco, now: relogioFixo,
  });
  const [r] = linhas(traco);
  assert.equal(r.outcome, "BLOCKED");
  assert.equal(r.command, spec.run);
  assert.equal(r.duration_s, null);
  assert.equal(r.exit_code, null);
});

test("teto do traço descarta o mais antigo e preserva o recente", () => {
  const dir = tmpdir();
  const traco = path.join(dir, "t.jsonl");
  config.maxTraceRecords = 3;
  /** @type {import("./runner.mjs").Result} */
  let result = { phase: "fast", boundaries: [], unmapped: [], notes: [] };
  for (let i = 1; i <= 5; i += 1) {
    result = verify([`fora/${i}.txt`], {
      manifest: manifest(), trace: traco, now: relogioFixo,
    });
  }
  assert.deepEqual(result.notes, []);
  assert.deepEqual(
    linhas(traco).map((r) => r.touched),
    [["fora/3.txt"], ["fora/4.txt"], ["fora/5.txt"]],
  );
  assert.deepEqual(fs.readdirSync(dir).filter((f) => f.endsWith(".tmp")), []);
});

test("falha ao gravar o traço não muda o resultado e emite nota", () => {
  // Um diretório no lugar do arquivo: escrever falha, a verificação não muda.
  const result = verify(["fora.txt"], {
    manifest: manifest(), trace: tmpdir(), now: relogioFixo,
  });
  assert.deepEqual(result.unmapped, ["fora.txt"]);
  assert.equal(failed(result), false);
  assert.equal(result.notes.length, 1);
  assert.match(result.notes[0], /^traço não gravado em/);
  assert.match(formatForModel(result), /traço não gravado/);
});

test("traço exige relógio que devolva Date válido", () => {
  // O equivalente Python rejeitava datetime sem timezone; um Date do JavaScript é
  // um instante e não pode ser ingênuo, então o que resta guardar é o tipo.
  const traco = path.join(tmpdir(), "t.jsonl");
  assert.throws(
    () => verify(["fora.txt"], {
      manifest: manifest(),
      trace: traco,
      now: /** @type {() => Date} */ (/** @type {unknown} */ (() => "2026-09-06")),
    }),
    /Date válido/,
  );
});

test("gate grava traço com touched vazio", () => {
  const traco = path.join(tmpdir(), "t.jsonl");
  const m = manifest({ id: "backend", commands: [cmd(0)] });
  verifyBoundaries(["backend"], {
    phase: "full", manifest: m, trace: traco, now: relogioFixo,
  });
  const [r] = linhas(traco);
  assert.equal(r.boundary, "backend");
  assert.equal(r.phase, "full");
  assert.deepEqual(r.touched, []);
});

// ------------------------------------------------------ as fronteiras de runtime
//
// Os tipos são apagados na execução. Estes testes cobrem os pontos onde dados sem
// tipo entram no módulo, que é onde o JSDoc não protege nada.

test("manifesto inválido falha nomeando o motivo, não mais adiante", () => {
  for (const [ruim, motivo] of /** @type {[unknown, RegExp][]} */ ([
    [[], /raiz não é um objeto/],
    [{}, /`boundaries` não é uma lista/],
    [{ boundaries: [{ paths: ["x/**"] }] }, /sem `id`/],
    [{ boundaries: [{ id: "b", paths: "x/**" }] }, /não é uma lista de strings/],
    [{ boundaries: [{ id: "b", paths: [], commands: [{}] }] }, /sem `run`/],
    [{ boundaries: [{ id: "b", paths: [], fast: [{ run: "x", prerequisites: [1] }] }] },
      /`prerequisites` não é lista de strings/],
  ])) {
    assert.throws(() => asManifest(ruim, "teste"), motivo, `aceitou ${JSON.stringify(ruim)}`);
  }
});

test("payload do hook fora do formato devolve lista vazia em vez de quebrar", () => {
  for (const ruim of [null, undefined, 42, "texto", {}, { tool_input: null },
    { tool_input: { file_path: 7 } }, { tool_input: { edits: [null, 3] } }]) {
    assert.deepEqual(pathsFromHook(ruim), []);
  }
});

test("payload do hook reúne file_path, notebook_path e edits", () => {
  const paths = pathsFromHook({
    tool_input: {
      file_path: "a.java",
      notebook_path: "b.ipynb",
      edits: [{ file_path: "c.ts" }, { naoTem: 1 }],
    },
  });
  assert.deepEqual(paths, ["a.java", "b.ipynb", "c.ts"]);
});

test("which resolve o que existe e recusa o que não existe", () => {
  assert.notEqual(which("node"), null);
  assert.equal(which("binario-que-nao-existe-aqui"), null);
});
