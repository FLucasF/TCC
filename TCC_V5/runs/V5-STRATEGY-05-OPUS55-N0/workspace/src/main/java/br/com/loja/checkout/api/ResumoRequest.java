package br.com.loja.checkout.api;

import java.math.BigDecimal;
import java.util.List;

/** Dados da compra enviados pelo site. Os códigos chegam como texto para que valores desconhecidos virem o erro certo. */
public record ResumoRequest(
        List<ItemRequest> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        BigDecimal parcelas,
        String nivelClube,
        String regiao) {
}
