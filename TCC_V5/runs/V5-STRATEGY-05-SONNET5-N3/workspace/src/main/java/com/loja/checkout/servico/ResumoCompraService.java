package com.loja.checkout.servico;

import com.loja.checkout.dominio.Item;
import com.loja.checkout.dominio.Regiao;
import com.loja.checkout.dominio.clube.NivelClube;
import com.loja.checkout.dominio.clube.RegistroNiveisClube;
import com.loja.checkout.dominio.cupom.Cupom;
import com.loja.checkout.dominio.cupom.RegistroCupons;
import com.loja.checkout.dominio.entrega.OpcaoEntrega;
import com.loja.checkout.dominio.entrega.RegistroOpcoesEntrega;
import com.loja.checkout.dominio.pagamento.CalculoPagamento;
import com.loja.checkout.dominio.pagamento.RegistroFormasPagamento;
import com.loja.checkout.dominio.pagamento.ResultadoPagamento;
import com.loja.checkout.util.Arredondamento;
import com.loja.checkout.validacao.CheckoutException;
import com.loja.checkout.validacao.ErroCodigo;
import com.loja.checkout.web.dto.ItemRequestDTO;
import com.loja.checkout.web.dto.ResumoRequestDTO;
import com.loja.checkout.web.dto.ResumoResponseDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ResumoCompraService {

    private final RegistroOpcoesEntrega registroOpcoesEntrega;
    private final RegistroCupons registroCupons;
    private final RegistroNiveisClube registroNiveisClube;
    private final RegistroFormasPagamento registroFormasPagamento;

    public ResumoCompraService(
            RegistroOpcoesEntrega registroOpcoesEntrega,
            RegistroCupons registroCupons,
            RegistroNiveisClube registroNiveisClube,
            RegistroFormasPagamento registroFormasPagamento) {
        this.registroOpcoesEntrega = registroOpcoesEntrega;
        this.registroCupons = registroCupons;
        this.registroNiveisClube = registroNiveisClube;
        this.registroFormasPagamento = registroFormasPagamento;
    }

    public ResumoResponseDTO calcular(ResumoRequestDTO request) {
        List<Item> itens = validarItens(request.itens());
        BigDecimal subtotalProdutos = Arredondamento.paraCentavos(somarSubtotal(itens));
        BigDecimal pesoTotal = somarPeso(itens);

        NivelClube nivel = validarNivelClube(request.nivelClube());
        Regiao regiao = validarRegiao(request.regiao());
        OpcaoEntrega opcaoEntrega = validarModalidade(request.modalidadeEntrega(), pesoTotal);

        Optional<Cupom> cupom = validarCupom(request.cupom(), itens, subtotalProdutos);

        int parcelas = request.parcelas() == null ? 1 : request.parcelas();
        CalculoPagamento calculoPagamento = validarFormaPagamento(request.formaPagamento());
        if (!calculoPagamento.parcelaPermitida(parcelas)) {
            throw new CheckoutException(ErroCodigo.PARCELAMENTO_INVALIDO);
        }

        BigDecimal freteBase = Arredondamento.paraCentavos(opcaoEntrega.custo(pesoTotal));
        BigDecimal freteExibido = nivel.freteGratis() ? BigDecimal.ZERO.setScale(2) : freteBase;

        BigDecimal seguro = Arredondamento.paraCentavos(subtotalProdutos.multiply(regiao.percentualSeguro()));

        BigDecimal descontoCupom = cupom
                .map(c -> Arredondamento.paraCentavos(c.desconto(itens, subtotalProdutos, freteExibido)))
                .orElse(BigDecimal.ZERO.setScale(2));

        BigDecimal totalPedido = subtotalProdutos.subtract(descontoCupom).add(freteExibido).add(seguro);

        if (!calculoPagamento.disponivel(totalPedido)) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INDISPONIVEL);
        }

        ResultadoPagamento resultadoPagamento = calculoPagamento.calcular(totalPedido, parcelas);

        BigDecimal creditoProximaCompra = Arredondamento.paraCentavos(
                subtotalProdutos.multiply(nivel.percentualCredito()));
        boolean brinde = nivel.elegivelBrinde(subtotalProdutos);

        return new ResumoResponseDTO(
                subtotalProdutos,
                descontoCupom,
                freteExibido,
                opcaoEntrega.prazoDias(),
                seguro,
                resultadoPagamento.ajuste(),
                resultadoPagamento.totalFinal(),
                parcelas,
                resultadoPagamento.valorParcela(),
                creditoProximaCompra,
                brinde);
    }

    private List<Item> validarItens(List<ItemRequestDTO> itensRequest) {
        if (itensRequest == null || itensRequest.isEmpty()) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        List<Item> itens = itensRequest.stream()
                .map(dto -> new Item(
                        dto.nome(),
                        dto.precoUnitario(),
                        dto.quantidade() == null ? 0 : dto.quantidade(),
                        dto.pesoKg()))
                .toList();
        boolean todosValidos = itens.stream().allMatch(Item::valido);
        if (!todosValidos) {
            throw new CheckoutException(ErroCodigo.PEDIDO_INVALIDO);
        }
        return itens;
    }

    private BigDecimal somarSubtotal(List<Item> itens) {
        return itens.stream().map(Item::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal somarPeso(List<Item> itens) {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private NivelClube validarNivelClube(String codigo) {
        if (codigo == null) {
            throw new CheckoutException(ErroCodigo.NIVEL_CLUBE_INVALIDO);
        }
        return registroNiveisClube.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCodigo.NIVEL_CLUBE_INVALIDO));
    }

    private Regiao validarRegiao(String codigo) {
        if (codigo == null) {
            throw new CheckoutException(ErroCodigo.REGIAO_INVALIDA);
        }
        try {
            return Regiao.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new CheckoutException(ErroCodigo.REGIAO_INVALIDA);
        }
    }

    private OpcaoEntrega validarModalidade(String codigo, BigDecimal pesoTotal) {
        if (codigo == null) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA);
        }
        OpcaoEntrega opcaoEntrega = registroOpcoesEntrega.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCodigo.MODALIDADE_INVALIDA));
        if (!opcaoEntrega.disponivel(pesoTotal)) {
            throw new CheckoutException(ErroCodigo.MODALIDADE_INDISPONIVEL);
        }
        return opcaoEntrega;
    }

    private Optional<Cupom> validarCupom(String codigo, List<Item> itens, BigDecimal subtotalProdutos) {
        if (codigo == null) {
            return Optional.empty();
        }
        Cupom cupom = registroCupons.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCodigo.CUPOM_INVALIDO));
        if (!cupom.aplicavel(itens, subtotalProdutos)) {
            throw new CheckoutException(ErroCodigo.CUPOM_NAO_APLICAVEL);
        }
        return Optional.of(cupom);
    }

    private CalculoPagamento validarFormaPagamento(String codigo) {
        if (codigo == null) {
            throw new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA);
        }
        return registroFormasPagamento.buscar(codigo)
                .orElseThrow(() -> new CheckoutException(ErroCodigo.FORMA_PAGAMENTO_INVALIDA));
    }
}
