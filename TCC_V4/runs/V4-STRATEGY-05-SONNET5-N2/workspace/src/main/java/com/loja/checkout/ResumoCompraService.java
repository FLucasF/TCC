package com.loja.checkout;

import com.loja.checkout.dominio.Carrinho;
import com.loja.checkout.dominio.Dinheiro;
import com.loja.checkout.dominio.ItemPedido;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.NiveisClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.Cupons;
import com.loja.checkout.dominio.entrega.ModalidadeEntrega;
import com.loja.checkout.dominio.entrega.ModalidadesEntrega;
import com.loja.checkout.dominio.pagamento.FormaPagamento;
import com.loja.checkout.dominio.pagamento.FormasPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.dto.ItemPedidoRequest;
import com.loja.checkout.dto.ResumoRequest;
import com.loja.checkout.dto.ResumoResponse;
import com.loja.checkout.erro.CheckoutException;
import com.loja.checkout.erro.ErroCheckout;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ResumoCompraService {

    private final ModalidadesEntrega modalidadesEntrega;
    private final Cupons cupons;
    private final NiveisClube niveisClube;
    private final FormasPagamento formasPagamento;

    public ResumoCompraService(ModalidadesEntrega modalidadesEntrega, Cupons cupons,
                                NiveisClube niveisClube, FormasPagamento formasPagamento) {
        this.modalidadesEntrega = modalidadesEntrega;
        this.cupons = cupons;
        this.niveisClube = niveisClube;
        this.formasPagamento = formasPagamento;
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Carrinho carrinho = montarCarrinho(request);
        if (!carrinho.valido()) {
            throw new CheckoutException(ErroCheckout.PEDIDO_INVALIDO);
        }

        NivelClube nivelClube = niveisClube.buscar(request.nivelClube())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.NIVEL_CLUBE_INVALIDO));

        Regiao regiao = regiaoDe(request.regiao())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.REGIAO_INVALIDA));

        ModalidadeEntrega modalidadeEntrega = modalidadesEntrega.buscar(request.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.MODALIDADE_INVALIDA));
        if (!modalidadeEntrega.disponivel(carrinho)) {
            throw new CheckoutException(ErroCheckout.MODALIDADE_INDISPONIVEL);
        }

        Optional<Cupom> cupom = buscarCupom(request.cupom(), carrinho);

        FormaPagamento formaPagamento = formasPagamento.buscar(request.formaPagamento())
                .orElseThrow(() -> new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = request.parcelas() != null ? request.parcelas() : 1;
        if (!formaPagamento.parcelamentoPermitido(parcelas)) {
            throw new CheckoutException(ErroCheckout.PARCELAMENTO_INVALIDO);
        }

        BigDecimal subtotalProdutos = carrinho.subtotalProdutos();
        BigDecimal frete = nivelClube.freteGratis()
                ? BigDecimal.ZERO.setScale(2)
                : modalidadeEntrega.custo(carrinho);
        BigDecimal descontoCupom = cupom.map(c -> Dinheiro.arredondar(c.desconto(carrinho, frete)))
                .orElse(BigDecimal.ZERO.setScale(2));
        BigDecimal seguro = Dinheiro.percentual(subtotalProdutos, regiao.percentualSeguro());
        BigDecimal totalPedido = Dinheiro.arredondar(
                subtotalProdutos.subtract(descontoCupom).add(frete).add(seguro));

        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCheckout.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = formaPagamento.calcular(totalPedido, parcelas);
        BigDecimal ajustePagamento = resultadoPagamento.valorFinal().subtract(totalPedido);
        BigDecimal credito = Dinheiro.arredondar(nivelClube.credito(subtotalProdutos));
        boolean brinde = nivelClube.brinde(subtotalProdutos);

        return new ResumoResponse(
                subtotalProdutos,
                descontoCupom,
                frete,
                modalidadeEntrega.prazoDias(),
                seguro,
                ajustePagamento,
                resultadoPagamento.valorFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                credito,
                brinde
        );
    }

    private Optional<Cupom> buscarCupom(String codigo, Carrinho carrinho) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = cupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCheckout.CUPOM_INVALIDO));
        if (!cupom.aplicavel(carrinho)) {
            throw new CheckoutException(ErroCheckout.CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupom);
    }

    private Carrinho montarCarrinho(ResumoRequest request) {
        List<ItemPedidoRequest> itens = request.itens();
        if (itens == null) {
            return new Carrinho(List.of());
        }
        List<ItemPedido> itensPedido = itens.stream()
                .map(this::converterItem)
                .toList();
        return new Carrinho(itensPedido);
    }

    private ItemPedido converterItem(ItemPedidoRequest item) {
        int quantidade = item.quantidade() != null ? item.quantidade() : 0;
        return new ItemPedido(item.nome(), item.precoUnitario(), quantidade, item.pesoKg());
    }

    private Optional<Regiao> regiaoDe(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Regiao.valueOf(codigo));
        } catch (IllegalArgumentException excecao) {
            return Optional.empty();
        }
    }
}
