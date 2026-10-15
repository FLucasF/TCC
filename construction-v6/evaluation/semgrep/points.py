"""The names the measures look for. Used by the rule generator (rules.py) and by the classifier (classify.py),
so the two never diverge. This is the only table that changes for another prompt.

The prompt's API is in English, so the cases are the English codes. "synonyms" are the same case with another
name in the code: the Portuguese names (a model may translate the API codes into its own Portuguese enum) and
other English spellings. Without them, "tier == OURO" would pass silently as "isolated". "classes" are name
fragments that point to the case (GoldTier, ExpressShipping, PixPayment): they serve the instanceof, the
pattern-matching switch (case Gold g ->) and the factory that returns an object (case "GOLD" -> gold;).
"""

from dataclasses import dataclass, field


@dataclass(frozen=True)
class Point:
    """A point where the prompt has cases: the Strategy points, and the order statuses."""

    id: str
    label: str
    cases: tuple[str, ...]
    synonyms: dict[str, str] = field(default_factory=dict)
    classes: str = ""

    @property
    def names(self) -> list[str]:
        """All the names of the point: the prompt's and the synonyms."""
        return [*self.cases, *self.synonyms]

    def case_of(self, name: str) -> str:
        return self.synonyms.get(name, name)


# P1 to P4: the Strategy points (the questions of V5's régua: location and selection).
POINTS = (
    Point("p1", "shipping method", ("ECONOMY", "EXPRESS", "STORE_PICKUP", "COURIER"),
          {"ECONOMICA": "ECONOMY", "EXPRESSA": "EXPRESS", "RETIRADA_LOJA": "STORE_PICKUP",
           "IN_STORE_PICKUP": "STORE_PICKUP", "PICKUP": "STORE_PICKUP", "MOTOBOY": "COURIER", "MOTORCYCLE": "COURIER"},
          "Economy|Express|Pickup|Courier|Economica|Expressa|Retirada|Motoboy|Motorcycle"),
    Point("p2", "coupon", ("WELCOME10", "MINUS50", "FREESHIPPING", "BUY3PAY2"),
          {"BEMVINDO10": "WELCOME10", "MENOS50": "MINUS50", "FIFTY_OFF": "MINUS50", "FREE_SHIPPING": "FREESHIPPING",
           "FRETEGRATIS": "FREESHIPPING", "BUY3_PAY2": "BUY3PAY2", "TAKE3PAY2": "BUY3PAY2", "LEVE3PAGUE2": "BUY3PAY2"},
          "Welcome|Minus50|FreeShipping|Freeshipping|Buy3|Take3|BemVindo|Bemvindo|Menos50|FreteGratis|Fretegratis|Leve3"),
    Point("p3", "payment method", ("PIX", "CREDIT_CARD", "BANK_SLIP"),
          {"CARD": "CREDIT_CARD", "CREDITCARD": "CREDIT_CARD", "CARTAO": "CREDIT_CARD", "BANKSLIP": "BANK_SLIP",
           "BILLET": "BANK_SLIP", "BOLETO": "BANK_SLIP"},
          "Pix|CreditCard|Card|BankSlip|Billet|Cartao|Boleto"),
    Point("p4", "club tier", ("BRONZE", "SILVER", "GOLD"),
          {"PRATA": "SILVER", "OURO": "GOLD"},
          "Bronze|Silver|Gold|Prata|Ouro"),
)

# The prompt's cross rules tie a case to a case of another point: FREESHIPPING does not apply to STORE_PICKUP, and
# GOLD's free shipping does not apply to COURIER. The rule has to name the other case somewhere, and the right
# place is one of the two homes (decided on 2026-10-14). So a case named inside its partner's home is not
# scattered; named in a third place (a service), it is. Each case -> the class name fragments of its partner.
CROSS_RULE_PARTNER_HOMES = {
    "STORE_PICKUP": "FreeShipping|Freeshipping|FreteGratis|Fretegratis",
    "FREESHIPPING": "Pickup|Retirada",
    "COURIER": "Gold|Ouro",
    "GOLD": "Courier|Motoboy|Motorcycle",
}

# P5, the negative control: the region (the insurance rate and limit, and the extra days). The prompt says only
# numbers change per region; a piece per region is the overengineering it measures.
REGIONS = ("SOUTHEAST", "SOUTH", "MIDWEST", "NORTH", "NORTHEAST")
REGION_SYNONYMS = {"CENTER_WEST": "MIDWEST", "CENTRAL_WEST": "MIDWEST", "SUDESTE": "SOUTHEAST", "SUL": "SOUTH",
                   "CENTRO_OESTE": "MIDWEST", "NORTE": "NORTH", "NORDESTE": "NORTHEAST"}
REGION_NAMES = [*REGIONS, *REGION_SYNONYMS]
REGION_CLASSES = "Southeast|South|Midwest|CenterWest|North|Northeast|Sudeste|Sul|CentroOeste|Norte|Nordeste"
REGION_WORDS = ("southeast|south|midwest|cent(?:er|ral)[-_]?west|north|northeast"
                "|sudeste|sul|centro[-_]?oeste|norte|nordeste")

# P6, the Adapter: the TransLog's own vocabulary. "wire" is what only the carrier's HTTP API speaks (its route,
# its field names, its error code); "codes" are its service codes. A good adapter keeps all of it in the class
# that makes the call (and in its response records); anywhere else, it leaked.
CARRIER_WIRE_TEXT = ("price_cents", "eta_hours", "ZIP_NOT_SERVED", "/v2/quote")
CARRIER_WIRE_NAMES = ("priceCents", "etaHours")
CARRIER_CODES = ("STD", "EXP")
HTTP_CLIENTS = ("RestClient|RestTemplate|WebClient|HttpClient|HttpURLConnection|HttpRequest|FeignClient"
                "|HttpExchange|PostExchange|GetExchange")

# P7, the State (secondary): the order statuses. The home of the decisions is the status itself (an enum with the
# transitions, or one class per status); a comparison with a status elsewhere is scattered.
STATUS = Point("p7", "order status", ("AWAITING_PAYMENT", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"),
               {"PENDING_PAYMENT": "AWAITING_PAYMENT", "CANCELED": "CANCELLED",
                "AGUARDANDO_PAGAMENTO": "AWAITING_PAYMENT",
                "PAGO": "PAID", "ENVIADO": "SHIPPED", "ENTREGUE": "DELIVERED", "CANCELADO": "CANCELLED"})
STATUS_HOME = "Status|State|Situacao|Estado"
# one class per status (the GoF State): PaidState, Shipped, Pago...
STATUS_CLASSES = "AwaitingPayment|Paid|Shipped|Delivered|Cancell?ed|Aguardando|Pago|Enviado|Entregue|Cancelado"

# Factory (descriptive, no hypothesis): the strategy classes are recognized by the point classes above.
FACTORY_CLASSES = "Factory|Builder|Fabrica|Construtor"
ALL_CLASSES = "|".join(p.classes for p in POINTS)
ALL_CASES = "|".join(n for p in POINTS for n in p.names)
