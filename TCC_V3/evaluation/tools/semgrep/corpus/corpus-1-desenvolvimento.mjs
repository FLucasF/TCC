// Corpus 1, de DESENVOLVIMENTO: o corpus adversarial que guiou a versao 2 (09/10).
// A versao 1 acertou 41 de 80 respostas nele. Cada pacote isola UMA forma de escrever; a resposta
// esperada pela regua (versao 4) fica em esperado.csv, escrita ANTES de rodar as regras.
// "*" = nao importa (o ponto nao aparece no pacote, e o alarme o marca indeterminado).
//
// Uso: node corpus-1-desenvolvimento.mjs <pasta-saida>

import { mkdirSync, writeFileSync, existsSync } from "node:fs";
import { join } from "node:path";

const saida = process.argv[2];
if (!saida || existsSync(saida)) { console.error("informe uma pasta nova"); process.exit(1); }

const casos = [];
// pacote(nome, arquivos {caminho: codigo}, esperado {P4_localizacao: ...})
const pacote = (nome, arquivos, esperado, porque) => casos.push({ nome, arquivos, esperado, porque });
const J = (pkg, corpo) => `package com.loja.${pkg};\n\nimport java.math.BigDecimal;\nimport java.util.*;\n\n${corpo}\n`;

// ---------------------------------------------------------------- P4 (e P3), positivos
const NIVEL_ENUM = J("clube", `public enum NivelClube {
    BRONZE(new BigDecimal("0"), false),
    PRATA(new BigDecimal("0.02"), false),
    OURO(new BigDecimal("0.05"), true);

    private final BigDecimal credito;
    private final boolean freteGratis;

    NivelClube(BigDecimal credito, boolean freteGratis) {
        this.credito = credito;
        this.freteGratis = freteGratis;
    }

    public BigDecimal credito(BigDecimal subtotal) { return subtotal.multiply(credito); }
    public boolean freteGratis() { return freteGratis; }
}`);

pacote("A01-enum-dados", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "servico/Servico.java": J("servico", `import com.loja.clube.NivelClube;

public class Servico {
    public BigDecimal frete(String codigo, BigDecimal frete) {
        NivelClube nivel = NivelClube.valueOf(codigo);
        return nivel.freteGratis() ? BigDecimal.ZERO : frete;
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "enum com dados, achado por valueOf");

const CLASSES = (extra = "") => ({
  "clube/Nivel.java": J("clube", `public interface Nivel {
    String codigo();
    BigDecimal credito(BigDecimal subtotal);
    boolean freteGratis();
}`),
  "clube/Bronze.java": J("clube", `public class Bronze implements Nivel {
    public String codigo() { return "BRONZE"; }
    public BigDecimal credito(BigDecimal s) { return BigDecimal.ZERO; }
    public boolean freteGratis() { return false; }
${extra}}`),
  "clube/Prata.java": J("clube", `public class Prata implements Nivel {
    public String codigo() { return "PRATA"; }
    public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); }
    public boolean freteGratis() { return false; }
}`),
  "clube/Ouro.java": J("clube", `public class Ouro implements Nivel {
    public String codigo() { return "OURO"; }
    public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.05")); }
    public boolean freteGratis() { return true; }
}`),
});

pacote("A02-classes-mapa", {
  ...CLASSES(),
  "clube/Catalogo.java": J("clube", `public class Catalogo {
    private final Map<String, Nivel> niveis = new HashMap<>();
    public Catalogo(List<Nivel> todos) { for (Nivel n : todos) niveis.put(n.codigo(), n); }
    public Optional<Nivel> buscar(String codigo) { return Optional.ofNullable(niveis.get(codigo)); }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "uma classe por nivel, mapa pelo codigo()");

