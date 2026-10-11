package com.loja.checkout.dominio.entrega;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Catalogo das opcoes de entrega disponiveis no sistema. */
@Component
public class CatalogoEntrega extends Catalogo<ModalidadeEntrega> {

    public CatalogoEntrega(List<ModalidadeEntrega> modalidades) {
        super(modalidades, ModalidadeEntrega::codigo);
    }
}
