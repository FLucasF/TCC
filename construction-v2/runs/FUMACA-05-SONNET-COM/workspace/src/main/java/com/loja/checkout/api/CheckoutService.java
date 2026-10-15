package com.loja.checkout.api;

import com.loja.checkout.api.dto.ItemDTO;
import com.loja.checkout.api.dto.ResumoRequest;
import com.loja.checkout.api.dto.ResumoResponse;
import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.CupomService;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.EntregaCalculada;
import com.loja.checkout.entrega.EntregaService;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.pagamento.PagamentoService;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CheckoutService {

    private final EntregaService entregaService;
    private final CupomService cupomService;
    private final PagamentoService pagamentoService;

    public CheckoutService(EntregaService entregaService, CupomService cupomService, PagamentoService pagamentoService) {
        this.entregaService = entregaService;
        this.cupomService = cupomService;
        this.pagamentoService = pagamentoService;
    }

    public ResumoResponse calcularResumo(ResumoRequest request) {
        Pedido pedido = construirPedido(request.itens());

        EntregaCalculada entrega = entregaService.calcular(request.modalidadeEntrega(), pedido);

        BigDecimal descontoCupom = cupomService.calcularDesconto(
                request.cupom(), new ContextoCupom(pedido, entrega.valor()));

        BigDecimal totalPedido = pedido.subtotalProdutos()
                .subtract(descontoCupom)
                .add(entrega.valor());

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        ResultadoPagamento pagamento = pagamentoService.calcular(request.formaPagamento(), parcelas, totalPedido);

        BigDecimal ajustePagamento = pagamento.totalFinal().subtract(totalPedido);

        return new ResumoResponse(
                pedido.subtotalProdutos(),
                descontoCupom,
                entrega.valor(),
                entrega.prazoDias(),
                ajustePagamento,
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela()
        );
    }

    private Pedido construirPedido(List<ItemDTO> itensDTO) {
        if (itensDTO == null || itensDTO.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }

        List<ItemPedido> itens = itensDTO.stream()
                .map(this::validarEConverter)
                .toList();

        return new Pedido(itens);
    }

    private ItemPedido validarEConverter(ItemDTO dto) {
        if (dto.precoUnitario() == null || dto.precoUnitario().signum() <= 0
                || dto.quantidade() == null || dto.quantidade() <= 0
                || dto.pesoKg() == null || dto.pesoKg().signum() <= 0) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new ItemPedido(dto.nome(), dto.precoUnitario(), dto.quantidade(), dto.pesoKg());
    }
}
