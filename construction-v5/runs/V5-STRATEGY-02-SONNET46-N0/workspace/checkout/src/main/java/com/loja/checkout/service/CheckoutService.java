package com.loja.checkout.service;

import com.loja.checkout.dto.ItemDto;
import com.loja.checkout.dto.PedidoDto;
import com.loja.checkout.dto.ResumoDto;
import com.loja.checkout.model.CheckoutException;
import com.loja.checkout.service.clube.ClubeService;
import com.loja.checkout.service.cupom.CupomService;
import com.loja.checkout.service.entrega.EntregaService;
import com.loja.checkout.service.entrega.ModalidadeEntrega;
import com.loja.checkout.service.pagamento.PagamentoService;
import com.loja.checkout.service.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CheckoutService {

    private static final Set<String> REGIOES_VALIDAS = Set.of(
            "SUDESTE", "SUL", "CENTRO_OESTE", "NORTE", "NORDESTE");

    private static final Map<String, BigDecimal> TAXA_SEGURO = Map.of(
            "SUDESTE",      BigDecimal.valueOf(0.01),
            "SUL",          BigDecimal.valueOf(0.01),
            "CENTRO_OESTE", BigDecimal.valueOf(0.015),
            "NORTE",        BigDecimal.valueOf(0.025),
            "NORDESTE",     BigDecimal.valueOf(0.02));

    private final EntregaService entregaService;
    private final CupomService cupomService;
    private final ClubeService clubeService;
    private final PagamentoService pagamentoService;

    public CheckoutService(EntregaService entregaService, CupomService cupomService,
                           ClubeService clubeService, PagamentoService pagamentoService) {
        this.entregaService = entregaService;
        this.cupomService = cupomService;
        this.clubeService = clubeService;
        this.pagamentoService = pagamentoService;
    }

    public ResumoDto calcular(PedidoDto pedido) {
        // 1. Validate cart
        validarItens(pedido.itens());

        // 2. Validate club level
        clubeService.validar(pedido.nivelClube());

        // 3. Validate region
        if (pedido.regiao() == null || !REGIOES_VALIDAS.contains(pedido.regiao())) {
            throw new CheckoutException("REGIAO_INVALIDA");
        }

        // 4. Compute subtotal and weight
        BigDecimal subtotalProdutos = calcularSubtotal(pedido.itens());
        double pesoTotal = calcularPeso(pedido.itens());

        // 5. Resolve delivery (validates modalidade code + availability)
        ModalidadeEntrega modalidade = entregaService.resolverModalidade(pedido.modalidadeEntrega(), pesoTotal);

        // 6. Compute frete (zeroed for OURO)
        BigDecimal frete;
        if (clubeService.freteGratis(pedido.nivelClube())) {
            frete = BigDecimal.ZERO.setScale(2);
        } else {
            frete = modalidade.calcularFrete(pesoTotal);
        }

        // 7. Coupon discount (validates cupom)
        int parcelas = pedido.parcelas() != null ? pedido.parcelas() : 1;
        BigDecimal descontoCupom = cupomService.calcularDesconto(
                pedido.cupom(), subtotalProdutos, frete, pedido.itens());

        // 8. Insurance — always on subtotalProdutos (no discount, no freight)
        BigDecimal taxaSeguro = TAXA_SEGURO.get(pedido.regiao());
        BigDecimal seguro = subtotalProdutos.multiply(taxaSeguro).setScale(2, RoundingMode.HALF_EVEN);

        // 9. Order total before payment adjustment
        BigDecimal totalPedido = subtotalProdutos
                .subtract(descontoCupom)
                .add(frete)
                .add(seguro);

        // 10. Payment adjustment (validates forma + parcelas + limits)
        ResultadoPagamento pagamento = pagamentoService.calcular(pedido.formaPagamento(), parcelas, totalPedido);

        // 11. Club credit — on subtotalProdutos (no discount, no freight)
        BigDecimal credito = clubeService.calcularCredito(pedido.nivelClube(), subtotalProdutos);

        // 12. Brinde
        boolean brinde = clubeService.temBrinde(pedido.nivelClube(), subtotalProdutos);

        return new ResumoDto(
                subtotalProdutos.doubleValue(),
                descontoCupom.doubleValue(),
                frete.doubleValue(),
                modalidade.getPrazoDias(),
                seguro.doubleValue(),
                pagamento.ajuste().doubleValue(),
                pagamento.totalFinal().doubleValue(),
                pagamento.parcelas(),
                pagamento.valorParcela().doubleValue(),
                credito.doubleValue(),
                brinde
        );
    }

    private void validarItens(List<ItemDto> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        for (ItemDto item : itens) {
            if (item.precoUnitario() == null || item.precoUnitario() <= 0
                    || item.quantidade() == null || item.quantidade() <= 0
                    || item.pesoKg() == null || item.pesoKg() <= 0) {
                throw new CheckoutException("PEDIDO_INVALIDO");
            }
        }
    }

    private BigDecimal calcularSubtotal(List<ItemDto> itens) {
        BigDecimal total = BigDecimal.ZERO;
        for (ItemDto item : itens) {
            BigDecimal linha = BigDecimal.valueOf(item.precoUnitario())
                    .multiply(BigDecimal.valueOf(item.quantidade()))
                    .setScale(2, RoundingMode.HALF_EVEN);
            total = total.add(linha);
        }
        return total.setScale(2, RoundingMode.HALF_EVEN);
    }

    private double calcularPeso(List<ItemDto> itens) {
        double peso = 0.0;
        for (ItemDto item : itens) {
            peso += item.pesoKg() * item.quantidade();
        }
        return peso;
    }
}
