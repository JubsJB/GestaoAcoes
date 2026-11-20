package com.projeto.temporalpoc;

import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.LocalDateJdbcType;
import java.sql.*;
import java.time.LocalDate;

/** Exclusivo do protótipo: escrita direta e leitura BC delegada ao caminho anterior PostgreSQL. */
final class BcCompatibilityJdbcType extends LocalDateJdbcType {
    private final boolean postgres;

    BcCompatibilityJdbcType(boolean postgres) { this.postgres = postgres; }

    @Override public int getJdbcTypeCode() { return Types.DATE; }

    @Override public <T> ValueExtractor<T> getExtractor(JavaType<T> javaType) {
        return new BasicExtractor<>(javaType, this) {
            @Override protected T doExtract(ResultSet r, int index, WrapperOptions options) throws SQLException {
                LocalDate civil = r.getObject(index, LocalDate.class);
                return javaType.wrap(postgres && civil != null && civil.getYear() <= 0
                        ? r.getDate(index) : civil, options);
            }
            @Override protected T doExtract(CallableStatement s, int index, WrapperOptions options) throws SQLException {
                LocalDate civil = s.getObject(index, LocalDate.class);
                return javaType.wrap(postgres && civil != null && civil.getYear() <= 0
                        ? s.getDate(index) : civil, options);
            }
            @Override protected T doExtract(CallableStatement s, String name, WrapperOptions options) throws SQLException {
                LocalDate civil = s.getObject(name, LocalDate.class);
                return javaType.wrap(postgres && civil != null && civil.getYear() <= 0
                        ? s.getDate(name) : civil, options);
            }
        };
    }
}
