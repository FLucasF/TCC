package com.loja;

import com.loja.dto.ErrorResponse;
import com.loja.dto.Pedido;
import com.loja.dto.ResumoPedido;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/resumo")
    public ResponseEntity<?> calcularResumo(@RequestBody Pedido pedido) {
        try {
            ResumoPedido resumo = checkoutService.calcularResumo(pedido);
            return ResponseEntity.ok(resumo);
        } catch (CheckoutService.ValidationException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        }
    }
}
