//package com.example.demo.handler;
//
//import org.apache.ibatis.type.BaseTypeHandler;
//import org.apache.ibatis.type.JdbcType;
//import java.sql.*;
//
//public class PointTypeHandler extends BaseTypeHandler<double[]> {
//    @Override
//    public void setNonNullParameter(PreparedStatement ps, int i, double[] parameter, JdbcType jdbcType) throws SQLException {
//        Array array = ps.getConnection().createArrayOf("float8", parameter);
//        ps.setArray(i, array);
//    }
//
//    @Override
//    public double[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
//        Array array = rs.getArray(columnName);
//        return array != null ? (double[]) array.getArray() : null;
//    }
//
//    @Override
//    public double[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
//        return null;
//    }
//
//    @Override
//    public double[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
//        return null;
//    }
//}