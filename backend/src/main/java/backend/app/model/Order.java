package backend.app.model;

import com.google.gson.Gson;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.io.Serializable;


public class Order implements Serializable {

    private static final String IDENTIFIER = "IDENTIFIER";

    public int id;
    public int cateforyId = -1;
    private String identifier;
    public String type = "";
    public String name = "";
    public int price = 0;
    public String brand = "";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCateforyId() {
        return cateforyId;
    }

    public void setCateforyId(int cateforyId) {
        this.cateforyId = cateforyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Order() {
    }

    public Order(int id, int cateforyId, String type, String name, int price, String brand) {
        this.id = id;
        this.cateforyId = cateforyId;
        this.type = type;
        this.name = name;
        this.price = price;
        this.brand = brand;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TextMessage to(TextMessage msg) throws JMSException {
        msg.setText(new Gson().toJson(this));
        return msg;
    }

    public static Order from(Message msg) throws JMSException {
        if (msg instanceof TextMessage) {
            return new Gson().fromJson(((TextMessage) msg).getText(), Order.class);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Order that = (Order) o;

        return identifier != null ? identifier.equals(that.identifier) : that.identifier == null;

    }

    @Override
    public int hashCode() {
        return identifier != null ? identifier.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Order{" +
                "identifier='" + identifier + '\'' +
                ", type ='" + type + '\'' +
                ", name ='" + name + '\'' +
                ", price ='" + price + '\'' +
                ", brand ='" + brand + '\'' +
                ", id='" + id + '\'' +
                '}';
    }


}
