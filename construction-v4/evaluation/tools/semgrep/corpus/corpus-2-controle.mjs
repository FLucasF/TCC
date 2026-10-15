// Corpus 2, de CONTROLE: formas que as regras novas nunca viram, escritas depois de
// fechar o corpus 1 e com a resposta da regua decidida antes de rodar. Inclui, de
// proposito, formas que eu prevejo que ainda falhem (marcadas "previsao: falha").
// Resultado (09/10): 42 de 44; as duas falhas foram as previstas (D03, a classe anonima, e
// D06, a configuracao), corrigidas depois. O D06 ganhou o application.properties que a
// primeira versao do caso esqueceu de criar (erro na montagem do caso, nao no Semgrep).
//
// Uso: node corpus-2-controle.mjs <pasta-saida>

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

pacote("C01-instanceof-NivelOuro", { ...CLASSES, "servico/Brinde.java": J("servico", `import com.loja.clube.*;

public class Brinde {
    public boolean ganha(Nivel n, BigDecimal s) {
        return n instanceof NivelOuro && s.compareTo(new BigDecimal("500")) > 0;
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "instanceof NivelOuro no servico");

pacote("C02-beans-por-nome", {
  "clube/Nivel.java": J("clube", `public interface Nivel { BigDecimal credito(BigDecimal s); }`),
  "clube/Bronze.java": J("clube", `@org.springframework.stereotype.Component("BRONZE")
public class Bronze implements Nivel { public BigDecimal credito(BigDecimal s) { return BigDecimal.ZERO; } }`),
  "clube/Prata.java": J("clube", `@org.springframework.stereotype.Component("PRATA")
public class Prata implements Nivel { public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); } }`),
  "clube/Ouro.java": J("clube", `@org.springframework.stereotype.Component("OURO")
public class Ouro implements Nivel { public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.05")); } }`),
  "clube/Servico.java": J("clube", `public class Servico {
    private final Map<String, Nivel> niveis;
    public Servico(Map<String, Nivel> niveis) { this.niveis = niveis; }
    public BigDecimal credito(String codigo, BigDecimal s) {
        Nivel n = niveis.get(codigo);
        if (n == null) throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        return n.credito(s);
    }
}`),
}, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "beans do Spring pelo nome");

pacote("C03-enum-corpo", { "clube/NivelClube.java": J("clube", `public enum NivelClube {
    BRONZE { public BigDecimal credito(BigDecimal s) { return BigDecimal.ZERO; } },
    PRATA { public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); } },
    OURO {
        public BigDecimal credito(BigDecimal s) { return s.multiply(new BigDecimal("0.05")); }
        @Override public boolean freteGratis() { return true; }
    };
    public abstract BigDecimal credito(BigDecimal s);
    public boolean freteGratis() { return false; }
}`), "servico/S.java": J("servico", `import com.loja.clube.NivelClube;

public class S {
    public BigDecimal frete(String c, BigDecimal f) { return NivelClube.valueOf(c).freteGratis() ? BigDecimal.ZERO : f; }
}`) }, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "enum com corpo por nivel, valueOf");

