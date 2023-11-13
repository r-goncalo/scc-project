package scc.data;

public class Rental {
    private String id;
    private String houseId;
    private String renterID;
    private String date; //assume rentals take place on a single month
    private double price;

    public Rental(String id, String houseId, String renterID, String date, double price) {
        this.id = id;
        this.houseId = houseId;
        this.renterID = renterID;
        this.date = date;
        this.price = price;
    }

    public Rental(RentalDao rental) {
        this(rental.getId(), rental.getHouseId(), rental.getUserId(), rental.getDay(), rental.getPrice());
    }

    public Rental(){

    }

    //getters
    public String getDate() {
        return date;
    }

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getRenterID() {
        return renterID;
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
