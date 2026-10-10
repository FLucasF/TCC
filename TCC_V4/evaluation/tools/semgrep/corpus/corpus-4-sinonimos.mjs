// Corpus 4, de DESENVOLVIMENTO: os nomes em ingles (GOLD, CREDIT_CARD, NORTH...) e a
// comparacao pela ordem (compareTo). Respostas escritas ANTES de implementar os
// sinonimos e o compareTo, mas escritas por quem implementou: nao valem como controle.
//
// Uso: node corpus-4-sinonimos.mjs <pasta-saida>

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

// Corpus 4: as tres limitacoes (nomes em ingles, compareTo/ordinal, codigo morto).
// Escrito ANTES de implementar. Colunas extras: avisos_contem / avisos_sem.
const TIER = (extra = "") => J("clube", `public enum Tier {
    BRONZE("BRONZE", BigDecimal.ZERO, false), SILVER("PRATA", new BigDecimal("0.02"), false), GOLD("OURO", new BigDecimal("0.05"), true);
    private final String codigo; private final BigDecimal pct; private final boolean freeShipping;
    Tier(String codigo, BigDecimal pct, boolean freeShipping) { this.codigo = codigo; this.pct = pct; this.freeShipping = freeShipping; }
    public static Tier from(String c) { return Arrays.stream(values()).filter(t -> t.codigo.equals(c)).findFirst().orElseThrow(); }
    public boolean freeShipping() { return freeShipping; }
${extra}}`);
pacote("F01-ingles-remendo", { "clube/Tier.java": TIER(), "clube/Shipping.java": J("clube", `public class Shipping {
    public BigDecimal frete(Tier tier, BigDecimal f) {
        if (tier == Tier.GOLD) return BigDecimal.ZERO;
        return f;
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "enum em ingles que traduz o codigo, e o remendo com GOLD");
pacote("F02-ingles-limpo", { "clube/Tier.java": TIER(), "clube/Shipping.java": J("clube", `public class Shipping {
    public BigDecimal frete(String nivel, BigDecimal f) { return Tier.from(nivel).freeShipping() ? BigDecimal.ZERO : f; }
}`) }, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "enum em ingles, sem remendo");
pacote("F03-compareTo", { "clube/NivelClube.java": NIVEL_ENUM, "clube/Brinde.java": J("clube", `public class Brinde {
    public boolean ganha(NivelClube n, BigDecimal s) {
        return n.compareTo(NivelClube.PRATA) > 0 && s.compareTo(new BigDecimal("500")) > 0;
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "acima de PRATA por compareTo");
pacote("F07-validos-ingles", { "clube/Tier.java": TIER(), "clube/Entrada.java": J("clube", `public class Entrada {
    private static final Set<Tier> TODOS = EnumSet.of(Tier.BRONZE, Tier.SILVER, Tier.GOLD);
    public boolean valido(Tier t) { return TODOS.contains(t); }
}`) }, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "conjunto com todos os niveis, em ingles");
pacote("F08-cartao-ingles", {
  "pagamento/PaymentMethod.java": J("pagamento", `public enum PaymentMethod { PIX, CREDIT_CARD, BANK_SLIP }`),
  "pagamento/Checkout.java": J("pagamento", `public class Checkout {
    public BigDecimal total(PaymentMethod m, BigDecimal t, int parcelas) {
        if (m == PaymentMethod.CREDIT_CARD && parcelas > 3) return t.multiply(new BigDecimal("1.0199"));
        if (m == PaymentMethod.PIX) return t.multiply(new BigDecimal("0.95"));
        return t;
    }
}`) }, { P3_localizacao: "espalhado", P3_selecao: "condicional-no-calculo" }, "pagamento em ingles calculado por if");
const R = (nome, arquivos, forma, proporcao, porque) => pacote(nome, arquivos, { P5_forma: forma, P5_proporcao: proporcao }, porque);
R("F09-regiao-ingles", { "regiao/Region.java": J("regiao", `public enum Region {
    SOUTHEAST(new BigDecimal("0.01")),
    SOUTH(new BigDecimal("0.01")),
    MIDWEST(new BigDecimal("0.015")),
    NORTH(new BigDecimal("0.025")),
    NORTHEAST(new BigDecimal("0.02"));
    private final BigDecimal rate;
    Region(BigDecimal rate) { this.rate = rate; }
}`) }, "enum-dados", "dados", "regioes em ingles");

mkdirSync(saida, { recursive: true });
const COLS = ["P1_localizacao", "P1_selecao", "P2_localizacao", "P2_selecao", "P3_localizacao", "P3_selecao", "P4_localizacao", "P4_selecao", "P5_forma", "P5_proporcao", "avisos_contem", "avisos_sem"];
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
