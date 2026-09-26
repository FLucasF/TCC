package com.loja.checkout;

import com.loja.checkout.cupom.Cupom;
import com.loja.checkout.dominio.Catalogo;
import com.loja.checkout.dominio.Cobranca;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.Entrega;
import com.loja.checkout.dominio.ErroCheckout;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Pedido;
import com.loja.checkout.dominio.ResumoCompra;
import com.loja.checkout.entrega.ModalidadeEntrega;
import com.loja.checkout.pagamento.FormaPagamento;
import com.loja.checkout.web.ItemRequest;
import com.loja.checkout.web.ResumoRequest;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/** Monta o resumo da compra na ordem combinada: produtos, cupom, frete, total, pagamento. */
@Service
public class CheckoutService {

    private static final int PARCELAS_PADRAO = 1;

    private final Catalogo<ModalidadeEntrega> entregas;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> pagamentos;

    public CheckoutService(Catalogo<ModalidadeEntrega> entregas, Catalogo<Cupom> cupons,
                           Catalogo<FormaPagamento> pagamentos) {
        this.entregas = entregas;
        this.cupons = cupons;
        this.pagamentos = pagamentos;
    }

    public ResumoCompra calcular(ResumoRequest requisicao) {
        Pedido pedido = pedidoValido(requisicao);
        Entrega entrega = entrega(pedido, requisicao.modalidadeEntrega());
        BigDecimal descontoCupom = descontoCupom(pedido, entrega, requisicao.cupom());

        BigDecimal totalPedido = Dinheiro.arredondar(
                pedido.subtotalProdutos().subtract(descontoCupom).add(entrega.frete()));

        int parcelas = requisicao.parcelas() == null ? PARCELAS_PADRAO : requisicao.parcelas();
        Cobranca cobranca = cobranca(totalPedido, requisicao.formaPagamento(), parcelas);

        return new ResumoCompra(pedido.subtotalProdutos(), descontoCupom, entrega.frete(),
                entrega.prazoDias(), Dinheiro.arredondar(cobranca.totalFinal().subtract(totalPedido)),
                cobranca.totalFinal(), parcelas, cobranca.valorParcela());
    }

    private Pedido pedidoValido(ResumoRequest requisicao) {
        List<ItemRequest> itens = requisicao.itens();
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(this::itemValido)) {
            throw ErroCheckout.PEDIDO_INVALIDO.excecao();
        }
        return new Pedido(itens.stream()
                .map(item -> new ItemPedido(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private boolean positivo(BigDecimal valor) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }

    private Entrega entrega(Pedido pedido, String codigo) {
        ModalidadeEntrega modalidade = entregas.buscar(codigo)
                .orElseThrow(ErroCheckout.MODALIDADE_INVALIDA::excecao);
        if (!modalidade.atende(pedido)) {
            throw ErroCheckout.MODALIDADE_INDISPONIVEL.excecao();
        }
        return modalidade.calcular(pedido);
    }

    private BigDecimal descontoCupom(Pedido pedido, Entrega entrega, String codigo) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(ErroCheckout.CUPOM_INVALIDO::excecao);
        if (!cupom.aplicavel(pedido, entrega)) {
            throw ErroCheckout.CUPOM_NAO_APLICAVEL.excecao();
        }
        return cupom.desconto(pedido, entrega);
    }

    private Cobranca cobranca(BigDecimal totalPedido, String codigo, int parcelas) {
        FormaPagamento forma = pagamentos.buscar(codigo)
                .orElseThrow(ErroCheckout.FORMA_PAGAMENTO_INVALIDA::excecao);
        if (!forma.aceitaParcelas(parcelas)) {
            throw ErroCheckout.PARCELAMENTO_INVALIDO.excecao();
        }
        if (!forma.atende(totalPedido)) {
            throw ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL.excecao();
        }
        return forma.cobrar(totalPedido, parcelas);
    }
}
