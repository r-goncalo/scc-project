package scc.data;

import java.util.List;

public class HouseDao {

    private String _rid;
    private String _ts;
    private String id;
    private String name;
    private String location;
    private String description;
    private List<String> photoIds;
    private double normalPrice;
    private double promotionPrice;

    public HouseDao(String id, String name, String location, String description, List<String> photos, double normalPrice, double promotionPrice) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.description = description;
        this.photoIds = photos;
        this.normalPrice = normalPrice;
        this.promotionPrice = promotionPrice;
    }

    public House toHouse(){
        return new House(this);//todo acho que pode dar problemas
    }

    public HouseDao(House house) {
        this(house.getId(), house.getName(), house.getLocation(), house.getDescription(), house.getPhotoIds(), house.getNormalPrice(), house.getPromotionPrice());
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
