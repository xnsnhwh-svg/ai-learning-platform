import request from '../utils/request'

/**
 * 对话会话相关API
 */

/**
 * 获取用户对话历史
 * @param {number} userId - 用户ID
 */
export function getSessions(userId) {
  return request({
    url: '/api/agent/sessions',
    method: 'get',
    params: { userId }
  })
}

/**
 * 获取会话详情
 * @param {number} sessionId - 会话ID
 */
export function getSessionDetail(sessionId) {
  return request({
    url: `/api/agent/sessions/${sessionId}`,
    method: 'get'
  })
}

/**
 * 获取会话消息列表
 * @param {number} sessionId - 会话ID
 */
export function getSessionMessages(sessionId) {
  return request({
    url: `/api/agent/sessions/${sessionId}/messages`,
    method: 'get'
  })
}

/**
 * 获取指定阶段消息
 * @param {number} sessionId - 会话ID
 * @param {string} phase - 阶段名称
 */
export function getSessionMessagesByPhase(sessionId, phase) {
  return request({
    url: `/api/agent/sessions/${sessionId}/messages/phase/${phase}`,
    method: 'get'
  })
}

/**
 * 创建会话
 * @param {Object} data - 会话数据 { userId, title }
 */
export function createSession(data) {
  return request({
    url: '/api/agent/sessions',
    method: 'post',
    data
  })
}

/**
 * 添加消息
 * @param {number} sessionId - 会话ID
 * @param {Object} data - 消息数据 { role, content, phase }
 */
export function sendMessage(sessionId, data) {
  return request({
    url: `/api/agent/sessions/${sessionId}/messages`,
    method: 'post',
    data
  })
}

/**
 * 更新会话状态
 * @param {number} sessionId - 会话ID
 * @param {string} status - 状态值 (active/completed)
 */
export function updateSessionStatus(sessionId, status) {
  return request({
    url: `/api/agent/sessions/${sessionId}/status`,
    method: 'put',
    params: { status }
  })
}

/**
 * 删除对话记录
 * @param {number} sessionId - 会话ID
 */
export function deleteSession(sessionId) {
  return request({
    url: `/api/agent/sessions/${sessionId}`,
    method: 'delete'
  })
}
