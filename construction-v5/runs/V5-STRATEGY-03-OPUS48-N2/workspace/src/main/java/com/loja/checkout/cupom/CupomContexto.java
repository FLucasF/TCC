package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Item;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dados que um cupom pode precisar para decidir e calcular o desconto. Reune o
 * que o cupom mais exigente pede: itens (LEVE3PAGUE2), subtotal (MENOS50,
 * BEMVINDO10) e o frete ja definido (FRETEGRATIS).
 */
public record CupomContexto(BigDecimal subtotalProdutos, List<Item> itens, BigDecimal frete) {
}
