package scc.data;

import java.util.List;

public class House {
    private String id;
    private String ownerId;
    private String renterId;
    private String name;
    private String location;
    private String description;
    private List<String> photoIds;
    private double normalPrice;
    private double promotionPrice;


    public House(String id, String ownerId, String renterId, String name, String location, String description, List<String> photos, double normalPrice, double promotionPrice) {
        this.id = id;
        this.ownerId = ownerId;
        this.renterId = renterId;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoIds = photos;
        this.normalPrice = normalPrice;
        this.promotionPrice = promotionPrice;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getRenterId() {
        return renterId;
    }

    public House(HouseDao house) {
        this(house.getId(), house.getOwnerId(), house.getRenterID(), house.getName(), house.getLocation(), house.getDescription(), house.getPhotoIds(), house.getNormalPrice(), house.getPromotionPrice());
    }

    public House(){

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

    public double getNormalPrice() {
        return normalPrice;
    }

    public double getPromotionPrice() {
        return promotionPrice;
    }
}
