package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

public record ContextoCupom(Carrinho carrinho, BigDecimal frete) {
}
