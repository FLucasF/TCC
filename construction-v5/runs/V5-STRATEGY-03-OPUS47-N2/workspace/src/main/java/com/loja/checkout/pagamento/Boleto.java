package com.loja.checkout.pagamento;

import com.loja.checkout.ErroPedido;

import java.math.BigDecimal;

public class Boleto implements FormaPagamento {
    private static final BigDecimal TARIFA = new BigDecimal("3.49");
    private static final BigDecimal LIMITE = new BigDecimal("1000.00");

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) throw new ErroPedido("PARCELAMENTO_INVALIDO");
    }

    @Override
    public void validar(BigDecimal totalPedido) {
        if (totalPedido.compareTo(LIMITE) > 0) {
            throw new ErroPedido("FORMA_PAGAMENTO_INDISPONIVEL");
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal totalFinal = totalPedido.add(TARIFA);
        return new ResultadoPagamento(TARIFA, totalFinal, totalFinal);
    }
}
