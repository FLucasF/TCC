// Lê a saída stream-json de uma run e escreve runs/<id>/meta.json.
// Uso: node extrair-meta.mjs <run_dir> --chave valor ...
import { readFileSync, writeFileSync, existsSync, readdirSync } from "node:fs";
import { join } from "node:path";
import { createHash } from "node:crypto";

const [runDir, ...resto] = process.argv.slice(2);
const arg = {};
for (let i = 0; i < resto.length; i += 2) arg[resto[i].replace(/^--/, "")] = resto[i + 1];

const eventos = [];
const linhasInvalidas = [];
const saida = join(runDir, "claude-output.jsonl");
if (existsSync(saida)) {
  readFileSync(saida, "utf8").split(/\r?\n/).forEach((linha, n) => {
    if (!linha.trim()) return;
    try { eventos.push(JSON.parse(linha)); } catch { linhasInvalidas.push(n + 1); }
  });
}

const init = eventos.find((e) => e.type === "system" && e.subtype === "init") ?? null;
const result = [...eventos].reverse().find((e) => e.type === "result") ?? null;

// Chamadas de ferramenta e comandos de terminal, a partir das mensagens do assistente.
const ferramentas = {};
const comandosBash = [];
const modelosNasMensagens = new Set();
for (const e of eventos) {
  if (e.type !== "assistant" || !e.message) continue;
  if (e.message.model) modelosNasMensagens.add(e.message.model);
  for (const bloco of e.message.content ?? []) {
    if (bloco.type !== "tool_use") continue;
    ferramentas[bloco.name] = (ferramentas[bloco.name] ?? 0) + 1;
    if (bloco.name === "Bash" && bloco.input?.command) comandosBash.push(bloco.input.command);
  }
}

