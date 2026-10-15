package br.com.loja.checkout.dominio.pagamento;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Cartao implements FormaPagamento {

    private static final int PARCELAS_MAXIMAS = 12;
    private static final int PARCELAS_SEM_JUROS = 3;
    /** Taxa mensal de juros, como fração (1,99% ao mês). */
    private static final BigDecimal TAXA_MENSAL = Dinheiro.de("0.0199");

    @Override
    public String codigo() {
        return "CARTAO";
    }

    @Override
    public boolean permiteParcelas(int parcelas) {
        return parcelas >= 1 && parcelas <= PARCELAS_MAXIMAS;
    }

    @Override
    public boolean atende(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public Cobranca cobrar(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal parcela = Dinheiro.centavos(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.CALCULO));
            return new Cobranca(Dinheiro.centavos(totalPedido), parcelas, parcela);
        }
        BigDecimal parcela = Dinheiro.centavos(parcelaPrice(totalPedido, parcelas));
        BigDecimal totalFinal = Dinheiro.centavos(parcela.multiply(BigDecimal.valueOf(parcelas)));
        return new Cobranca(totalFinal, parcelas, parcela);
    }

    /** Tabela Price: total × taxa ÷ (1 − (1 + taxa)^−parcelas). */
    private BigDecimal parcelaPrice(BigDecimal totalPedido, int parcelas) {
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL).pow(parcelas, Dinheiro.CALCULO);
        BigDecimal divisor = BigDecimal.ONE
                .subtract(BigDecimal.ONE.divide(fator, Dinheiro.CALCULO));
        return totalPedido.multiply(TAXA_MENSAL).divide(divisor, Dinheiro.CALCULO);
    }
}
