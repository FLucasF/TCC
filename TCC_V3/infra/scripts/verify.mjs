// Confere que as fontes de dados de um lote batem entre si, antes de qualquer numero
// ir para o texto. Versao enxuta (decisao de 07/10): 4 checagens, as que acusam um
// defeito que nenhuma outra peca acusa. Ver PLANO-IMPLEMENTACAO.md, Parte 3.
//
// Uso (da raiz do TCC):
//   node infra/scripts/verify.mjs <desenho.json> [--mapa M.csv] [--amostra PASTA] [--semgrep S.csv] [--raiz DIR]
//   node infra/scripts/verify.mjs experiment/desenho-v4.json
//   node infra/scripts/verify.mjs experiment/desenho-v4.json --mapa ../mapa.csv --amostra evaluation/reading --semgrep analysis/semgrep-V4.csv
//
// As checagens (o numero e o do plano):
//   [4] desenho   todas as replicas de cada modelo x nivel, e nada a mais; o modelo, o
//                 braco, o harness, a imagem, o Claude Code e o effort certos em cada
//                 meta.json; cada quarteto comecou junto; nenhuma execucao interrompida
//                 pela cota; todas medidas pela mesma suite (a do arquivo atual).
//   [6] gabarito  o enunciado_hash do cabecalho do gabarito e o hash do enunciado, o lote
//                 esta nos "lotes" do cabecalho, e cada execucao rodou e foi medida com
//                 esse enunciado.
//   [2] csv       o resultados.csv tem uma linha por execucao do desenho, igual a que o
//                 aggregate.mjs gera hoje dos meta.json.
//   [1] leitura   (so com --mapa) o mapa liga cada execucao do desenho a um codigo, um
//                 para um; com --amostra, a amostra tem um codigo por modelo x nivel e a
//                 planilha do Lucas (e a da releitura) tem exatamente esses codigos, com
//                 valores da lista fechada da regua; com --semgrep, o CSV do Semgrep tem
//                 todos os pacotes.
//
// Sai com 1 e lista os problemas, ou com 0. Garante COERENCIA entre as fontes, nao que a
// classificacao esteja certa (isso e a conferencia do Semgrep, compare.mjs). So roda sobre
// lotes com desenho (o V4); TESTE e BATCH nao tem o desenho e falhariam por construcao.
// A prova de que acusa e o verify-teste.mjs, que corrompe copias sinteticas de proposito.
//
// O mapa e lido, mas o script nunca imprime a ligacao codigo -> execucao de um pacote
// coerente: so as que estao erradas, e essas ja nao sao segredo de nenhuma leitura.

import { readFileSync, existsSync, mkdtempSync, rmSync, readdirSync } from "node:fs";
import { createHash } from "node:crypto";
import { join, resolve } from "node:path";
import { tmpdir } from "node:os";
import { execFileSync } from "node:child_process";
import { fileURLToPath } from "node:url";

// Os valores que a regua (versao 4, secao 2 e 3) aceita em cada pergunta.
const VALORES = {
  P4_localizacao: ["isolado", "espalhado"],
  P4_selecao: ["consulta", "condicional-unica", "condicional-no-calculo"],
  P5_forma: ["enum-dados", "enum-abstrato", "classes", "mapa", "switch", "outro"],
  P5_proporcao: ["dados", "condicional", "estrutura"],
};

const args = process.argv.slice(2);
const COM_VALOR = ["--mapa", "--amostra", "--semgrep", "--raiz"];
const opt = (nome) => { const i = args.indexOf(`--${nome}`); return i >= 0 ? args[i + 1] : null; };
const [arqDesenho] = args.filter((a, i, l) => !a.startsWith("--") && !COM_VALOR.includes(l[i - 1]));
if (!arqDesenho) {
  console.error("uso: node infra/scripts/verify.mjs <desenho.json> [--mapa M.csv] [--amostra PASTA] [--semgrep S.csv] [--raiz DIR]");
  process.exit(2);
}
const RAIZ = resolve(opt("raiz") ?? ".");
const daRaiz = (p) => resolve(RAIZ, p);
const AGGREGATE = join(fileURLToPath(new URL(".", import.meta.url)), "aggregate.mjs");

