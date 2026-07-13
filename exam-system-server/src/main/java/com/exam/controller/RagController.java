package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.rag.RagQueryDTO;
import com.exam.dto.rag.RagResultDTO;
import com.exam.service.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG 知识问答", description = "基于数据库内容的检索增强问答，减少 AI 幻觉")
public class RagController {

    @Autowired
    private RagService ragService;

    @PostMapping("/query")
    @Operation(summary = "RAG 问答查询", description = "基于知识库内容检索后生成回答，附带来源引用")
    public Result<RagResultDTO> query(@RequestBody RagQueryDTO dto) {
        if (dto.getQuery() == null || dto.getQuery().isBlank()) {
            return Result.error("查询内容不能为空");
        }
        RagResultDTO result = ragService.query(dto.getQuery(), dto.getCourseId(), dto.getTopK());
        return Result.success(result);
    }
}
