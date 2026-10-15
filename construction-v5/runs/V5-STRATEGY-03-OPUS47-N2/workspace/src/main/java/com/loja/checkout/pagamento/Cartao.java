package com.loja.checkout.pagamento;

import com.loja.checkout.Dinheiro;
import com.loja.checkout.ErroPedido;

import java.math.BigDecimal;

public class Cartao implements FormaPagamento {
    private static final BigDecimal TAXA = new BigDecimal("0.0199");
    private static final int SEM_JUROS_ATE = 3;

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas < 1 || parcelas > 12) throw new ErroPedido("PARCELAMENTO_INVALIDO");
    }

    @Override
    public void validar(BigDecimal totalPedido) {}

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        if (parcelas <= SEM_JUROS_ATE) {
            BigDecimal parcela = Dinheiro.arredonda(
                    totalPedido.divide(BigDecimal.valueOf(parcelas), Dinheiro.MC));
            return new ResultadoPagamento(Dinheiro.ZERO, totalPedido, parcela);
        }
        BigDecimal um = BigDecimal.ONE;
        BigDecimal pot = um.add(TAXA).pow(parcelas, Dinheiro.MC);
        BigDecimal denom = um.subtract(um.divide(pot, Dinheiro.MC), Dinheiro.MC);
        BigDecimal parcelaBruta = totalPedido.multiply(TAXA, Dinheiro.MC).divide(denom, Dinheiro.MC);
        BigDecimal parcela = Dinheiro.arredonda(parcelaBruta);
        BigDecimal totalFinal = parcela.multiply(BigDecimal.valueOf(parcelas));
        BigDecimal ajuste = totalFinal.subtract(totalPedido);
        return new ResultadoPagamento(ajuste, totalFinal, parcela);
    }
}
