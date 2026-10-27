package com.example.demo.mapper.postgresql;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PilgrimageAgentMapper {
    @Select("SELECT kind, alias, canonical FROM acgn0721_gis.tb_pilgrimage_alias ORDER BY kind,alias LIMIT 2000")
    @Options(timeout = 5)
    List<Map<String, Object>> aliases();

    @Select("SELECT point_id::text AS id, access_status AS status, photo_note AS note, source_url AS source, verified_at::text AS verified FROM acgn0721_gis.tb_pilgrimage_spot_info")
    @Options(timeout = 5)
    List<Map<String, Object>> spotInfo();

    @Select(value = """
        INSERT INTO acgn0721_gis.tb_pilgrimage_route(id,owner_id,title,plan,plan_hash)
        VALUES(#{id}::uuid,#{owner},#{title},#{plan}::jsonb,#{hash})
        ON CONFLICT(owner_id,plan_hash) DO UPDATE SET title=EXCLUDED.title,updated_at=now()
        RETURNING id::text
        """, affectData = true)
    @Options(timeout = 5, flushCache = Options.FlushCachePolicy.TRUE)
    String saveRoute(@Param("id") String id, @Param("owner") String owner, @Param("title") String title,
                     @Param("plan") String plan, @Param("hash") String hash);

    @Select("SELECT id::text,title,updated_at::text AS \"updatedAt\",jsonb_array_length(plan->'days') AS days FROM acgn0721_gis.tb_pilgrimage_route WHERE owner_id=#{owner} ORDER BY updated_at DESC LIMIT 50")
    @Options(timeout = 5)
    List<Map<String, Object>> routes(String owner);

    @Select("SELECT id::text,title,plan::text AS plan FROM acgn0721_gis.tb_pilgrimage_route WHERE id=#{id}::uuid AND owner_id=#{owner}")
    @Options(timeout = 5)
    Map<String, Object> route(@Param("id") String id, @Param("owner") String owner);

    @Delete("DELETE FROM acgn0721_gis.tb_pilgrimage_route WHERE id=#{id}::uuid AND owner_id=#{owner}")
    @Options(timeout = 5)
    int deleteRoute(@Param("id") String id, @Param("owner") String owner);

    @Select("SELECT payload::text AS payload,version FROM acgn0721_gis.tb_pilgrimage_session WHERE id=#{id}::uuid AND owner_id=#{owner} AND mode=#{mode} AND expires_at>now()")
    @Options(timeout = 5)
    Map<String, Object> session(@Param("id") String id, @Param("owner") String owner, @Param("mode") String mode);

    @Insert("INSERT INTO acgn0721_gis.tb_pilgrimage_session(id,owner_id,mode,payload,expires_at) VALUES(#{id}::uuid,#{owner},#{mode},#{payload}::jsonb,now()+interval '30 minutes') ON CONFLICT DO NOTHING")
    @Options(timeout = 5)
    int insertSession(@Param("id") String id, @Param("owner") String owner, @Param("mode") String mode, @Param("payload") String payload);

    @Update("UPDATE acgn0721_gis.tb_pilgrimage_session SET payload=#{payload}::jsonb,version=version+1,expires_at=now()+interval '30 minutes',updated_at=now() WHERE id=#{id}::uuid AND owner_id=#{owner} AND version=#{version} AND expires_at>now()")
    @Options(timeout = 5)
    int updateSession(@Param("id") String id, @Param("owner") String owner, @Param("payload") String payload, @Param("version") long version);

}
