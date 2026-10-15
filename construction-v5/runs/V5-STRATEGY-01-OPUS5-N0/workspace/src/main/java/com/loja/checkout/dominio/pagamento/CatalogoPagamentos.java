package com.loja.checkout.dominio.pagamento;

import com.loja.checkout.dominio.Catalogo;
import java.util.List;
import org.springframework.stereotype.Component;

/** Catalogo das formas de pagamento aceitas. */
@Component
public class CatalogoPagamentos extends Catalogo<FormaPagamento> {

    public CatalogoPagamentos(List<FormaPagamento> formas) {
        super(formas, FormaPagamento::codigo);
    }
}
