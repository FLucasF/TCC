package com.loja.checkout.api;

import com.loja.checkout.cupom.ContextoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.cupom.Cupons;
import com.loja.checkout.dominio.CheckoutException;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.entrega.ModalidadesEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.FormasPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Monta o resumo da compra na ordem combinada com o financeiro. */
@Service
public class ResumoCompraService {

    private final ModalidadesEntrega modalidades;
    private final Cupons cupons;
    private final FormasPagamento formasPagamento;

    public ResumoCompraService(ModalidadesEntrega modalidades, Cupons cupons, FormasPagamento formasPagamento) {
        this.modalidades = modalidades;
        this.cupons = cupons;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        if (request == null) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }

        Pedido pedido = validarPedido(request.itens());

        ModalidadeEntrega modalidade = modalidades.buscar(request.modalidadeEntrega());
        if (!modalidade.atende(pedido)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }
        BigDecimal frete = Dinheiro.arredondar(modalidade.calcularFrete(pedido));

        BigDecimal subtotalProdutos = pedido.subtotalProdutos();
        BigDecimal descontoCupom = calcularDesconto(request.cupom(), pedido, frete);

        FormaPagamento formaPagamento = formasPagamento.buscar(request.formaPagamento());
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete));
        if (!formaPagamento.atende(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido));

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidade.prazoEntregaDias(),
                ajustePagamento,
                Dinheiro.arredondar(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()));
    }

    private BigDecimal calcularDesconto(String codigoCupom, Pedido pedido, BigDecimal frete) {
        if (codigoCupom == null || codigoCupom.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigoCupom);
        ContextoCupom contexto = new ContextoCupom(pedido, frete);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.calcularDesconto(contexto));
    }

    private Pedido validarPedido(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }
        List<ItemPedido> validados = new ArrayList<>(itens.size());
        for (ItemRequest item : itens) {
            if (item == null
                    || naoPositivo(item.precoUnitario())
                    || item.quantidade() == null || item.quantidade() <= 0
                    || naoPositivo(item.pesoKg())) {
                throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
            }
            validados.add(new ItemPedido(
                    item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()));
        }
        return new Pedido(validados);
    }

    private boolean naoPositivo(BigDecimal valor) {
        return valor == null || valor.compareTo(BigDecimal.ZERO) <= 0;
    }
}