const problemas = [];
const avisos = [];
const erro = (chk, msg) => problemas.push(`[${chk}] ${msg}`);
const aviso = (chk, msg) => avisos.push(`[${chk}] ${msg}`);
const sha256 = (arq) => createHash("sha256").update(readFileSync(arq)).digest("hex");

// CSV com aspas (o aggregate.mjs e a planilha do Lucas usam). Nunca split(",").
function lerCsv(arq) {
  const texto = readFileSync(arq, "utf8").replace(/^﻿/, "");
  const linhas = [];
  let campo = "", linha = [], aspas = false;
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (aspas) {
      if (c === '"' && texto[i + 1] === '"') { campo += '"'; i++; }
      else if (c === '"') aspas = false;
      else campo += c;
    } else if (c === '"') aspas = true;
    else if (c === ",") { linha.push(campo); campo = ""; }
    else if (c === "\n" || c === "\r") {
      if (c === "\r" && texto[i + 1] === "\n") i++;
      linha.push(campo); campo = "";
      if (linha.some((v) => v !== "")) linhas.push(linha);
      linha = [];
    } else campo += c;
  }
  linha.push(campo);
  if (linha.some((v) => v !== "")) linhas.push(linha);
  const uteis = linhas.filter((l) => !l[0].startsWith("#"));
  const [cab, ...corpo] = uteis;
  return { cab: cab ?? [], linhas: corpo, objetos: corpo.map((l) => Object.fromEntries(cab.map((n, i) => [n, l[i] ?? ""]))) };
}

// ------------------------------------------------------------------ o desenho
const D = JSON.parse(readFileSync(daRaiz(arqDesenho), "utf8"));
for (const campo of ["prefixo", "replicas", "niveis", "modelos", "image_id", "claude_code_version", "effort", "suite", "gabarito", "resultados"]) {
  if (D[campo] === undefined || D[campo] === null) {
    console.error(`ERRO: o desenho nao tem "${campo}"` + (campo === "modelos" ? " (os modelos saem do mapa, passo 3 do MINIPLANO-V4)" : ""));
    process.exit(2);
  }
}
const NIVEIS = Object.keys(D.niveis);
const MODELOS = Object.keys(D.modelos);
const rep = (r) => String(r).padStart(2, "0");
const ESPERADAS = [];
for (let r = 1; r <= D.replicas; r++)
  for (const m of MODELOS)
    for (const n of NIVEIS)
      ESPERADAS.push({ run_id: `${D.prefixo}-${rep(r)}-${m}-${n}`, replica: r, modelo: m, nivel: n });
const ID_ESPERADO = new Set(ESPERADAS.map((e) => e.run_id));
const RUNS = daRaiz("runs");

// ------------------------------------------------------------------ [4] desenho
const metas = new Map();
const presentes = existsSync(RUNS)
  ? readdirSync(RUNS).filter((r) => r.startsWith(`${D.prefixo}-`))
  : [];
for (const r of presentes)
  if (!ID_ESPERADO.has(r))
    erro("desenho", `runs/${r} nao e do desenho (sobra de quarteto refeito? tire de runs/ e registre)`);

// Um id de modelo bate com o pedido se e ele, ou ele com a data da versao no fim.
const mesmoModelo = (visto, pedido) => visto === pedido || new RegExp(`^${pedido.replace(/[.]/g, "\\.")}-\\d{8}$`).test(visto);
const prefixoHash = (h) => (h ?? "").slice(0, 16);
const SUITE_HASH = existsSync(daRaiz(D.suite)) ? sha256(daRaiz(D.suite)).slice(0, 16) : null;
if (!SUITE_HASH) erro("desenho", `a suite ${D.suite} nao existe`);

