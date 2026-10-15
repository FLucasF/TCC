package br.com.loja.checkout.api;

import br.com.loja.checkout.clube.NivelClube;
import br.com.loja.checkout.cupom.Cupom;
import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Dinheiro;
import br.com.loja.checkout.dominio.ErroNegocio;
import br.com.loja.checkout.dominio.Item;
import br.com.loja.checkout.dominio.Regiao;
import br.com.loja.checkout.entrega.ModalidadeEntrega;
import br.com.loja.checkout.pagamento.Cobranca;
import br.com.loja.checkout.pagamento.FormaPagamento;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ResumoService {

    private final Map<String, NivelClube> niveis;
    private final Map<String, ModalidadeEntrega> modalidades;
    private final Map<String, Cupom> cupons;
    private final Map<String, FormaPagamento> formasPagamento;

    public ResumoService(List<NivelClube> niveis, List<ModalidadeEntrega> modalidades,
                         List<Cupom> cupons, List<FormaPagamento> formasPagamento) {
        this.niveis = porCodigo(niveis, NivelClube::codigo);
        this.modalidades = porCodigo(modalidades, ModalidadeEntrega::codigo);
        this.cupons = porCodigo(cupons, Cupom::codigo);
        this.formasPagamento = porCodigo(formasPagamento, FormaPagamento::codigo);
    }

    private static <T> Map<String, T> porCodigo(List<T> lista, Function<T, String> codigo) {
        return lista.stream().collect(Collectors.toMap(codigo, Function.identity()));
    }

    public ResumoResponse calcular(ResumoRequest req) {
        Carrinho carrinho = montarCarrinho(req.itens());

        NivelClube nivel = req.nivelClube() == null ? null : niveis.get(req.nivelClube());
        if (nivel == null) {
            throw new ErroNegocio("NIVEL_CLUBE_INVALIDO");
        }
        Regiao regiao = Regiao.de(req.regiao());
        if (regiao == null) {
            throw new ErroNegocio("REGIAO_INVALIDA");
        }
        ModalidadeEntrega modalidade = req.modalidadeEntrega() == null ? null : modalidades.get(req.modalidadeEntrega());
        if (modalidade == null) {
            throw new ErroNegocio("MODALIDADE_INVALIDA");
        }
        if (!modalidade.atende(carrinho)) {
            throw new ErroNegocio("MODALIDADE_INDISPONIVEL");
        }

        BigDecimal subtotal = carrinho.subtotal();
        BigDecimal frete = nivel.freteGratis() ? Dinheiro.ZERO : Dinheiro.arredondar(modalidade.frete(carrinho));

        BigDecimal desconto = Dinheiro.ZERO;
        if (req.cupom() != null) {
            Cupom cupom = cupons.get(req.cupom());
            if (cupom == null) {
                throw new ErroNegocio("CUPOM_INVALIDO");
            }
            if (!cupom.aplicavel(carrinho)) {
                throw new ErroNegocio("CUPOM_NAO_APLICAVEL");
            }
            desconto = Dinheiro.arredondar(cupom.desconto(carrinho, frete));
        }

        FormaPagamento forma = req.formaPagamento() == null ? null : formasPagamento.get(req.formaPagamento());
        if (forma == null) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INVALIDA");
        }
        int parcelas = req.parcelas() == null ? 1 : req.parcelas();
        if (!forma.parcelasPermitidas(parcelas)) {
            throw new ErroNegocio("PARCELAMENTO_INVALIDO");
        }

        BigDecimal seguro = regiao.seguro(subtotal);
        BigDecimal totalPedido = subtotal.subtract(desconto).add(frete).add(seguro);
        if (!forma.disponivel(totalPedido)) {
            throw new ErroNegocio("FORMA_PAGAMENTO_INDISPONIVEL");
        }
        Cobranca cobranca = forma.cobrar(totalPedido, parcelas);

        return new ResumoResponse(
                subtotal,
                desconto,
                frete,
                modalidade.prazoDias(),
                seguro,
                cobranca.totalFinal().subtract(totalPedido),
                cobranca.totalFinal(),
                parcelas,
                cobranca.valorParcela(),
                nivel.credito(subtotal),
                nivel.brinde(subtotal));
    }

    private static Carrinho montarCarrinho(List<ItemRequest> itens) {
        if (itens == null || itens.isEmpty()) {
            throw new ErroNegocio("PEDIDO_INVALIDO");
        }
        List<Item> lista = new ArrayList<>();
        for (ItemRequest i : itens) {
            if (i == null
                    || i.precoUnitario() == null || i.precoUnitario().signum() <= 0
                    || i.quantidade() == null || i.quantidade() <= 0
                    || i.pesoKg() == null || i.pesoKg().signum() <= 0) {
                throw new ErroNegocio("PEDIDO_INVALIDO");
            }
            lista.add(new Item(i.nome(), i.precoUnitario(), i.quantidade(), i.pesoKg()));
        }
        return new Carrinho(lista);
    }
}
