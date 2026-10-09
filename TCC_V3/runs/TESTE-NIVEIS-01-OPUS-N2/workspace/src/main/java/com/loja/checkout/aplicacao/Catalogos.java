package com.loja.checkout.aplicacao;

import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;

/** As opcoes que a loja oferece hoje, de cada tipo. */
public record Catalogos(
        Catalogo<ModalidadeEntrega> entregas,
        Catalogo<Cupom> cupons,
        Catalogo<NivelClube> niveis,
        Catalogo<FormaPagamento> pagamentos) {
}
