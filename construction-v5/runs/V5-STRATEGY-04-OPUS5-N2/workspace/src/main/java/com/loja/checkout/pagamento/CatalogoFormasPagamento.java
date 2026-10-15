package com.loja.checkout.pagamento;

import com.loja.checkout.dominio.Catalogo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CatalogoFormasPagamento extends Catalogo<FormaPagamento> {

    public CatalogoFormasPagamento(List<FormaPagamento> formas) {
        super(formas);
    }
}
