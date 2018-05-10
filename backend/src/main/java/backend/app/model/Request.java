package backend.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

// TODO dates
@JsonIgnoreProperties(ignoreUnknown = true)
public class Request implements Serializable {

    private int id;
    private String orderingId;
    private String type;
    private String name;
    private String brand;
    private int price;

    public Request() {
    }

    public Request(Order order) {
        this.id = order.getId();
        this.orderingId = order.getIdentifier();
        this.type = order.getType();
        this.name = order.getName();
        this.price = order.getPrice();
        this.brand = order.getBrand();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOrderingId() {
        return orderingId;
    }

    public void setOrderingId(String orderingId) {
        this.orderingId = orderingId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Request{" +
                "orderingId='" + orderingId + '\'' +
                "id ='" + id + '\'' +
                "name ='" + name + '\'' +
                "brand='" + brand + '\'' +
                "price='" + price + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
