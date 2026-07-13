import { useAgentStore } from '@/stores/agent'
import request from '../utils/request'

export async function sendMessage(userMessage) {
  const store = useAgentStore()

  store.isStreaming = true
  store.addMessage({ role: 'user', content: userMessage })

  const response = await fetch('/api/agent/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      sessionId: store.sessionId,
      message: userMessage,
      userId: store.userId
    })
  })

  if (!response.ok) {
    store.isStreaming = false
    store.addMessage({ role: 'assistant', content: '请求失败，请稍后重试' })
    return
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let currentEvent = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const lines = buffer.split('\n')
    buffer = lines.pop() || ''

    for (const line of lines) {
      if (line.startsWith('event:')) {
        currentEvent = line.replace('event:', '').trim()
      } else if (line.startsWith('data:')) {
        try {
          const data = JSON.parse(line.replace('data:', '').trim())
          handleSSEEvent(currentEvent, data)
        } catch (e) {
          console.warn('SSE parse error:', e)
        }
      }
    }
  }

  store.isStreaming = false
}

function handleSSEEvent(event, data) {
  const store = useAgentStore()

  if (event === 'error') {
    store.isStreaming = false
    store.addMessage({ role: 'assistant', content: '系统错误：' + (data.message || '未知错误，请稍后重试') })
    return
  }

  if (data.sessionId && !store.sessionId) {
    store.sessionId = data.sessionId
  }

  // 兼容新旧两种 SSE 事件格式
  if (event === 'phase' || event === 'agent-event') {
    // agent-event 格式转换
    if (event === 'agent-event') {
      if (data.type === 'stage') {
        data = { phase: data.stage || 'orchestrating', content: { text: data.data }, progress: 0.1 }
      } else if (data.type === 'decision') {
        data = { phase: 'orchestrating', content: { text: '正在分析您的需求...' }, progress: 0.2 }
      } else if (data.type === 'report') {
        data = { phase: data.data?.stage || 'planning', content: { text: data.data?.summary || '正在处理...' }, progress: 0.5 }
      } else if (data.type === 'done') {
        data = { phase: 'done', content: {}, progress: 1.0 }
      }
    }

    const phase = data.phase
    store.setPhase(phase, data)

    if (data.content?.text) {
      const role = phase === 'profiling' ? 'assistant' : 'system'
      store.addMessage({
        role,
        content: data.content.text,
        phase,
        options: data.content.options || null,
        type: data.content.type || 'text'
      })
    }

    if (phase === 'done') {
      store.addMessage({
        role: 'system',
        content: '学习计划已生成，请在右侧面板查看详情',
        phase: 'done'
      })
    }
  }
}

export async function fetchProfile(userId) {
  const res = await fetch(`/api/user/profile/${userId}`)
  if (!res.ok) return null
  const json = await res.json()
  return json.code === 200 ? json.data : null
}

export async function fetchLearningPath(pathId) {
  const res = await fetch(`/api/learning-path/${pathId}`)
  if (!res.ok) return null
  const json = await res.json()
  return json.code === 200 ? json.data : null
}

export async function fetchLearningPathsByUser(userId) {
  const res = await fetch(`/api/learning-path/user/${userId}`)
  if (!res.ok) return []
  const json = await res.json()
  return json.code === 200 ? json.data : []
}

export async function fetchNodeResources(pathNodeId) {
  const res = await fetch(`/api/generated-resources?pathNodeId=${pathNodeId}`)
  if (!res.ok) return []
  const json = await res.json()
  if (json.code === 200) {
    const data = json.data
    if (Array.isArray(data)) return data
    if (data?.resources) return data.resources
    return []
  }
  return []
}

export async function fetchSessions(userId) {
  const res = await fetch(`/api/agent/sessions?userId=${userId}`)
  if (!res.ok) return []
  const json = await res.json()
  return json.code === 200 ? json.data : []
}

export async function fetchSessionMessages(sessionId) {
  const res = await fetch(`/api/agent/sessions/${sessionId}/messages`)
  if (!res.ok) return []
  const json = await res.json()
  return json.code === 200 ? json.data : []
}

export async function updateNodeStatus(nodeId, status) {
  await fetch(`/api/learning-path/nodes/${nodeId}/status?status=${status}`, {
    method: 'PUT'
  })
}

/**
 * 从诊断问卷创建画像 + 完整学习路径
 * @param {Object} params
 * @param {number} params.userId
 * @param {string} params.diagnosticText - 问卷答案拼成的文本
 */
export function profileFromDiagnostic(params) {
  return request({
    url: '/api/agent/profile-from-diagnostic',
    method: 'post',
    data: params
  })
}
