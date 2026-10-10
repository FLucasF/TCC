// As tabelas e o veredito de cada hipotese do OBJETIVO (secao 4), pelas regras de
// leitura do secao 4.1, escritas aqui ANTES dos dados (09/10). Com o script congelado,
// a analise do V4 e so rodar: nenhuma escolha (qual par conta, o que e "melhor", quem
// esta no teto) fica para depois de ver os resultados.
//
// Uso (da raiz do TCC):
//   node evaluation/tools/hipoteses.mjs <desenho.json> --semgrep S.csv [--mapa M.csv]
//        --aceitacao A.csv --metricas X.csv --resultados R.csv
//        [--conferencia C.md] [--exploratorio] [--json saida.json] [--raiz DIR]
//   (o relatorio em markdown sai no stdout)
//
// COMO LER. Quatro partes:
//   1. junta, por execucao, o que cada instrumento mediu: o Semgrep (pelo codigo cego e
//      o mapa), a suite, as metricas, o custo e o meta.json (se usou a skill, o revisor);
//   2. MEDIDAS: transforma isso nos numeros que as hipoteses comparam (o P4 certo ou
//      nao, os pontos da suite, a complexidade cognitiva, os tokens...);
//   3. HIPOTESES: a lista, cada uma com a medida, os dois niveis comparados (N1 x N0,
//      N2 x N1 ou N3 x N2), o tipo de regra e o que e "melhor";
//   4. as REGRAS do secao 4.1, uma funcao por tipo, e o relatorio.
//
// Um PAR e o mesmo modelo e a mesma replica em dois niveis vizinhos (25 por comparacao).
// Tres escolhas que o OBJETIVO nao fazia, decididas aqui antes dos dados (DECISOES.md):
//   - um par com "indeterminado" (ou sem medida) sai da conta como "sem dado", e e contado;
//   - o saldo maximo da nao-inferioridade e 13% dos pares, arredondado (3 em 25, 2 em 15,
//     a mesma proporcao que o OBJETIVO usa para justificar o 3);
//   - na hipotese do modelo, se dois modelos empatam como o mais fraco, ela e apoiada se
//     o maior saldo, sem empate, for de um deles.
// Com --conferencia (a saida do compare.mjs), as hipoteses que dependem de uma pergunta
// reprovada (abaixo de 18 de 20) viram "descritiva". Com --exploratorio, nenhuma
// hipotese tem veredito: o lote so descreve.

import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { join, resolve } from "node:path";

// ------------------------------------------------------------------ argumentos e leitura
const args = process.argv.slice(2);
const COM_VALOR = ["--semgrep", "--mapa", "--aceitacao", "--metricas", "--resultados", "--conferencia", "--json", "--raiz"];
const opt = (n) => { const i = args.indexOf(`--${n}`); return i >= 0 ? args[i + 1] : null; };
const [arqDesenho] = args.filter((a, i, l) => !a.startsWith("--") && !COM_VALOR.includes(l[i - 1]));
const morrer = (m) => { console.error(`ERRO: ${m}`); process.exit(2); };
if (!arqDesenho) morrer("uso: hipoteses.mjs <desenho.json> --semgrep S.csv [--mapa M.csv] --aceitacao A.csv --metricas X.csv --resultados R.csv");
for (const o of ["semgrep", "aceitacao", "metricas", "resultados"]) if (!opt(o)) morrer(`falta --${o}`);
const RAIZ = resolve(opt("raiz") ?? ".");
const EXPLORATORIO = args.includes("--exploratorio");

// CSV com aspas (nunca split(",")).
function lerCsv(arq) {
  const t = readFileSync(arq, "utf8").replace(/^﻿/, "");
  const linhas = []; let campo = "", linha = [], aspas = false;
  for (let i = 0; i < t.length; i++) {
    const c = t[i];
    if (aspas) { if (c === '"' && t[i + 1] === '"') { campo += '"'; i++; } else if (c === '"') aspas = false; else campo += c; }
    else if (c === '"') aspas = true;
    else if (c === ",") { linha.push(campo); campo = ""; }
    else if (c === "\n" || c === "\r") { if (c === "\r" && t[i + 1] === "\n") i++; linha.push(campo); campo = ""; if (linha.some((v) => v !== "")) linhas.push(linha); linha = []; }
    else campo += c;
  }
  linha.push(campo); if (linha.some((v) => v !== "")) linhas.push(linha);
  const uteis = linhas.filter((l) => !l[0].startsWith("#"));
  const [cab, ...corpo] = uteis;
  return corpo.map((l) => Object.fromEntries(cab.map((n, i) => [n, (l[i] ?? "").trim()])));
}
const porChave = (linhas, chave) => new Map(linhas.map((l) => [l[chave], l]));

