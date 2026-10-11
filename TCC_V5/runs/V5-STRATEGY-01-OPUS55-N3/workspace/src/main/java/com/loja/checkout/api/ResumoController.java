package com.loja.checkout.api;

import com.loja.checkout.ResumoService;
import com.loja.checkout.comum.ResumoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ResumoController {

    private final ResumoService service;

    ResumoController(ResumoService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    ResumoResponse resumo(@RequestBody ResumoRequest request) {
        return service.calcular(request);
    }

    @ExceptionHandler(ResumoException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErroResponse recusado(ResumoException e) {
        return new ErroResponse(e.codigo());
    }

    /** Corpo que nem chega a ser lido (JSON malformado, número com texto) é pedido inválido. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    ErroResponse ilegivel() {
        return new ErroResponse("PEDIDO_INVALIDO");
    }
}
