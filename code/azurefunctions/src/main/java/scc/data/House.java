package scc.data;

import java.util.List;

public class House {
    private String id;
    private String ownerId;
    private String name;
    private String location;
    private String description;
    private List<String> photoIds;
    private String discountMonth;


    public House(String id, String ownerId, String name, String location, String description, List<String> photos, String discountMonth) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoIds = photos;
        this.discountMonth = discountMonth;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public House(HouseDao house) {
        this(house.getId(), house.getOwnerId(), house.getName(), house.getLocation(), house.getDescription(), house.getPhotoIds(), house.getDiscountMonth());
    }

    public House(){

    }

    public String getDiscountMonth() {
        return discountMonth;
    }

    public String getId() {
        return id;
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

    public void setId(String id) {
        this.id = id;
    }

}