const D = JSON.parse(readFileSync(resolve(RAIZ, arqDesenho), "utf8"));
if (!D.modelos) morrer("o desenho nao tem modelos");
const NIVEIS = Object.keys(D.niveis);
const MODELOS = Object.keys(D.modelos);
const rep = (r) => String(r).padStart(2, "0");
const runId = (r, m, n) => `${D.prefixo}-${rep(r)}-${m}-${n}`;

const semgrep = lerCsv(resolve(RAIZ, opt("semgrep")));
const mapa = opt("mapa") ? porChave(lerCsv(resolve(RAIZ, opt("mapa"))), "run_id") : new Map();
const semPorPacote = porChave(semgrep, "pacote");
const aceitacao = porChave(lerCsv(resolve(RAIZ, opt("aceitacao"))), "run_id");
const metricas = porChave(lerCsv(resolve(RAIZ, opt("metricas"))), "run_id");
const resultados = porChave(lerCsv(resolve(RAIZ, opt("resultados"))), "run_id");

// As perguntas que a conferencia reprovou (linhas "| P4_localizacao | 16 de 20 | vira **descritiva** |").
const reprovadas = new Set();
if (opt("conferencia")) for (const l of readFileSync(resolve(RAIZ, opt("conferencia")), "utf8").split(/\r?\n/)) {
  const m = l.match(/^\|\s*(P\d_\w+)\s*\|.*descritiva/); if (m) reprovadas.add(m[1]);
}

// ------------------------------------------------------------------ 2. medidas
// Os 9 casos de borda da suite (secao 4.4): fronteiras, precedencia entre erros e a colisao.
const BORDA = [
  "OURO + FRETEGRATIS + cartao 10x",
  "precedencia: nivel antes de regiao", "precedencia: regiao antes de modalidade",
  "precedencia: cupom antes de pagamento", "precedencia: pedido invalido primeiro",
  "fronteira: motoboy com 5 kg exatos", "fronteira: OURO com produtos em 500,00 exatos",
  "fronteira: MENOS50 com produtos em 300,00 exatos", "fronteira: boleto com 1000,00 exatos",
];
const TRAVA = new Set(["no_pom", "build_failed", "app_did_not_start"]);
const num = (v) => (v === undefined || v === null || v === "" || isNaN(Number(v)) ? null : Number(v));
const acerto = (s, p) => {
  const loc = s?.[`${p}_localizacao`], sel = s?.[`${p}_selecao`];
  if (!loc || !sel || loc === "indeterminado" || sel === "indeterminado") return null;
  return loc === "isolado" && (sel === "consulta" || sel === "condicional-unica") ? 1 : 0;
};

