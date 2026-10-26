package com.example.demo.mapper.mysql;

import com.example.demo.com.Emp;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface EmpMapper {
    @Select("select * from db_03.tb_user where username = #{username} and password = #{password}")
    Emp gerByUsernameAndPassword(Emp emp);

    @Select("select * from db_03.tb_emp")
    public List<Emp> showdb ();
    public int delete(Integer id);
    @Options(useGeneratedKeys = true,keyProperty = "id")
    @Insert("insert into db_03.tb_emp(username, password, name, gender, job, entryday, createtime, updatetime, dpt_id)" +
            " values(#{username},#{password},#{name},#{gender},#{job},#{entryday},#{createtime},#{updatetime},#{dpt_id})")
    public void add(Emp emp);
//    @Update("update db_03.tb_emp set username=#{username},password=#{password},name=#{name},gender=#{gender},job=#{job},entryday=#{entryday},createtime=#{createtime},updatetime=#{updatetime},dpt_id=#{dpt_id} where id =2")
    public void update(Emp emp);
    @Select("select * from db_03.tb_emp where id = #{id}")
    public Emp findById(Integer id);
//    @Select("select * from db_03.tb_emp " +
//            "where name like concat('%',#{name},'%') and gender=#{gender} and entryday between #{entryday1} and #{entryday2} order by updatetime desc ")
    public void Deletes(List<Integer> ids);
    public List<Emp> findByName(String name, String gender, LocalDate entryday1, LocalDate entryday2);

}
