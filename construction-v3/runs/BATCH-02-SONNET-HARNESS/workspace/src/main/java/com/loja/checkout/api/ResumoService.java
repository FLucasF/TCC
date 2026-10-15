package com.loja.checkout.api;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.CupomRegistry;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadeEntregaRegistry;
import com.loja.checkout.erro.CodigoErro;
import com.loja.checkout.erro.NegocioException;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormaPagamentoRegistry;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ResumoService {

    private final ModalidadeEntregaRegistry modalidadeEntregaRegistry;
    private final CupomRegistry cupomRegistry;
    private final FormaPagamentoRegistry formaPagamentoRegistry;

    public ResumoService(ModalidadeEntregaRegistry modalidadeEntregaRegistry, CupomRegistry cupomRegistry,
                          FormaPagamentoRegistry formaPagamentoRegistry) {
        this.modalidadeEntregaRegistry = modalidadeEntregaRegistry;
        this.cupomRegistry = cupomRegistry;
        this.formaPagamentoRegistry = formaPagamentoRegistry;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Pedido pedido = Pedido.de(itensValidos(request.itens()));

        ModalidadeEntrega modalidade = modalidadeEntregaRegistry.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new NegocioException(CodigoErro.MODALIDADE_INVALIDA));
        if (!modalidade.disponivelPara(pedido)) {
            throw new NegocioException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = modalidade.calcularFrete(pedido);

        BigDecimal descontoCupom = BigDecimal.ZERO.setScale(2);
        if (request.cupom() != null) {
            Cupom cupom = cupomRegistry.buscar(request.cupom())
                    .orElseThrow(() -> new NegocioException(CodigoErro.CUPOM_INVALIDO));
            if (!cupom.aplicavel(pedido)) {
                throw new NegocioException(CodigoErro.CUPOM_NAO_APLICAVEL);
            }
            descontoCupom = cupom.calcularDesconto(pedido, frete);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(
                pedido.subtotalProdutos().subtract(descontoCupom).add(frete));

        FormaPagamento formaPagamento = formaPagamentoRegistry.buscar(request.formaPagamento())
                .orElseThrow(() -> new NegocioException(CodigoErro.FORMA_PAGAMENTO_INVALIDA));

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelasValidas(parcelas)) {
            throw new NegocioException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivelPara(totalPedido, parcelas)) {
            throw new NegocioException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultado = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(pedido.subtotalProdutos(), descontoCupom, frete, modalidade.prazoDias(),
                resultado.ajuste(), resultado.totalFinal(), parcelas, resultado.valorParcela());
    }

    private List<Item> itensValidos(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new NegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        return itens.stream().map(this::itemValido).toList();
    }

    private Item itemValido(ItemRequest item) {
        if (item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() == null || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0) {
            throw new NegocioException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }
}