const execucoes = new Map(); // run_id -> { modelo, replica, nivel, m: {medidas} }
const faltas = [];
for (let r = 1; r <= D.replicas; r++) for (const mo of MODELOS) for (const n of NIVEIS) {
  const id = runId(r, mo, n);
  const s = semPorPacote.get(mapa.get(id)?.blind_code ?? id);
  const a = aceitacao.get(id), x = metricas.get(id), q = resultados.get(id);
  if (!s || !a || !x || !q) faltas.push(`${id}: ${[!s && "Semgrep", !a && "suite", !x && "metricas", !q && "resultados"].filter(Boolean).join(", ")}`);
  const travado = TRAVA.has(a?.status);
  const pontos = (k) => (a ? (travado ? 0 : num(a[k])) : null);
  let borda = null;
  const accTxt = join(RAIZ, "runs", id, "acceptance.txt");
  if (a && travado) borda = 0;
  else if (existsSync(accTxt)) {
    const falhas = readFileSync(accTxt, "utf8").split(/\r?\n/).filter((l) => /^\s*FALHA /.test(l)).map((l) => l.replace(/^\s*FALHA /, ""));
    borda = BORDA.filter((c) => !falhas.some((f) => f.startsWith(c + ":") || f.startsWith(c + " "))).length;
  }
  let meta = null;
  const arqMeta = join(RAIZ, "runs", id, "meta.json");
  if (existsSync(arqMeta)) meta = JSON.parse(readFileSync(arqMeta, "utf8"));
  const p13 = ["P1", "P2", "P3"].map((p) => acerto(s, p));
  execucoes.set(id, {
    modelo: mo, replica: r, nivel: n, perfil: s ? ["P1", "P2", "P3", "P4"].flatMap((p) => [s[`${p}_localizacao`], s[`${p}_selecao`]]).concat(s.P5_proporcao).join("|") : null,
    m: {
      p4_acerto: acerto(s, "P4"),
      p1p3_acertos: p13.some((v) => v === null) ? null : p13.reduce((x, y) => x + y, 0),
      p5_exagero: !s?.P5_proporcao || s.P5_proporcao === "indeterminado" ? null : s.P5_proporcao === "estrutura" ? 1 : 0,
      pontos: pontos("pontos"), contas: pontos("contas"), recusas: pontos("recusas"),
      build_ok: a ? (travado ? 0 : 1) : null,
      borda,
      cognitiva: num(x?.sonar_cognitive_complexity), smells: num(x?.sonar_code_smells),
      duplicacao: num(x?.sonar_duplicated_lines_density),
      classes: num(x?.sonar_classes), cbo: num(x?.ck_cbo_media), lcom: num(x?.ck_lcom_media),
      tokens: num(q?.input_total), tempo: num(q?.duration_api_ms), turnos: num(q?.turns),
      usou_skill: meta ? ((meta.outcome?.tool_calls_by_name?.Skill ?? 0) > 0 ? 1 : 0) : null,
      usou_revisor: meta ? ((meta.outcome?.subagents_spawned ?? 0) > 0 ? 1 : 0) : null,
    },
  });
}

