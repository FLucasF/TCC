package com.loja.checkout.api;

import com.loja.checkout.api.dto.CheckoutRequest;
import com.loja.checkout.api.dto.ErroResponse;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.domain.CheckoutException;
import com.loja.checkout.domain.CheckoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Recebe a compra em /checkout/resumo e devolve o resumo calculado. */
@RestController
public class CheckoutController {

    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResumoResponse resumo(@RequestBody CheckoutRequest request) {
        return service.calcular(request);
    }

    /** Pedido recusado: devolve so o codigo do problema. */
    @ExceptionHandler(CheckoutException.class)
    public ResponseEntity<ErroResponse> tratarRecusa(CheckoutException ex) {
        return ResponseEntity.unprocessableEntity().body(new ErroResponse(ex.getCodigo()));
    }
}
