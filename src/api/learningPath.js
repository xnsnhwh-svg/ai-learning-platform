import request from '../utils/request'

/**
 * 学习路径相关API
 */

/**
 * 获取用户所有路径
 * @param {number} userId - 用户ID
 */
export function getUserPaths(userId) {
  return request({
    url: `/api/learning-path/user/${userId}`,
    method: 'get'
  })
}

/**
 * 获取路径详情（含节点）
 * @param {number} pathId - 路径ID
 */
export function getPathDetail(pathId) {
  return request({
    url: `/api/learning-path/${pathId}`,
    method: 'get'
  })
}

/**
 * 获取用户在某课程的活跃路径
 * @param {number} userId - 用户ID
 * @param {number} courseId - 课程ID
 */
export function getActivePath(userId, courseId) {
  return request({
    url: `/api/learning-path/user/${userId}/course/${courseId}`,
    method: 'get'
  })
}

/**
 * 创建学习路径
 * @param {Object} data - 路径数据
 */
export function createLearningPath(data) {
  return request({
    url: '/api/learning-path',
    method: 'post',
    data
  })
}

/**
 * 更新路径状态
 * @param {number} pathId - 路径ID
 * @param {string} status - 状态值 (active/completed/abandoned)
 */
export function updatePathStatus(pathId, status) {
  return request({
    url: `/api/learning-path/${pathId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 添加路径节点
 * @param {number} pathId - 路径ID
 * @param {Object} data - 节点数据
 */
export function addPathNode(pathId, data) {
  return request({
    url: `/api/learning-path/${pathId}/nodes`,
    method: 'post',
    data
  })
}

/**
 * 更新节点状态
 * @param {number} nodeId - 节点ID
 * @param {string} status - 状态值 (pending/in_progress/completed)
 */
export function updateNodeStatus(nodeId, status) {
  return request({
    url: `/api/learning-path/nodes/${nodeId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除学习路径
 * @param {number} pathId - 路径ID
 */
export function deleteLearningPath(pathId) {
  return request({
    url: `/api/learning-path/${pathId}`,
    method: 'delete'
  })
}
