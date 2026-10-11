package com.loja.cupom;

import com.loja.model.ItemPedido;
import com.loja.util.Arredondamento;
import java.math.BigDecimal;
import java.util.List;

public class CupomMenos50 implements Cupom {
    private static final BigDecimal MINIMO_NECESSARIO = BigDecimal.valueOf(300.00);
    private static final BigDecimal DESCONTO_FIXO = BigDecimal.valueOf(50.00);

    @Override
    public BigDecimal calcularDesconto(List<ItemPedido> itens, BigDecimal subtotal, BigDecimal frete) {
        return Arredondamento.arredondarParaPar(DESCONTO_FIXO);
    }

    @Override
    public boolean podeAplicar(List<ItemPedido> itens, BigDecimal subtotal) {
        return subtotal.compareTo(MINIMO_NECESSARIO) >= 0;
    }

    @Override
    public String obterCodigo() {
        return "MENOS50";
    }
}
