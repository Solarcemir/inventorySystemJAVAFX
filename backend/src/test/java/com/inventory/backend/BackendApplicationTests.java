package com.inventory.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IsolatedDatabaseTest
class BackendApplicationTests {

	@Autowired
	private DataSource dataSource;

	@Test
	void testsRunOnTheInMemoryDatabase() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
		}
	}

}
