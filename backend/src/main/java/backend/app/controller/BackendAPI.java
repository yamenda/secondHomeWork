package backend.app.controller;

import backend.app.model.Order;
import backend.app.model.Request;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactory;
import ch.maxant.generic_jca_adapter.BasicTransactionAssistanceFactoryImpl;
import ch.maxant.generic_jca_adapter.TransactionAssistant;
import com.atomikos.icatch.jta.UserTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import java.util.UUID;

@RestController
public class BackendAPI implements MessageListener {

    private static final Logger logger = LoggerFactory.getLogger(BackendAPI.class);

    @Autowired
    @Qualifier("categoryService")
    protected HTTPClient categoryService;

    @Autowired
    @Qualifier("productService")
    protected HTTPClient productService;

    @Autowired
    @Qualifier("agencyDB")
    private JdbcTemplate agency;


    @Autowired
    @Qualifier("jmsTemplate")
    private JmsTemplate jms;

    @Autowired
    @Qualifier("responseQueue")
    private Queue responseQueue;

    @Autowired
    @Qualifier("xaTransactionManager")
    protected UserTransactionManager tm;

    @PostMapping("/backend/performBooking")
    public String bookAll(@RequestBody Order request) throws Exception {
        tm.begin(); // Start JTA transaction
        logger.info("Received WS-request {} transactionId={}", request, tm.getTransaction());
        try {
            performBooking(request);
            tm.commit();
            return request.getIdentifier();
        } catch (Exception e) {
            logger.error("Error processing WS-request {} ({})", request, e.getMessage());
            tm.rollback();
            throw e;
        }
    }

    // TODO after getting messages redelivered several times, following message is printed by Atomikos
    // TODO Possible poison message detected - check https://www.atomikos.com/Documentation/PoisonMessage:
    // TODO see MessageConsumerSession.checkRedeliveryLimit() - this code only prints warning, but doesn't prevent from further redelovery?
    // We are already in JTA transaction because we are using Atomikos JMS container
    @Override
    public void onMessage(Message message) {
        logger.info("receiving message...", message);
        try {
            Order request = Order.from(message);
            logger.info("Received JMS-request {} transactionId={}", request, tm.getTransaction());
            performBooking(request);
        } catch (Exception e) {
            logger.error("Error processing JMS-request {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    // Must be in a global XA transaction, otherwise SAGAs can be used
    protected void performBooking(Order rq) throws Exception {
        // Booking identifier
        String orderingId = UUID.randomUUID().toString();
        rq.setIdentifier(orderingId);

        // Storing booking in agency DB
        agency.update("INSERT INTO orders (id,identifier,type, name , price, brand) VALUES (?,?,?,?,?,?)",
                rq.getId(), rq.getIdentifier(),  rq.getType(), rq.getName(), rq.getPrice(), rq.getBrand());

        if(rq.type.equals("category")) {
            BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/categoryService");
            try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()) {
                String categoryId = transactionAssistant.executeInActiveTransaction(
                        xaTransactionId -> categoryService.add(new Request(rq), xaTransactionId));
        }catch (Exception e) {
            e.printStackTrace();
        }
        }else if(rq.type.equals("product")){
            BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/productService");
            try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()) {
                String productId = transactionAssistant.executeInActiveTransaction(
                        xaTransactionId -> productService.add(new Request(rq), xaTransactionId));
            }catch (Exception e){
                e.printStackTrace();
            }
        }else if(rq.type.equals("both")){
            BasicTransactionAssistanceFactory transferServiceTransactionFactory = new BasicTransactionAssistanceFactoryImpl("xa/productService");
            try (TransactionAssistant transactionAssistant = transferServiceTransactionFactory.getTransactionAssistant()) {
                String cateoryId = transactionAssistant.executeInActiveTransaction(
                        xaTransactionId -> categoryService.add(new Request(rq), xaTransactionId));

                int testCatId = 0;
                try{
                    testCatId = Integer.parseInt(cateoryId);
                }catch (Exception e) {
                    e.printStackTrace();
                }

                rq.setCateforyId(testCatId);

                String productId = transactionAssistant.executeInActiveTransaction(
                        xaTransactionId -> productService.add(new Request(rq), xaTransactionId));

            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Send response back via JMS
        jms.send(responseQueue, session -> rq.to(session.createTextMessage()));
    }
}