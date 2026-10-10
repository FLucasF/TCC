// Gera regras.yml, as regras do Semgrep que leem o desenho de cada pacote.
//
// Uso:  node evaluation/tools/semgrep/gerar-regras.mjs > evaluation/tools/semgrep/regras.yml
//
// Os pontos positivos (P1 a P4) recebem as mesmas regras, mudando so os nomes dos casos;
// o controle negativo (P5) tem regras proprias. Os nomes vem do gabarito do Strategy e
// sao os mesmos nos enunciados do V3 e do V4. Deterministico: o mesmo pacote da sempre o
// mesmo resultado. Nao usa IA.
//
// Versao 2 (09/10, depois da revisao com o corpus adversarial, ver README): comparacoes
// nos dois sentidos, Objects.equals, constantes com outro nome (pela propagacao de
// constantes do Semgrep), sufixos como .name(); switch com pattern matching (Java 21);
// fabrica por if; validacao que recusa sem throw; conjuntos que nomeiam parte dos casos;
// instanceof fora do equals(); no P5, objetos por regiao (a classe e capturada, e o
// classificador separa "uma classe so" de "uma classe por regiao"), funcoes por regiao,
// constantes static final, entry() e if com x.equals("NORTE"). O Semgrep roda numa copia
// sem comentarios (detect.sh), e por isso as regras de texto nao leem comentario.
//
// COMO LER. Cada regra procura UMA forma de escrever e marca a linha. Ha dois tipos:
// "languages: [java]" (o Semgrep entende o codigo: um if, uma comparacao, um new) e
// "languages: [regex]" (busca de texto, para o que o parser nao ve bem, como o case
// com pattern matching). Por ponto positivo (P1 a P4):
//   cond-calc       uma comparacao que nomeia um caso (nivel == OURO, "OURO".equals(x));
//   cond-validacao  a mesma, dentro de um if que recusa com throw;
//   cond-recusa     a mesma, dentro de um if que devolve um erro (sem throw);
//   cond-fabrica    a mesma, dentro de um if que so devolve ou guarda um objeto;
//   instanceof      instanceof de uma classe de caso, fora do equals();
//   switch-label    um "case OURO" ou "case Ouro o" (rotulo que nomeia o caso);
//   switch-fabrica  o rotulo que so devolve um objeto (fabrica);
//   conjunto        EnumSet/Set/List.of com casos (so conta se nao citar todos);
//   nome            o alarme: o nome do caso aparece em algum lugar?
// No P5: enum-corpo, enum-dados, classe-regiao, mapa, mapa-funcao, objeto (com a
// classe capturada), config, switch-label, switch-fabrica e traducao.
// A decisao (o que e espalhado, o que e consulta...) NAO esta aqui: esta no
// classificar.mjs. Aqui so se marca o que existe.

import { PONTOS as BASE, NOMES_REGIOES, REGIOES_CLASSE, nomes } from "./pontos.mjs";

// Cada ponto com todos os nomes dos casos (os do enunciado e os sinonimos em ingles).
const PONTOS = BASE.map((p) => ({ ...p, lista: nomes(p), casos: nomes(p).join("|") }));
const REGIOES = NOMES_REGIOES.join("|");
// Tipos de valor: "new BigDecimal(...)" e um numero, nao um objeto de estrategia.
const VALOR = "BigDecimal|BigInteger|Double|Integer|Long|Float|String|Object";
const OBJETO = String.raw`new\s+(?!(?:${VALOR})\b)[A-Z]\w*\s*\(`;
// Argumentos com ate tres niveis de parenteses: NORTE(BigDecimal.valueOf(25).divide(BigDecimal.valueOf(1000)))
const ARGS = String.raw`\((?:[^()]|\((?:[^()]|\((?:[^()]|\([^()]*\))*\))*\))*\)`;
const SO_MAIN = `paths: { include: ["**/src/main/**/*.java"] }`;

// O caso, como texto ou constante, com prefixo (NivelClube.OURO) e um sufixo sem
// argumento (.name(), .getCodigo()).
const casoRegex = (c) => `^"?([A-Za-z_]\\w*\\.)*(${c})"?(\\.\\w+\\(\\))?$`;

// As comparacoes que nomeiam um caso. O literal escrito na regra tambem acha uma
// constante de outro nome com o mesmo valor (VIP = "OURO"), pela propagacao do Semgrep.
const comparacoes = ({ casos, lista }) => `
          - patterns:
              - pattern-either:
                  - pattern: $X == $C
                  - pattern: $C == $X
                  - pattern: $X != $C
                  - pattern: $C != $X
                  - pattern: $C.equals($X)
                  - pattern: $X.equals($C)
                  - pattern: $X.equalsIgnoreCase($C)
                  - pattern: $C.equalsIgnoreCase($X)
                  - pattern: Objects.equals($X, $C)
                  - pattern: Objects.equals($C, $X)
                  - pattern: $X.compareTo($C)
                  - pattern: $C.compareTo($X)
              - metavariable-regex:
                  metavariable: $C
                  regex: '${casoRegex(casos)}'${lista.map((c) => `
          - pattern: $X.equals("${c}")
          - pattern: '"${c}".equals($X)'
          - pattern: $X.equalsIgnoreCase("${c}")
          - pattern: Objects.equals($X, "${c}")
          - pattern: Objects.equals("${c}", $X)`).join("")}`;

