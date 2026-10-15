// Corpus 3, de DESENVOLVIMENTO: casos que sairam da revisao do codigo das regras
// (o switch que devolve um numero com o nome do caso, E01 a E03) e da regressao nos
// pacotes reais (E04 e E05, dois defeitos que a versao 2 tinha introduzido e que esta
// pegou). Escritos junto com a correcao: nao valem como controle.
//
// Uso: node corpus-3-revisao.mjs <pasta-saida>

import { mkdirSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const saida = process.argv[2];
if (!saida || existsSync(saida)) { console.error("informe uma pasta nova"); process.exit(1); }
const casos = [];
const pacote = (nome, arquivos, esperado, porque) => casos.push({ nome, arquivos, esperado, porque });
const J = (pkg, corpo) => `package com.loja.${pkg};\n\nimport java.math.BigDecimal;\nimport java.util.*;\nimport java.util.function.*;\n\n${corpo}\n`;

const NIVEL_ENUM = J("clube", `public enum NivelClube {
    BRONZE(BigDecimal.ZERO), PRATA(new BigDecimal("0.02")), OURO(new BigDecimal("0.05"));
    private final BigDecimal pct;
    NivelClube(BigDecimal pct) { this.pct = pct; }
    public BigDecimal credito(BigDecimal s) { return s.multiply(pct); }
}`);
const CLASSES = {
  "clube/Nivel.java": J("clube", `public interface Nivel { BigDecimal credito(BigDecimal s); boolean freteGratis(); }`),
  "clube/NivelBronze.java": J("clube", `public class NivelBronze implements Nivel { public BigDecimal credito(BigDecimal s) { return BigDecimal.ZERO; } public boolean freteGratis() { return false; } }`),
  "clube/NivelPrata.java": J("clube", `public class NivelPrata implements Nivel { public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); } public boolean freteGratis() { return false; } }`),
  "clube/NivelOuro.java": J("clube", `public class NivelOuro implements Nivel { public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.05")); } public boolean freteGratis() { return true; } }`),
};

// Corpus 3: casos da revisao do codigo das regras (desenvolvimento, nao controle).
pacote("E01-switch-constante-numero", { "clube/Servico.java": J("clube", `public class Servico {
    private static final BigDecimal PERCENTUAL_PRATA = new BigDecimal("0.02");
    private static final BigDecimal PERCENTUAL_OURO = new BigDecimal("0.05");
    public BigDecimal credito(String nivel, BigDecimal s) {
        BigDecimal pct = switch (nivel) {
            case "PRATA" -> PERCENTUAL_PRATA;
            case "OURO" -> PERCENTUAL_OURO;
            default -> BigDecimal.ZERO;
        };
        return s.multiply(pct);
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch que devolve a constante do numero");
pacote("E02-switch-variavel-numero", { "clube/Servico.java": J("clube", `public class Servico {
    private final BigDecimal creditoPrata = new BigDecimal("0.02");
    private final BigDecimal creditoOuro = new BigDecimal("0.05");
    public BigDecimal credito(String nivel, BigDecimal s) {
        BigDecimal pct = switch (nivel) {
            case "PRATA" -> creditoPrata;
            case "OURO" -> creditoOuro;
            default -> BigDecimal.ZERO;
        };
        return s.multiply(pct);
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch que devolve uma variavel de numero com o nome do nivel");
pacote("E03-switch-objeto-nome", { ...CLASSES, "clube/Fabrica.java": J("clube", `public class Fabrica {
    private final Nivel nivelBronze = new NivelBronze();
    private final Nivel nivelPrata = new NivelPrata();
    private final Nivel nivelOuro = new NivelOuro();
    public Nivel de(String c) {
        return switch (c) {
            case "BRONZE" -> nivelBronze;
            case "PRATA" -> nivelPrata;
            case "OURO" -> nivelOuro;
            default -> throw new IllegalArgumentException();
        };
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "switch que devolve o objeto ja criado");
// Da regressao nos pacotes reais (09/10): os dois defeitos que a versao 2 introduziu.
pacote("E04-switch-varios-rotulos", { "regiao/Regiao.java": J("regiao", `public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }`),
  "regiao/Seguro.java": J("regiao", `public class Seguro {
    public BigDecimal taxa(Regiao regiao) {
        BigDecimal percentual = switch (regiao) {
            case SUDESTE, SUL -> new BigDecimal("0.01");
            case CENTRO_OESTE -> new BigDecimal("0.015");
            case NORTE -> new BigDecimal("0.025");
            case NORDESTE -> new BigDecimal("0.02");
        };
        return percentual;
    }
}`) }, { P5_forma: "switch", P5_proporcao: "condicional" }, "case SUDESTE, SUL -> numero (como no TESTE-NIVEIS-01-HAIKU-N0)");
pacote("E05-instanceof-na-recusa", {
  "pagamento/FormaPagamento.java": J("pagamento", `public interface FormaPagamento { BigDecimal ajuste(BigDecimal t); }`),
  "pagamento/PagamentoPix.java": J("pagamento", `public class PagamentoPix implements FormaPagamento { public BigDecimal ajuste(BigDecimal t) { return t.multiply(new BigDecimal("-0.05")); } }`),
  "pagamento/PagamentoCartao.java": J("pagamento", `public class PagamentoCartao implements FormaPagamento { public BigDecimal ajuste(BigDecimal t) { return BigDecimal.ZERO; } }`),
  "pagamento/PagamentoBoleto.java": J("pagamento", `public class PagamentoBoleto implements FormaPagamento { public BigDecimal ajuste(BigDecimal t) { return new BigDecimal("3.49"); } }`),
  "pagamento/Servico.java": J("pagamento", `public class Servico {
    public FormaPagamento criar(String forma) {
        return switch (forma) {
            case "PIX" -> new PagamentoPix();
            case "CARTAO" -> new PagamentoCartao();
            case "BOLETO" -> new PagamentoBoleto();
            default -> throw new IllegalArgumentException("FORMA_PAGAMENTO_INVALIDA");
        };
    }
    public void validar(FormaPagamento forma, int parcelas) {
        if (forma instanceof PagamentoPix || forma instanceof PagamentoBoleto) {
            if (parcelas != 1) {
                throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
            }
        }
    }
}`) }, { P3_localizacao: "espalhado", P3_selecao: "condicional-unica" }, "instanceof so para recusar (como no BATCH-02-HAIKU-HARNESS)");

mkdirSync(saida, { recursive: true });
const COLS = ["P1_localizacao", "P1_selecao", "P2_localizacao", "P2_selecao", "P3_localizacao", "P3_selecao", "P4_localizacao", "P4_selecao", "P5_forma", "P5_proporcao"];
const linhas = ["pacote," + COLS.join(",") + ",porque"];
for (const c of casos) {
  for (const [arq, codigo] of Object.entries(c.arquivos)) {
    const caminho = arq.startsWith("/") ? join(saida, "pacotes", c.nome, arq.slice(1)) : join(saida, "pacotes", c.nome, "src/main/java/com/loja", arq);
    mkdirSync(join(caminho, ".."), { recursive: true });
    writeFileSync(caminho, codigo);
  }
  linhas.push([c.nome, ...COLS.map((k) => c.esperado[k] ?? "*"), `"${c.porque}"`].join(","));
}
writeFileSync(join(saida, "esperado.csv"), linhas.join("\n") + "\n");
console.log(`${casos.length} pacotes em ${saida}`);
