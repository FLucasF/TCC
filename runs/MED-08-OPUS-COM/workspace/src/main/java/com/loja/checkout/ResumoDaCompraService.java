package com.loja.checkout;

import com.loja.checkout.api.ItemRequest;
import com.loja.checkout.api.ResumoRequest;
import com.loja.checkout.api.ResumoResponse;
import com.loja.checkout.cupom.BaseDoCupom;
import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.CodigoErro;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ErroDeCheckout;
import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.pagamento.ResultadoPagamento;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ResumoDaCompraService {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> pagamentos;

    public ResumoDaCompraService(Catalogo<ModalidadeEntrega> entregas,
                                 Catalogo<Cupom> cupons,
                                 Catalogo<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoResponse calcular(ResumoRequest pedidoDoSite) {
        Pedido pedido = pedidoValidado(pedidoDoSite.itens());

        ModalidadeEntrega entrega = entregas.buscar(pedidoDoSite.modalidadeEntrega())
                .orElseThrow(() -> new ErroDeCheckout(CodigoErro.MODALIDADE_INVALIDA));
        if (!entrega.atende(pedido)) {
            throw new ErroDeCheckout(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = pedido.subtotalProdutos();
        BigDecimal frete = entrega.frete(pedido);
        BigDecimal desconto = descontoDoCupom(pedidoDoSite.cupom(), new BaseDoCupom(pedido, frete));
        BigDecimal totalDoPedido = Dinheiro.arredondar(subtotal.subtract(desconto).add(frete));

        FormaPagamento pagamento = pagamentos.buscar(pedidoDoSite.formaPagamento())
                .orElseThrow(() -> new ErroDeCheckout(CodigoErro.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = pedidoDoSite.parcelas() == null ? PARCELAS_PADRAO : pedidoDoSite.parcelas();
        if (!pagamento.parcelamentoPermitido(parcelas)) {
            throw new ErroDeCheckout(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!pagamento.atende(totalDoPedido)) {
            throw new ErroDeCheckout(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento cobranca = pagamento.cobrar(totalDoPedido, parcelas);
        BigDecimal ajuste = Dinheiro.arredondar(cobranca.totalFinal().subtract(totalDoPedido));

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                entrega.prazoEntregaDias(),
                ajuste,
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela());
    }

    private Pedido pedidoValidado(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroDeCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Pedido(itens.stream().map(this::itemValidado).toList());
    }

    private Item itemValidado(ItemRequest item) {
        if (item == null
                || !positivo(item.precoUnitario())
                || item.quantidade() == null || item.quantidade() <= 0
                || !positivo(item.pesoKg())) {
            throw new ErroDeCheckout(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal descontoDoCupom(String codigo, BaseDoCupom base) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new ErroDeCheckout(CodigoErro.CUPOM_INVALIDO));
        if (!cupom.aplicavel(base)) {
            throw new ErroDeCheckout(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return cupom.desconto(base);
    }
}
