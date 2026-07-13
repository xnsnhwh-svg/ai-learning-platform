package com.exam.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "RAG 查询请求")
public class RagQueryDTO {
    private String query;
    private Long courseId;
    private Integer topK = 10;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
