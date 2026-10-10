package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.ResultadoPagamento;
import java.math.BigDecimal;

public class Boleto implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return totalPedido.compareTo(new BigDecimal("1000.00")) <= 0;
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new com.loja.checkout.servico.ErroNegocioException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal tarifa = new BigDecimal("3.49");
        BigDecimal totalFinal = totalPedido.add(tarifa);
        return new ResultadoPagamento(
            totalFinal,
            1,
            totalFinal,
            tarifa
        );
    }
}
