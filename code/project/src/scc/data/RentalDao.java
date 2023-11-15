package scc.data;

//Rental: Each rental should include information about the house being rented, the
//user renting the house, the period and price of the rental.
//make the Rental class

public class RentalDao {
    private String _rid;
    private String _ts;
    private String id;
    private String houseId;
    private String renterId;
    private String period;
    private double price;

    public RentalDao(String id, String houseId, String renterId, String period, double price) {
        this.id = id;
        this.houseId = houseId;
        this.renterId = renterId;
        this.period = period;
        this.price = price;
    }

    public RentalDao(Rental rental) {
        this(rental.getId(), rental.getHouseId(), rental.getRenterId(), rental.getPeriod(), rental.getPrice());
    }

    public RentalDao(){

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

    public Rental toRental() {
        return new Rental(this);
    }
}
