package test.app.controller;

import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import test.app.model.Constants;
import test.app.model.Order;


import javax.annotation.PostConstruct;
import java.util.List;

public class CacheLoader {

    private static final Logger logger = LoggerFactory.getLogger(CacheLoader.class);

    @Autowired
    private HazelcastInstance cache;

//    @Autowired
//    private OrderService orderService;
//
//    @PostConstruct
//    public void loadDataToCache() {
//        List<Order> allOrders = orderService.getAll();
//        cache.getList(Constants.CATEGORY_FINISHED).addAll(allOrders);
//        logger.info("{} categories loaded to cache", allOrders.size());
//    }
}
