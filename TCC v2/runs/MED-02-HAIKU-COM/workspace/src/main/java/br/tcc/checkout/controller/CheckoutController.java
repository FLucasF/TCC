package br.tcc.checkout.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import br.tcc.checkout.domain.Item;
import br.tcc.checkout.domain.Pedido;
import br.tcc.checkout.dto.CheckoutRequest;
import br.tcc.checkout.dto.ErroResponse;
import br.tcc.checkout.dto.ItemRequest;
import br.tcc.checkout.dto.ResumoResponse;
import br.tcc.checkout.service.CalculadoraResumo;
import br.tcc.checkout.service.ResumoException;

@RestController
public class CheckoutController {
    @PostMapping("/checkout/resumo")
    public ResponseEntity<?> resumo(@RequestBody CheckoutRequest request) {
        try {
            List<Item> itens = new ArrayList<>();
            for (ItemRequest itemRequest : request.itens) {
                Item item = new Item(
                        itemRequest.nome,
                        new BigDecimal(itemRequest.precoUnitario.toString()),
                        itemRequest.quantidade,
                        itemRequest.pesoKg);
                itens.add(item);
            }

            Pedido pedido = new Pedido(
                    itens,
                    request.modalidadeEntrega,
                    request.cupom,
                    request.formaPagamento,
                    request.parcelas);

            CalculadoraResumo calculadora = new CalculadoraResumo(pedido);
            ResumoResponse resposta = calculadora.calcular();

            return ResponseEntity.ok(resposta);
        } catch (ResumoException e) {
            return ResponseEntity.badRequest().body(new ErroResponse(e.getCodigo()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErroResponse("ERRO_INTERNO"));
        }
    }
}
