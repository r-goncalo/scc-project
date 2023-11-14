package scc.data;

public class Availabity {

    private String id;
    private String houseId;
    private String fromDate;
    private String toDate;
    private double cost;
    private double discount; //to compute final price, multiply cost by discount

    public Availabity(String id, String houseId, String fromDate, String toDate, double cost, double discount) {
        this.id = id;
        this.houseId = houseId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.cost = cost;
        this.discount = discount;
    }

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public Availabity(AvailabityDao availabity) {
        this(availabity.getId(), availabity.getHouseId(), availabity.getFromDate(), availabity.getToDate(), availabity.getCost(), availabity.isDiscount());
    }

    public Availabity() {

    }

    public String getFromDate() {
        return fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public double getCost() {
        return cost;
    }

    public double getDiscount() {
        return discount;
    }

    public void setId(String id) {
        this.id = id;
    }
}