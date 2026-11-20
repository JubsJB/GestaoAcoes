package com.projeto.persistence;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;

/** Field-scoped civil DATE binding, with explicit PostgreSQL legacy BC reads. */
public final class ReferenciaDataJdbcType extends LocalDateJdbcType {
    @Override
    public int getJdbcTypeCode() {
        return Types.DATE;
    }

    private boolean legacyBc(LocalDate civil, WrapperOptions options) {
        return civil != null && civil.getYear() <= 0
                && options.getSessionFactory().getJdbcServices().getDialect() instanceof PostgreSQLDialect;
    }

    @Override
    public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override
            protected T doExtract(ResultSet result, int index, WrapperOptions options) throws SQLException {
                LocalDate civil = result.getObject(index, LocalDate.class);
                // Delegate BC to the previous codec, including its pre-existing errors.
                return javaType.wrap(legacyBc(civil, options) ? result.getDate(index) : civil, options);
            }

            @Override
            protected T doExtract(CallableStatement statement, int index, WrapperOptions options) throws SQLException {
                LocalDate civil = statement.getObject(index, LocalDate.class);
                return javaType.wrap(legacyBc(civil, options) ? statement.getDate(index) : civil, options);
            }

            @Override
            protected T doExtract(CallableStatement statement, String name, WrapperOptions options) throws SQLException {
                LocalDate civil = statement.getObject(name, LocalDate.class);
                return javaType.wrap(legacyBc(civil, options) ? statement.getDate(name) : civil, options);
            }
        };
    }
}
