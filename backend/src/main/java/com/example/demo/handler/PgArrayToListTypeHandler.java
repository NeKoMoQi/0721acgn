package com.example.demo.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

public class PgArrayToListTypeHandler extends BaseTypeHandler<List<Double>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Double> list, JdbcType jdbcType) {
        // 不需要实现写入操作
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return convertArrayToList(rs.getArray(columnName));
    }

    @Override
    public List<Double> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return convertArrayToList(rs.getArray(columnIndex));
    }

    @Override
    public List<Double> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return convertArrayToList(cs.getArray(columnIndex));
    }

    private List<Double> convertArrayToList(Array pgArray) throws SQLException {
        if (pgArray == null) return null;
        Object[] array = (Object[]) pgArray.getArray();
        return Arrays.stream(array)
                .map(o -> ((Number) o).doubleValue())
                .toList();
    }
}