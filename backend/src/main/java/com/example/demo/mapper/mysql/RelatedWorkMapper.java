package com.example.demo.mapper.mysql;

import com.example.demo.com.RelatedLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RelatedWorkMapper {
    List<RelatedLink> listBySource(@Param("srcType") String srcType, @Param("srcId") int srcId);
}