for (const e of ESPERADAS) {
  const dir = join(RUNS, e.run_id);
  const arq = join(dir, "meta.json");
  if (!existsSync(arq)) { erro("desenho", `${e.run_id}: falta (sem meta.json)`); continue; }
  const m = JSON.parse(readFileSync(arq, "utf8"));
  metas.set(e.run_id, m);
  const nivel = D.niveis[e.nivel];
  const id = D.modelos[e.modelo];
  const env = m.environment ?? {};
  const dif = (o, a, b) => erro("desenho", `${e.run_id}: ${o} e ${JSON.stringify(a)}, o desenho pede ${JSON.stringify(b)}`);

  if (m.run_id !== e.run_id) dif("run_id", m.run_id, e.run_id);
  if (m.replicate !== e.replica) dif("replicate", m.replicate, e.replica);
  if (m.model_requested !== id) dif("model_requested", m.model_requested, id);
  if (m.model_init && !mesmoModelo(m.model_init, id)) dif("model_init", m.model_init, id);
  for (const v of m.models_observed?.messages ?? [])
    if (v !== "<synthetic>" && !mesmoModelo(v, id)) erro("desenho", `${e.run_id}: respondeu o modelo ${v}, pedido ${id}`);
  if (m.condition !== nivel.condition) dif("condition", m.condition, nivel.condition);
  const hh = prefixoHash(env.harness_hash);
  if (nivel.harness_hash === null ? hh !== "" : hh !== prefixoHash(nivel.harness_hash))
    dif("harness_hash", hh || null, nivel.harness_hash);
  if (env.image_id !== D.image_id) dif("image_id", env.image_id, D.image_id);
  if (env.claude_code_version !== D.claude_code_version) dif("claude_code_version", env.claude_code_version, D.claude_code_version);
  if (m.parameters?.effort !== D.effort) dif("effort", m.parameters?.effort, D.effort);

  // Interrompida pela cota ou pela API: o quarteto inteiro e refeito (OBJETIVO, secao 2).
  // Outro termino (tempo, desistencia do agente) e dado do modelo, e so avisa.
  const o = m.outcome ?? {};
  if (o.termination !== "completed") {
    const daBancada = o.terminal_reason === "api_error" || /limit|rate|overloaded/i.test(o.final_message ?? "");
    if (daBancada) erro("desenho", `${e.run_id}: interrompida (${o.terminal_reason}: "${(o.final_message ?? "").slice(0, 60)}"); refazer o quarteto inteiro`);
    else aviso("desenho", `${e.run_id}: terminou como "${o.termination}" (${o.terminal_reason}); confira se e do modelo, e nao da bancada`);
  }

  // A medicao: a suite que esta no arquivo hoje, e o enunciado conferido.
  const acc = join(dir, "acceptance.txt");
  if (!existsSync(acc)) { erro("desenho", `${e.run_id}: sem acceptance.txt (a suite nao rodou)`); continue; }
  const campo = (k) => (readFileSync(acc, "utf8").match(new RegExp(`^${k}: (.*)$`, "m")) ?? [])[1] ?? "";
  const suite = campo("suite").split(" ")[1];
  if (SUITE_HASH && suite !== SUITE_HASH) erro("desenho", `${e.run_id}: medida com a suite ${suite}, a atual e ${SUITE_HASH}; rode o acceptance.sh de novo`);
  if (campo("image") !== D.image_id) erro("desenho", `${e.run_id}: a suite rodou na imagem ${campo("image")}, o desenho pede ${D.image_id}`);
}

// Um quarteto (mesma replica e modelo) comeca junto: o run-levels.sh lanca os niveis no
// mesmo instante. Um nivel refeito sozinho, depois, aparece aqui.
const janela = (D.simultaneos_minutos ?? 5) * 60 * 1000;
for (let r = 1; r <= D.replicas; r++)
  for (const mo of MODELOS) {
    const ids = NIVEIS.map((n) => `${D.prefixo}-${rep(r)}-${mo}-${n}`).filter((id) => metas.has(id));
    const inicios = ids.map((id) => Date.parse(metas.get(id).timing?.start));
    if (inicios.some(Number.isNaN)) { erro("desenho", `${D.prefixo}-${rep(r)}-${mo}: inicio ilegivel no meta.json`); continue; }
    if (inicios.length > 1 && Math.max(...inicios) - Math.min(...inicios) > janela)
      erro("desenho", `${D.prefixo}-${rep(r)}-${mo}: os niveis nao comecaram juntos (${Math.round((Math.max(...inicios) - Math.min(...inicios)) / 60000)} min de diferenca); refazer o quarteto inteiro`);
  }

