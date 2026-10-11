package br.com.loja.checkout.api;

import br.com.loja.checkout.Catalogo;
import br.com.loja.checkout.CheckoutException;
import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.carrinho.Carrinho;
import br.com.loja.checkout.carrinho.Item;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Calcula o resumo da compra. As verificações seguem a ordem dos códigos de erro combinada com o site.
 */
@Service
public class ResumoCompraService {

    private final Catalogo<NivelClube> niveis;
    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<FormaPagamento> formasPagamento;

    public ResumoCompraService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                               List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = new Catalogo<>(niveis);
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(ResumoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.frete(modalidade.frete(carrinho));
        BigDecimal descontoCupom = descontoCupom(pedido.cupom(), new ContextoCupom(carrinho, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(descontoCupom).add(frete).add(seguro);

        Cobranca cobranca = cobrar(pedido, totalPedido);

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                cobranca.parcelas(),
                cobranca.valorParcela(),
                nivel.creditoProximaCompra(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty() || !itens.stream().allMatch(ResumoCompraService::itemValido)) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens.stream()
                .map(item -> new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg()))
                .toList());
    }

    private static boolean itemValido(ItemRequest item) {
        return item != null
                && positivo(item.precoUnitario())
                && item.quantidade() != null && item.quantidade() > 0
                && positivo(item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return cupom.desconto(contexto);
    }

    private Cobranca cobrar(ResumoRequest pedido, BigDecimal totalPedido) {
        FormaPagamento forma = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Optional.ofNullable(pedido.parcelas()).orElse(1);
        if (!forma.permiteParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }
        if (!forma.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        return forma.cobrar(totalPedido, parcelas);
    }
}
