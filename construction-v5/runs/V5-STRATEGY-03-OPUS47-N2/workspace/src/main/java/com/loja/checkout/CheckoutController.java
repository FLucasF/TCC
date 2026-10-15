package com.loja.checkout;

import com.loja.checkout.dto.CheckoutRequest;
import com.loja.checkout.dto.ErroResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CheckoutController {
    private final CheckoutService service;

    public CheckoutController(CheckoutService service) {
        this.service = service;
    }

    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest req) {
        return ResponseEntity.ok(service.calcular(req));
    }

    @ExceptionHandler(ErroPedido.class)
    public ResponseEntity<ErroResponse> trataErro(ErroPedido e) {
        return ResponseEntity.ok(new ErroResponse(e.codigo()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> trataPayload(HttpMessageNotReadableException e) {
        return ResponseEntity.ok(new ErroResponse("PEDIDO_INVALIDO"));
    }
}
