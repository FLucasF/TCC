// Le a saida stream-json de uma run e escreve runs/<id>/meta.json.
//
// Uso: node extract-meta.mjs <run_dir> --chave valor ...
//
// Este script NAO julga nada. Ele transcreve o que aconteceu. O campo `valid`
// sai sempre `null`: quem decide se uma execucao vale e humano.
//
// COMO LER. Quem chama e o run-one.sh, no fim de cada execucao. Quatro partes:
//   1. "transcricao": le o claude-output.jsonl (uma linha JSON por evento do Claude
//      Code) e separa o "init" (como a sessao comecou: modelo, ferramentas, skills) e
//      os "result" (como terminou: tokens, custo, turnos);
//   2. "o projeto": olha o workspace e acha o pom.xml, as versoes pedidas (Java 21,
//      Spring Boot 4.1.1), as dependencias e o pacote raiz;
//   3. "o encerramento": decide o termino (completed, error, interrupted...) pelo
//      codigo de saida e pelo result;
//   4. "meta.json": monta o arquivo. Cada campo tem um comentario dizendo de onde vem.
// O meta.json e a fonte de tudo que o aggregate.mjs e o verify.mjs leem depois.

import { readFileSync, writeFileSync, existsSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { createHash } from "node:crypto";
import { hostname } from "node:os";

const [runDir, ...resto] = process.argv.slice(2);
const arg = {};
for (let i = 0; i < resto.length; i += 2) arg[resto[i].replace(/^--/, "")] = resto[i + 1];

// ----------------------------------------------------------------- transcricao
const eventos = [];
const linhasInvalidas = [];
const saida = join(runDir, "claude-output.jsonl");
if (existsSync(saida)) {
  readFileSync(saida, "utf8").split(/\r?\n/).forEach((linha, n) => {
    if (!linha.trim()) return;
    try { eventos.push(JSON.parse(linha)); } catch { linhasInvalidas.push(n + 1); }
  });
}

// A transcricao pode ter MAIS DE UM par init/result. Acontece quando o agente
// deixa uma tarefa rodando em segundo plano (`mvn spring-boot:run`, por
// exemplo) e o CLI segmenta a sessao: cada segmento fecha com o seu `result`,
// numerado em `result_index`.
//
// Medido na SMOKE-01-HAIKU-HARNESS: dois segmentos, o primeiro com 52 turnos e
// 2.150.552 de entrada, o segundo com 1 turno e 67.525. Ler apenas o ULTIMO
// reportava 67 mil onde foram 2,2 milhoes — erro de 32x, e a H2 e justamente
// sobre consumo.
//
// O `init` continua sendo o PRIMEIRO: e o estado com que o agente comecou.
const init = eventos.find((e) => e.type === "system" && e.subtype === "init") ?? null;
const resultados = eventos.filter((e) => e.type === "result");
const result = resultados.length ? resultados[resultados.length - 1] : null;

// As semanticas sao MISTURADAS, e isso foi conferido campo a campo:
//
//   POR SEGMENTO, somar: num_turns, duration_ms, usage.* (entrada, saida,
//                        cache, thinking)
//   CUMULATIVOS, pegar o ultimo: duration_api_ms, total_cost_usd, modelUsage
//   ESTADO FINAL, pegar o ultimo: subtype, is_error, stop_reason,
//                        terminal_reason, result
//
// `modelUsage` e cumulativo e isso foi verificado: out=26988 no primeiro
// segmento vira 27161 no segundo, que e 26988 + 173.
const somaSegmentos = (f) => resultados.reduce((s, e) => s + (typeof f(e) === "number" ? f(e) : 0), 0);

// Chamadas de ferramenta e modelos, a partir das mensagens do assistente.
const ferramentas = {};
const modelosNasMensagens = new Set();
for (const e of eventos) {
  if (e.type !== "assistant" || !e.message) continue;
  if (e.message.model) modelosNasMensagens.add(e.message.model);
  for (const bloco of e.message.content ?? []) {
    if (bloco.type !== "tool_use") continue;
    ferramentas[bloco.name] = (ferramentas[bloco.name] ?? 0) + 1;
  }
}

const num = (v) => (typeof v === "number" ? v : 0);

// Run interrompida nao tem evento `result`, que e onde vem os tokens. Reconstroi
// o que der a partir das mensagens do assistente.
//
// Cada mensagem aparece varias vezes no stream (parciais do streaming), sempre
// com o mesmo `message.id`: a ULTIMA ocorrencia de cada id traz o usage fechado.
// Entrada e cache batem exatamente com o `result`; `output_tokens` NAO bate — as
// parciais trazem a contagem do instante em que foram emitidas. Por isso a saida
// fica nula em vez de receber um numero errado.
function reconstruirUsage() {
  const ultimaPorId = new Map();
  for (const e of eventos) if (e.type === "assistant" && e.message?.id) ultimaPorId.set(e.message.id, e);
  const soma = { input_tokens: 0, cache_creation_input_tokens: 0, cache_read_input_tokens: 0 };
  for (const e of ultimaPorId.values())
    for (const k of Object.keys(soma)) soma[k] += num(e.message.usage?.[k]);
  return { ...soma, output_tokens: null };
}

// Soma o `usage` de TODOS os segmentos. Com um segmento so — que e o caso
// normal — isto da exatamente o mesmo que ler o unico result.
function somarUsage() {
  const soma = {
    input_tokens: 0, cache_creation_input_tokens: 0, cache_read_input_tokens: 0,
    output_tokens: 0, thinking_tokens: 0,
  };
  for (const e of resultados) {
    const u = e.usage ?? {};
    soma.input_tokens += num(u.input_tokens);
    soma.cache_creation_input_tokens += num(u.cache_creation_input_tokens);
    soma.cache_read_input_tokens += num(u.cache_read_input_tokens);
    soma.output_tokens += num(u.output_tokens);
    soma.thinking_tokens += num(u.output_tokens_details?.thinking_tokens);
  }
  return soma;
}

const usage = resultados.length ? somarUsage() : reconstruirUsage();
const fonteTokens = resultados.length ? "result" : "reconstructed";
const input = num(usage.input_tokens);
const cacheWrite = num(usage.cache_creation_input_tokens);
const cacheRead = num(usage.cache_read_input_tokens);

// ------------------------------------------------------------------- o projeto
function varrer(dir, fora = new Set(["target", "node_modules", ".git"])) {
  const achados = [];
  const andar = (d, prof) => {
    let itens;
    try { itens = readdirSync(d, { withFileTypes: true }); } catch { return; }
    for (const i of itens) {
      if (i.isDirectory()) { if (!fora.has(i.name)) andar(join(d, i.name), prof + 1); }
      else achados.push({ caminho: join(d, i.name), nome: i.name, prof });
    }
  };
  andar(dir, 0);
  return achados;
}

function dependenciasDoPom(texto) {
  const bloco = texto.match(/<dependencies>([\s\S]*?)<\/dependencies>/);
  if (!bloco) return [];
  return [...bloco[1].matchAll(/<groupId>\s*([^<]+?)\s*<\/groupId>\s*<artifactId>\s*([^<]+?)\s*<\/artifactId>/g)]
    .map((m) => `${m[1]}:${m[2]}`)
    .sort();
}

const raizWs = join(runDir, "workspace");
const arquivos = existsSync(raizWs) ? varrer(raizWs) : [];
const rel = (p) => p.slice(raizWs.length + 1).split("\\").join("/");

// O projeto nao esta necessariamente na raiz: sem esqueleto o agente escolhe
// onde poe. Usa o pom mais raso, ignorando target/.
const caminhoPom = arquivos.filter((a) => a.nome === "pom.xml").sort((a, b) => a.prof - b.prof)[0]?.caminho ?? null;
const gradle = arquivos.find((a) => a.nome === "build.gradle" || a.nome === "build.gradle.kts");

const foundation = {
  build_tool: caminhoPom ? "maven" : gradle ? "gradle" : null,
  project_at: caminhoPom ? rel(caminhoPom) : gradle ? rel(gradle.caminho) : null,
  at_root: caminhoPom ? rel(caminhoPom) === "pom.xml" : null,
  spring_boot: null,
  java: null,
  root_package: null,
  starters: [],
  dependencies: { added: [] },
};

if (caminhoPom) {
  const texto = readFileSync(caminhoPom, "utf8");
  foundation.spring_boot = texto.match(/<parent>[\s\S]*?<version>\s*([^<]+?)\s*<\/version>[\s\S]*?<\/parent>/)?.[1] ?? null;
  foundation.java = texto.match(/<java\.version>\s*([^<]+?)\s*<\/java\.version>/)?.[1]
    ?? texto.match(/<maven\.compiler\.release>\s*([^<]+?)\s*<\/maven\.compiler\.release>/)?.[1]
    ?? texto.match(/<maven\.compiler\.source>\s*([^<]+?)\s*<\/maven\.compiler\.source>/)?.[1] ?? null;
  const deps = dependenciasDoPom(texto);
  foundation.starters = deps.map((d) => d.split(":")[1]);
  // Sem pom de partida, tudo que esta declarado foi o agente que acrescentou.
  foundation.dependencies.added = deps;
}

// Pacote raiz: maior prefixo comum dos .java de producao.
// Cuidado com o projeto na raiz: ali o caminho relativo comeca em `src/`, sem
// barra na frente, e um separador que exija a barra inicial nunca casa.
const pacotes = arquivos
  .filter((a) => a.nome.endsWith(".java"))
  .map((a) => rel(a.caminho).split(/(?:^|\/)src\/main\/java\//)[1]?.split("/").slice(0, -1) ?? [])
  .filter((p) => p.length);
if (pacotes.length) {
  const comum = pacotes.reduce((a, b) => a.filter((seg, i) => b[i] === seg));
  foundation.root_package = comum.join(".") || null;
}

// P7: as versoes sao PEDIDAS no enunciado, nao impostas. Desobedecer NAO
// invalida a run; vira taxa reportada por modelo e condicao.
foundation.versions_obeyed =
  foundation.spring_boot == null && foundation.java == null
    ? null
    : foundation.spring_boot === "4.1.1" && foundation.java === "21";

// --------------------------------------------------------------- o encerramento
const stderr = existsSync(join(runDir, "stderr.txt")) ? readFileSync(join(runDir, "stderr.txt"), "utf8") : "";
const pre = stderr.split(/\r?\n/).filter((l) => l.startsWith("[pre]"));

const exitCode = Number(arg.exit_code);
let termination = "completed";
if ([130, 137, 143].includes(exitCode)) termination = "interrupted";
else if (!result) termination = "no_result";
else if (result.is_error) termination = result.subtype === "error_max_turns" ? "turn_limit" : "error";

// ------------------------------------------------------------------- meta.json
const meta = {
  run_id: arg.run_id,

  // Decisao HUMANA, pela tabela de excecoes do plano. Comeca nula de proposito:
  // preencher exige olhar. Nenhum script opina sobre isto.
  valid: null,
  invalid_reason: null,

  model_requested: arg.model,
  model_init: init?.model ?? null,
  // CONTROLE: todo modelo que apareceu nas mensagens. Se vier mais de um, parte
  // do codigo pode ter sido escrita por outro modelo.
  models_observed: {
    messages: [...modelosNasMensagens],
    usage_by_model: result?.modelUsage ? Object.keys(result.modelUsage) : [],
  },
  condition: arg.condition,
  // `null` na fumaca, que nao tem replica. OBRIGATORIO no lote: com n=3 por
  // celula, sem ele nao da para dizer qual run e qual.
  replicate: arg.replicate ? Number(arg.replicate) : null,

  environment: {
    image: arg.image,
    image_id: arg.image_id,
    claude_code_version: init?.claude_code_version ?? null,
    prompt_hash: arg.prompt_hash,
    harness_hash: arg.harness_hash || null,
    preflight: pre,
    machine: hostname(),
    network: arg.network || null,
    // "none" prova que rodou pela ASSINATURA e nao por chave de API. E a
    // verificacao do .env vista do lado de dentro do container.
    api_key_source: init?.apiKeySource ?? null,
  },

  parameters: {
    // O que foi PEDIDO. A transcricao nao reporta o effort aplicado de volta —
    // nem o init nem o result trazem esse campo —, entao nao ha como confirmar
    // pelo dado o que foi aplicado.
    effort: arg.effort ?? "medium",
    permission_mode_init: init?.permissionMode ?? null,
    fast_mode_state: init?.fast_mode_state ?? null,
  },

  timing: {
    start: arg.start,
    end: arg.end,
    // Relogio de parede: CONTAMINADO pelo paralelismo das seis execucoes.
    duration_s: Number(arg.duration_s),
    // `duration_ms` e POR SEGMENTO: soma.
    duration_cli_ms: resultados.length ? somaSegmentos((e) => e.duration_ms) : null,
    // `duration_api_ms` e CUMULATIVO: pega o ultimo. A medida limpa para a H2.
    duration_api_ms: result?.duration_api_ms ?? null,
  },

  outcome: {
    exit_code: exitCode,
    termination,
    result_subtype: result?.subtype ?? null,
    // Numero de segmentos. Quase sempre 1; ver a nota no topo.
    result_segments: resultados.length,
    // SOMA dos segmentos, nao o ultimo.
    turns: resultados.length ? somaSegmentos((e) => e.num_turns) : null,
    tool_calls: Object.values(ferramentas).reduce((a, b) => a + b, 0),
    tool_calls_by_name: ferramentas,
    final_message: typeof result?.result === "string" ? result.result.slice(0, 500) : null,
    build_ok: Number(arg.build_code) === 0,
    stop_reason: result?.stop_reason ?? null,
    terminal_reason: result?.terminal_reason ?? null,
    // Tem que ficar VAZIO. Se nao ficar, o agente esbarrou em permissao e nao
    // trabalhou livre — a comparacao entre bracos fica suja.
    // O MAIOR entre os segmentos: correto tanto se o campo for cumulativo
    // quanto se for por segmento. Nas sete execucoes conferidas ficou vazio nas
    // duas leituras, entao a duvida nao pode ser fechada pelo dado ainda.
    permission_denials: resultados.length
      ? resultados.map((e) => e.permission_denials ?? []).sort((x, y) => y.length - x.length)[0]
      : null,
    // ATENCAO: este objeto SEMPRE vem, com a estrutura inteira zerada. Nao
    // teste a existencia dele nem conte as chaves — o campo que diz se houve
    // delegacao e `spawned`. Conferido contra 3 transcricoes reais em que
    // Agent/Task estavam bloqueados: todas trazem o objeto com spawned=0.
    //
    // Se `spawned` > 0, o modelo delegou parte do trabalho a outro modelo, e o
    // pacote avaliado nao foi escrito so por ele.
    subagent_stats: result?.subagent_stats ?? null,
    // Idem: o MAIOR `spawned` entre os segmentos. E este o campo que responde
    // "o modelo delegou?", nao a existencia do objeto acima.
    subagents_spawned: resultados.length
      ? Math.max(...resultados.map((e) => num(e.subagent_stats?.spawned)))
      : null,
  },

  tokens: {
    source: fonteTokens,
    input,
    output: typeof usage.output_tokens === "number" ? usage.output_tokens : null,
    cache_read: cacheRead,
    cache_write: cacheWrite,
    // REPORTE ESTE. O campo `input` sozinho fica na casa das centenas em
    // execucoes que consumiram milhoes — quase tudo entra por cache.
    input_total: input + cacheWrite + cacheRead,
    thinking: resultados.length ? num(usage.thinking_tokens) : null,
    cost_estimated_usd: result?.total_cost_usd ?? null,
    usage_by_model: result?.modelUsage ?? null,
  },

  foundation,

  // CONTROLE: o que o agente realmente recebeu. O conjunto de ferramentas NAO e
  // igual entre modelos, e como o modelo e o fator de bloco isso entra na H3.
  isolation_init: init
    ? {
        cwd: init.cwd ?? null,
        tools_available: init.tools ?? null,
        mcp_servers: init.mcp_servers ?? null,
        slash_commands: init.slash_commands ?? null,
        agents: init.agents ?? null,
        skills: init.skills ?? null,
        plugins: init.plugins ?? null,
      }
    : null,

  invalid_jsonl_lines: linhasInvalidas,
};

writeFileSync(join(runDir, "meta.json"), JSON.stringify(meta, null, 2) + "\n");

const spawned = num(meta.outcome.subagent_stats?.spawned);
console.log(
  `meta.json: ${termination}, turns=${meta.outcome.turns}, ` +
  `tools=${meta.outcome.tool_calls}, input_total=${meta.tokens.input_total}, ` +
  `output=${meta.tokens.output}, build_ok=${meta.outcome.build_ok}` +
  (spawned > 0 ? `  ** ${spawned} SUBAGENTE(S) **` : "")
);
