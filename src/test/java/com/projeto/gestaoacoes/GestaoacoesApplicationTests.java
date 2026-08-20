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
        Assertions.assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));

        try (Connection connection = dataSource.getConnection()) {
            Assertions.assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:"));
            try (var table = connection.getMetaData().getTables(null, null, "CORRETORA", new String[]{"TABLE"})) {
                Assertions.assertTrue(table.next(), "Liquibase deve criar a tabela CORRETORA antes do Hibernate validate");
            }
            try (var changelog = connection.createStatement().executeQuery(
                    "SELECT COUNT(*) FROM DATABASECHANGELOG WHERE ID = '001-create-corretora'"
            )) {
                Assertions.assertTrue(changelog.next());
                Assertions.assertEquals(1, changelog.getInt(1));
            }
        }
    }

}
