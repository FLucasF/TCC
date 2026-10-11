package com.loja.checkout;

import com.loja.checkout.dto.ErroResponse;
import com.loja.checkout.dto.PedidoRequest;
import com.loja.checkout.dto.ResumoResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> resumo(@RequestBody PedidoRequest pedido) {
        try {
            ResumoResponse resposta = checkoutService.processar(pedido);
            return ResponseEntity.ok(resposta);
        } catch (CheckoutException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErroResponse(e.getErroCode().name()));
        }
    }
}
