package com.loja.checkout.pagamento;

import com.loja.checkout.Arredondamento;
import com.loja.checkout.CheckoutException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

@Component
public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA_JUROS = new BigDecimal("0.0199");
    private static final int PARCELAS_SEM_JUROS = 3;
    private static final int PARCELAS_MAX = 12;

    @Override
    public String codigo() { return "CARTAO"; }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > PARCELAS_MAX) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public void validarDisponibilidade(BigDecimal totalPedido) {}

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= PARCELAS_SEM_JUROS) {
            BigDecimal valorParcela = Arredondamento.arredondar(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), 10, RoundingMode.HALF_EVEN));
            return new ResultadoPagamento(Arredondamento.ZERO, totalPedido, valorParcela);
        }

        BigDecimal r = TAXA_JUROS;
        BigDecimal onePlusR = BigDecimal.ONE.add(r);
        BigDecimal onePlusRtoN = onePlusR.pow(parcelas, new MathContext(20, RoundingMode.HALF_EVEN));
        BigDecimal fator = onePlusRtoN.subtract(BigDecimal.ONE)
                .divide(onePlusRtoN, 20, RoundingMode.HALF_EVEN);
        BigDecimal valorParcela = Arredondamento.arredondar(
                totalPedido.multiply(r).divide(fator, 20, RoundingMode.HALF_EVEN));
        BigDecimal totalFinal = Arredondamento.arredondar(
                valorParcela.multiply(BigDecimal.valueOf(parcelas)));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, valorParcela);
    }
}