pacote("A03-registro-suporta", {
  "clube/Nivel.java": J("clube", `public interface Nivel {
    boolean suporta(String codigo);
    BigDecimal credito(BigDecimal subtotal);
}`),
  "clube/Bronze.java": J("clube", `public class Bronze implements Nivel {
    public boolean suporta(String codigo) { return "BRONZE".equals(codigo); }
    public BigDecimal credito(BigDecimal s) { return BigDecimal.ZERO; }
}`),
  "clube/Prata.java": J("clube", `public class Prata implements Nivel {
    public boolean suporta(String codigo) { return "PRATA".equals(codigo); }
    public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); }
}`),
  "clube/NivelOuro.java": J("clube", `public class NivelOuro implements Nivel {
    @Override
    public boolean suporta(String codigo) {
        return codigo.equals("OURO");
    }
    public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.05")); }
}`),
  "clube/Resolvedor.java": J("clube", `public class Resolvedor {
    private final List<Nivel> niveis;
    public Resolvedor(List<Nivel> niveis) { this.niveis = niveis; }
    public Nivel resolver(String codigo) {
        return niveis.stream().filter(n -> n.suporta(codigo)).findFirst().orElseThrow();
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "registro: cada classe diz se suporta o codigo, e um laco generico escolhe");

pacote("A04-pattern-switch", {
  "clube/Nivel.java": J("clube", `public sealed interface Nivel permits Bronze, Prata, Ouro {}`),
  "clube/Bronze.java": J("clube", `public record Bronze() implements Nivel {}`),
  "clube/Prata.java": J("clube", `public record Prata() implements Nivel {}`),
  "clube/Ouro.java": J("clube", `public record Ouro() implements Nivel {}`),
  "clube/Calculo.java": J("clube", `public class Calculo {
    public BigDecimal credito(Nivel nivel, BigDecimal s) {
        return switch (nivel) {
            case Bronze b -> BigDecimal.ZERO;
            case Prata p -> s.multiply(new BigDecimal("0.02"));
            case Ouro o -> s.multiply(new BigDecimal("0.05"));
        };
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch com pattern matching (Java 21) que faz a conta");

pacote("A05-switch-fabrica", {
  ...CLASSES(),
  "clube/Fabrica.java": J("clube", `public class Fabrica {
    public static Nivel criar(String codigo) {
        return switch (codigo) {
            case "BRONZE" -> new Bronze();
            case "PRATA" -> new Prata();
            case "OURO" -> new Ouro();
            default -> throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        };
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "fabrica por switch");

pacote("A06-if-fabrica", {
  ...CLASSES(),
  "clube/Fabrica.java": J("clube", `public class Fabrica {
    public static Nivel criar(String codigo) {
        if ("BRONZE".equals(codigo)) return new Bronze();
        if ("PRATA".equals(codigo)) {
            return new Prata();
        }
        if (codigo.equals("OURO")) return new Ouro();
        throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "fabrica por cadeia de if");

pacote("A07-enum-parse-switch", {
  "clube/NivelClube.java": J("clube", `public enum NivelClube {
    BRONZE(new BigDecimal("0")),
    PRATA(new BigDecimal("0.02")),
    OURO(new BigDecimal("0.05"));

    private final BigDecimal credito;
    NivelClube(BigDecimal credito) { this.credito = credito; }
    public BigDecimal credito(BigDecimal s) { return s.multiply(credito); }

    public static NivelClube de(String codigo) {
        return switch (codigo) {
            case "BRONZE" -> BRONZE;
            case "PRATA" -> PRATA;
            case "OURO" -> OURO;
            default -> throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        };
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "switch que so traduz o texto para a constante do enum");

pacote("A08-switch-calcula", {
  "clube/Servico.java": J("clube", `public class Servico {
    public BigDecimal credito(String nivel, BigDecimal s) {
        switch (nivel) {
            case "BRONZE": return BigDecimal.ZERO;
            case "PRATA": return s.multiply(new BigDecimal("0.02"));
            case "OURO": return s.multiply(new BigDecimal("0.05"));
            default: throw new IllegalArgumentException();
        }
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch que faz a conta");

const SERVICO_REMENDO = (cond) => ({
  "clube/NivelClube.java": NIVEL_ENUM.replace("OURO(new BigDecimal(\"0.05\"), true)", "OURO(new BigDecimal(\"0.05\"), false)"),
  "servico/Frete.java": J("servico", `import com.loja.clube.NivelClube;

public class Frete {
    private static final String NIVEL_VIP = "OURO";
    public BigDecimal frete(NivelClube nivel, String codigo, BigDecimal frete) {
        if (${cond}) {
            return BigDecimal.ZERO;
        }
        return frete;
    }
}`),
});
pacote("A09-yoda", SERVICO_REMENDO("NivelClube.OURO == nivel"),
  { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "remendo com a constante a esquerda");
pacote("A10-objects-equals", SERVICO_REMENDO("Objects.equals(codigo, \"OURO\")"),
  { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "remendo com Objects.equals");
pacote("A11-constante-apelido", SERVICO_REMENDO("NIVEL_VIP.equals(codigo)"),
  { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "remendo com uma constante de outro nome");
pacote("A12-name", SERVICO_REMENDO("codigo.equals(NivelClube.OURO.name())"),
  { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "remendo com .name()");

pacote("A13-validacao-sem-throw", {
  "pagamento/FormaPagamento.java": J("pagamento", `public enum FormaPagamento {
    PIX(new BigDecimal("-0.05")),
    CARTAO(BigDecimal.ZERO),
    BOLETO(new BigDecimal("3.49"));
    private final BigDecimal ajuste;
    FormaPagamento(BigDecimal ajuste) { this.ajuste = ajuste; }
    public BigDecimal ajuste() { return ajuste; }
}`),
  "pagamento/Erro.java": J("pagamento", `public record Erro(String erro) {}`),
  "pagamento/Validador.java": J("pagamento", `public class Validador {
    public Optional<Erro> validar(FormaPagamento forma, BigDecimal total) {
        if (forma == FormaPagamento.BOLETO && total.compareTo(new BigDecimal("1000")) > 0) {
            return Optional.of(new Erro("FORMA_PAGAMENTO_INDISPONIVEL"));
        }
        return Optional.empty();
    }
}`),
}, { P3_localizacao: "espalhado", P3_selecao: "consulta" }, "validacao que recusa devolvendo um erro, sem throw");

pacote("A14-switch-lista-validos", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "clube/Validacao.java": J("clube", `public class Validacao {
    public void validar(String codigo) {
        switch (codigo) {
            case "BRONZE", "PRATA", "OURO" -> { }
            default -> throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "switch que so confere se o codigo existe");

pacote("A15-equals-instanceof", CLASSES(`    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Bronze)) return false;
        return true;
    }
    @Override
    public int hashCode() { return 1; }
`), { P4_localizacao: "isolado", P4_selecao: "consulta" }, "instanceof dentro do equals() gerado");

pacote("A16-comentario", {
  "clube/NivelClube.java": NIVEL_ENUM.replace("public boolean freteGratis()", "// antes: case OURO -> frete zero; agora o enum responde\n    public boolean freteGratis()"),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "um switch citado num comentario");

pacote("A17-brinde-escondido", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "clube/Brinde.java": J("clube", `public class Brinde {
    public boolean ganha(NivelClube nivel, BigDecimal s) {
        if (nivel != NivelClube.OURO) return false;
        return s.compareTo(new BigDecimal("500")) > 0;
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "o brinde feito por um if que nomeia o OURO (exemplo do guia)");

pacote("A18-validacao-throw", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "clube/Regra.java": J("clube", `public class Regra {
    public void conferir(NivelClube nivel, int parcelas) {
        if (nivel == NivelClube.OURO && parcelas > 12) {
            throw new IllegalArgumentException("PARCELAMENTO_INVALIDO");
        }
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "consulta" }, "if que nomeia o nivel so para recusar");

pacote("A19-lista-validos-if", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "clube/Entrada.java": J("clube", `public class Entrada {
    public void validar(String c) {
        if (c == null || (!c.equals("BRONZE")
                && !c.equals("PRATA")
                && !c.equals("OURO"))) {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "lista de validos em if");

pacote("A20-outros-nomes", {
  "clube/Tier.java": J("clube", `public enum Tier {
    TIER_1(BigDecimal.ZERO), TIER_2(new BigDecimal("0.02")), TIER_3(new BigDecimal("0.05"));
    private final BigDecimal c;
    Tier(BigDecimal c) { this.c = c; }
}`),
}, { P4_localizacao: "indeterminado", P4_selecao: "indeterminado" }, "o agente nao usou os nomes do enunciado nem os em ingles (o alarme; trocado em 09/10, quando SILVER e GOLD viraram sinonimos)");

pacote("A21-isOuro", {
  "clube/NivelClube.java": NIVEL_ENUM.replace("public boolean freteGratis() { return freteGratis; }", "public boolean freteGratis() { return freteGratis; }\n    public boolean isOuro() { return this == OURO; }"),
  "servico/Frete.java": J("servico", `import com.loja.clube.NivelClube;

public class Frete {
    public BigDecimal frete(NivelClube nivel, BigDecimal frete) {
        return nivel.isOuro() ? BigDecimal.ZERO : frete;
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "isOuro(): o enum nomeia o OURO para o servico perguntar");

pacote("A22-mapa-lambdas", {
  "clube/Creditos.java": J("clube", `import java.util.function.UnaryOperator;

public class Creditos {
    private static final Map<String, UnaryOperator<BigDecimal>> CREDITO = Map.of(
        "BRONZE", s -> BigDecimal.ZERO,
        "PRATA", s -> s.multiply(new BigDecimal("0.02")),
        "OURO", s -> s.multiply(new BigDecimal("0.05")));
    public BigDecimal credito(String nivel, BigDecimal s) { return CREDITO.get(nivel).apply(s); }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "mapa do nivel para uma funcao");

pacote("A23-ternario-calcula", {
  "clube/NivelClube.java": NIVEL_ENUM,
  "clube/Calc.java": J("clube", `public class Calc {
    public BigDecimal credito(NivelClube n, BigDecimal s) {
        return n == NivelClube.OURO ? s.multiply(new BigDecimal("0.05"))
             : n == NivelClube.PRATA ? s.multiply(new BigDecimal("0.02")) : BigDecimal.ZERO;
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "ternario que nomeia e calcula");

pacote("A24-enumset", {
  "clube/NivelClube.java": NIVEL_ENUM.replace("OURO(new BigDecimal(\"0.05\"), true)", "OURO(new BigDecimal(\"0.05\"), false)"),
  "servico/Frete.java": J("servico", `import com.loja.clube.NivelClube;

public class Frete {
    private static final Set<NivelClube> SEM_FRETE = EnumSet.of(NivelClube.OURO);
    public BigDecimal frete(NivelClube nivel, BigDecimal frete) {
        return SEM_FRETE.contains(nivel) ? BigDecimal.ZERO : frete;
    }
}`),
}, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "conjunto que nomeia o OURO no calculo do frete");

// ---------------------------------------------------------------- P5, controle negativo
const R = (nome, arquivos, forma, proporcao, porque) => pacote(nome, arquivos, { P5_forma: forma, P5_proporcao: proporcao }, porque);

R("B01-enum-dados", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));
    private final BigDecimal taxa;
    Regiao(BigDecimal taxa) { this.taxa = taxa; }
    public BigDecimal seguro(BigDecimal s) { return s.multiply(taxa); }
}`) }, "enum-dados", "dados", "enum que so carrega a taxa");

R("B02-enum-parenteses", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE(BigDecimal.valueOf(10).divide(BigDecimal.valueOf(1000))),
    SUL(BigDecimal.valueOf(10).divide(BigDecimal.valueOf(1000))),
    CENTRO_OESTE(BigDecimal.valueOf(15).divide(BigDecimal.valueOf(1000))),
    NORTE(BigDecimal.valueOf(25).divide(BigDecimal.valueOf(1000))),
    NORDESTE(BigDecimal.valueOf(20).divide(BigDecimal.valueOf(1000)));
    private final BigDecimal taxa;
    Regiao(BigDecimal taxa) { this.taxa = taxa; }
}`) }, "enum-dados", "dados", "enum com dois niveis de parenteses no argumento");

R("B03-lista-objetos", {
  "regiao/Regiao.java": J("regiao", `public interface Regiao { String codigo(); BigDecimal percentual(); }`),
  "regiao/RegiaoFixa.java": J("regiao", `public record RegiaoFixa(String codigo, BigDecimal percentual) implements Regiao {}`),
  "regiao/Catalogo.java": J("regiao", `public class Catalogo {
    public final List<Regiao> todas = List.of(
        new RegiaoFixa("SUDESTE", new BigDecimal("1")),
        new RegiaoFixa("SUL", new BigDecimal("1")),
        new RegiaoFixa("CENTRO_OESTE", new BigDecimal("1.5")),
        new RegiaoFixa("NORTE", new BigDecimal("2.5")),
        new RegiaoFixa("NORDESTE", new BigDecimal("2")));
}`),
}, "outro", "dados", "lista de objetos de uma classe so (o caso do Opus 5)");

R("B04-constantes", { "regiao/Regiao.java": J("regiao", `public final class Regiao {
    public static final Regiao SUDESTE = new Regiao("SUDESTE", new BigDecimal("0.01"));
    public static final Regiao SUL = new Regiao("SUL", new BigDecimal("0.01"));
    public static final Regiao CENTRO_OESTE = new Regiao("CENTRO_OESTE", new BigDecimal("0.015"));
    public static final Regiao NORTE = new Regiao("NORTE", new BigDecimal("0.025"));
    public static final Regiao NORDESTE = new Regiao("NORDESTE", new BigDecimal("0.02"));
    private final String codigo; private final BigDecimal taxa;
    private Regiao(String codigo, BigDecimal taxa) { this.codigo = codigo; this.taxa = taxa; }
}`) }, "outro", "dados", "constantes static final de uma classe so");

R("B05-mapa-objeto-valor", { "regiao/Seguro.java": J("regiao", `public class Seguro {
    record Taxa(BigDecimal valor) {}
    private static final Map<String, Taxa> TAXAS = Map.of(
        "SUDESTE", new Taxa(new BigDecimal("0.01")),
        "SUL", new Taxa(new BigDecimal("0.01")),
        "CENTRO_OESTE", new Taxa(new BigDecimal("0.015")),
        "NORTE", new Taxa(new BigDecimal("0.025")),
        "NORDESTE", new Taxa(new BigDecimal("0.02")));
}`) }, "mapa", "dados", "mapa da regiao para um objeto de valor de uma classe so");

const SEG_CLASSES = Object.fromEntries(["Sudeste", "Sul", "CentroOeste", "Norte", "Nordeste"].map((r, i) => [
  `regiao/Seguro${r}.java`, J("regiao", `public class Seguro${r} implements SeguroRegiao {
    public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.0${i + 1}")); }
}`)]));
SEG_CLASSES["regiao/SeguroRegiao.java"] = J("regiao", `public interface SeguroRegiao { BigDecimal calcular(BigDecimal s); }`);

R("B06-mapa-classes", { ...SEG_CLASSES, "regiao/Seguros.java": J("regiao", `public class Seguros {
    static final Map<String, SeguroRegiao> MAPA = Map.of(
        "SUDESTE", new SeguroSudeste(), "SUL", new SeguroSul(), "CENTRO_OESTE", new SeguroCentroOeste(),
        "NORTE", new SeguroNorte(), "NORDESTE", new SeguroNordeste());
}`) }, "classes", "estrutura", "uma classe por regiao num mapa");

R("B07-mapa-lambdas", { "regiao/Seguro.java": J("regiao", `import java.util.function.UnaryOperator;

public class Seguro {
    static final Map<String, UnaryOperator<BigDecimal>> CALC = Map.of(
        "SUDESTE", s -> s.multiply(new BigDecimal("0.01")),
        "SUL", s -> s.multiply(new BigDecimal("0.01")),
        "CENTRO_OESTE", s -> s.multiply(new BigDecimal("0.015")),
        "NORTE", s -> s.multiply(new BigDecimal("0.025")),
        "NORDESTE", s -> s.multiply(new BigDecimal("0.02")));
}`) }, "mapa", "estrutura", "uma funcao por regiao");

R("B08-if-equals", { "regiao/Seguro.java": J("regiao", `public class Seguro {
    public BigDecimal taxa(String regiao) {
        if (regiao.equals("SUDESTE") || regiao.equals("SUL")) return new BigDecimal("0.01");
        if (regiao.equals("CENTRO_OESTE")) return new BigDecimal("0.015");
        if (regiao.equals("NORTE")) return new BigDecimal("0.025");
        if (regiao.equals("NORDESTE")) return new BigDecimal("0.02");
        throw new IllegalArgumentException("REGIAO_INVALIDA");
    }
}`) }, "switch", "condicional", "cadeia de if com x.equals(\"NORTE\") que devolve o numero");

R("B09-ofentries", { "regiao/Seguro.java": J("regiao", `import static java.util.Map.entry;

public class Seguro {
    static final Map<String, BigDecimal> TAXA = Map.ofEntries(
        entry("SUDESTE", new BigDecimal("0.01")),
        entry("SUL", new BigDecimal("0.01")),
        entry("CENTRO_OESTE", new BigDecimal("0.015")),
        entry("NORTE", new BigDecimal("0.025")),
        entry("NORDESTE", new BigDecimal("0.02")));
}`) }, "mapa", "dados", "Map.ofEntries com entry() importado");

R("B10-enum-e-parse", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));
    private final BigDecimal taxa;
    Regiao(BigDecimal taxa) { this.taxa = taxa; }
    public static Regiao de(String c) {
        return switch (c) {
            case "SUDESTE" -> Regiao.SUDESTE;
            case "SUL" -> Regiao.SUL;
            case "CENTRO_OESTE" -> Regiao.CENTRO_OESTE;
            case "NORTE" -> Regiao.NORTE;
            case "NORDESTE" -> Regiao.NORDESTE;
            default -> throw new IllegalArgumentException("REGIAO_INVALIDA");
        };
    }
}`) }, "enum-dados", "dados", "enum com dados e um switch que so traduz o texto");

R("B11-enum-corpo", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE { public BigDecimal seguro(BigDecimal s) { return s.multiply(new BigDecimal("0.01")); } },
    SUL { public BigDecimal seguro(BigDecimal s) { return s.multiply(new BigDecimal("0.01")); } },
    CENTRO_OESTE { public BigDecimal seguro(BigDecimal s) { return s.multiply(new BigDecimal("0.015")); } },
    NORTE { public BigDecimal seguro(BigDecimal s) { return s.multiply(new BigDecimal("0.025")); } },
    NORDESTE { public BigDecimal seguro(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); } };
    public abstract BigDecimal seguro(BigDecimal s);
}`) }, "enum-abstrato", "estrutura", "constante de enum com corpo");

R("B12-classes", { ...SEG_CLASSES }, "classes", "estrutura", "uma classe por regiao");

R("B13-switch-objeto", { ...SEG_CLASSES, "regiao/Fabrica.java": J("regiao", `public class Fabrica {
    public SeguroRegiao de(String r) {
        return switch (r) {
            case "SUDESTE" -> new SeguroSudeste();
            case "SUL" -> new SeguroSul();
            case "CENTRO_OESTE" -> new SeguroCentroOeste();
            case "NORTE" -> new SeguroNorte();
            case "NORDESTE" -> new SeguroNordeste();
            default -> throw new IllegalArgumentException();
        };
    }
}`) }, "classes", "estrutura", "switch que devolve uma classe por regiao");

R("B14-switch-numero", { "regiao/Seguro.java": J("regiao", `public class Seguro {
    public BigDecimal taxa(String r) {
        return switch (r) {
            case "SUDESTE", "SUL" -> new BigDecimal("0.01");
            case "CENTRO_OESTE" -> new BigDecimal("0.015");
            case "NORTE" -> new BigDecimal("0.025");
            case "NORDESTE" -> new BigDecimal("0.02");
            default -> throw new IllegalArgumentException();
        };
    }
}`) }, "switch", "condicional", "switch que devolve o numero");

R("B15-comentario", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE(new BigDecimal("0.01")),
    SUL(new BigDecimal("0.01")),
    CENTRO_OESTE(new BigDecimal("0.015")),
    NORTE(new BigDecimal("0.025")),
    NORDESTE(new BigDecimal("0.02"));
    // antes era: case NORTE -> new SeguroNorte(); trocado por dados
    private final BigDecimal taxa;
    Regiao(BigDecimal taxa) { this.taxa = taxa; }
}`) }, "enum-dados", "dados", "uma fabrica citada num comentario");

R("B16-enummap", { "regiao/Regiao.java": J("regiao", `public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }`),
  "regiao/Seguro.java": J("regiao", `public class Seguro {
    private final Map<Regiao, BigDecimal> taxa = new EnumMap<>(Regiao.class);
    public Seguro() {
        taxa.put(Regiao.SUDESTE, new BigDecimal("0.01"));
        taxa.put(Regiao.SUL, new BigDecimal("0.01"));
        taxa.put(Regiao.CENTRO_OESTE, new BigDecimal("0.015"));
        taxa.put(Regiao.NORTE, new BigDecimal("0.025"));
        taxa.put(Regiao.NORDESTE, new BigDecimal("0.02"));
    }
}`) }, "mapa", "dados", "EnumMap preenchido com put");

// ---------------------------------------------------------------- escrita
mkdirSync(saida, { recursive: true });
const COLS = ["P1_localizacao", "P1_selecao", "P2_localizacao", "P2_selecao", "P3_localizacao", "P3_selecao", "P4_localizacao", "P4_selecao", "P5_forma", "P5_proporcao"];
const linhas = ["pacote," + COLS.join(",") + ",porque"];
for (const c of casos) {
  for (const [arq, codigo] of Object.entries(c.arquivos)) {
    const caminho = join(saida, "pacotes", c.nome, "src/main/java/com/loja", arq);
    mkdirSync(join(caminho, ".."), { recursive: true });
    writeFileSync(caminho, codigo);
  }
  linhas.push([c.nome, ...COLS.map((k) => c.esperado[k] ?? "*"), `"${c.porque}"`].join(","));
}
writeFileSync(join(saida, "esperado.csv"), linhas.join("\n") + "\n");
console.log(`${casos.length} pacotes em ${saida}`);
