package com.loja.checkout.api;

import com.loja.checkout.model.Item;
import java.util.List;

public record ResumoRequest(
    List<Item> itens,
    String modalidadeEntrega,
    String cupom,
    String formaPagamento,
    Integer parcelas,
    String nivelClube,
    String regiao
) {
    public int getParcelas() {
        return parcelas != null ? parcelas : 1;
    }
}
