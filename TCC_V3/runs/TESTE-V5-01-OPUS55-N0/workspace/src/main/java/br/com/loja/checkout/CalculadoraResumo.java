package br.com.loja.checkout;

import br.com.loja.checkout.api.ItemRequest;
import br.com.loja.checkout.api.ResumoRequest;
import br.com.loja.checkout.api.ResumoResponse;
import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.ContextoCupom;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Catalogo;
import br.com.loja.checkout.dominio.CheckoutException;
import br.com.loja.checkout.dominio.CodigoErro;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.FormaPagamento;
import br.com.loja.checkout.pagamento.Pagamento;
import br.com.loja.checkout.seguro.Regiao;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Calcula o resumo da compra. As validações seguem a ordem combinada com o site:
 * o primeiro problema encontrado é o que volta como erro.
 */
@Service
public class CalculadoraResumo {

    private final Catalogo<ModalidadeEntrega> modalidades;
    private final Catalogo<Cupom> cupons;
    private final Catalogo<NivelClube> niveis;
    private final Catalogo<FormaPagamento> formasPagamento;

    public CalculadoraResumo(List<ModalidadeEntrega> modalidades, List<Cupom> cupons,
                             List<NivelClube> niveis, List<FormaPagamento> formasPagamento) {
        this.modalidades = new Catalogo<>(modalidades, ModalidadeEntrega::codigo);
        this.cupons = new Catalogo<>(cupons, Cupom::codigo);
        this.niveis = new Catalogo<>(niveis, NivelClube::codigo);
        this.formasPagamento = new Catalogo<>(formasPagamento, FormaPagamento::codigo);
    }

    public ResumoResponse calcular(ResumoRequest request) {
        Carrinho carrinho = carrinho(request.itens());
        NivelClube nivel = exigir(niveis.buscar(request.nivelClube()), CodigoErro.NIVEL_CLUBE_INVALIDO);
        Regiao regiao = exigir(Regiao.buscar(request.regiao()), CodigoErro.REGIAO_INVALIDA);
        ModalidadeEntrega modalidade = exigir(modalidades.buscar(request.modalidadeEntrega()),
                CodigoErro.MODALIDADE_INVALIDA);
        if (!modalidade.atende(carrinho)) {
            throw new CheckoutException(CodigoErro.MODALIDADE_INDISPONIVEL);
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : modalidade.frete(carrinho);
        BigDecimal desconto = descontoCupom(request.cupom(), new ContextoCupom(carrinho, subtotal, frete));
        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);

        FormaPagamento forma = exigir(formasPagamento.buscar(request.formaPagamento()),
                CodigoErro.FORMA_PAGAMENTO_INVALIDA);
        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new CheckoutException(CodigoErro.PARCELAMENTO_INVALIDO);
        }
        if (!forma.disponivel(totalPedido)) {
            throw new CheckoutException(CodigoErro.FORMA_PAGAMENTO_INDISPONIVEL);
        }
        Pagamento pagamento = forma.pagar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                Dinheiro.arredondar(pagamento.totalFinal().subtract(totalPedido)),
                Dinheiro.arredondar(pagamento.totalFinal()),
                pagamento.parcelas(),
                Dinheiro.arredondar(pagamento.valorParcela()),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho carrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Carrinho(itens.stream().map(CalculadoraResumo::item).toList());
    }

    private static Item item(ItemRequest item) {
        if (item == null || !positivo(item.precoUnitario()) || !positivo(item.pesoKg())
                || item.quantidade() == null || item.quantidade() <= 0) {
            throw new CheckoutException(CodigoErro.PEDIDO_INVALIDO);
        }
        return new Item(item.nome(), item.precoUnitario(), item.quantidade(), item.pesoKg());
    }

    private static boolean positivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private BigDecimal descontoCupom(String codigo, ContextoCupom contexto) {
        if (codigo == null) {
            return Dinheiro.ZERO;
        }
        Cupom cupom = exigir(cupons.buscar(codigo), CodigoErro.CUPOM_INVALIDO);
        if (!cupom.aplicavel(contexto)) {
            throw new CheckoutException(CodigoErro.CUPOM_NAO_APLICAVEL);
        }
        return Dinheiro.arredondar(cupom.desconto(contexto));
    }

    private static <T> T exigir(Optional<T> valor, CodigoErro erro) {
        return valor.orElseThrow(() -> new CheckoutException(erro));
    }
}
