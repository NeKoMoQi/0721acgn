package com.example.demo;

import com.example.demo.dto.SearchCriteria;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchPaginationMapperTests {
    @Test
    void appliesFiltersAndSortBeforePagination() throws Exception {
        String resource = "com/example/demo/mapper/mysql/SearchResultMapper.xml";
        Configuration configuration = new Configuration();
        try (var mapper = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(mapper, configuration, resource, configuration.getSqlFragments()).parse();
        }
        String namespace = "com.example.demo.mapper.mysql.SearchResultMapper.";
        var criteria = new SearchCriteria("雪", null, null, 2020, null, "update", "comic", 24, 48);
        String count = configuration.getMappedStatement(namespace + "countPage").getBoundSql(criteria).getSql();
        String listing = configuration.getMappedStatement(namespace + "searchPage").getBoundSql(criteria).getSql();

        assertTrue(count.contains("tb_cartoons"));
        assertTrue(count.contains("releasedate"));
        assertFalse(count.contains("tb_galgame"));
        assertFalse(count.contains("LIMIT"));
        assertTrue(listing.contains("matched.updatedAt DESC"));
        assertTrue(listing.indexOf("matched.updatedAt DESC") < listing.indexOf("LIMIT"));
    }
}
