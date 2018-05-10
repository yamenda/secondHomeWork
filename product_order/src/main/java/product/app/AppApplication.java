package product.app;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@RestController
@Configuration
@SpringBootApplication
public class AppApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppApplication.class, args);
	}

	@PostMapping("/product/add")
	public String addpro(@RequestBody Map<String, String> rq) {

		String productIdentity = "product-" + UUID.randomUUID().toString();

		String insertTableSQL = "INSERT INTO products"
				+ "(identity,category, name, price, prand) VALUES"
				+ "(?,?,?, ?)";

		int insertingId = db().update(insertTableSQL, rq.get("category_id"), rq.get("product_name"), rq.get("price"), rq.get("prand"));

		if (insertingId < 1) {
			throw new RuntimeException("error in inserting product");
		}

		return productIdentity;
	}

	@PostMapping("/product/{orderingId}/confirm")
	public void confirmAdding(@PathVariable String orderingId) {

	}

	@PostMapping("/product/{orderingId}/cancel")
	public void canceladding(@PathVariable String orderingId) {

	}

	// Needed by XA resource
	@GetMapping("/product/unfinished")
	public List<String> getUnfinishedTransfers() {
		List<String> temp = new ArrayList<>();
		return temp;
	}


	@Bean
	public JdbcTemplate db() {
		return new JdbcTemplate(dataSource());
	}

	@Bean
	public DataSource dataSource() {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setPort(3306);
		dataSource.setServerName("localhost");
		dataSource.setUser("root");
		dataSource.setPassword("");
		dataSource.setDatabaseName("apisecond");
		return dataSource;
	}


}
