package scc.data;

import java.util.Date;

//Rental: Each rental should include information about the house being rented, the
//user renting the house, the period and price of the rental.
//make the Rental class

public class Rental {
    private String id;
    private String houseId;
    private String userId;
    private Date day;
    private double price;

    public Rental(String id, String houseId, String userId, Date day, double price) {
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.day = day;
        this.price = price;
    }

    public Rental(RentalDao rental) {
        this(rental.getId(), rental.getHouseId(), rental.getUserId(), rental.getDay(), rental.getPrice());
    }

    public Rental(){

    }

    //getters
    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getUserId() {
        return userId;
    }

    public Date getDay() {
        return day;
    }

    public double getPrice() {
        return price;
    }

    //setters
    public void setPrice(double price) {
        this.price = price;
    }
}
