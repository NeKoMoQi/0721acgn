package com.example.demo.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 把 PostgreSQL 二维 double 数组 映射到 Java List<List<Double>>
 */
public class PgArrayToListListTypeHandler extends BaseTypeHandler<List<List<Double>>> {

    /* ===============  入库：Java → JDBC  =============== */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<List<Double>> parameter, JdbcType jdbcType) throws SQLException {
        Connection conn = ps.getConnection();
        // 转成二维原始数组
        Double[][] array = parameter.stream()
                .map(l -> l.toArray(new Double[0]))
                .toArray(Double[][]::new);
        Array sqlArray = conn.createArrayOf("float8", array);
        ps.setArray(i, sqlArray);
    }

    /* ===============  出库：JDBC → Java  =============== */
    @Override
    public List<List<Double>> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return extract(rs.getArray(columnName));
    }

    @Override
    public List<List<Double>> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return extract(rs.getArray(columnIndex));
    }

    @Override
    public List<List<Double>> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return extract(cs.getArray(columnIndex));
    }

    /* -------------------------------------------------- */
    private List<List<Double>> extract(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        // PostgreSQL 返回 Object[]，每个元素是 Double[]
        Object[] raw = (Object[]) sqlArray.getArray();
        return Arrays.stream(raw)
                .map(row -> Arrays.stream((Double[]) row)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}