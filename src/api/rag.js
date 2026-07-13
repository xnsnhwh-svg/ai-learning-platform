import request from '../utils/request'

export function ragQuery(query, courseId, topK = 10) {
  return request({
    url: '/api/rag/query',
    method: 'post',
    data: { query, courseId, topK }
  })
}
