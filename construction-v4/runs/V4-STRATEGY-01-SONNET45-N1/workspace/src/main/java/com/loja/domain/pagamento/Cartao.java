package com.loja.domain.pagamento;

import com.loja.util.Dinheiro;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;

    @Override
    public BigDecimal calcularAjuste(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return new BigDecimal("0.00");
        }

        BigDecimal valorComJuros = calcularValorComJuros(totalPedido, parcelas);
        return Dinheiro.arredondar(valorComJuros.subtract(totalPedido));
    }

    @Override
    public BigDecimal calcularValorParcela(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            return Dinheiro.arredondar(totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
        }

        BigDecimal umMaisTaxa = BigDecimal.ONE.add(TAXA_JUROS);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = umMaisTaxaElevado.subtract(BigDecimal.ONE);
        BigDecimal numerador = TAXA_JUROS.multiply(umMaisTaxaElevado);
        BigDecimal fatorTabela = numerador.divide(denominador, 10, RoundingMode.HALF_EVEN);

        BigDecimal valorParcela = totalPedido.multiply(fatorTabela);
        return Dinheiro.arredondar(valorParcela);
    }

    @Override
    public boolean isParcelamentoValido(int parcelas) {
        return parcelas >= 1 && parcelas <= 12;
    }

    @Override
    public boolean isDisponivel(BigDecimal totalPedido) {
        return true;
    }

    private BigDecimal calcularValorComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = calcularValorParcela(totalPedido, parcelas);
        return valorParcela.multiply(BigDecimal.valueOf(parcelas));
    }
}
