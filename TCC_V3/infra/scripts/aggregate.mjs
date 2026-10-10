// Junta os meta.json num CSV, uma linha por execucao.
//
// Uso:
//   node infra/scripts/aggregate.mjs                     # todas as runs
//   node infra/scripts/aggregate.mjs --prefix BATCH      # so o lote
//   node infra/scripts/aggregate.mjs --out analysis/resultados.csv
//
// So junta o que o meta.json tem. A avaliacao dos pacotes e outra coisa (a suite,
// o Semgrep, as metricas e a leitura do Lucas), e e cruzada com isto depois — nao aqui.
//
// COMO LER. A lista COLUNAS, logo abaixo, e o coracao do script: cada linha e uma
// coluna do CSV, com o nome e a funcao que tira o valor do meta.json (ex.: "turns" vem
// de meta.outcome.turns). O resto so percorre runs/, filtra pelo prefixo e escreve.
// No V4: node infra/scripts/aggregate.mjs --prefix V4-STRATEGY-, e o verify.mjs confere
// que o CSV e igual ao que este script gera dos meta.json.

import { readdirSync, existsSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";

const args = process.argv.slice(2);
const opt = (nome, padrao) => {
  const i = args.indexOf(`--${nome}`);
  return i >= 0 ? args[i + 1] : padrao;
};
const prefix = opt("prefix", null);
const out = opt("out", join("analysis", "resultados.csv"));

// A ordem aqui e a ordem das colunas. Agrupada por assunto, nao alfabetica:
// quem abre o CSV numa planilha quer identidade, depois desfecho, depois custo.
const COLUNAS = [
  ["run_id", (m) => m.run_id],
  ["replicate", (m) => m.replicate],
  ["model", (m) => m.model_requested],
  ["condition", (m) => m.condition],
  ["valid", (m) => m.valid],

  ["termination", (m) => m.outcome?.termination],
  ["build_ok", (m) => m.outcome?.build_ok],
  ["turns", (m) => m.outcome?.turns],
  ["tool_calls", (m) => m.outcome?.tool_calls],

  // H2. `input_total` e NAO `input`: quase tudo entra por cache, e o campo
  // `input` sozinho fica na casa das centenas — numero sem significado.
  ["input_total", (m) => m.tokens?.input_total],
  ["input", (m) => m.tokens?.input],
  ["cache_read", (m) => m.tokens?.cache_read],
  ["cache_write", (m) => m.tokens?.cache_write],
  ["output", (m) => m.tokens?.output],
  ["thinking", (m) => m.tokens?.thinking],
  ["cost_estimated_usd", (m) => m.tokens?.cost_estimated_usd],

  // `duration_api_ms` e a medida limpa: `duration_s` e relogio de parede e as
  // seis execucoes rodam em paralelo, disputando CPU.
  ["duration_api_ms", (m) => m.timing?.duration_api_ms],
  ["duration_cli_ms", (m) => m.timing?.duration_cli_ms],
  ["duration_s", (m) => m.timing?.duration_s],

  // Pedido, nao imposto. A desobediencia e dado, nao motivo de exclusao.
  ["versions_obeyed", (m) => m.foundation?.versions_obeyed],
  ["spring_boot", (m) => m.foundation?.spring_boot],
  ["java", (m) => m.foundation?.java],
  ["project_at", (m) => m.foundation?.project_at],
  ["root_package", (m) => m.foundation?.root_package],
  ["dependencies_added", (m) => (m.foundation?.dependencies?.added ?? []).length],

  // Controle do isolamento e da comparabilidade entre bracos.
  ["models_observed", (m) => (m.models_observed?.messages ?? []).join(" ")],
  ["tools_available_n", (m) => (m.isolation_init?.tools_available ?? []).length],
  // `spawned`, e nao a existencia do objeto: `subagent_stats` sempre vem, com
  // a estrutura zerada, mesmo quando nada foi delegado.
  ["subagents_spawned", (m) => m.outcome?.subagents_spawned ?? null],
  // Quase sempre 1. Mais de um quer dizer que a sessao foi segmentada por
  // tarefa em segundo plano — os numeros de consumo ja vem somados.
  ["result_segments", (m) => m.outcome?.result_segments ?? null],
  ["permission_denials_n", (m) => (m.outcome?.permission_denials ?? []).length],
  ["api_key_source", (m) => m.environment?.api_key_source],
  ["effort", (m) => m.parameters?.effort],
  ["claude_code_version", (m) => m.environment?.claude_code_version],
  ["image_id", (m) => m.environment?.image_id],
  ["prompt_hash", (m) => (m.environment?.prompt_hash ?? "").slice(0, 16)],
  ["harness_hash", (m) => (m.environment?.harness_hash ?? "").slice(0, 16)],
  ["tokens_source", (m) => m.tokens?.source],
];

// Campo com virgula, aspas ou quebra de linha precisa de aspas duplas, e aspas
// internas dobram. Sem isso o CSV quebra silenciosamente.
//
// E o reverso tambem vale: NAO analise este CSV depois com split(","). Numa
// versao anterior isso produziu numero errado num relatorio.
const csv = (v) => {
  if (v === null || v === undefined) return "";
  const s = String(v);
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

const runsDir = "runs";
if (!existsSync(runsDir)) {
  console.error("pasta runs/ nao encontrada. Rode a partir da raiz do repositorio.");
  process.exitCode = 2;
} else {
  const linhas = [];
  let ignoradas = 0;
  for (const r of readdirSync(runsDir).sort()) {
    if (r === "logs") continue;
    if (prefix && !r.startsWith(prefix)) { ignoradas++; continue; }
    const p = join(runsDir, r, "meta.json");
    if (!existsSync(p)) { console.log(`  sem meta.json: ${r}`); continue; }
    const m = JSON.parse(readFileSync(p, "utf8"));
    linhas.push(COLUNAS.map(([, f]) => csv(f(m))).join(","));
  }

  mkdirSync(dirname(out), { recursive: true });
  writeFileSync(out, COLUNAS.map(([n]) => n).join(",") + "\n" + linhas.join("\n") + "\n");

  console.log(`${linhas.length} execucoes -> ${out}`);
  console.log(`${COLUNAS.length} colunas`);
  if (ignoradas) console.log(`${ignoradas} fora do prefixo "${prefix}", ignoradas`);

  // Aviso, nao erro: run sem `valid` preenchida ainda nao passou pelo julgamento
  // humano, e entrar na analise assim seria contar o que nao foi olhado.
  const semValid = linhas.filter((l) => l.split(",")[4] === "").length;
  if (semValid) {
    console.log(`\n${semValid} execucao(oes) com \`valid\` vazia.`);
    console.log("Preencher olhando o meta.json. Nenhum script opina sobre isso.");
  }
}
