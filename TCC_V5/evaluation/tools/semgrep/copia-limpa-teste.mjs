// Prova o copia-limpa.mjs nos formatos de pasta que um agente pode deixar: para cada
// pacote sintetico, o conjunto EXATO de arquivos que o Semgrep le, e os avisos. Nasceu
// do ensaio de 10/10, em que 4 pacotes com o projeto numa subpasta ficaram sem nenhum
// arquivo lido e o Semgrep respondeu "indeterminado" em tudo.
//
// Uso:  node evaluation/tools/semgrep/copia-limpa-teste.mjs
//
// COMO LER. Cada caso monta um pacote numa pasta temporaria (arquivo -> conteudo), roda
// o copia-limpa de verdade e compara: os arquivos copiados (caminho relativo ao pacote)
// e as linhas de AVISO do pacote. Termina com "N de N casos como a regra manda".

import { mkdtempSync, mkdirSync, writeFileSync, readdirSync, rmSync } from "node:fs";
import { join, dirname, relative } from "node:path";
import { tmpdir } from "node:os";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";

const AQUI = dirname(fileURLToPath(import.meta.url));
const POM = "<project><modelVersion>4.0.0</modelVersion></project>";
const PAI = (...m) => `<project><packaging>pom</packaging><modules>${m.map((x) => `<module>${x}</module>`).join("")}</modules></project>`;
const J = "class X {}\n";

const CASOS = [
  { nome: "raiz", arq: { "pom.xml": POM, "src/main/java/A.java": J, "src/test/java/T.java": J, "target/classes/Z.java": J },
    lidos: ["src/main/java/A.java"], avisos: [] },
  { nome: "subpasta", arq: { "checkout/pom.xml": POM, "checkout/src/main/java/B.java": J, "README.md": "x" },
    lidos: ["checkout/src/main/java/B.java"], avisos: [] },
  { nome: "raiz-com-rascunho", arq: { "pom.xml": POM, "src/main/java/C.java": J, "rascunho/pom.xml": POM, "rascunho/src/main/java/R.java": J },
    lidos: ["src/main/java/C.java"], avisos: [] },
  { nome: "multimodulo", arq: {
      "pom.xml": PAI("app", "dominio").replace("</modules>", "<!-- <module>velho</module> --></modules>"),
      "app/pom.xml": POM, "app/src/main/java/App.java": J,
      "dominio/pom.xml": POM, "dominio/src/main/java/D.java": J,
      "velho/pom.xml": POM, "velho/src/main/java/V.java": J },
    lidos: ["app/src/main/java/App.java", "dominio/src/main/java/D.java"], avisos: ["multimodulo"] },
  { nome: "sem-pom", arq: { "src/main/java/E.java": J },
    lidos: ["src/main/java/E.java"], avisos: ["sem pom.xml"] },
  { nome: "dois-poms", arq: { "a/pom.xml": POM, "a/src/main/java/A1.java": J, "b/pom.xml": POM, "b/src/main/java/B1.java": J },
    lidos: ["a/src/main/java/A1.java"], avisos: ["dois pom.xml"] },
  { nome: "sem-codigo", arq: { "README.md": "so texto" },
    lidos: [], avisos: ["nenhum src/main"] },
  { nome: "git-e-target", arq: { "pom.xml": POM, "src/main/java/G.java": J, ".git/x/src/main/java/Lixo.java": J, "target/src/main/java/Gerado.java": J },
    lidos: ["src/main/java/G.java"], avisos: [] },
  { nome: "recursos", arq: { "pom.xml": POM, "src/main/resources/application.properties": "a=1\n",
      "src/main/resources/application.yml": "a: 1\n", "src/main/resources/notas.md": "x", "src/main/java/H.java": J },
    lidos: ["src/main/java/H.java", "src/main/resources/application.properties", "src/main/resources/application.yml"], avisos: [] },
];

const base = mkdtempSync(join(tmpdir(), "copia-limpa-teste-"));
const raiz = join(base, "pacotes"), destino = join(base, "limpa");
for (const c of CASOS)
  for (const [arq, txt] of Object.entries(c.arq)) {
    mkdirSync(dirname(join(raiz, c.nome, arq)), { recursive: true });
    writeFileSync(join(raiz, c.nome, arq), txt);
  }
const r = spawnSync(process.execPath, [join(AQUI, "copia-limpa.mjs"), raiz, destino], { encoding: "utf8" });
if (r.status !== 0) { console.error(r.stderr); process.exit(1); }

const lista = (dir, out = []) => {
  for (const e of readdirSync(dir, { withFileTypes: true }))
    e.isDirectory() ? lista(join(dir, e.name), out) : out.push(join(dir, e.name));
  return out;
};
let ok = 0;
for (const c of CASOS) {
  const lidos = lista(join(destino, c.nome)).map((f) => relative(join(destino, c.nome), f).replace(/\\/g, "/")).sort();
  const avisos = r.stderr.split(/\r?\n/).filter((l) => l.startsWith(`AVISO ${c.nome}:`));
  const erros = [];
  if (JSON.stringify(lidos) !== JSON.stringify([...c.lidos].sort())) erros.push(`leu [${lidos.join(", ")}], devia ler [${c.lidos.join(", ")}]`);
  for (const a of c.avisos) if (!avisos.some((l) => l.includes(a))) erros.push(`faltou o aviso "${a}"`);
  if (!c.avisos.length && avisos.length) erros.push(`aviso inesperado: ${avisos.join(" | ")}`);
  if (erros.length) console.log(`FALHOU ${c.nome}: ${erros.join("; ")}`);
  else ok++;
}
rmSync(base, { recursive: true, force: true });
console.log(`${ok} de ${CASOS.length} casos como a regra manda.`);
process.exit(ok === CASOS.length ? 0 : 1);
