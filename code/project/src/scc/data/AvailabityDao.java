package scc.data;

public class AvailabityDao {
    private String _rid;
    private String _ts;
    private String id;
    private String houseId;
    private String fromDate;
    private String toDate;
    private double cost;
    private boolean discount;

    public String getId() {
        return id;
    }

    public String getHouseId() {
        return houseId;
    }

    public AvailabityDao(String id, String houseId, String fromDate, String toDate, double cost, boolean discount) {
        this.id = id;
        this.houseId = houseId;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.cost = cost;
        this.discount = discount;
    }

    public AvailabityDao(Availabity availabity) {
        this(availabity.getId(), availabity.getHouseId(), availabity.getFromDate(), availabity.getToDate(), availabity.getCost(), availabity.isDiscount());
    }

    public AvailabityDao() {

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

    public boolean isDiscount() {
        return discount;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Availabity toAvailabity() {
        return new Availabity(this);
    }
}
