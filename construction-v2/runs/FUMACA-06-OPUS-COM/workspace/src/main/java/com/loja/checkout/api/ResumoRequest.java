package com.loja.checkout.api;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.SolicitacaoResumo;
import java.util.List;

public record ResumoRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas) {

    SolicitacaoResumo paraSolicitacao() {
        return new SolicitacaoResumo(itens, modalidadeEntrega, cupom, formaPagamento, parcelas);
    }
}
