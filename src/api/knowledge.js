import request from '../utils/request'

/**
 * 知识图谱相关API
 */

/**
 * 获取课程列表
 */
export function getCourses() {
  return request({
    url: '/api/knowledge/courses',
    method: 'get'
  })
}

/**
 * 获取课程详情
 * @param {number} id - 课程ID
 */
export function getCourseDetail(id) {
  return request({
    url: `/api/knowledge/courses/${id}`,
    method: 'get'
  })
}

/**
 * 获取课程完整知识图谱（树形+依赖关系）
 * @param {number} courseId - 课程ID
 */
export function getCourseGraph(courseId) {
  return request({
    url: `/api/knowledge/graph/${courseId}`,
    method: 'get'
  })
}

/**
 * 获取知识图谱树形结构
 * @param {number} courseId - 课程ID
 */
export function getCourseTree(courseId) {
  return request({
    url: `/api/knowledge/tree/${courseId}`,
    method: 'get'
  })
}

/**
 * 获取课程知识点列表
 * @param {number} courseId - 课程ID
 */
export function getKnowledgePoints(courseId) {
  return request({
    url: `/api/knowledge/points/course/${courseId}`,
    method: 'get'
  })
}

/**
 * 获取知识点详情
 * @param {number} id - 知识点ID
 */
export function getKnowledgePointDetail(id) {
  return request({
    url: `/api/knowledge/points/${id}`,
    method: 'get'
  })
}

/**
 * 新增知识点
 * @param {Object} data - 知识点数据
 */
export function createKnowledgePoint(data) {
  return request({
    url: '/api/knowledge/points',
    method: 'post',
    data
  })
}

/**
 * 修改知识点
 * @param {number} id - 知识点ID
 * @param {Object} data - 知识点数据
 */
export function updateKnowledgePoint(id, data) {
  return request({
    url: `/api/knowledge/points/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除知识点
 * @param {number} id - 知识点ID
 */
export function deleteKnowledgePoint(id) {
  return request({
    url: `/api/knowledge/points/${id}`,
    method: 'delete'
  })
}

/**
 * 获取知识点前置依赖链
 * @param {number} id - 知识点ID
 */
export function getPrerequisiteChain(id) {
  return request({
    url: `/api/knowledge/points/${id}/prerequisite-chain`,
    method: 'get'
  })
}

/**
 * 获取课程依赖关系
 * @param {number} courseId - 课程ID
 */
export function getKnowledgeEdges(courseId) {
  return request({
    url: `/api/knowledge/edges/course/${courseId}`,
    method: 'get'
  })
}

/**
 * 新增依赖关系
 * @param {Object} data - 依赖关系数据
 */
export function createKnowledgeEdge(data) {
  return request({
    url: '/api/knowledge/edges',
    method: 'post',
    data
  })
}

/**
 * 删除依赖关系
 * @param {number} fromKpId - 起始知识点ID
 * @param {number} toKpId - 目标知识点ID
 */
export function deleteKnowledgeEdge(fromKpId, toKpId) {
  return request({
    url: '/api/knowledge/edges',
    method: 'delete',
    params: { fromKpId, toKpId }
  })
}
