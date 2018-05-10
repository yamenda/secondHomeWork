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

	public String image;
	public int parent_id;
	public String name;
	public int identity;


	@PostMapping("/category/add")
	public String addcat(@RequestBody Map<String, String> rq) {

		String categoryIdentity = "category-" + UUID.randomUUID().toString();

		this.identity = 0;
		this.name = rq.get("name");
		this.parent_id = 0;
		this.image = "";


		String insertTableSQL = "INSERT INTO categories"
				+ "(identity, parent_id, name, image) VALUES"
				+ "(?,?, ?, ?)";

		int insertingId = db().update(insertTableSQL, categoryIdentity, 0, rq.get("name"), "");

		if (insertingId < 1) {
			throw new RuntimeException("error in inserting category");
		}

		String theId = "" + insertingId;

		return theId;
	}


	@PostMapping("/category/{orderingId}/confirm")
	public void confirmAdding(@PathVariable String orderingId) {

	}

	@PostMapping("/category/{orderingId}/cancel")
	public void canceladding(@PathVariable String orderingId) {

	}

	// Needed by XA resource
	@GetMapping("/category/unfinished")
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
		dataSource.setDatabaseName("apidb");
		return dataSource;
	}


}
