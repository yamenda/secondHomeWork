//package test.app.service;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import test.app.Util.IterableConverter;
//import test.app.model.Order;
//import test.app.repository.OrderRepository;
//
//import java.util.List;
//
//@Service
//public class OrderService {
//
//    @Autowired
//    OrderRepository orderRepository;
//
//    public Order insert(Order order) {
//        return this.orderRepository.save(order);
//    }
//
//    public List<Order> getAll() {
//        return IterableConverter.toList(this.orderRepository.findAll());
//    }
//
//    public Order find(Integer id) {
//        return this.orderRepository.findOne(id);
//    }
//
//}
