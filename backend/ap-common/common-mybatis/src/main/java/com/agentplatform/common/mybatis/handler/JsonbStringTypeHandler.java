package com.agentplatform.common.mybatis.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis type handler for PostgreSQL JSONB columns stored as String in Java.
 * Uses PGobject to correctly bind the value as jsonb type during inserts/updates,
 * avoiding the "column is of type jsonb but expression is of type character varying" error.
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbStringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject pgObj = new PGobject();
        pgObj.setType("jsonb");
        pgObj.setValue(parameter);
        ps.setObject(i, pgObj);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
