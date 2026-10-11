package br.com.loja.checkout;

import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;

    public CheckoutService(List<ModalidadeEntrega> modalidades, List<Cupom> cupons) {
        this.modalidades = modalidades.stream().collect(Collectors.toMap(ModalidadeEntrega::codigo, Function.identity()));
        this.cupons = cupons.stream().collect(Collectors.toMap(Cupom::codigo, Function.identity()));
    }

    public ResumoCompra resumir(CheckoutRequest request) {
        Carrinho carrinho = carrinhoValido(request.itens());
        NivelClube nivel = buscar(NivelClube.class, request.nivelClube())
                .orElseThrow(() -> new CheckoutException("NIVEL_CLUBE_INVALIDO"));
        Regiao regiao = buscar(Regiao.class, request.regiao())
                .orElseThrow(() -> new CheckoutException("REGIAO_INVALIDA"));
        ModalidadeEntrega modalidade = modalidadeValida(request.modalidadeEntrega(), carrinho);
        Optional<Cupom> cupom = cupomValido(request.cupom(), carrinho);
        FormaPagamento forma = buscar(FormaPagamento.class, request.formaPagamento())
                .orElseThrow(() -> new CheckoutException("FORMA_PAGAMENTO_INVALIDA"));
        int parcelas = Objects.requireNonNullElse(request.parcelas(), 1);
        if (!forma.aceitaParcelas(parcelas)) {
            throw new CheckoutException("PARCELAMENTO_INVALIDO");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.centavos(BigDecimal.ZERO) : modalidade.frete(carrinho);
        BigDecimal desconto = cupom.map(c -> c.desconto(carrinho, frete)).orElse(Dinheiro.centavos(BigDecimal.ZERO));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!forma.atende(totalPedido)) {
            throw new CheckoutException("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Pagamento pagamento = forma.pagar(totalPedido, parcelas);

        return new ResumoCompra(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                pagamento.totalFinal().subtract(totalPedido),
                pagamento.totalFinal(),
                parcelas,
                pagamento.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinhoValido(List<Item> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(item -> item == null || !item.valido())) {
            throw new CheckoutException("PEDIDO_INVALIDO");
        }
        return new Carrinho(itens);
    }

    private ModalidadeEntrega modalidadeValida(String codigo, Carrinho carrinho) {
        ModalidadeEntrega modalidade = Optional.ofNullable(codigo).map(modalidades::get)
                .orElseThrow(() -> new CheckoutException("MODALIDADE_INVALIDA"));
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException("MODALIDADE_INDISPONIVEL");
        }
        return modalidade;
    }

    private Optional<Cupom> cupomValido(String codigo, Carrinho carrinho) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = Optional.ofNullable(cupons.get(codigo))
                .orElseThrow(() -> new CheckoutException("CUPOM_INVALIDO"));
        if (!cupom.aplicavel(carrinho)) {
            throw new CheckoutException("CUPOM_NAO_APLICAVEL");
        }
        return Optional.of(cupom);
    }

    private static <E extends Enum<E>> Optional<E> buscar(Class<E> tipo, String codigo) {
        return Arrays.stream(tipo.getEnumConstants()).filter(e -> e.name().equals(codigo)).findFirst();
    }
}
