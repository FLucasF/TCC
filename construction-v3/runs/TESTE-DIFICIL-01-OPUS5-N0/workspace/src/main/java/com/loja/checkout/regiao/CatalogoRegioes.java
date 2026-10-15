package com.loja.checkout.regiao;

import com.loja.checkout.dominio.Catalogo;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Tabela de percentuais da seguradora, por regiao do cliente. */
@Component
public class CatalogoRegioes extends Catalogo<Regiao> {

    public CatalogoRegioes() {
        super(List.of(
                new RegiaoFixa("SUDESTE", new BigDecimal("1")),
                new RegiaoFixa("SUL", new BigDecimal("1")),
                new RegiaoFixa("CENTRO_OESTE", new BigDecimal("1.5")),
                new RegiaoFixa("NORTE", new BigDecimal("2.5")),
                new RegiaoFixa("NORDESTE", new BigDecimal("2"))));
    }
}
