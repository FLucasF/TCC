package com.loja.checkout.calculo.pagamento;

import com.loja.checkout.util.Arredondador;
import java.math.BigDecimal;

public class PagamentoCartao implements PagamentoCalculador {
    private int parcelas;
    private BigDecimal totalPedidoArmazenado;
    private BigDecimal parcelaCalculada;

    public PagamentoCartao(int parcelas) {
        this.parcelas = parcelas;
    }

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido) {
        this.totalPedidoArmazenado = totalPedido;

        if (parcelas <= 3) {
            this.parcelaCalculada = null;
            return Arredondador.arredondar(BigDecimal.ZERO);
        }

        BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);

        BigDecimal denominador = umMaisTaxaElevado.subtract(BigDecimal.ONE);
        BigDecimal numerador = totalPedido.multiply(taxaMensal).multiply(umMaisTaxaElevado);

        BigDecimal parcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        parcela = Arredondador.arredondar(parcela);
        this.parcelaCalculada = parcela;

        BigDecimal totalComJuros = parcela.multiply(BigDecimal.valueOf(parcelas));
        return totalComJuros.subtract(totalPedido);
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalFinal, int nParcelas) {
        if (nParcelas <= 3) {
            return Arredondador.arredondar(totalFinal.divide(BigDecimal.valueOf(nParcelas), 10, java.math.RoundingMode.HALF_EVEN));
        }

        if (parcelaCalculada != null) {
            return parcelaCalculada;
        }

        BigDecimal taxaMensal = BigDecimal.valueOf(0.0199);
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxaMensal);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(nParcelas);

        BigDecimal denominador = umMaisTaxaElevado.subtract(BigDecimal.ONE);
        BigDecimal numerador = totalPedidoArmazenado.multiply(taxaMensal).multiply(umMaisTaxaElevado);

        BigDecimal parcela = numerador.divide(denominador, 10, java.math.RoundingMode.HALF_EVEN);
        return Arredondador.arredondar(parcela);
    }

    @Override
    public int getParcelasMaximas() {
        return 12;
    }
}
