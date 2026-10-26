package com.example.demo.service;

import com.example.demo.com.RelatedWorkVo;

import java.util.List;

public interface RelatedWorkService {
    List<RelatedWorkVo> listBySource(String srcType, int srcId);
}