// ------------------------------------------------------------------ 3. hipoteses
// tipo: direcional | nao-inferioridade | sem-direcao-continua | sem-direcao-binaria
// melhor: "mais" ou "menos" (o que conta como melhor no nivel de cima)
// teto: o valor que, no nivel de baixo nas 5 replicas, deixa o modelo sem espaco para
// melhorar (so nas direcionais de medida limitada; null nas continuas)
// depende: as perguntas da conferencia de que o veredito depende
const H = [
  { nome: "Desenho: isola cada caso no P4", principal: true, medida: "p4_acerto", par: ["N0", "N1"], tipo: "direcional", melhor: "mais", teto: 1, depende: ["P4_localizacao", "P4_selecao"] },
  { nome: "Exagero: aplica onde nao pede", principal: true, medida: "p5_exagero", par: ["N0", "N1"], tipo: "sem-direcao-binaria", depende: ["P5_proporcao"] },
  { nome: "Correcao: suite inteira", principal: true, medida: "pontos", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais" },
  { nome: "Qualidade: menos complexidade", principal: true, medida: "cognitiva", par: ["N0", "N1"], tipo: "direcional", melhor: "menos", teto: null },
  { nome: "Modelo: efeito maior no mais fraco", principal: true, tipo: "modelo", depende: ["P4_localizacao", "P4_selecao"] },

  { nome: "Desenho: isola cada caso no P1 a P3", medida: "p1p3_acertos", par: ["N0", "N1"], tipo: "direcional", melhor: "mais", teto: 3 },
  { nome: "Desenho: nao repete o comum", medida: "duplicacao", par: ["N0", "N1"], tipo: "direcional", melhor: "menos", teto: null },
  { nome: "Desenho: replicas mais parecidas", tipo: "descritiva-replicas", par: ["N0", "N1"] },
  { nome: "Exagero: mais arquivos", medida: "classes", par: ["N0", "N1"], tipo: "direcional", melhor: "mais", teto: null },
  { nome: "Exagero: estrutura especulativa (CBO)", medida: "cbo", par: ["N0", "N1"], tipo: "sem-direcao-continua" },
  { nome: "Exagero: estrutura especulativa (LCOM)", medida: "lcom", par: ["N0", "N1"], tipo: "sem-direcao-continua" },
  { nome: "Correcao: contas", medida: "contas", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais" },
  { nome: "Correcao: recusas", medida: "recusas", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais" },
  { nome: "Correcao: nao quebra o build", medida: "build_ok", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais" },
  { nome: "Correcao: casos de borda", medida: "borda", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais" },
  { nome: "Qualidade: menos code smells", medida: "smells", par: ["N0", "N1"], tipo: "direcional", melhor: "menos", teto: null },
  { nome: "Custo: tokens de entrada", medida: "tokens", par: ["N0", "N1"], tipo: "sem-direcao-continua" },
  { nome: "Custo: tempo de API", medida: "tempo", par: ["N0", "N1"], tipo: "sem-direcao-continua" },
  { nome: "Custo: processo de trabalho (turnos)", medida: "turnos", par: ["N0", "N1"], tipo: "sem-direcao-continua" },
  { nome: "Custo: sinal muda por modelo", medida: "tokens", par: ["N0", "N1"], tipo: "descritiva-sinal" },
  { nome: "Modelo: no teto, nao piora (desenho)", medida: "p4_acerto", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais", soNoTeto: { medida: "p4_acerto", teto: 1 }, depende: ["P4_localizacao", "P4_selecao"] },
  { nome: "Modelo: no teto, nao piora (correcao)", medida: "pontos", par: ["N0", "N1"], tipo: "nao-inferioridade", melhor: "mais", soNoTeto: { medida: "p4_acerto", teto: 1 } },
  { nome: "Skills: mais efeito no desenho", medida: "p4_acerto", par: ["N1", "N2"], tipo: "direcional", melhor: "mais", teto: 1, depende: ["P4_localizacao", "P4_selecao"] },
  { nome: "Skills: mais custo (tokens)", medida: "tokens", par: ["N1", "N2"], tipo: "direcional", melhor: "mais", teto: null },
  { nome: "Skills: mudam o exagero", medida: "p5_exagero", par: ["N1", "N2"], tipo: "sem-direcao-binaria", depende: ["P5_proporcao"] },
  { nome: "Skills: so valem se carregadas", medida: "usou_skill", nivel: "N2", tipo: "descritiva-uso" },
  { nome: "Processo: corrige mais", medida: "pontos", par: ["N2", "N3"], tipo: "direcional", melhor: "mais", teto: 21 },
  { nome: "Processo: mais efeito no desenho", medida: "p4_acerto", par: ["N2", "N3"], tipo: "direcional", melhor: "mais", teto: 1, depende: ["P4_localizacao", "P4_selecao"] },
  { nome: "Processo: menos exagero", medida: "p5_exagero", par: ["N2", "N3"], tipo: "direcional", melhor: "menos", teto: 0, depende: ["P5_proporcao"] },
  { nome: "Processo: mais custo (tokens)", medida: "tokens", par: ["N2", "N3"], tipo: "direcional", melhor: "mais", teto: null },
  { nome: "Processo: mais custo (tempo)", medida: "tempo", par: ["N2", "N3"], tipo: "direcional", melhor: "mais", teto: null },
  { nome: "Processo: so vale se usado", medida: "usou_revisor", nivel: "N3", tipo: "descritiva-uso" },
];

// ------------------------------------------------------------------ 4. as regras do secao 4.1
const C = (n, k) => { let r = 1; for (let i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; };
const caudaDupla = (n, k) => { let s = 0; for (let i = k; i <= n; i++) s += C(n, i); return Math.min(1, 2 * s / 2 ** n); };
// Menor k (do lado maior) com a chance por sorte, nos dois lados, ate o limite.
const minimoSinal = (n, limite) => { for (let k = Math.floor(n / 2) + 1; k <= n; k++) if (caudaDupla(n, k) <= limite) return k; return Infinity; };
const limiteSaldo = (n) => Math.round(0.13 * n);

const valor = (r, mo, n, medida) => execucoes.get(runId(r, mo, n))?.m[medida] ?? null;
// Os pares de uma comparacao, por modelo: "melhor", "pior", "igual" ou "sem dado".
function pares(h, modelos = MODELOS) {
  const [baixo, cima] = h.par;
  const porModelo = {};
  for (const mo of modelos) {
    const l = (porModelo[mo] = { melhor: 0, pior: 0, igual: 0, semDado: 0, sobe: 0, desce: 0 });
    for (let r = 1; r <= D.replicas; r++) {
      const b = valor(r, mo, baixo, h.medida), c = valor(r, mo, cima, h.medida);
      if (b === null || c === null) { l.semDado++; continue; }
      if (c === b) { l.igual++; continue; }
      c > b ? l.sobe++ : l.desce++;
      const melhorou = h.melhor === "menos" ? c < b : c > b;
      melhorou ? l.melhor++ : l.pior++;
    }
  }
  return porModelo;
}
const soma = (pm, k) => Object.values(pm).reduce((s, l) => s + l[k], 0);
// No teto: o nivel de baixo ja tem o melhor valor possivel nas 5 replicas.
const noTeto = (mo, nivel, medida, teto) => teto !== null && teto !== undefined &&
  Array.from({ length: D.replicas }, (_, i) => valor(i + 1, mo, nivel, medida)).every((v) => v === teto);

function veredito(h) {
  if (h.tipo === "direcional") {
    const pm = pares(h);
    const fora = MODELOS.filter((mo) => !noTeto(mo, h.par[0], h.medida, h.teto));
    let v;
    if (!(soma(pm, "melhor") + soma(pm, "pior") + soma(pm, "igual"))) v = "sem dado";
    else if (!fora.length) v = "sem espaco para efeito (todos no teto)";
    else {
      const apoiam = fora.filter((mo) => pm[mo].melhor > pm[mo].pior).length;
      const contrariam = fora.filter((mo) => pm[mo].pior > pm[mo].melhor).length;
      const algumPiora = MODELOS.some((mo) => pm[mo].pior > pm[mo].melhor);
      v = apoiam > fora.length / 2 && !algumPiora ? "apoiada" : contrariam > fora.length / 2 ? "contrariada" : "inconclusiva";
    }
    return { v, pm, fora, regra: `maioria dos ${fora.length} modelo(s) fora do teto com mais pares melhores que piores, e nenhum modelo com mais piores` };
  }
  if (h.tipo === "nao-inferioridade") {
    const mods = h.soNoTeto ? MODELOS.filter((mo) => noTeto(mo, h.par[0], h.soNoTeto.medida, h.soNoTeto.teto)) : MODELOS;
    if (!mods.length) return { v: "sem modelo no teto", pm: {}, regra: "" };
    const pm = pares(h, mods);
    const n = soma(pm, "melhor") + soma(pm, "pior") + soma(pm, "igual");
    if (!n) return { v: "sem dado", pm, regra: "nenhum par com as duas medidas" };
    const saldo = soma(pm, "pior") - soma(pm, "melhor");
    const lim = limiteSaldo(n);
    return { v: saldo <= lim ? "apoiada" : "contrariada", pm, regra: `saldo (piores - melhores) = ${saldo}, limite ${lim} (13% de ${n} pares)` };
  }
  if (h.tipo === "sem-direcao-continua") {
    const pm = pares(h);
    const n = soma(pm, "melhor") + soma(pm, "pior") + soma(pm, "igual");
    if (!n) return { v: "sem dado", pm, regra: "nenhum par com as duas medidas" };
    const maior = Math.max(soma(pm, "sobe"), soma(pm, "desce"));
    const k = minimoSinal(n, 0.05);
    const v = maior >= k ? "apoiada (altera)" : maior <= Math.floor(0.6 * n) ? "contrariada" : "inconclusiva";
    return { v, pm, regra: `lado maior ${maior} de ${n} pares; apoiada com ${k} ou mais, contrariada com ${Math.floor(0.6 * n)} ou menos` };
  }
  if (h.tipo === "sem-direcao-binaria") {
    const pm = pares(h);
    if (!(soma(pm, "melhor") + soma(pm, "pior") + soma(pm, "igual"))) return { v: "sem dado", pm, regra: "nenhum par com as duas medidas" };
    const sobe = soma(pm, "sobe"), desce = soma(pm, "desce"), d = sobe + desce;
    const maior = Math.max(sobe, desce), menor = Math.min(sobe, desce);
    const k = minimoSinal(d, 0.063);
    const v = d >= 5 && maior >= k ? "apoiada (altera)" : d <= 2 || menor >= d / 3 ? "contrariada" : "inconclusiva";
    return { v, pm, regra: `${d} par(es) nao empatado(s) (${sobe} sobem, ${desce} descem); com ${d}, o lado maior precisa de ${isFinite(k) ? k : "-"}` };
  }
  if (h.tipo === "modelo") {
    const base = H[0];
    const pm = pares(base);
    const mediaN0 = Object.fromEntries(MODELOS.map((mo) => {
      const vs = Array.from({ length: D.replicas }, (_, i) => valor(i + 1, mo, "N0", base.medida)).filter((v) => v !== null);
      return [mo, vs.length ? vs.reduce((a, b) => a + b, 0) / vs.length : null];
    }));
    const menor = Math.min(...Object.values(mediaN0).filter((v) => v !== null));
    const fracos = MODELOS.filter((mo) => mediaN0[mo] === menor);
    const saldo = Object.fromEntries(MODELOS.map((mo) => [mo, pm[mo].melhor - pm[mo].pior]));
    const max = Math.max(...Object.values(saldo));
    const noMax = MODELOS.filter((mo) => saldo[mo] === max);
    const v = noMax.length > 1 ? "inconclusiva (empate no maior saldo)" : fracos.includes(noMax[0]) ? "apoiada" : "contrariada";
    return { v, pm, regra: `mais fraco(s) no N0: ${fracos.join(", ")} (acerto medio ${menor.toFixed(2)}); maior saldo: ${noMax.join(", ")} (${max})`, extra: { mediaN0, saldo } };
  }
  return null;
}

// ------------------------------------------------------------------ o relatorio
const out = [];
const linhaTabela = (mo, l, teto) => `| ${mo}${teto ? " (teto)" : ""} | ${l.melhor} | ${l.igual} | ${l.pior} | ${l.semDado} | ${(l.melhor - l.pior) >= 0 ? "+" : ""}${l.melhor - l.pior} |`;
out.push(`# Hipoteses: ${D.prefixo}${EXPLORATORIO ? " (EXPLORATORIO: sem veredito)" : ""}`);
out.push(`\nGerado por evaluation/tools/hipoteses.mjs. Regras: OBJETIVO, secao 4.1. ${D.replicas} replicas x ${MODELOS.length} modelos x ${NIVEIS.length} niveis.`);
if (faltas.length) out.push(`\n**Execucoes com medida faltando (${faltas.length}):** ${faltas.slice(0, 10).join("; ")}${faltas.length > 10 ? "..." : ""}`);
if (reprovadas.size) out.push(`\n**A conferencia reprovou:** ${[...reprovadas].join(", ")}. As hipoteses que dependem delas sao descritivas.`);

const resumo = [];
for (const h of H) {
  out.push(`\n## ${h.principal ? "★ " : ""}${h.nome}`);
  if (h.tipo === "descritiva-replicas") {
    out.push("\nSem regra de pares no OBJETIVO: descritiva. Perfis diferentes do Semgrep (P1 a P5) entre as replicas de cada modelo; menos perfis = mais parecidas.\n");
    out.push(`| modelo | ${h.par[0]} | ${h.par[1]} |`); out.push("|---|---|---|");
    for (const mo of MODELOS) {
      const perfis = (n) => new Set(Array.from({ length: D.replicas }, (_, i) => execucoes.get(runId(i + 1, mo, n))?.perfil).filter(Boolean)).size;
      out.push(`| ${mo} | ${perfis(h.par[0])} | ${perfis(h.par[1])} |`);
    }
    resumo.push({ nome: h.nome, veredito: "descritiva" }); continue;
  }
  if (h.tipo === "descritiva-sinal") {
    const pm = pares(h);
    out.push("\nSem regra de pares no OBJETIVO: descritiva. A direcao dos tokens (N1 x N0) em cada modelo:\n");
    out.push("| modelo | sobem | descem | iguais |"); out.push("|---|---|---|---|");
    for (const mo of MODELOS) out.push(`| ${mo} | ${pm[mo].sobe} | ${pm[mo].desce} | ${pm[mo].igual} |`);
    resumo.push({ nome: h.nome, veredito: "descritiva" }); continue;
  }
  if (h.tipo === "descritiva-uso") {
    out.push(`\nSem regra de pares no OBJETIVO: descritiva. Execucoes do ${h.nivel} que usaram (${h.medida === "usou_skill" ? "chamaram a skill" : "chamaram o revisor"}):\n`);
    out.push("| modelo | usaram | de |"); out.push("|---|---|---|");
    for (const mo of MODELOS) {
      const vs = Array.from({ length: D.replicas }, (_, i) => valor(i + 1, mo, h.nivel, h.medida)).filter((v) => v !== null);
      out.push(`| ${mo} | ${vs.filter((v) => v === 1).length} | ${vs.length} |`);
    }
    resumo.push({ nome: h.nome, veredito: "descritiva" }); continue;
  }
  const r = veredito(h);
  const descr = (h.depende ?? []).some((p) => reprovadas.has(p));
  const v = EXPLORATORIO ? "exploratorio (sem veredito)" : descr ? `descritiva (a conferencia reprovou ${h.depende.filter((p) => reprovadas.has(p)).join(", ")})` : r.v;
  out.push(`\n${h.tipo === "modelo" ? "Pares do desenho no P4 (N1 x N0)" : `Medida: ${h.medida}, ${h.par[1]} x ${h.par[0]}`}; regra: ${h.tipo}${h.melhor ? `, melhor = ${h.melhor}` : ""}.\n`);
  out.push("| modelo | melhores | iguais | piores | sem dado | saldo |"); out.push("|---|---|---|---|---|---|");
  for (const [mo, l] of Object.entries(r.pm)) out.push(linhaTabela(mo, l, h.tipo === "direcional" && !r.fora?.includes(mo)));
  const tot = { melhor: soma(r.pm, "melhor"), igual: soma(r.pm, "igual"), pior: soma(r.pm, "pior"), semDado: soma(r.pm, "semDado") };
  out.push(linhaTabela("**todos**", tot));
  const n = tot.melhor + tot.igual + tot.pior;
  out.push(`\n${r.regra}. Tamanho do efeito (saldo / pares): ${n ? ((tot.melhor - tot.pior) / n).toFixed(2) : "-"}.`);
  out.push(`\n**Veredito: ${v}.**`);
  resumo.push({ nome: h.nome, principal: !!h.principal, veredito: v });
}

// O teste de Page, so como informacao (secao 4.1): a medida sobe de N0 a N3?
function page(medida, melhor) {
  const blocos = [];
  for (let r = 1; r <= D.replicas; r++) for (const mo of MODELOS) {
    const vs = NIVEIS.map((n) => valor(r, mo, n, medida));
    if (vs.every((v) => v !== null)) blocos.push(vs.map((v) => (melhor === "menos" ? -v : v)));
  }
  const k = NIVEIS.length, nb = blocos.length;
  if (nb < 2) return null;
  let L = 0;
  for (const b of blocos) {
    const ordem = b.map((v, i) => ({ v, i })).sort((x, y) => x.v - y.v);
    const rank = new Array(k);
    for (let i = 0; i < k;) { let j = i; while (j + 1 < k && ordem[j + 1].v === ordem[i].v) j++; for (let t = i; t <= j; t++) rank[ordem[t].i] = (i + j) / 2 + 1; i = j + 1; }
    rank.forEach((rk, j) => { L += (j + 1) * rk; });
  }
  const mu = nb * k * (k + 1) ** 2 / 4, va = nb * k ** 2 * (k + 1) * (k ** 2 - 1) / 144;
  const z = (L - mu) / Math.sqrt(va);
  const phi = (x) => 0.5 * (1 + Math.sign(x) * Math.sqrt(1 - Math.exp(-2 * x * x / Math.PI)));
  return { nb, L, z, p: 1 - phi(z) };
}
out.push("\n## Tendencia de N0 a N3 (teste de Page, so informacao)\n");
out.push("| medida | quartetos | z | p (unilateral) |"); out.push("|---|---|---|---|");
for (const [medida, melhor] of [["p4_acerto", "mais"], ["pontos", "mais"], ["cognitiva", "menos"], ["tokens", "mais"]]) {
  const p = page(medida, melhor);
  out.push(p ? `| ${medida} (${melhor === "menos" ? "cair" : "subir"}) | ${p.nb} | ${p.z.toFixed(2)} | ${p.p.toFixed(3)} |` : `| ${medida} | - | - | - |`);
}

out.push("\n## Resumo\n");
out.push("| hipotese | veredito |"); out.push("|---|---|");
for (const r of resumo) out.push(`| ${r.principal ? "★ " : ""}${r.nome} | ${r.veredito} |`);
console.log(out.join("\n"));
if (opt("json")) writeFileSync(resolve(RAIZ, opt("json")), JSON.stringify({ lote: D.prefixo, faltas, reprovadas: [...reprovadas], resumo }, null, 2));
