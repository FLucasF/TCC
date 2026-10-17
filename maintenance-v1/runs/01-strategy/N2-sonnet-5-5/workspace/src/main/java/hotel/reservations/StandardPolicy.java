package hotel.reservations;

final class StandardPolicy extends FullRefundPolicy {

    @Override
    public Price price(Stay stay, Price fullPrice) {
        return fullPrice;
    }
}
