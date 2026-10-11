package com.loja.checkout.api;

import com.loja.checkout.dominio.ErroNegocio;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.servico.ResumoCompraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumoCompraController {

    private final ResumoCompraService servico;

    public ResumoCompraController(ResumoCompraService servico) {
        this.servico = servico;
    }

    @PostMapping("/checkout/resumo")
    public ResumoCompra resumo(@RequestBody PedidoRequest pedido) {
        return servico.calcular(pedido);
    }

    @ExceptionHandler(ErroNegocio.class)
    public ResponseEntity<ErroResponse> erroNegocio(ErroNegocio erro) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErroResponse(erro.codigo().name()));
    }

    public record ErroResponse(String erro) {
    }
}
