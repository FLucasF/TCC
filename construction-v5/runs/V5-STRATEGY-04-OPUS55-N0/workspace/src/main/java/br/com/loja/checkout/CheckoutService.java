package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.PedidoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.carrinho.Carrinho;
import br.com.loja.checkout.carrinho.Item;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import br.com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CheckoutService {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                           List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcularResumo(PedidoRequest pedido) {
        Carrinho carrinho = carrinho(pedido.itens());
        NivelClube nivel = niveis.buscar(pedido.nivelClube())
                .orElseThrow(() -> new CheckoutException(Erros.NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao())
                .orElseThrow(() -> new CheckoutException(Erros.REGIAO_INVALIDA));
        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> new CheckoutException(Erros.MODALIDADE_INVALIDA));
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException(Erros.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.isentoDeFrete() ? Dinheiro.ZERO : modalidade.frete(carrinho);
        BigDecimal descontoCupom = descontoCupom(pedido.cupom(), new ContextoCupom(carrinho, subtotal, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = Dinheiro.centavos(subtotal.subtract(descontoCupom).add(frete).add(seguro));

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> new CheckoutException(Erros.FORMA_PAGAMENTO_INVALIDA));
        int parcelas = parcelas(pedido.parcelas());
        if (!formaPagamento.aceitaParcelas(parcelas)) {
            throw new CheckoutException(Erros.PARCELAMENTO_INVALIDO);
        }
        if (!formaPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(Erros.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.pagar(totalPedido, parcelas);
        BigDecimal totalFinal = Dinheiro.centavos(pagamento.totalFinal());

        return new ResumoResponse(
                subtotal,
                descontoCupom,
                frete,
                modalidade.prazoDias(),
                seguro,
                totalFinal.subtract(totalPedido),
                totalFinal,
                pagamento.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()),
                Dinheiro.percentual(subtotal, nivel.percentualCredito()),
                nivel.ganhaBrinde(subtotal));
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null || codigo.isBlank()) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> new CheckoutException(Erros.CUPOM_INVALIDO));
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(Erros.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.centavos(cupom.desconto(contexto));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(Erros.PEDIDO_INVALIDO);
        }
        List<Item> convertidos = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())) {
                throw new CheckoutException(Erros.PEDIDO_INVALIDO);
            }
            int quantidade = inteiro(item.quantidade())
                    .filter(q -> q > 0)
                    .orElseThrow(() -> new CheckoutException(Erros.PEDIDO_INVALIDO));
            convertidos.add(new Item(item.nome(), item.precoUnitario(), quantidade, item.pesoKg()));
        }
        return new Carrinho(convertidos);
    }

    private static int parcelas(BigDecimal parcelas) {
        if (parcelas == null) {
            return 1;
        }
        return inteiro(parcelas).orElseThrow(() -> new CheckoutException(Erros.PARCELAMENTO_INVALIDO));
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private static Optional<Integer> inteiro(BigDecimal valor) {
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valor.intValueExact());
        } catch (ArithmeticException e) {
            return Optional.empty();
        }
    }
}
