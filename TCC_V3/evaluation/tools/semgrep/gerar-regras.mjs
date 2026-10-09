// Gera regras.yml, as regras do Semgrep que leem o desenho de cada pacote.
//
// Uso:  node evaluation/tools/semgrep/gerar-regras.mjs > evaluation/tools/semgrep/regras.yml
//
// Os pontos positivos (P1 a P4) recebem as mesmas cinco regras, mudando so os
// nomes dos casos; o controle negativo (P5) tem regras proprias. Os nomes vem do
// gabarito do Strategy e sao os mesmos nos enunciados do V3 e do V4.
// Deterministico: o mesmo pacote da sempre o mesmo resultado. Nao usa IA.

import { PONTOS as BASE, REGIOES as LISTA_REGIOES, REGIOES_CLASSE } from "./pontos.mjs";

const PONTOS = BASE.map((p) => ({ ...p, casos: p.casos.join("|") }));
const REGIOES = LISTA_REGIOES.join("|");
// "new BigDecimal(...)" e um numero, nao um objeto de estrategia.
const OBJETO = String.raw`new\s+(?!(?:BigDecimal|BigInteger|Double|Integer|Long|Float|String)\b)[A-Z]\w*\s*\(`;
// Argumentos de construtor, com um nivel de parenteses dentro: SUDESTE(new BigDecimal("0.12"))
const ARGS = String.raw`\((?:[^()]|\([^()]*\))*\)`;
const SO_MAIN = `paths: { include: ["**/src/main/**/*.java"] }`;

const comparacoes = (c) => `
          - pattern: $X == $C
          - pattern: $X != $C
          - pattern: $C.equals($X)
          - pattern: $X.equals($C)
          - pattern: $X.equalsIgnoreCase($C)
          - pattern: $C.equalsIgnoreCase($X)
      - metavariable-regex:
          metavariable: $C
          regex: ^"?([A-Za-z_]\\w*\\.)*(${c})"?$`;

const positivo = ({ id, casos, classes }) => `
  - id: ${id}-cond-calc
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso e faz conta
    ${SO_MAIN}
    patterns:
      - pattern-either:${comparacoes(casos)}
      # validacao: um if cujo corpo recusa o pedido, mesmo um nivel abaixo
      - pattern-not-inside: if (...) throw $E;
      - pattern-not-inside: if (...) { ... throw $E; ... }
  - id: ${id}-cond-validacao
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso so para recusar
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern-inside: if (...) throw $E;
          - pattern-inside: if (...) { ... throw $E; ... }
      - pattern-either:${comparacoes(casos)}
  - id: ${id}-instanceof
    languages: [java]
    severity: INFO
    message: instanceof de uma classe de caso
    ${SO_MAIN}
    patterns:
      - pattern: $X instanceof $K
      - metavariable-regex: { metavariable: $K, regex: "(${classes})" }
  - id: ${id}-switch-label
    languages: [regex]
    severity: INFO
    message: rotulo de switch que nomeia um caso
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+"?(?:\\w+\\.)?(?:${casos})"?\\s*(?:,|->|:)'
  - id: ${id}-switch-fabrica
    languages: [regex]
    severity: INFO
    message: rotulo de switch que so devolve um objeto (fabrica ou registro)
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+"?(?:\\w+\\.)?(?:${casos})"?\\s*(?:->|:)\\s*(?:return\\s+)?(?:${OBJETO}|[A-Z]\\w*\\.(?:${casos})\\b)'
  - id: ${id}-nome
    languages: [regex]
    severity: INFO
    message: o nome de um caso aparece no codigo (alarme - sem nenhum, o agente usou outros nomes)
    ${SO_MAIN}
    pattern-regex: '\\b(?:${casos})\\b|\\b\\w*(?:${classes})\\w*\\b'
`;

const negativo = `
  - id: p5-nome
    languages: [regex]
    severity: INFO
    message: o nome de uma regiao aparece no codigo (alarme)
    ${SO_MAIN}
    pattern-regex: '\\b(?:${REGIOES})\\b|\\b\\w*(?:${REGIOES_CLASSE})\\w*\\b'
  - id: p5-enum-corpo
    languages: [regex]
    severity: INFO
    message: constante de regiao com corpo proprio
    ${SO_MAIN}
    pattern-regex: '(?m)^\\s*(?:${REGIOES})\\s*(?:${ARGS})?\\s*\\{'
  - id: p5-enum-dados
    languages: [regex]
    severity: INFO
    message: constante de regiao que so carrega valores
    ${SO_MAIN}
    pattern-regex: '(?m)^\\s*(?:${REGIOES})\\s*${ARGS}\\s*[,;]'
  - id: p5-classe-regiao
    languages: [regex]
    severity: INFO
    message: classe por regiao
    ${SO_MAIN}
    pattern-regex: '\\bclass\\s+\\w*(?:${REGIOES_CLASSE})\\w*[^{]*\\b(?:implements|extends)\\b'
  - id: p5-mapa
    languages: [regex]
    severity: INFO
    message: mapa de regiao para valor
    ${SO_MAIN}
    pattern-regex: '(?:Map\\.(?:of|entry)\\s*\\(\\s*|put\\s*\\(\\s*)"?(?:\\w+\\.)?(?:${REGIOES})"?\\s*,\\s*(?!${OBJETO})'
  - id: p5-mapa-objeto
    languages: [regex]
    severity: INFO
    message: mapa de regiao para objeto (estrategia)
    ${SO_MAIN}
    pattern-regex: '(?:Map\\.(?:of|entry)\\s*\\(\\s*|put\\s*\\(\\s*)"?(?:\\w+\\.)?(?:${REGIOES})"?\\s*,\\s*${OBJETO}'
  - id: p5-switch-label
    languages: [regex]
    severity: INFO
    message: rotulo de switch ou condicao que nomeia uma regiao
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+"?(?:\\w+\\.)?(?:${REGIOES})"?\\s*(?:,|->|:)|"(?:${REGIOES})"\\s*\\.equals|[=!]=\\s*(?:\\w+\\.)?(?:${REGIOES})\\b'
  - id: p5-switch-fabrica
    languages: [regex]
    severity: INFO
    message: switch de regiao que devolve objeto
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+"?(?:\\w+\\.)?(?:${REGIOES})"?\\s*(?:->|:)\\s*(?:return\\s+)?${OBJETO}'
`;

process.stdout.write(
  "# GERADO por gerar-regras.mjs. Nao edite a mao: mude o gerador e gere de novo.\n" +
  "rules:" + PONTOS.map(positivo).join("") + negativo,
);
