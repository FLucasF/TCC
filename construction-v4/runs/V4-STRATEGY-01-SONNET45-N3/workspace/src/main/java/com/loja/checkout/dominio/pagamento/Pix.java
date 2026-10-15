package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public boolean estaDisponivel(BigDecimal totalPedido) {
        return true;
    }

    @Override
    public void validarParcelas(int parcelas) {
        if (parcelas != 1) {
            throw new com.loja.checkout.servico.ErroNegocioException("PARCELAMENTO_INVALIDO");
        }
    }

    @Override
    public ResultadoPagamento calcular(BigDecimal totalPedido, int parcelas) {
        BigDecimal desconto = Arredondamento.arredondar(
            totalPedido.multiply(new BigDecimal("0.05"))
        );
        BigDecimal totalFinal = totalPedido.subtract(desconto);
        return new ResultadoPagamento(
            totalFinal,
            1,
            totalFinal,
            desconto.negate()
        );
    }
}