// Um if que recusa: throw, ou um return de erro (sem throw). "return false" NAO e
// recusa: e o brinde feito por um if (exemplo do guia).
// O metavariable-regex do Semgrep casa a partir do comeco do texto (re.match): o ".*"
// deixa o erro aparecer em qualquer lugar do retorno (Optional.of(new Erro(...))).
const RECUSA = String.raw`(?i).*(erro|error|badrequest|bad_request|recus|invalid|indispon|unprocessable|status\(\s*4\d\d|HttpStatus\.(BAD|UNPROC|CONFLICT))`;

// Uma variavel com o nome do caso (ouro, nivelOuro) e um objeto ja criado, e devolve-la
// e escolher (fabrica). Mas nao se o nome e de numero (creditoOuro, PERCENTUAL_OURO):
// ai o switch ou if esta fazendo a conta. Constante em maiusculas tambem nao conta.
const NOME_DE_NUMERO = "pct|percent|taxa|credito|desconto|valor|aliquota|fator|rate|frete|juros|bonus|cashback|brinde|parcela|limite";
const objetoComNome = (classes) => `(?![A-Z0-9_]+\\b)(?!\\w*(?i:${NOME_DE_NUMERO}))\\w*(?i:${classes})\\w*`;
// Um if que so escolhe o objeto (fabrica): devolve ou guarda um objeto novo, a
// constante do caso, ou um objeto com o nome do caso.
const fabricaRegex = (p) => `^(new\\s+(?!(${VALOR})\\b)[A-Z]\\w*\\s*\\(.*|([A-Za-z_]\\w*\\.)*(${p.casos})|(this\\.)?${objetoComNome(p.classes)}|\\w*(${p.classes})\\w*::new)$`;

const positivo = (p) => {
  const { id, casos, classes } = p;
  return `
  - id: ${id}-cond-calc
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso (o classificador tira as de recusa e as de fabrica)
    ${SO_MAIN}
    pattern-either:${comparacoes(p)}
  - id: ${id}-cond-validacao
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso so para recusar
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern-inside: if (...) throw $E;
          - pattern-inside: if (...) { ... throw $E; ... }
      - pattern-either:${comparacoes(p)}
  - id: ${id}-cond-recusa
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso so para recusar, devolvendo um erro (sem throw)
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern-inside: if (...) return $R;
          - pattern-inside: if (...) { ... return $R; }
      - metavariable-regex: { metavariable: $R, regex: '${RECUSA}' }
      - pattern-either:${comparacoes(p)}
  - id: ${id}-cond-fabrica
    languages: [java]
    severity: INFO
    message: condicao que nomeia um caso so para escolher o objeto (fabrica)
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern-inside: if (...) return $V;
          - pattern-inside: if (...) { return $V; }
          - pattern-inside: if (...) $Y = $V;
          - pattern-inside: if (...) { $Y = $V; }
      - metavariable-regex: { metavariable: $V, regex: '${fabricaRegex(p)}' }
      - pattern-either:${comparacoes(p)}
  - id: ${id}-instanceof
    languages: [java]
    severity: INFO
    message: instanceof de uma classe de caso (fora do equals)
    ${SO_MAIN}
    patterns:
      - pattern: $X instanceof $K
      - metavariable-regex: { metavariable: $K, regex: ".*(${classes})" }
      - pattern-not-inside: |
          boolean equals(Object $O) { ... }
  - id: ${id}-instanceof-recusa
    languages: [java]
    severity: INFO
    message: instanceof de uma classe de caso dentro de um if que recusa (o classificador conta como recusa)
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern-inside: if (...) throw $E;
          - pattern-inside: if (...) { ... throw $E; ... }
      - pattern: $X instanceof $K
      - metavariable-regex: { metavariable: $K, regex: ".*(${classes})" }
  - id: ${id}-switch-label
    languages: [regex]
    severity: INFO
    message: rotulo de switch que nomeia um caso (inclusive pattern matching, Java 21)
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+(?:"?(?:\\w+\\.)?(?:${casos})"?\\s*(?:,|->|:)|\\w*(?:${classes})\\w*\\s*(?:\\(|\\s+\\w+\\s*(?:->|:|when\\b)))'
  - id: ${id}-switch-fabrica
    languages: [regex]
    severity: INFO
    message: rotulo de switch que so devolve um objeto (fabrica ou traducao para a constante)
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+"?(?:\\w+\\.)?(?:${casos})"?\\s*(?:->|:)\\s*(?:return\\s+)?(?:${OBJETO}|(?:\\w+\\.)?(?:${casos})\\s*;|(?:this\\.)?${objetoComNome(classes)}\\s*;|\\w*(?:${classes})\\w*::new)'
  - id: ${id}-conjunto
    languages: [regex]
    severity: INFO
    message: conjunto ou lista que cita casos (o classificador conta so os que nao citam todos)
    ${SO_MAIN}
    pattern-regex: '\\b(?:EnumSet|Set|List)\\.of\\s*\\([^()]*\\b(?:${casos})\\b[^()]*\\)'
  - id: ${id}-nome
    languages: [regex]
    severity: INFO
    message: o nome de um caso aparece no codigo (alarme - sem nenhum, o agente usou outros nomes)
    ${SO_MAIN}
    pattern-regex: '\\b(?:${casos})\\b|\\b\\w*(?:${classes})\\w*\\b'
`;
};

