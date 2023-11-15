package scc.data;

public class Rental {
    private String id;
    private String houseId;
    private String renterId;
    private String period; //assume rentals take place on a single month
    private double price;

    public Rental(String id, String houseId, String renterId, String period, double price) {
        this.id = id;
        this.houseId = houseId;
        this.renterId = renterId;
        this.period = period;
        this.price = price;
    }

    public Rental(RentalDao rental) {
        this(rental.getId(), rental.getHouseId(), rental.getRenterId(), rental.getPeriod(), rental.getPrice());
    }

    public Rental(){

    }

    //getters
    public String getPeriod() {
        return period;
    }

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getRenterId() {
        return renterId;
    }

    public double getPrice() {
        return price;
    }

    //setters
    public void setPrice(double price) {
        this.price = price;
    }

    public void setId(String id) {
        this.id = id;
    }
}