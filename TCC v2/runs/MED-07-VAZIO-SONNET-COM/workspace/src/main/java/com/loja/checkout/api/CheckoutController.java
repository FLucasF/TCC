package com.loja.checkout.api;

import com.loja.checkout.api.dto.ItemRequest;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.servico.ResumoCompra;
import com.loja.checkout.servico.ResumoCompraService;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final ResumoCompraService resumoCompraService;

    public CheckoutController(ResumoCompraService resumoCompraService) {
        this.resumoCompraService = resumoCompraService;
    }

    @PostMapping("/resumo")
    public ResumoResponse resumo(@RequestBody ResumoRequest request) {
        Pedido pedido = new Pedido(paraItens(request.itens()));
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;

        ResumoCompra resumo = resumoCompraService.calcular(
                pedido, request.modalidadeEntrega(), request.cupom(), request.formaPagamento(), parcelas);

        return new ResumoResponse(
                resumo.subtotalProdutos(),
                resumo.descontoCupom(),
                resumo.frete(),
                resumo.prazoEntregaDias(),
                resumo.ajustePagamento(),
                resumo.totalFinal(),
                resumo.parcelas(),
                resumo.valorParcela());
    }

    private List<Item> paraItens(List<ItemRequest> itens) {
        if (itens == null) {
            return Collections.emptyList();
        }
        return itens.stream()
                .map(item -> new Item(
                        item.nome(),
                        item.precoUnitario(),
                        item.quantidade() != null ? item.quantidade() : 0,
                        item.pesoKg()))
                .toList();
    }
}
