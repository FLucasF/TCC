package com.loja.checkout.coupon;

import com.loja.checkout.dto.ItemRequest;
import java.math.BigDecimal;
import java.util.List;

public interface Cupom {

    String codigo();

    boolean aplicavelPara(List<ItemRequest> itens, BigDecimal subtotalProdutos);

    BigDecimal calcularDesconto(List<ItemRequest> itens, BigDecimal subtotalProdutos, BigDecimal frete);
}
