package com.exam.dto.rag;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "RAG 查询结果")
public class RagResultDTO {

    private String answer;
    private List<Source> sources;

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public List<Source> getSources() { return sources; }
    public void setSources(List<Source> sources) { this.sources = sources; }

    @Schema(description = "引用来源")
    public static class Source {
        private String id;
        private String title;
        private String snippet;
        private String type;
        private double score;
        private String url;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
