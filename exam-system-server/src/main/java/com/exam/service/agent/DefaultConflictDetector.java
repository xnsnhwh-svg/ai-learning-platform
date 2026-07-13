package com.exam.service.agent;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DefaultConflictDetector implements ConflictDetector {

    @Override
    public List<String> detect(List<AgentOutput> outputs) {
        List<String> warnings = new ArrayList<>();
        for (AgentOutput output : outputs) {
            if (output.getData() instanceof Map) {
                Map<String, Object> data = (Map<String, Object>) output.getData();
                if (data.containsKey("resources") && data.get("resources") instanceof List list) {
                    if (list.isEmpty()) warnings.add("警告：未生成任何学习资源");
                }
                if (data.containsKey("nodes") && data.get("nodes") instanceof List list) {
                    if (list.isEmpty()) warnings.add("警告：学习路径为空，请重新规划");
                }
            }
        }
        return warnings;
    }
}
