// Os pontos do gabarito do Strategy e os nomes dos casos de cada um. Usado pelo
// gerador das regras e pelo classificador, para os dois nunca divergirem.
// Os nomes sao os mesmos nos enunciados do V3 e do V4.
//
// "casos": como o caso aparece escrito no enunciado (o texto que o site manda).
// "sinonimos": o mesmo caso com outro nome no codigo, em ingles (09/10): um agente
// pode escrever enum Tier { BRONZE, SILVER, GOLD } e traduzir o texto do site; sem os
// sinonimos, um remendo "tier == GOLD" passaria em silencio como "isolado".
// "classes": pedacos de nome de classe ou de variavel que indicam o caso (NivelOuro,
// EntregaMotoboy, PagamentoPix): servem para o instanceof, o switch com pattern
// matching (case Ouro o ->) e a fabrica que devolve um objeto (case "OURO" -> ouro;).
// REGIOES, REGIOES_SINONIMOS e REGIOES_CLASSE: o mesmo para o P5 (o seguro por regiao).

export const PONTOS = [
  { id: "p1", casos: ["ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY"],
    sinonimos: { ECONOMY: "ECONOMICA", EXPRESS: "EXPRESSA", STORE_PICKUP: "RETIRADA_LOJA", IN_STORE_PICKUP: "RETIRADA_LOJA", PICKUP: "RETIRADA_LOJA", MOTORCYCLE: "MOTOBOY", COURIER: "MOTOBOY" },
    classes: "Economica|Expressa|Retirada|Motoboy|Economy|Express|Pickup|Motorcycle|Courier" },
  { id: "p2", casos: ["BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"],
    sinonimos: { WELCOME10: "BEMVINDO10", MINUS50: "MENOS50", FIFTY_OFF: "MENOS50", FREE_SHIPPING: "FRETEGRATIS", FREESHIPPING: "FRETEGRATIS", BUY3PAY2: "LEVE3PAGUE2", TAKE3PAY2: "LEVE3PAGUE2" },
    classes: "BemVindo|Bemvindo|Menos50|FreteGratis|Fretegratis|Leve3|Welcome|Minus50|Buy3|Take3" },
  { id: "p3", casos: ["PIX", "CARTAO", "BOLETO"],
    sinonimos: { CARD: "CARTAO", CREDIT_CARD: "CARTAO", CREDITCARD: "CARTAO", BANK_SLIP: "BOLETO", BANKSLIP: "BOLETO", BILLET: "BOLETO" },
    classes: "Pix|Cartao|Boleto|Card|BankSlip|Billet" },
  { id: "p4", casos: ["BRONZE", "PRATA", "OURO"],
    sinonimos: { SILVER: "PRATA", GOLD: "OURO" },
    classes: "Bronze|Prata|Ouro|Silver|Gold" },
];

export const REGIOES = ["SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE"];
export const REGIOES_SINONIMOS = { SOUTHEAST: "SUDESTE", SOUTH: "SUL", MIDWEST: "CENTRO_OESTE", CENTER_WEST: "CENTRO_OESTE", CENTRAL_WEST: "CENTRO_OESTE", NORTH: "NORTE", NORTHEAST: "NORDESTE" };
export const REGIOES_CLASSE = "Sudeste|Sul|CentroOeste|Norte|Nordeste|Southeast|South|Midwest|CenterWest|North|Northeast";

// Todos os nomes de um ponto (os do enunciado e os sinonimos), e o caso de cada nome.
export const nomes = (p) => [...p.casos, ...Object.keys(p.sinonimos ?? {})];
export const casoDe = (p, nome) => p.sinonimos?.[nome] ?? nome;
export const NOMES_REGIOES = [...REGIOES, ...Object.keys(REGIOES_SINONIMOS)];
