package com.exam.service.agent;

import com.exam.entity.LearningPath;
import com.exam.entity.PathNode;
import com.exam.service.agent.model.PlanReport;
import com.exam.service.agent.model.ResourceReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 鍚堟垚寮曟搸
 * 璐熻矗鑱氬悎璺緞鑺傜偣鍜岀敓鎴愮殑璧勬簮锛屽幓閲嶅苟鏍煎紡鍖栦负鍓嶇鍙嬪ソ鐨勭粨鏋? */
@Component
public class SynthesisEngine {

    private static final Logger log = LoggerFactory.getLogger(SynthesisEngine.class);

    /**
     * 鍚堟垚瀛︿範鍖?     * @param path 瀛︿範璺緞
     * @param resources 鐢熸垚鐨勮祫婧愬垪琛?     * @return 鍚堟垚鍚庣殑瀛︿範鍖?     */
    public LearningPackage synthesize(LearningPath path, List<ResourceReport> resources) {
        LearningPackage pkg = new LearningPackage();
        pkg.setPathId(path.getId());
        pkg.setTitle("瀛︿範璁″垝");
        pkg.setCourseId(path.getCourseId());

        // 鎸夎矾寰勮妭鐐瑰垎缁勮祫婧?        Map<Long, List<ResourceReport>> resourcesByNode = resources.stream()
                .collect(Collectors.groupingBy(ResourceReport::getPathNodeId));

        // 鏋勫缓鑺傜偣鍖?        List<NodePackage> nodePackages = new ArrayList<>();
        if (path.getNodes() != null) {
            for (PathNode node : path.getNodes()) {
                NodePackage nodePkg = new NodePackage();
                nodePkg.setNodeId(node.getId());
                nodePkg.setTitle(node.getTitle());
                nodePkg.setNodeType(node.getNodeType());
                nodePkg.setEstimatedMinutes(node.getEstimatedMinutes());
                nodePkg.setReason(node.getReason());
                nodePkg.setStatus(node.getStatus());

                // 鑾峰彇璇ヨ妭鐐圭殑璧勬簮骞跺幓閲?                List<ResourceReport> nodeResources = resourcesByNode.getOrDefault(node.getId(), List.of());
                List<ResourceReport> deduplicatedResources = deduplicateResources(nodeResources);
                nodePkg.setResources(deduplicatedResources);

                nodePackages.add(nodePkg);
            }
        }

        pkg.setNodes(nodePackages);
        pkg.setTotalResources(resources.size());
        pkg.setDeduplicatedResources(countDeduplicatedResources(resources));

        return pkg;
    }

    /**
     * 璧勬簮鍘婚噸
     * 鍚屼竴鐭ヨ瘑鐐瑰彲鑳借澶氭鐢熸垚璧勬簮锛屽彧淇濈暀鏈€鏂扮殑
     */
    private List<ResourceReport> deduplicateResources(List<ResourceReport> resources) {
        Map<String, ResourceReport> uniqueResources = new LinkedHashMap<>();

        for (ResourceReport resource : resources) {
            String key = resource.getResourceType() + "_" +
                    (resource.getKnowledgePointId() != null ? resource.getKnowledgePointId() : resource.getTitle());
            uniqueResources.put(key, resource);
        }

        return new ArrayList<>(uniqueResources.values());
    }

    /**
     * 缁熻鍘婚噸鍚庣殑璧勬簮鏁伴噺
     */
    private int countDeduplicatedResources(List<ResourceReport> resources) {
        Set<String> uniqueKeys = new HashSet<>();
        for (ResourceReport resource : resources) {
            String key = resource.getResourceType() + "_" +
                    (resource.getKnowledgePointId() != null ? resource.getKnowledgePointId() : resource.getTitle());
            uniqueKeys.add(key);
        }
        return uniqueKeys.size();
    }

    /**
     * 瀛︿範鍖?     */
    public static class LearningPackage {
        private Long pathId;
        private String title;
        private Long courseId;
        private List<NodePackage> nodes;
        private int totalResources;
        private int deduplicatedResources;

        public Long getPathId() {
            return pathId;
        }

        public void setPathId(Long pathId) {
            this.pathId = pathId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Long getCourseId() {
            return courseId;
        }

        public void setCourseId(Long courseId) {
            this.courseId = courseId;
        }

        public List<NodePackage> getNodes() {
            return nodes;
        }

        public void setNodes(List<NodePackage> nodes) {
            this.nodes = nodes;
        }

        public int getTotalResources() {
            return totalResources;
        }

        public void setTotalResources(int totalResources) {
            this.totalResources = totalResources;
        }

        public int getDeduplicatedResources() {
            return deduplicatedResources;
        }

        public void setDeduplicatedResources(int deduplicatedResources) {
            this.deduplicatedResources = deduplicatedResources;
        }
    }

    /**
     * 鑺傜偣鍖?     */
    public static class NodePackage {
        private Long nodeId;
        private String title;
        private String nodeType;
        private Integer estimatedMinutes;
        private String reason;
        private String status;
        private List<ResourceReport> resources;

        public Long getNodeId() {
            return nodeId;
        }

        public void setNodeId(Long nodeId) {
            this.nodeId = nodeId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getNodeType() {
            return nodeType;
        }

        public void setNodeType(String nodeType) {
            this.nodeType = nodeType;
        }

        public Integer getEstimatedMinutes() {
            return estimatedMinutes;
        }

        public void setEstimatedMinutes(Integer estimatedMinutes) {
            this.estimatedMinutes = estimatedMinutes;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public List<ResourceReport> getResources() {
            return resources;
        }

        public void setResources(List<ResourceReport> resources) {
            this.resources = resources;
        }
    }
}
