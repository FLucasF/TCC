package br.tcc.checkout.dominio.pagamento;

import br.tcc.checkout.util.Arredondador;
import java.math.BigDecimal;

public class FormaPagamentoCartao implements FormaPagamento {
    @Override
    public String getCodigo() {
        return "CARTAO";
    }

    @Override
    public boolean ehValida(Integer parcelas, BigDecimal totalPedido) {
        int p = parcelas != null ? parcelas : 1;
        return p >= 1 && p <= 12;
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, Integer parcelas) {
        int p = parcelas != null ? parcelas : 1;

        if (p <= 3) {
            return calcularSemJuros(totalPedido, p);
        } else {
            return calcularComJuros(totalPedido, p);
        }
    }

    private ResultadoPagamento calcularSemJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = totalPedido.divide(new BigDecimal(parcelas), 2, java.math.RoundingMode.HALF_EVEN);
        valorParcela = Arredondador.arredondarParaCentavos(valorParcela);
        return new ResultadoPagamento(totalPedido, valorParcela, Arredondador.arredondarParaCentavos(BigDecimal.ZERO));
    }

    private ResultadoPagamento calcularComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal taxaMensal = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = taxaMensal.add(BigDecimal.ONE);

        BigDecimal potencia = umMaisTaxa.pow(-parcelas, new java.math.MathContext(10));

        BigDecimal numerador = totalPedido.multiply(taxaMensal);
        BigDecimal denominador = BigDecimal.ONE.subtract(potencia);

        BigDecimal valorParcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        valorParcela = Arredondador.arredondarParaCentavos(valorParcela);

        BigDecimal totalFinal = valorParcela.multiply(new BigDecimal(parcelas));
        totalFinal = Arredondador.arredondarParaCentavos(totalFinal);

        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        ajuste = Arredondador.arredondarParaCentavos(ajuste);

        return new ResultadoPagamento(totalFinal, valorParcela, ajuste);
    }
}
