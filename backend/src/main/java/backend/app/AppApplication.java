package backend.app;

import backend.app.controller.BackendAPI;
import backend.app.controller.HTTPClient;
import backend.app.model.Constants;
import ch.maxant.generic_jca_adapter.CommitRollbackCallback;
import ch.maxant.generic_jca_adapter.MicroserviceXAResource;
import ch.maxant.generic_jca_adapter.TransactionConfigurator;
import com.atomikos.icatch.config.UserTransactionServiceImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jms.extra.MessageDrivenContainer;
import com.mysql.jdbc.jdbc2.optional.MysqlXADataSource;
import org.apache.activemq.ActiveMQXAConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jta.atomikos.AtomikosConnectionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.Queue;
import javax.sql.XADataSource;
import javax.transaction.SystemException;
import java.io.File;
import java.io.IOException;


@Configuration
@SpringBootApplication
public class AppApplication {

	private static final File TX_LOG_PATH = new File("E:\\projects\\testclient\\transaction_log\\");
	private static final File TX_LOG_PATH_PRODUCT_SERVICE = new File(TX_LOG_PATH, "logs_product_service");
	private static final File TX_LOG_PATH_CATEGORY_SERVICE = new File(TX_LOG_PATH, "logs_category_service");

	public static void main(String[] args)  throws IOException {

		FileUtils.cleanDirectory(TX_LOG_PATH);
		TX_LOG_PATH_PRODUCT_SERVICE.mkdirs();
		TX_LOG_PATH_CATEGORY_SERVICE.mkdirs();

		SpringApplication.run(AppApplication.class, args);
	}


	// JMS configuration
	@Bean("jmsTemplate")
	public JmsTemplate jmsTemplate() {
		JmsTemplate template = new JmsTemplate(amqAtomikosXA());
		template.setSessionTransacted(true); // TODO probably doesn't make sense for Atomimks, only needed if nonxa tx used
		return template;
	}

	@Bean("requestQueue")
	public Queue requestQueue() {
		return new ActiveMQQueue("requestQueue");
	}

	@Bean("responseQueue")
	public Queue responseQueue() {
		return new ActiveMQQueue("responseQueue");
	}

	@Autowired
	@Bean(name = "jmsContainer", initMethod = "start", destroyMethod = "stop")
	public MessageDrivenContainer requestJMSContainer(BackendAPI listener) {
		MessageDrivenContainer container = new MessageDrivenContainer();
		container.setAtomikosConnectionFactoryBean(amqAtomikosXA());
		container.setTransactionTimeout(100);
		container.setDestination(requestQueue());
		container.setMessageListener(listener);
		return container;
	}

	@Bean("xaConnectionFactory")
	public ActiveMQXAConnectionFactory xaConnectionFactory() {
		ActiveMQXAConnectionFactory amq = new ActiveMQXAConnectionFactory("tcp://localhost:61616");

		RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
		redeliveryPolicy.setMaximumRedeliveries(10);
		redeliveryPolicy.setInitialRedeliveryDelay(500); // 5 seconds redelivery delay
		redeliveryPolicy.setBackOffMultiplier(2);
		redeliveryPolicy.setUseExponentialBackOff(true);

		amq.setRedeliveryPolicy(redeliveryPolicy);

		// TODO define policy for specific queues
//        RedeliveryPolicyMap map = new RedeliveryPolicyMap();
//        map.setRedeliveryPolicyEntries();

		return amq;
	}

	@Primary
	@Bean(initMethod = "init", destroyMethod = "close")
	public AtomikosConnectionFactoryBean amqAtomikosXA() {
		AtomikosConnectionFactoryBean ds = new AtomikosConnectionFactoryBean();
		ds.setUniqueResourceName("activemq");
		ds.setMaxPoolSize(10);
		ds.setMinPoolSize(5);
		ds.setXaConnectionFactory(xaConnectionFactory());
		return ds;
	}

	// DB configuration
	@Bean("agencyDB")
	public JdbcTemplate agencyDB() {
		return new JdbcTemplate(agencyAtomikosDS());
	}



	@Primary
	@Bean("apidb")
	public XADataSource apidb() {
		MysqlXADataSource xaDataSource = new MysqlXADataSource();
		xaDataSource.setPort(3306);
		xaDataSource.setServerName("localhost");
		xaDataSource.setUser("root");
		xaDataSource.setPassword("");
		xaDataSource.setDatabaseName("apidb");
		xaDataSource.setPinGlobalTxToPhysicalConnection(true); // https://www.atomikos.com/Documentation/KnownProblems#MySQL_XA_bug
		return xaDataSource;
	}


	@Bean(name = "agencyAtomikosDS", initMethod = "init", destroyMethod = "close")
	public AtomikosDataSourceBean agencyAtomikosDS() {
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
//        ds.setLogWriter(null); // TODO try custom log writer, do we need log reader?
		ds.setUniqueResourceName("apidb");
		ds.setXaDataSource(apidb());
		ds.setMaxPoolSize(10);
		ds.setMinPoolSize(5);
		ds.setTestQuery("select 1");
		return ds;
	}

	@Primary
	@Bean(name = "xaTransactionManager", initMethod = "init", destroyMethod = "close")
	public UserTransactionManager xaTransactionManager() throws SystemException {
		UserTransactionManager txManager = new UserTransactionManager();
		txManager.setTransactionTimeout(300);
		txManager.setForceShutdown(false);
		return txManager;
	}

	// TODO why do we need both transactionService and transactionManager?
	// TODO what is the difference between them?
	@Bean(name = "xaTransactionService", initMethod = "init", destroyMethod = "shutdownWait")
	public UserTransactionServiceImp xaTransactionService() {
		return new UserTransactionServiceImp();
	}

	// TODO add LocalLogAdministrator

	@Bean("categoryService")
	public HTTPClient categoryService() {
		return new HTTPClient("localhost:7001/category");
	}

	@Bean("productService")
	public HTTPClient productService() {
		return new HTTPClient("localhost:7002/product");
	}

	@Bean
	@Autowired
	public CommitRollbackCallback carRentalServiceTransactionWrapper(
			@Qualifier("productService") HTTPClient productService) {
		CommitRollbackCallback callback = new CommitRollbackCallback() {
			@Override
			public void commit(String xaId) throws Exception {
				productService.confirm(xaId);
			}

			@Override
			public void rollback(String xaId) throws Exception {
				productService.cancel(xaId);
			}
		};

		TransactionConfigurator.setup("xa/productService", callback);
		// After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
		// We assume that external system automatically rollbacks transaction after >=30 seconds

		// We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
		MicroserviceXAResource.configure(30000L, TX_LOG_PATH_PRODUCT_SERVICE);
		return callback;
	}

	@Bean
	@Autowired
	public CommitRollbackCallback flightBookingServiceTransactionWrapper(
			@Qualifier("categoryService") HTTPClient categoryService) {
		CommitRollbackCallback callback = new CommitRollbackCallback() {
			@Override
			public void commit(String xaId) throws Exception {
				categoryService.confirm(xaId);
			}

			@Override
			public void rollback(String xaId) throws Exception {
				categoryService.cancel(xaId);
			}
		};

		TransactionConfigurator.setup("xa/categoryService", callback);
		// After 30 seconds TransactionManager will not recover failed transactions. This parameter depends on external system configuration.
		// We assume that external system automatically rollbacks transaction after >=30 seconds

		// We are using file storage for transaction recovery records. That may be any different storage (even JMS queues)
		MicroserviceXAResource.configure(30000L, TX_LOG_PATH_CATEGORY_SERVICE);
		return callback;
	}
}
