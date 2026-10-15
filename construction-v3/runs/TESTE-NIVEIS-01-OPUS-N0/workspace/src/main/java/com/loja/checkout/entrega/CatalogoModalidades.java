package com.loja.checkout.entrega;

import com.loja.checkout.comum.Catalogo;
import com.loja.checkout.comum.CodigoErro;
import java.util.List;
import org.springframework.stereotype.Component;

/** Todas as modalidades de entrega disponiveis no sistema. */
@Component
public class CatalogoModalidades extends Catalogo<ModalidadeEntrega> {

    public CatalogoModalidades(List<ModalidadeEntrega> modalidades) {
        super(modalidades, CodigoErro.MODALIDADE_INVALIDA);
    }
}
