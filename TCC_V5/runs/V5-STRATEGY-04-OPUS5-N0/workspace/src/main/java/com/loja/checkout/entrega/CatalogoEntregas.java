package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

/** Todas as modalidades de entrega registradas no sistema. */
@Component
public class CatalogoEntregas extends Catalogo<ModalidadeEntrega> {

    public CatalogoEntregas(List<ModalidadeEntrega> modalidades) {
        super(modalidades);
    }
}
