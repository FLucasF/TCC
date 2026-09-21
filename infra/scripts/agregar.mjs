// Junta os meta.json num CSV, uma linha por execução.
//
// Uso:
//   node infra/scripts/agregar.mjs                     # todas as runs
//   node infra/scripts/agregar.mjs --prefixo LOTE      # só as do lote
//   node infra/scripts/agregar.mjs --saida analise/resultados.csv
//
// Existe porque toda tabela desta bancada, até 20/09/2026, saía de um `node -e`
// improvisado. Para 24 execuções dá; para o lote com a avaliação junto, não.
//
// Só junta o que o meta.json tem: parâmetros, custo, tempo, fundação e
// auditoria. As notas da rubrica e do teste de extensão vêm das planilhas de
// `avaliacao/`, e são cruzadas na análise, não aqui.

import { readdirSync, existsSync, readFileSync, writeFileSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";

const args = process.argv.slice(2);
const opt = (nome, padrao) => {
  const i = args.indexOf(`--${nome}`);
  return i >= 0 ? args[i + 1] : padrao;
};
const prefixo = opt("prefixo", null);
const saida = opt("saida", join("analise", "resultados.csv"));

// A ordem aqui é a ordem das colunas. Agrupada por assunto, não alfabética:
// quem abre o CSV numa planilha quer identidade, depois desfecho, depois custo.
const COLUNAS = [
  ["run_id", (m) => m.run_id],
  ["repeticao", (m) => m.repeticao],
  ["modelo", (m) => m.modelo_solicitado],
  ["condicao", (m) => m.condicao],
  ["valida", (m) => m.valida],
  ["valida_proposta", (m) => m.valida_proposta],
  ["motivo_proposta", (m) => m.motivo_proposta],

  ["encerramento", (m) => m.resultado_execucao?.encerramento],
  ["build_ok", (m) => m.resultado_execucao?.build_pos_execucao_ok],
  ["turnos", (m) => m.resultado_execucao?.turnos],
  ["chamadas_ferramenta", (m) => m.resultado_execucao?.chamadas_ferramenta],

  // H2. `entrada_total` e NAO `entrada`: quase tudo entra por cache, e o campo
  // `entrada` sozinho fica entre 38 e 345, que e numero sem significado.
  ["entrada_total", (m) => m.tokens?.entrada_total],
  ["entrada", (m) => m.tokens?.entrada],
  ["cache_leitura", (m) => m.tokens?.cache_leitura],
  ["cache_escrita", (m) => m.tokens?.cache_escrita],
  ["saida", (m) => m.tokens?.saida],
  ["raciocinio", (m) => m.tokens?.raciocinio],
  ["custo_estimado_usd", (m) => m.tokens?.custo_estimado_usd],

  // `duracao_api_ms` e a medida limpa: `duracao_s` e relogio de parede e as
  // execucoes rodam em paralelo, disputando CPU.
  ["duracao_api_ms", (m) => m.tempo?.duracao_api_ms],
  ["duracao_cli_ms", (m) => m.tempo?.duracao_cli_ms],
  ["duracao_s", (m) => m.tempo?.duracao_s],

  // P7: pedido, nao imposto. A desobediencia e dado, nao motivo de exclusao.
  ["obedeceu_versoes", (m) => m.fundacao?.obedeceu_versoes],
  ["spring_boot", (m) => m.fundacao?.spring_boot],
  ["java", (m) => m.fundacao?.java],
  ["projeto_em", (m) => m.fundacao?.projeto_em],
  ["na_raiz", (m) => m.fundacao?.na_raiz],
  ["pacote_raiz", (m) => m.fundacao?.pacote_raiz],
  ["starters", (m) => (m.fundacao?.starters ?? []).join(" ")],
  ["deps_acrescentadas", (m) => (m.dependencias?.acrescentadas ?? []).length],

  ["chamadas_web", (m) => m.auditoria?.chamadas_web],
  ["acesso_web_suspeito", (m) => m.auditoria?.acesso_web_suspeito],
  ["comandos_bash", (m) => m.auditoria?.comandos_bash],
  ["bloqueadas_disponiveis", (m) => (m.auditoria?.ferramentas_bloqueadas_disponiveis ?? []).length],

  ["effort", (m) => m.parametros?.effort],
  ["permission_mode", (m) => m.parametros?.permission_mode_init],
  ["claude_code", (m) => m.ambiente?.claude_code_versao],
  ["imagem", (m) => m.ambiente?.imagem],
  ["imagem_id", (m) => (m.ambiente?.imagem_id ?? "").slice(0, 19)],
  ["hash_prompt", (m) => (m.ambiente?.hash_prompt ?? "").slice(0, 12)],
  ["hash_harness", (m) => (m.ambiente?.hash_harness ?? "").slice(0, 12)],
  ["maquina", (m) => m.ambiente?.maquina],
  ["rede", (m) => m.ambiente?.rede],
  ["modelo_init", (m) => m.modelo_init],
  ["modelos_nas_mensagens", (m) => (m.modelos_observados?.mensagens ?? []).join(" ")],
];

// CSV de verdade: campo com virgula, aspas ou quebra de linha vai entre aspas.
// `null` e `undefined` viram campo vazio, nao a string "null" -- senao a
// planilha le como texto e a coluna inteira vira texto.
const celula = (v) => {
  if (v === null || v === undefined) return "";
  const s = String(v);
  return /[",\n;]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
};

// Campos acrescentados ao extrator em 20/09/2026 nao existem nos meta.json
// gravados antes disso. Sao derivaveis do que ja esta la, entao o agregador
// deriva em vez de exigir reprocessamento.
//
// `obedeceu_versoes` tem uma sutileza: nas execucoes COM esqueleto as versoes
// vinham do pom de partida, nao de obediencia ao enunciado. Derivar ali daria
// "true" para algo que o modelo nao escolheu. Essas ficam vazias de proposito.
function completar(m) {
  if (m.fundacao && m.fundacao.obedeceu_versoes === undefined) {
    const comEsqueleto = m.parametros?.esqueleto === "sim";
    m.fundacao.obedeceu_versoes =
      comEsqueleto || (m.fundacao.spring_boot == null && m.fundacao.java == null)
        ? null
        : m.fundacao.spring_boot === "4.1.1" && m.fundacao.java === "21";
  }
  if (m.auditoria && m.auditoria.chamadas_web === undefined) {
    const f = m.resultado_execucao?.chamadas_por_ferramenta ?? {};
    m.auditoria.chamadas_web = (f.WebSearch ?? 0) + (f.WebFetch ?? 0);
  }
  return m;
}

const linhas = [];
for (const run of readdirSync("runs").sort()) {
  if (prefixo && !run.startsWith(prefixo)) continue;
  const caminho = join("runs", run, "meta.json");
  if (!existsSync(caminho)) continue;
  const m = completar(JSON.parse(readFileSync(caminho, "utf8")));
  linhas.push(COLUNAS.map(([, f]) => celula(f(m))).join(","));
}

if (!linhas.length) {
  console.error(prefixo ? `nenhuma execucao com prefixo ${prefixo}` : "nenhuma execucao encontrada");
  process.exit(1);
}

mkdirSync(dirname(saida), { recursive: true });
writeFileSync(saida, COLUNAS.map(([n]) => n).join(",") + "\n" + linhas.join("\n") + "\n");

console.log(`${linhas.length} execucoes -> ${saida}`);
console.log(`${COLUNAS.length} colunas`);

// Aviso, nao erro: run sem `valida` preenchida ainda nao passou pelo julgamento
// humano da §13.3, e entrar na analise assim seria contar o que nao foi olhado.
const semValida = linhas.filter((l) => l.split(",")[4] === "").length;
if (semValida) {
  console.log(`\n${semValida} execucao(oes) com \`valida\` vazia.`);
  console.log("Preencher antes de analisar: a proposta do extrator esta em `valida_proposta`,");
  console.log("e divergir dela exige motivo escrito (§13.3).");
}
