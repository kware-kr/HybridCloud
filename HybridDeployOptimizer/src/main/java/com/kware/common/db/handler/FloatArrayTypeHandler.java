package com.kware.common.db.handler;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;




/**
 * @author Manni Wood
 */
@MappedJdbcTypes(JdbcType.OTHER)
@MappedTypes(Float[].class)
public class FloatArrayTypeHandler extends BaseTypeHandler<Float[]> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
            Float[] parameter, JdbcType jdbcType) throws SQLException {
        Connection c = ps.getConnection();
        Array inArray = c.createArrayOf("float", parameter);
        ps.setArray(i, inArray);
    }

    @Override
    public Float[] getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        Array outputArray = rs.getArray(columnName);
        if (outputArray == null) {
            return null;
        }
        return (Float[])outputArray.getArray();
    }

    @Override
    public Float[] getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        Array outputArray = rs.getArray(columnIndex);
        if (outputArray == null) {
            return null;
        }
        return (Float[])outputArray.getArray();
    }

    @Override
    public Float[] getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        Array outputArray = cs.getArray(columnIndex);
        if (outputArray == null) {
            return null;
        }
        return (Float[])outputArray.getArray();
    }
}