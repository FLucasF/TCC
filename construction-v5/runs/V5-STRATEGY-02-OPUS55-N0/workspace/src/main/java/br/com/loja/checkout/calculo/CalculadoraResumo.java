package br.com.loja.checkout.calculo;

import static br.com.loja.checkout.calculo.CodigoErro.*;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.PedidoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.catalogo.Catalogo;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.ResultadoPagamento;
import br.com.loja.checkout.regiao.Regiao;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Calcula o resumo da compra. As validações seguem a ordem combinada com o site:
 * o primeiro problema encontrado é o que recusa o pedido.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
            List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades);
        this.cupons = new Catalogo<>(cupons);
        this.niveis = new Catalogo<>(niveis);
        this.formasPagamento = new Catalogo<>(formasPagamento);
    }

    public ResumoResponse calcular(PedidoRequest pedido) {
        Carrinho carrinho = Carrinho.de(itensValidos(pedido.itens()));
        NivelClube nivel = niveis.buscar(pedido.nivelClube()).orElseThrow(() -> recusa(NIVEL_CLUBE_INVALIDO));
        Regiao regiao = Regiao.buscar(pedido.regiao()).orElseThrow(() -> recusa(REGIAO_INVALIDA));

        ModalidadeEntrega modalidade = modalidades.buscar(pedido.modalidadeEntrega())
                .orElseThrow(() -> recusa(MODALIDADE_INVALIDA));
        if (!modalidade.atende(carrinho)) {
            throw recusa(MODALIDADE_INDISPONIVEL);
        }

        Optional<Cupom> cupom = cupomInformado(pedido.cupom(), carrinho);

        FormaPagamento formaPagamento = formasPagamento.buscar(pedido.formaPagamento())
                .orElseThrow(() -> recusa(FORMA_PAGAMENTO_INVALIDA));
        int parcelas = parcelasValidas(pedido.parcelas(), formaPagamento);

        BigDecimal subtotal = carrinho.subtotalProdutos();
        BigDecimal frete = nivel.isentaFrete() ? Dinheiro.ZERO : modalidade.calcularFrete(carrinho);
        BigDecimal desconto = cupom.map(c -> c.calcularDesconto(carrinho, frete)).orElse(Dinheiro.ZERO);
        BigDecimal seguro = Dinheiro.percentual(subtotal, regiao.porcentagemSeguro());
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        if (!formaPagamento.atende(totalPedido)) {
            throw recusa(FORMA_PAGAMENTO_INDISPONIVEL);
        }
        ResultadoPagamento pagamento = formaPagamento.calcular(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.centavos(pagamento.totalFinal().subtract(totalPedido)),
                Dinheiro.centavos(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.centavos(pagamento.valorParcela()),
                Dinheiro.percentual(subtotal, nivel.porcentagemCredito()),
                nivel.ganhaBrinde(carrinho));
    }

    private static List<Item> itensValidos(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw recusa(PEDIDO_INVALIDO);
        }
        List<Item> validos = new ArrayList<>();
        for (ItemRequest item : itens) {
            if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())) {
                throw recusa(PEDIDO_INVALIDO);
            }
            int quantidade = inteiro(item.quantidade()).filter(q -> q > 0).orElseThrow(() -> recusa(PEDIDO_INVALIDO));
            validos.add(new Item(item.nome(), item.precoUnitario(), quantidade, item.pesoKg()));
        }
        return validos;
    }

    private Optional<Cupom> cupomInformado(String codigo, Carrinho carrinho) {
        if (codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }
        Cupom cupom = cupons.buscar(codigo).orElseThrow(() -> recusa(CUPOM_INVALIDO));
        if (!cupom.aplicavel(carrinho)) {
            throw recusa(CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupom);
    }

    private static int parcelasValidas(BigDecimal parcelas, FormaPagamento formaPagamento) {
        if (parcelas == null) {
            parcelas = BigDecimal.ONE;
        }
        return inteiro(parcelas).filter(formaPagamento::permiteParcelas)
                .orElseThrow(() -> recusa(PARCELAMENTO_INVALIDO));
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

    private static PedidoRecusadoException recusa(CodigoErro codigo) {
        return new PedidoRecusadoException(codigo);
    }
}