// ------------------------------------------------------------------ [6] gabarito
const arqGab = daRaiz(D.gabarito);
if (!existsSync(arqGab)) erro("gabarito", `${D.gabarito} nao existe`);
else {
  const cab = (readFileSync(arqGab, "utf8").match(/^---\r?\n([\s\S]*?)\r?\n---/) ?? [])[1] ?? "";
  const valor = (k) => (cab.match(new RegExp(`^${k}:\\s*(.*)$`, "m")) ?? [])[1]?.trim() ?? "";
  const enunciado = valor("enunciado"), hash = valor("enunciado_hash");
  const lotes = valor("lotes").split(/[\s,]+/).filter(Boolean);
  if (!enunciado || !hash) erro("gabarito", `o cabecalho de ${D.gabarito} nao tem enunciado e enunciado_hash`);
  else {
    if (!existsSync(daRaiz(enunciado))) erro("gabarito", `o enunciado ${enunciado} nao existe`);
    else if (sha256(daRaiz(enunciado)) !== hash) erro("gabarito", `o enunciado_hash do gabarito (${hash.slice(0, 16)}) nao e o hash de ${enunciado} (${sha256(daRaiz(enunciado)).slice(0, 16)})`);
    if (!lotes.includes(D.prefixo)) erro("gabarito", `o lote ${D.prefixo} nao esta nos "lotes" do gabarito (${lotes.join(", ") || "vazio"})`);
    for (const [id, m] of metas) {
      if (m.environment?.prompt_hash !== hash) erro("gabarito", `${id}: rodou com o enunciado ${prefixoHash(m.environment?.prompt_hash)}, o gabarito e de ${hash.slice(0, 16)}`);
      const acc = join(RUNS, id, "acceptance.txt");
      if (existsSync(acc)) {
        const p = (readFileSync(acc, "utf8").match(/^prompt: (\S+) (\S+)/m) ?? []);
        if (p[1] !== hash.slice(0, 16) || p[2] !== "confere") erro("gabarito", `${id}: a suite foi medida com o enunciado "${p[1]} ${p[2]}", o gabarito e de ${hash.slice(0, 16)}`);
      }
    }
  }
}

// ------------------------------------------------------------------ [2] csv
const arqRes = daRaiz(D.resultados);
if (!existsSync(arqRes)) erro("csv", `${D.resultados} nao existe (rode o aggregate.mjs)`);
else {
  const tmp = mkdtempSync(join(tmpdir(), "verify-"));
  try {
    execFileSync(process.execPath, [AGGREGATE, "--prefix", `${D.prefixo}-`, "--out", join(tmp, "r.csv")], { cwd: RAIZ, stdio: "pipe" });
    const novo = lerCsv(join(tmp, "r.csv"));
    const atual = lerCsv(arqRes);
    if (novo.cab.join(",") !== atual.cab.join(","))
      erro("csv", `as colunas de ${D.resultados} nao sao as do aggregate.mjs atual; gere de novo`);
    else {
      const porId = (t) => { const mp = new Map(); for (const l of t.linhas) mp.set(l[0], [...(mp.get(l[0]) ?? []), l]); return mp; };
      const A = porId(atual), N = porId(novo);
      for (const [id, ls] of A) {
        if (!id.startsWith(`${D.prefixo}-`)) continue;
        if (!ID_ESPERADO.has(id)) erro("csv", `${id}: linha no CSV, mas nao e do desenho`);
        if (ls.length > 1) erro("csv", `${id}: ${ls.length} linhas no CSV`);
      }
      for (const id of ID_ESPERADO) {
        if (!metas.has(id)) continue;
        const a = A.get(id)?.[0], n = N.get(id)?.[0];
        if (!a) { erro("csv", `${id}: tem meta.json, mas nao tem linha no CSV`); continue; }
        const difs = novo.cab.filter((c, i) => a[i] !== n[i]);
        if (difs.length) erro("csv", `${id}: o CSV difere do meta.json em ${difs.join(", ")}`);
      }
    }
  } finally {
    rmSync(tmp, { recursive: true, force: true });
  }
}