const REG = `"?(?:\\w+\\.)?(?:${REGIOES})"?`;
// A chave de um mapa: entre aspas ("NORTE") ou qualificada (Regiao.NORTE). O nome solto
// fica de fora porque e assim que aparece num "case SUDESTE, SUL ->", que nao e mapa
// (defeito achado na regressao de 09/10: o "SUL ->" parecia uma funcao).
const CHAVE = `(?:"(?:${REGIOES})"|\\w+\\.(?:${REGIOES})\\b)`;
const regiaoRegex = `^"?([A-Za-z_]\\w*\\.)*(${REGIOES})"?$`;
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
    pattern-regex: '\\b(?:class|record)\\s+\\w*(?:${REGIOES_CLASSE})\\w*[^{]*\\b(?:implements|extends)\\b'
  - id: p5-mapa
    languages: [regex]
    severity: INFO
    message: mapa de regiao para valor
    ${SO_MAIN}
    # o \\s* fica DENTRO da verificacao: fora, ele devolve o espaco e a verificacao passa
    # a olhar " new Seguro()", que nao comeca por "new" (defeito da versao 1)
    pattern-regex: '(?:Map\\.(?:of|entry)\\s*\\(\\s*|\\bentry\\s*\\(\\s*|\\bput\\s*\\(\\s*)${REG}\\s*,(?!\\s*(?:${OBJETO}|(?:\\([^()]*\\)|\\w+)\\s*->|[\\w.]+::\\w+))'
  - id: p5-mapa-funcao
    languages: [regex]
    severity: INFO
    message: mapa de regiao para uma funcao ou classe anonima (uma por regiao)
    ${SO_MAIN}
    pattern-regex: '${CHAVE}\\s*,\\s*(?:\\([^()]*\\)|\\w+)\\s*->|${CHAVE}\\s*,\\s*[\\w.]+::\\w+|${CHAVE}\\s*,\\s*new\\s+[A-Z]\\w*\\s*\\([^()]*\\)\\s*\\{'
  - id: p5-config
    languages: [regex]
    severity: INFO
    message: taxa por regiao num arquivo de configuracao (dado)
    paths: { include: ["**/src/main/resources/**"] }
    pattern-regex: '(?im)^\\s*[\\w.\\[\\]-]*\\b(?:sudeste|sul|centro[-_]?oeste|norte|nordeste|southeast|south|midwest|cent(?:er|ral)[-_]?west|north|northeast)\\b\\s*[:=]\\s*[0-9]'
  - id: p5-objeto
    languages: [java]
    severity: INFO
    message: objeto por regiao (o classificador ve se e uma classe so ou uma por regiao)
    ${SO_MAIN}
    patterns:
      - pattern-either:
          - pattern: new $T($R, ...)
          - pattern: Map.of(..., $R, new $T(...), ...)
          - pattern: Map.entry($R, new $T(...))
          - pattern: entry($R, new $T(...))
          - pattern: $M.put($R, new $T(...))
          - pattern: $TT $R = new $T(...);
      - metavariable-regex: { metavariable: $R, regex: '${regiaoRegex}' }
      - metavariable-regex: { metavariable: $T, regex: '^(?!(${VALOR})$)\\w+$' }
  - id: p5-switch-label
    languages: [regex]
    severity: INFO
    message: rotulo de switch ou condicao que nomeia uma regiao
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+${REG}\\s*(?:,|->|:)|"(?:${REGIOES})"\\s*\\.equals|\\.equals(?:IgnoreCase)?\\(\\s*${REG}\\s*\\)|[=!]=\\s*(?:\\w+\\.)?(?:${REGIOES})\\b'
  - id: p5-switch-fabrica
    languages: [regex]
    severity: INFO
    message: switch de regiao que devolve objeto
    ${SO_MAIN}
    pattern-regex: '\\bcase\\s+${REG}\\s*(?:->|:)\\s*(?:return\\s+)?${OBJETO}'
  - id: p5-traducao
    languages: [regex]
    severity: INFO
    message: switch ou if que so traduz o texto para a constante da regiao
    ${SO_MAIN}
    pattern-regex: '(?:\\bcase\\s+${REG}\\s*(?:->|:)|\\.equals(?:IgnoreCase)?\\(\\s*${REG}\\s*\\)\\s*\\)\\s*\\{?)\\s*(?:return\\s+)?(?:\\w+\\.)?(?:${REGIOES})\\s*;'
`;

process.stdout.write(
  "# GERADO por gerar-regras.mjs. Nao edite a mao: mude o gerador e gere de novo.\n" +
  "rules:" + PONTOS.map(positivo).join("") + negativo,
);