pacote("C04-switch-this", { "clube/NivelClube.java": J("clube", `public enum NivelClube {
    BRONZE, PRATA, OURO;
    public BigDecimal credito(BigDecimal s) {
        switch (this) {
            case PRATA: return s.multiply(new BigDecimal("0.02"));
            case OURO: return s.multiply(new BigDecimal("0.05"));
            default: return BigDecimal.ZERO;
        }
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch (this) dentro do enum que calcula (regua 2.2)");

pacote("C05-mapa-mais-remendo", { "clube/Servico.java": J("clube", `public class Servico {
    private static final Map<String, BigDecimal> CREDITO = Map.of(
        "BRONZE", BigDecimal.ZERO, "PRATA", new BigDecimal("0.02"), "OURO", new BigDecimal("0.05"));
    public BigDecimal credito(String n, BigDecimal s) { return s.multiply(CREDITO.get(n)); }
    public BigDecimal frete(String n, BigDecimal f) {
        if (n.equals("OURO")) return BigDecimal.ZERO;
        return f;
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "mapa de dados, mas o frete do OURO por if");

pacote("C06-else-if-atribui", { ...CLASSES, "clube/Fabrica.java": J("clube", `public class Fabrica {
    public Nivel de(String c) {
        Nivel estrategia;
        if ("BRONZE".equals(c)) {
            estrategia = new NivelBronze();
        } else if ("PRATA".equals(c)) {
            estrategia = new NivelPrata();
        } else if ("OURO".equals(c)) {
            estrategia = new NivelOuro();
        } else {
            throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        }
        return estrategia;
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "fabrica por else-if que atribui");

pacote("C07-switch-campos", { ...CLASSES, "clube/Fabrica.java": J("clube", `public class Fabrica {
    private final NivelBronze bronze = new NivelBronze();
    private final NivelPrata prata = new NivelPrata();
    private final NivelOuro ouro = new NivelOuro();
    public Nivel de(String c) {
        return switch (c) {
            case "BRONZE" -> bronze;
            case "PRATA" -> prata;
            case "OURO" -> ouro;
            default -> throw new IllegalArgumentException();
        };
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "switch que devolve campos ja criados");

pacote("C08-set-validos", { "clube/NivelClube.java": NIVEL_ENUM, "clube/Entrada.java": J("clube", `public class Entrada {
    private static final Set<String> VALIDOS = Set.of("BRONZE", "PRATA", "OURO");
    public NivelClube ler(String c) {
        if (!VALIDOS.contains(c)) throw new IllegalArgumentException("NIVEL_CLUBE_INVALIDO");
        return NivelClube.valueOf(c);
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "conjunto com todos os validos");

pacote("C09-badrequest-motoboy", {
  "entrega/Modalidade.java": J("entrega", `public enum Modalidade {
    ECONOMICA(new BigDecimal("12"), 7), EXPRESSA(new BigDecimal("25"), 2), RETIRADA_LOJA(BigDecimal.ZERO, 1), MOTOBOY(new BigDecimal("18"), 0);
    private final BigDecimal base; private final int prazo;
    Modalidade(BigDecimal base, int prazo) { this.base = base; this.prazo = prazo; }
    public BigDecimal base() { return base; }
}`),
  "entrega/Controller.java": J("entrega", `import org.springframework.http.ResponseEntity;

public class Controller {
    public ResponseEntity<?> resumo(Modalidade modalidade, BigDecimal peso) {
        if (modalidade == Modalidade.MOTOBOY && peso.compareTo(new BigDecimal("5")) > 0) {
            return ResponseEntity.badRequest().body(Map.of("erro", "MODALIDADE_INDISPONIVEL"));
        }
        return ResponseEntity.ok(modalidade.base());
    }
}`),
}, { P1_localizacao: "espalhado", P1_selecao: "consulta" }, "recusa por badRequest, sem throw");

pacote("C10-switch-chaves", { "clube/Servico.java": J("clube", `public class Servico {
    public BigDecimal credito(String nivel, BigDecimal s) {
        BigDecimal pct;
        switch (nivel) {
            case "PRATA": {
                pct = new BigDecimal("0.02");
                break;
            }
            case "OURO": {
                pct = new BigDecimal("0.05");
                break;
            }
            default:
                pct = BigDecimal.ZERO;
        }
        return s.multiply(pct);
    }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "switch antigo com blocos");

pacote("C11-ignorecase", { "clube/NivelClube.java": NIVEL_ENUM, "servico/Frete.java": J("servico", `public class Frete {
    public BigDecimal frete(String nivel, BigDecimal f) { return "OURO".equalsIgnoreCase(nivel) ? BigDecimal.ZERO : f; }
}`) }, { P4_localizacao: "espalhado", P4_selecao: "condicional-no-calculo" }, "equalsIgnoreCase no remendo");

pacote("C12-stream", { "clube/NivelClube.java": J("clube", `public enum NivelClube {
    BRONZE("BRONZE", BigDecimal.ZERO), PRATA("PRATA", new BigDecimal("0.02")), OURO("OURO", new BigDecimal("0.05"));
    private final String codigo; private final BigDecimal pct;
    NivelClube(String codigo, BigDecimal pct) { this.codigo = codigo; this.pct = pct; }
    public static Optional<NivelClube> de(String c) {
        return Arrays.stream(values()).filter(n -> n.codigo.equals(c)).findFirst();
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "consulta" }, "laco generico por stream");

pacote("C13-switch-supplier", { ...CLASSES, "clube/Fabrica.java": J("clube", `public class Fabrica {
    public Supplier<Nivel> de(String c) {
        return switch (c) {
            case "BRONZE" -> NivelBronze::new;
            case "PRATA" -> NivelPrata::new;
            case "OURO" -> NivelOuro::new;
            default -> throw new IllegalArgumentException();
        };
    }
}`) }, { P4_localizacao: "isolado", P4_selecao: "condicional-unica" }, "switch que devolve o construtor");

pacote("C14-instanceof-cupom", {
  "cupom/Cupom.java": J("cupom", `public interface Cupom { BigDecimal desconto(BigDecimal s); }`),
  "cupom/BemVindo10.java": J("cupom", `public class BemVindo10 implements Cupom { public BigDecimal desconto(BigDecimal s) { return s.multiply(new BigDecimal("0.1")); } }`),
  "cupom/Leve3Pague2.java": J("cupom", `public class Leve3Pague2 implements Cupom { public BigDecimal desconto(BigDecimal s) { return BigDecimal.ZERO; } }`),
  "cupom/Servico.java": J("cupom", `public class Servico {
    public BigDecimal desconto(Cupom c, BigDecimal s, List<BigDecimal> itens) {
        if (c instanceof Leve3Pague2) {
            return itens.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return c.desconto(s);
    }
}`),
}, { P2_localizacao: "espalhado", P2_selecao: "condicional-no-calculo" }, "o cupom LEVE3PAGUE2 calculado por instanceof no servico");

// ---------------------------------------------------------------- P5
const R = (nome, arquivos, forma, proporcao, porque) => pacote(nome, arquivos, { P5_forma: forma, P5_proporcao: proporcao }, porque);
R("D01-enum-dois-args", { "regiao/Regiao.java": J("regiao", `public enum Regiao {
    SUDESTE("Sudeste",
            new BigDecimal("0.01")),
    SUL("Sul", new BigDecimal("0.01")),
    CENTRO_OESTE("Centro-Oeste", new BigDecimal("0.015")),
    NORTE("Norte", new BigDecimal("0.025")),
    NORDESTE("Nordeste", new BigDecimal("0.02"));
    private final String nome; private final BigDecimal taxa;
    Regiao(String nome, BigDecimal taxa) { this.nome = nome; this.taxa = taxa; }
}`) }, "enum-dados", "dados", "enum com dois argumentos, um quebrado em duas linhas");

R("D02-switch-dois-pontos", { "regiao/Regiao.java": J("regiao", `public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }`),
  "regiao/Seguro.java": J("regiao", `public class Seguro {
    public BigDecimal taxa(Regiao r) {
        switch (r) {
            case SUDESTE:
            case SUL:
                return new BigDecimal("0.01");
            case CENTRO_OESTE:
                return new BigDecimal("0.015");
            case NORTE:
                return new BigDecimal("0.025");
            default:
                return new BigDecimal("0.02");
        }
    }
}`) }, "switch", "condicional", "switch com dois pontos sobre o enum");

R("D03-anonimas", { "regiao/Seguro.java": J("regiao", `public abstract class Seguro {
    public abstract BigDecimal calcular(BigDecimal s);
    public static final Map<String, Seguro> POR_REGIAO = Map.of(
        "SUDESTE", new Seguro() { public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.01")); } },
        "SUL", new Seguro() { public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.01")); } },
        "CENTRO_OESTE", new Seguro() { public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.015")); } },
        "NORTE", new Seguro() { public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.025")); } },
        "NORDESTE", new Seguro() { public BigDecimal calcular(BigDecimal s) { return s.multiply(new BigDecimal("0.02")); } });
}`) }, "mapa", "estrutura", "classe anonima por regiao (previsao: falha, parece uma classe so)");

R("D04-records", { "regiao/Taxa.java": J("regiao", `public record Taxa(String regiao, BigDecimal pct) {
    public static final List<Taxa> TODAS = List.of(
        new Taxa("SUDESTE", new BigDecimal("0.01")), new Taxa("SUL", new BigDecimal("0.01")),
        new Taxa("CENTRO_OESTE", new BigDecimal("0.015")), new Taxa("NORTE", new BigDecimal("0.025")),
        new Taxa("NORDESTE", new BigDecimal("0.02")));
}`) }, "outro", "dados", "lista de records");

R("D05-if-enum", { "regiao/Regiao.java": J("regiao", `public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }`),
  "regiao/Seguro.java": J("regiao", `public class Seguro {
    public BigDecimal taxa(Regiao r) {
        if (r == Regiao.NORTE) return new BigDecimal("0.025");
        if (r == Regiao.NORDESTE) return new BigDecimal("0.02");
        if (r == Regiao.CENTRO_OESTE) return new BigDecimal("0.015");
        return new BigDecimal("0.01");
    }
}`) }, "switch", "condicional", "cadeia de if sobre o enum");

R("D06-properties", { "/src/main/resources/application.properties": "# taxas do seguro por regiao\nseguro.taxas.sudeste=0.01\nseguro.taxas.sul=0.01\nseguro.taxas.centro-oeste=0.015\nseguro.taxas.norte=0.025\nseguro.taxas.nordeste=0.02\n", "regiao/SeguroConfig.java": J("regiao", `@org.springframework.boot.context.properties.ConfigurationProperties("seguro")
public record SeguroConfig(Map<String, BigDecimal> taxas) {
    public BigDecimal taxa(String regiao) { return taxas.get(regiao.toLowerCase()); }
}`) }, "outro", "dados", "as taxas no application.properties (previsao: falha, o Semgrep so le .java)");

R("D07-sealed-records", { "regiao/Regiao.java": J("regiao", `public sealed interface Regiao permits Sudeste, Sul, CentroOeste, Norte, Nordeste {
    BigDecimal taxa();
}`), "regiao/Sudeste.java": J("regiao", `public record Sudeste() implements Regiao { public BigDecimal taxa() { return new BigDecimal("0.01"); } }`),
  "regiao/Sul.java": J("regiao", `public record Sul() implements Regiao { public BigDecimal taxa() { return new BigDecimal("0.01"); } }`),
  "regiao/CentroOeste.java": J("regiao", `public record CentroOeste() implements Regiao { public BigDecimal taxa() { return new BigDecimal("0.015"); } }`),
  "regiao/Norte.java": J("regiao", `public record Norte() implements Regiao { public BigDecimal taxa() { return new BigDecimal("0.025"); } }`),
  "regiao/Nordeste.java": J("regiao", `public record Nordeste() implements Regiao { public BigDecimal taxa() { return new BigDecimal("0.02"); } }`),
}, "classes", "estrutura", "um record por regiao numa interface selada");

R("D08-mapa-enum", { "regiao/Regiao.java": J("regiao", `public enum Regiao { SUDESTE, SUL, CENTRO_OESTE, NORTE, NORDESTE }`),
  "regiao/Seguro.java": J("regiao", `public class Seguro {
    static final Map<Regiao, BigDecimal> TAXA = Map.of(
        Regiao.SUDESTE, new BigDecimal("0.01"), Regiao.SUL, new BigDecimal("0.01"),
        Regiao.CENTRO_OESTE, new BigDecimal("0.015"), Regiao.NORTE, new BigDecimal("0.025"),
        Regiao.NORDESTE, new BigDecimal("0.02"));
}`) }, "mapa", "dados", "Map.of com chaves do enum");

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
