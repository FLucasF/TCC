package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

/** Todas as formas de pagamento aceitas. */
@Component
public class CatalogoFormasPagamento extends Catalogo<FormaPagamento> {

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        super(formas);
    }
}
