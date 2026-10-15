package com.loja.checkout.domain.pagamento;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Pix implements FormaPagamento {

    @Override
    public AjustePagamento calcularAjuste(BigDecimal total, int parcelas) {
        BigDecimal desconto = Arredondamento.arredondar(total.multiply(new BigDecimal("0.05")));
        BigDecimal totalFinal = total.subtract(desconto);
        BigDecimal ajuste = totalFinal.subtract(total);

        return new AjustePagamento(ajuste, totalFinal, totalFinal);
    }

    @Override
    public boolean validarParcelas(int parcelas) {
        return parcelas == 1;
    }

    @Override
    public boolean estaDisponivel(BigDecimal total) {
        return true;
    }
}
