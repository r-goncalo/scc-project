package scc.data;

import java.util.List;

public class HouseDao {

    private String _rid;
    private String _ts;
    private String id;
    private String ownerId;
    private String name;
    private String location;
    private String description;
    private List<String> photoIds;
    // discount month
    private String discountMonth;

    public HouseDao(String id, String ownerId, String name, String location, String description, List<String> photos, String discountMonth) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoIds = photos;
        this.discountMonth = discountMonth;
    }

    public House toHouse(){
        return new House(this);//todo acho que pode dar problemas
    }

    public HouseDao(House house) {
        this(house.getId(), house.getOwnerId(), house.getName(), house.getLocation(), house.getDescription(), house.getPhotoIds(), house.getDiscountMonth());
    }

    public HouseDao(){

    }

    public String get_rid() {
        return _rid;
    }

    public String get_ts() {
        return _ts;
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPhotoIds() {
        return photoIds;
    }

    public String getDiscountMonth() {
        return discountMonth;
    }
    // setters

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public void setId(String id) {
        this.id = id;
    }
}