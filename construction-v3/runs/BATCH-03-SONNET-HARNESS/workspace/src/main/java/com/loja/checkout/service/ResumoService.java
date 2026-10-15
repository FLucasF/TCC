package com.loja.checkout.service;

import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.domain.ItemPedido;
import com.loja.checkout.domain.Pedido;
import com.loja.checkout.dto.ItemRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.exception.NegocioException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ResumoService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public ResumoService(ModalidadeEntregaRegistry modalidadeEntregaRegistry,
                          CupomRegistry cupomRegistry,
                          FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = validarPedido(request);

        ModalidadeEntrega modalidade = validarModalidade(request.modalidadeEntrega());
        BigDecimal pesoTotal = pedido.pesoTotalKg();
        if (!modalidade.disponivelPara(pesoTotal)) {
            throw new NegocioException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotalProdutos = Dinheiro.arredondar(pedido.subtotalProdutos());
        BigDecimal frete = modalidade.calcularFrete(pesoTotal);

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        String codigoCupom = normalizarCupom(request.cupom());
        if (codigoCupom != null) {
            Cupom cupom = cupomRegistry.buscar(codigoCupom)
                    .orElseThrow(() -> new NegocioException("CUPOM_INVALIDO"));
            ContextoCupom contexto = new ContextoCupom(pedido, subtotalProdutos, frete);
            if (!cupom.aplicavel(contexto)) {
                throw new NegocioException("CUPOM_NAO_APLICAVEL");
            }
            descontoCupom = cupom.calcularDesconto(contexto);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(subtotalProdutos.subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new NegocioException("FORMA_PAGAMENTO_INVALIDA"));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new NegocioException("PARCELAMENTO_INVALIDO");
        }
        if (!formaPagamento.disponivelPara(totalPedido)) {
            throw new NegocioException("FORMA_PAGAMENTO_INDISPONIVEL");
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(resultadoPagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.getPrazoDias(),
                ajustePagamento,
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela()
        );
    }

    private Pedido validarPedido(ResumoRequest request) {
        List<ItemRequest> itensRequest = request.itens();
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new NegocioException("PEDIDO_INVALIDO");
        }

        List<ItemPedido> itens = itensRequest.stream()
                .map(i -> new ItemPedido(i.nome(), i.precoUnitario(),
                        i.quantidade() == null ? 0 : i.quantidade(), i.pesoKg()))
                .toList();

        for (ItemPedido item : itens) {
            if (!item.valido()) {
                throw new NegocioException("PEDIDO_INVALIDO");
            }
        }

        return new Pedido(itens);
    }

    private ModalidadeEntrega validarModalidade(String codigo) {
        return modalidadeEntregaRegistry.buscar(codigo)
                .orElseThrow(() -> new NegocioException("MODALIDADE_INVALIDA"));
    }

    private String normalizarCupom(String cupom) {
        return (cupom == null || cupom.isBlank()) ? null : cupom;
    }
}
