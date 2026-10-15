package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Cartao implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new com.loja.checkout.servico.ErroNegocioException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= 3) {
            return calcularSemJuros(totalPedido, parcelas);
        } else {
            return calcularComJuros(totalPedido, parcelas);
        }
    }

    private ResultadoPagamento calcularSemJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal valorParcela = Arredondamento.arredondar(
            totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN)
        );
        return new ResultadoPagamento(
            totalPedido,
            parcelas,
            valorParcela,
            new BigDecimal("0.00")
        );
    }

    private ResultadoPagamento calcularComJuros(BigDecimal totalPedido, int parcelas) {
        BigDecimal taxa = new BigDecimal("0.0199");
        BigDecimal umMaisTaxa = BigDecimal.ONE.add(taxa);
        BigDecimal umMaisTaxaElevado = umMaisTaxa.pow(parcelas);
        BigDecimal denominador = BigDecimal.ONE.subtract(
            BigDecimal.ONE.divide(umMaisTaxaElevado, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal valorParcela = Arredondamento.arredondar(
            totalPedido.multiply(taxa).divide(denominador, 10, RoundingMode.HALF_EVEN)
        );

        BigDecimal totalFinal = valorParcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);

        return new ResultadoPagamento(
            totalFinal,
            parcelas,
            valorParcela,
            ajuste
        );
    }
}
