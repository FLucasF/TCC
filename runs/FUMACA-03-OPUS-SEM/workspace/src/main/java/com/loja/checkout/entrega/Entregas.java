package com.loja.checkout.entrega;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.Pedido;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Catalogo das opcoes de entrega disponiveis no sistema. */
@Component
public class Entregas {

    private final Map<String, ModalidadeEntrega> porCodigo = new HashMap<>();

    public Entregas(List<ModalidadeEntrega> modalidades) {
        for (ModalidadeEntrega modalidade : modalidades) {
            porCodigo.put(modalidade.codigo(), modalidade);
        }
    }

    /** Resolve o codigo enviado pelo site e confere se a modalidade atende o pedido. */
    public ModalidadeEntrega resolver(String codigo, Pedido pedido) {
        ModalidadeEntrega modalidade = codigo == null ? null : porCodigo.get(codigo);
        if (modalidade == null) {
            throw new ErroCheckout(CodigoErro.MODALIDADE_INVALIDA);
        }
        if (!modalidade.atende(pedido)) {
            throw new ErroCheckout(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        return modalidade;
    }
}
