import request from '../utils/request'

/**
 * 用户画像相关API
 */

/**
 * 获取用户学习画像
 * @param {number} userId - 用户ID
 */
export function getUserProfile(userId) {
  return request({
    url: `/api/user/profile/${userId}`,
    method: 'get'
  })
}

/**
 * 创建用户学习画像
 * @param {Object} data - 画像数据
 */
export function createUserProfile(data) {
  return request({
    url: '/api/user/profile',
    method: 'post',
    data
  })
}

/**
 * 更新用户学习画像
 * @param {number} userId - 用户ID
 * @param {Object} data - 画像数据
 */
export function updateUserProfile(userId, data) {
  return request({
    url: `/api/user/profile/${userId}`,
    method: 'put',
    data
  })
}

/**
 * 删除用户学习画像
 * @param {number} userId - 用户ID
 */
export function deleteUserProfile(userId) {
  return request({
    url: `/api/user/profile/${userId}`,
    method: 'delete'
  })
}
