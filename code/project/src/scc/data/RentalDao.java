package scc.data;

import java.util.Date;

//Rental: Each rental should include information about the house being rented, the
//user renting the house, the period and price of the rental.
//make the Rental class

public class RentalDao {
    private String _rid;
    private String _ts;
    private String id;
    private String houseId;
    private String userId;
    private Date day;
    private double price;

    public RentalDao(String id, String houseId, String userId, Date day, double price) {
        this.id = id;
        this.houseId = houseId;
        this.userId = userId;
        this.day = day;
        this.price = price;
    }

    public RentalDao(Rental rental) {
        this(rental.getId(), rental.getHouseId(), rental.getUserId(), rental.getDay(), rental.getPrice());
    }

    public RentalDao(){

    }


    //getters
    public Date getDay() {
        return day;
    }

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public String getUserId() {
        return userId;
    }

    public double getPrice() {
        return price;
    }

    //setters
    public void setPrice(double price) {
        this.price = price;
    }
}
