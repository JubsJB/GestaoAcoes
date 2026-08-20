package com.projeto.gestaoacoes;

import com.projeto.GestaoacoesApplication;
import com.projeto.services.DBService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest(classes = GestaoacoesApplication.class)
@ActiveProfiles("test")
class GestaoacoesApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoadsAndDiscoversComponents() {
        Assertions.assertNotNull(applicationContext.getBean(DBService.class));
    }

    @Test
    void usesOnlyTestProfileAndH2() throws SQLException {
        Assertions.assertArrayEquals(new String[]{"test"}, environment.getActiveProfiles());

        try (Connection connection = dataSource.getConnection()) {
            Assertions.assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
        }
    }

}