// ------------------------------------------------------------------ [1] leitura
const arqMapa = opt("mapa");
if (!arqMapa) aviso("leitura", "pulada: sem --mapa (so roda depois da anonimizacao)");
else {
  const mapa = lerCsv(daRaiz(arqMapa)).objetos;
  const porCodigo = new Map(), porRun = new Map();
  for (const l of mapa) {
    if (porCodigo.has(l.blind_code)) erro("leitura", `o codigo ${l.blind_code} aparece duas vezes no mapa`);
    porCodigo.set(l.blind_code, l.run_id);
    if (porRun.has(l.run_id)) erro("leitura", `${l.run_id} tem dois codigos no mapa`);
    porRun.set(l.run_id, l.blind_code);
    if (!ID_ESPERADO.has(l.run_id)) erro("leitura", `o codigo ${l.blind_code} aponta para ${l.run_id}, que nao e do desenho`);
  }
  for (const id of ID_ESPERADO) if (!porRun.has(id)) erro("leitura", `${id} nao tem codigo no mapa`);

  const pasta = opt("amostra");
  if (pasta) {
    const amostra = lerCsv(daRaiz(join(pasta, "amostra.csv"))).objetos.map((l) => l.blind_code);
    const grupos = new Map();
    for (const c of amostra) {
      const run = porCodigo.get(c);
      if (!run) { erro("leitura", `a amostra tem o codigo ${c}, que nao esta no mapa`); continue; }
      const e = ESPERADAS.find((x) => x.run_id === run);
      if (!e) continue;
      const g = `${e.modelo} ${e.nivel}`;
      grupos.set(g, (grupos.get(g) ?? 0) + 1);
    }
    for (const mo of MODELOS) for (const n of NIVEIS) {
      const q = grupos.get(`${mo} ${n}`) ?? 0;
      if (q !== 1) erro("leitura", `a amostra tem ${q} pacote(s) de ${mo} ${n}, o desenho pede 1`);
    }
    const conjAmostra = new Set(amostra);

    const planilha = (nome, exata) => {
      const arq = daRaiz(join(pasta, nome));
      if (!existsSync(arq)) { aviso("leitura", `${nome} nao existe ainda`); return; }
      const t = lerCsv(arq);
      const codigos = t.objetos.map((l) => l.blind_code);
      const vistos = new Set();
      for (const c of codigos) {
        if (vistos.has(c)) erro("leitura", `${nome}: o codigo ${c} aparece duas vezes`);
        vistos.add(c);
        if (!conjAmostra.has(c)) erro("leitura", `${nome}: o codigo ${c} nao esta na amostra`);
      }
      if (exata) for (const c of conjAmostra) if (!vistos.has(c)) erro("leitura", `${nome}: falta o codigo ${c} da amostra`);
      let vazias = 0;
      for (const l of t.objetos)
        for (const [p, ok] of Object.entries(VALORES)) {
          const v = (l[p] ?? "").trim();
          if (v === "") vazias++;
          else if (v !== "indeterminado" && !ok.includes(v))
            erro("leitura", `${nome}: ${l.blind_code} ${p} = "${v}", fora da lista da regua (${ok.join(", ")}, indeterminado)`);
        }
      if (vazias) aviso("leitura", `${nome}: ${vazias} celula(s) vazia(s); na comparacao contam como discordancia`);
    };
    planilha("leitura-lucas.csv", true);
    if (existsSync(daRaiz(join(pasta, "releitura.csv")))) planilha("releitura-lucas.csv", false);
  }

  const arqSem = opt("semgrep");
  if (arqSem) {
    const pacotes = lerCsv(daRaiz(arqSem)).objetos.map((l) => l.pacote);
    const conj = new Set(pacotes);
    if (conj.size !== pacotes.length) erro("leitura", `${arqSem}: pacote repetido`);
    // O Semgrep roda nos pacotes cegos (codigos) ou no original (run_id); qualquer um serve.
    const universo = [...porCodigo.keys()].some((c) => conj.has(c)) ? [...porCodigo.keys()] : [...ID_ESPERADO];
    for (const p of universo) if (!conj.has(p)) erro("leitura", `${arqSem}: falta o pacote ${p}`);
    for (const p of conj) if (!universo.includes(p)) erro("leitura", `${arqSem}: o pacote ${p} nao e do desenho`);
  }
}

// ------------------------------------------------------------------ resultado
console.log(`lote ${D.prefixo}: ${ESPERADAS.length} execucoes no desenho (${D.replicas} replicas x ${MODELOS.length} modelos x ${NIVEIS.length} niveis), ${metas.size} com meta.json`);
for (const a of avisos) console.log(`  aviso ${a}`);
if (problemas.length) {
  for (const p of problemas) console.log(`  ERRO  ${p}`);
  console.log(`\n${problemas.length} problema(s).`);
  process.exit(1);
}
console.log("\ntudo coerente.");
