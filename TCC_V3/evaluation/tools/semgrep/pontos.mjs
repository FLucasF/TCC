// Os pontos do gabarito do Strategy e os nomes dos casos de cada um. Usado pelo
// gerador das regras e pelo classificador, para os dois nunca divergirem.
// Os nomes sao os mesmos nos enunciados do V3 e do V4.

export const PONTOS = [
  { id: "p1", casos: ["ECONOMICA", "EXPRESSA", "RETIRADA_LOJA", "MOTOBOY"], classes: "Economica|Expressa|Retirada|Motoboy" },
  { id: "p2", casos: ["BEMVINDO10", "MENOS50", "FRETEGRATIS", "LEVE3PAGUE2"], classes: "BemVindo|Bemvindo|Menos50|FreteGratis|Fretegratis|Leve3" },
  { id: "p3", casos: ["PIX", "CARTAO", "BOLETO"], classes: "Pix|Cartao|Boleto" },
  { id: "p4", casos: ["BRONZE", "PRATA", "OURO"], classes: "Bronze|Prata|Ouro" },
];

export const REGIOES = ["SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE"];
export const REGIOES_CLASSE = "Sudeste|Sul|CentroOeste|Norte|Nordeste";
