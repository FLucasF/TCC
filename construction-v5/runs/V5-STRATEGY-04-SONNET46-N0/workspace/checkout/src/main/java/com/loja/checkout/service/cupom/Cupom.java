package com.loja.checkout.service.cupom;

public interface Cupom {
    String getCodigo();
    boolean aplicavel(ContextoCupom ctx);
    ResultadoCupom calcular(ContextoCupom ctx);
}
