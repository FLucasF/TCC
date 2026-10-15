package com.loja.checkout.web;

import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.ErroNegocio;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TratadorErros {

    @ExceptionHandler(ErroNegocio.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> negocio(ErroNegocio e) {
        return Map.of("erro", e.codigo().name());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> corpoIlegivel(HttpMessageNotReadableException e) {
        return Map.of("erro", CodigoErro.PEDIDO_INVALIDO.name());
    }
}