// Acesso à rede pelo Bash, contornando o bloqueio de WebSearch/WebFetch.
//
// O agente sobe a própria aplicação e chama o endereço dela para conferir os
// resultados contra os exemplos do enunciado — na FUMACA-01 foram seis `curl`
// para `localhost:8080`. Isso é o agente se verificando, não acesso externo, e
// marcava toda run. Só conta o que sai da máquina.
const ENDERECO_LOCAL = /^(localhost|127(\.\d+){3}|\[::1\]|0\.0\.0\.0|host\.docker\.internal)(:\d+)?$/i;
const externo = (c) => {
  const urls = c.match(/\bhttps?:\/\/[^\s"'`)]+/gi) ?? [];
  const alvos = urls.filter((u) => {
    try { return !ENDERECO_LOCAL.test(new URL(u).host); } catch { return true; }
  });
  if (alvos.length) return true;
  // `curl`/`wget` sem URL http explícita: só é suspeito se não for endereço local.
  return /\b(curl|wget)\b/i.test(c) && !urls.length && !/\b(localhost|127\.0\.0\.1|::1)\b/i.test(c);
};
const suspeitos = comandosBash.filter(externo);
const num = (v) => (typeof v === "number" ? v : 0);

// Run interrompida não tem evento `result`, que é onde vêm os tokens. Reconstrói
// o que der a partir das mensagens do assistente.
//
// Cada mensagem aparece várias vezes no stream (parciais do streaming), sempre
// com o mesmo `message.id`: a última ocorrência de cada id traz o usage fechado.
// Conferido na FUMACA-01 contra o `result`: entrada, cache de leitura e cache de
// escrita batem exatamente. `output_tokens` NÃO bate — as parciais trazem a
// contagem do instante em que foram emitidas (129 somados contra 26.150 reais),
// e por isso a saída fica nula em vez de receber um número errado.
function reconstruirUsage() {
  const ultimaPorId = new Map();
  for (const e of eventos) if (e.type === "assistant" && e.message?.id) ultimaPorId.set(e.message.id, e);
  const soma = { input_tokens: 0, cache_creation_input_tokens: 0, cache_read_input_tokens: 0 };
  for (const e of ultimaPorId.values())
    for (const k of Object.keys(soma)) soma[k] += num(e.message.usage?.[k]);
  return { ...soma, output_tokens: null, mensagens: ultimaPorId.size };
}

const usage = result?.usage ?? reconstruirUsage();
const fonteTokens = result ? "result" : "reconstruido";
const entrada = num(usage.input_tokens), cacheEscrita = num(usage.cache_creation_input_tokens),
  cacheLeitura = num(usage.cache_read_input_tokens);

// Dependências: o Maven roda online, então o agente PODE acrescentar biblioteca
// se julgar necessário. Não é impedido, é registrado — comparando o pom que
// entrou com o que sobrou no fim.
function dependenciasDoPom(texto) {
  const bloco = texto.match(/<dependencies>([\s\S]*?)<\/dependencies>/);
  if (!bloco) return [];
  return [...bloco[1].matchAll(/<groupId>\s*([^<]+?)\s*<\/groupId>\s*<artifactId>\s*([^<]+?)\s*<\/artifactId>/g)]
    .map((m) => `${m[1]}:${m[2]}`)
    .sort();
}

// O projeto não está necessariamente na raiz: sem esqueleto o agente escolhe
// onde põe. Usa o pom mais raso, ignorando `target/`.
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

const raizWs = join(runDir, "workspace");
const arquivos = existsSync(raizWs) ? varrer(raizWs) : [];
const rel = (p) => p.slice(raizWs.length + 1).split("\\").join("/");
const caminhoPom = arquivos.filter((a) => a.nome === "pom.xml").sort((a, b) => a.prof - b.prof)[0]?.caminho ?? null;
const gradle = arquivos.find((a) => a.nome === "build.gradle" || a.nome === "build.gradle.kts");

// Sem esqueleto não existe pom de partida, então `hash_antes` e `alterado`
// ficam nulos e `acrescentadas` passa a ser a lista inteira do que o agente
// declarou. Os campos foram mantidos para as 24 execuções de medição, que
// rodaram com esqueleto, continuarem com a mesma forma de arquivo.
const pom = { alterado: null, hash_antes: null, hash_depois: null, acrescentadas: [], removidas: [] };
const fundacao = {
  ferramenta: caminhoPom ? "maven" : gradle ? "gradle" : null,
  projeto_em: caminhoPom ? rel(caminhoPom) : gradle ? rel(gradle.caminho) : null,
  na_raiz: caminhoPom ? rel(caminhoPom) === "pom.xml" : null,
  spring_boot: null, java: null, pacote_raiz: null, starters: [],
};

if (caminhoPom) {
  const texto = readFileSync(caminhoPom, "utf8");
  pom.hash_depois = createHash("sha256").update(texto).digest("hex");
  pom.alterado = pom.hash_antes != null && pom.hash_antes !== pom.hash_depois;
  const antes = new Set(JSON.parse(arg.deps_antes ?? "[]"));
  const depois = new Set(dependenciasDoPom(texto));
  pom.acrescentadas = [...depois].filter((d) => !antes.has(d));
  pom.removidas = [...antes].filter((d) => !depois.has(d));

  fundacao.spring_boot = texto.match(/<parent>[\s\S]*?<version>\s*([^<]+?)\s*<\/version>[\s\S]*?<\/parent>/)?.[1] ?? null;
  fundacao.java = texto.match(/<java\.version>\s*([^<]+?)\s*<\/java\.version>/)?.[1]
    ?? texto.match(/<maven\.compiler\.release>\s*([^<]+?)\s*<\/maven\.compiler\.release>/)?.[1]
    ?? texto.match(/<maven\.compiler\.source>\s*([^<]+?)\s*<\/maven\.compiler\.source>/)?.[1] ?? null;
  fundacao.starters = dependenciasDoPom(texto).map((d) => d.split(":")[1]);
}

// Pacote raiz: maior prefixo comum dos .java de produção.
// Cuidado com o projeto na raiz: aí o caminho relativo começa em `src/`, sem
// barra na frente, e um separador que exija a barra inicial nunca casa.
const pacotes = arquivos
  .filter((a) => a.nome.endsWith(".java"))
  .map((a) => rel(a.caminho).split(/(?:^|\/)src\/main\/java\//)[1]?.split("/").slice(0, -1) ?? [])
  .filter((p) => p.length);
if (pacotes.length) {
  const comum = pacotes.reduce((a, b) => a.filter((seg, i) => b[i] === seg));
  fundacao.pacote_raiz = comum.join(".") || null;
}

const stderr = existsSync(join(runDir, "stderr.txt")) ? readFileSync(join(runDir, "stderr.txt"), "utf8") : "";
const pre = stderr.split(/\r?\n/).filter((l) => l.startsWith("[pre]"));

const codigoSaida = Number(arg.codigo_saida);
let encerramento = "concluido";
if ([130, 137, 143].includes(codigoSaida)) encerramento = "interrompido";
else if (!result) encerramento = "erro_sem_resultado";
else if (result.is_error) encerramento = result.subtype === "error_max_turns" ? "limite_turnos" : "erro";

const meta = {
  run_id: arg.run_id,
  valida: null, // decidido por humano, conforme as regras de exceção do plano
  motivo_invalidade: null,
  modelo_solicitado: arg.modelo,
  modelo_init: init?.model ?? null,
  modelos_observados: {
    mensagens: [...modelosNasMensagens],
    uso_por_modelo: result?.modelUsage ? Object.keys(result.modelUsage) : [],
  },
  condicao: arg.condicao,
  ambiente: {
    imagem: arg.imagem,
    imagem_id: arg.imagem_id,
    claude_code_versao: init?.claude_code_version ?? null,
    hash_prompt: arg.hash_prompt,
    hash_harness: arg.hash_harness || null,
    verificacoes_pre_execucao: pre,
  },
  parametros: {
    effort: arg.effort ?? "medium",
    ferramentas_bloqueadas: arg.ferramentas_bloqueadas.split(","),
    permission_mode_init: init?.permissionMode ?? null,
  },
  tempo: {
    inicio: arg.inicio,
    fim: arg.fim,
    duracao_s: Number(arg.duracao_s),
    duracao_cli_ms: result?.duration_ms ?? null,
    duracao_api_ms: result?.duration_api_ms ?? null,
  },
  resultado_execucao: {
    codigo_saida: codigoSaida,
    encerramento,
    subtype_resultado: result?.subtype ?? null,
    turnos: result?.num_turns ?? null,
    chamadas_ferramenta: Object.values(ferramentas).reduce((a, b) => a + b, 0),
    chamadas_por_ferramenta: ferramentas,
    resposta_final: typeof result?.result === "string" ? result.result.slice(0, 500) : null,
    build_pos_execucao_ok: Number(arg.build_codigo) === 0,
  },
  dependencias: pom,
  fundacao,
  tokens: {
    fonte: fonteTokens,
    entrada,
    saida: typeof usage.output_tokens === "number" ? usage.output_tokens : null,
    raciocinio: result?.usage?.output_tokens_details?.thinking_tokens ?? null,
    cache_leitura: cacheLeitura,
    cache_escrita: cacheEscrita,
    entrada_total: entrada + cacheEscrita + cacheLeitura,
    custo_estimado_usd: result?.total_cost_usd ?? null,
    uso_por_modelo: result?.modelUsage ?? null,
  },
  isolamento_init: init
    ? {
        cwd: init.cwd ?? null,
        ferramentas_disponiveis: init.tools ?? null,
        mcp_servers: init.mcp_servers ?? null,
        slash_commands: init.slash_commands ?? null,
        agents: init.agents ?? null,
        skills: init.skills ?? null,
        plugins: init.plugins ?? null,
      }
    : null,
  auditoria: {
    comandos_bash: comandosBash.length,
    acesso_web_suspeito: suspeitos.length > 0,
    comandos_suspeitos: suspeitos,
    ferramentas_bloqueadas_disponiveis: (init?.tools ?? []).filter((t) =>
      arg.ferramentas_bloqueadas.split(",").includes(t)),
    linhas_jsonl_invalidas: linhasInvalidas,
  },
};

writeFileSync(join(runDir, "meta.json"), JSON.stringify(meta, null, 2) + "\n");
console.log(`meta.json: ${encerramento}, turnos=${meta.resultado_execucao.turnos}, ` +
  `ferramentas=${meta.resultado_execucao.chamadas_ferramenta}, entrada_total=${meta.tokens.entrada_total}, ` +
  `saida=${meta.tokens.saida}, build_ok=${meta.resultado_execucao.build_pos_execucao_ok}`);
