<template>
  <div class="chat-page page-enter">
    <AppNavbar />

    <div class="chat-container">
      <aside class="session-sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">对话历史</span>
          <el-button type="primary" size="small" @click="handleNewSession" :icon="Plus" circle />
        </div>
        <div class="session-list" v-loading="sessionLoading">
          <div
            v-for="session in sessionList"
            :key="session.id"
            class="session-item"
            :class="{ active: currentSessionId === session.id }"
            @click="handleSelectSession(session.id)"
          >
            <div class="session-avatar">{{ session.title?.charAt(0) || 'N' }}</div>
            <div class="session-info">
              <div class="session-title">{{ session.title || '新对话' }}</div>
              <div class="session-time">{{ formatTime(session.updatedAt || session.createdAt) }}</div>
            </div>
            <el-icon class="session-delete" @click.stop="handleDeleteSession(session.id)"><Delete /></el-icon>
          </div>
          <el-empty v-if="!sessionLoading && sessionList.length === 0" description="暂无对话历史" :image-size="60" />
        </div>
      </aside>

      <main class="chat-main">
        <div class="phase-bar" v-if="messages.length > 0 || currentPhase">
          <div class="phase-inner">
            <div
              v-for="(phase, index) in phases"
              :key="phase.key"
              class="phase-item"
              :class="{ active: currentPhase === phase.key, done: phaseOrder(currentPhase) > index }"
            >
              <div class="phase-dot">
                <el-icon v-if="phaseOrder(currentPhase) > index"><Check /></el-icon>
                <span v-else>{{ index + 1 }}</span>
              </div>
              <span class="phase-label">{{ phase.label }}</span>
              <div class="phase-line" v-if="index < phases.length - 1"></div>
            </div>
          </div>
          <div class="mode-indicator">
            <el-tag size="small" type="info" effect="plain">知识问答</el-tag>
          </div>
        </div>

        <div class="message-list" ref="messageListRef" v-loading="messageLoading">
          <div v-if="messages.length === 0 && !messageLoading" class="empty-chat">
            <div class="empty-illustration">
              <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
                <circle cx="40" cy="40" r="38" stroke="#e2e8f0" stroke-width="2" fill="#f8fafc"/>
                <path d="M30 32h20v16H30z" fill="#dbeafe" stroke="#93c5fd" stroke-width="1.5" rx="4"/>
                <circle cx="36" cy="38" r="2" fill="#3b82f6"/>
                <circle cx="40" cy="38" r="2" fill="#3b82f6"/>
                <circle cx="44" cy="38" r="2" fill="#3b82f6"/>
                <path d="M34 44h12" stroke="#93c5fd" stroke-width="1.5" stroke-linecap="round"/>
                <path d="M34 48h8" stroke="#93c5fd" stroke-width="1" stroke-linecap="round"/>
              </svg>
            </div>
            <h3>开始你的智能学习之旅</h3>
            <p>告诉我你想学什么，我会为你规划个性化的学习路径</p>
              <div class="suggestion-chips">
              <div class="chip" v-for="s in initialSuggestions" :key="s" @click="inputMessage = s; handleSend()">
                <el-icon style="margin-right:4px"><ChatDotRound /></el-icon>{{ s }}
              </div>
            </div>
          </div>

          <div
            v-for="(msg, idx) in messages"
            :key="msg.id || idx"
            class="message-item"
            :class="msg.role"
          >
            <div class="msg-avatar" :class="msg.role">
              <el-icon v-if="msg.role === 'user'"><User /></el-icon>
              <el-icon v-else><Robot /></el-icon>
            </div>
            <div class="msg-content">
              <div class="msg-meta">{{ msg.role === 'user' ? '你' : 'AI 助手' }}<span class="msg-time">{{ msg.time || '' }}</span></div>
              <div class="msg-bubble" v-html="renderContent(msg.content)"></div>
              <el-tag v-if="msg.phase" size="small" class="msg-phase" round>{{ phaseLabel(msg.phase) }}</el-tag>
              <div v-if="msg.role === 'assistant' && msg.suggestions && msg.suggestions.length" class="msg-suggestions">
                <div
                  v-for="s in msg.suggestions"
                  :key="s"
                  class="suggestion-chip"
                  @click="sendSuggestion(s)"
                >{{ s }}</div>
              </div>
              <div v-if="msg.role === 'assistant' && msg.sources && msg.sources.length" class="msg-sources">
                <div class="sources-header" @click="msg._showSources = !msg._showSources">
                  <el-icon><Reading /></el-icon>
                  引用来源 ({{ msg.sources.length }})
                  <el-icon :class="{ rotated: msg._showSources }"><ArrowDown /></el-icon>
                </div>
                <div v-if="msg._showSources" class="sources-list">
                  <div v-for="(src, si) in msg.sources" :key="si" class="source-item">
                    <span class="source-badge">{{ si + 1 }}</span>
                    <div class="source-info">
                      <span class="source-title">{{ src.title }}</span>
                      <span class="source-type">{{ src.type }}</span>
                      <p class="source-snippet">{{ src.snippet }}</p>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-if="aiTyping" class="message-item assistant">
            <div class="msg-avatar assistant"><el-icon><Robot /></el-icon></div>
            <div class="msg-content">
              <div class="msg-meta">AI 助手</div>
              <div class="msg-bubble typing">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
              </div>
            </div>
          </div>

          <!-- 智能产出结果卡片 -->
          <div class="result-cards" v-if="hasResults">
          <div v-if="agentData.profiling" class="result-card profile-result">
            <div class="result-card-header"><el-icon><UserFilled /></el-icon> 学习画像</div>
            <div class="result-card-body">
              <el-descriptions :column="2" size="small" border>
                <el-descriptions-item label="目标">{{ agentData.profiling.goal || '-' }}</el-descriptions-item>
                <el-descriptions-item label="水平">{{ agentData.profiling.level || '-' }}</el-descriptions-item>
                <el-descriptions-item label="风格">{{ agentData.profiling.style || '-' }}</el-descriptions-item>
                <el-descriptions-item label="每周时间">{{ agentData.profiling.hoursPerWeek || '-' }}h</el-descriptions-item>
              </el-descriptions>
            </div>
            <div class="result-card-footer">
              <el-button size="small" round @click="router.push('/profile')">查看完整画像</el-button>
            </div>
          </div>
          <div v-if="agentData.planning" class="result-card plan-result">
            <div class="result-card-header"><el-icon><Guide /></el-icon> 学习路径</div>
            <div class="result-card-body">
              <el-timeline>
                <el-timeline-item
                  v-for="(n, i) in (agentData.planning.nodes || [])"
                  :key="i"
                  :timestamp="n.type === 'new_learn' ? '新学' : n.type === 'review' ? '复习' : '强化'"
                >
                  {{ n.title }}（{{ n.minutes }}分钟）
                </el-timeline-item>
              </el-timeline>
            </div>
            <div class="result-card-footer">
              <el-button size="small" round @click="router.push('/learning-path')">查看完整路径</el-button>
            </div>
          </div>
          <div v-if="agentData.generating" class="result-card gen-result">
            <div class="result-card-header"><el-icon><Files /></el-icon> 生成资源</div>
            <div class="result-card-body">
              <div v-for="(r, i) in (agentData.generating.resources || [])" :key="i" class="resource-chip">
                <el-tag size="small" round>{{ r.resourceType }}</el-tag>
                <span>{{ r.title }}</span>
              </div>
            </div>
          </div>
        </div>

        </div>

        <div class="input-area">
          <div class="input-wrapper">
            <el-input
              v-model="inputMessage"
              type="textarea"
              :rows="1"
              placeholder="输入你想学的内容..."
              resize="none"
              @keydown.enter.exact.prevent="handleSend"
              :disabled="aiTyping"
            />
            <div class="input-actions">
              <el-tooltip content="生成个性化学习路径" placement="top">
                <el-button
                  size="default"
                  @click="handleStartPlanning"
                  :disabled="aiTyping"
                  :icon="Guide"
                >
                  规划路径
                </el-button>
              </el-tooltip>
              <el-button
                type="primary"
                @click="handleSend"
                :loading="aiTyping"
                :disabled="!inputMessage.trim()"
                :icon="Promotion"
                circle
              />
            </div>
          </div>
          <p class="input-hint">基于知识库回答，附带来源引用 · 点击「规划路径」生成个性化学习方案</p>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppNavbar from '../components/AppNavbar.vue'
import { useAuthStore } from '../stores/auth'
import { getSessions, getSessionMessages, createSession, deleteSession } from '../api/session'
import { ragQuery } from '../api/rag'

const authStore = useAuthStore()
const router = useRouter()
const userId = computed(() => authStore.userId)

const sessionLoading = ref(false)
const sessionList = ref([])
const currentSessionId = ref(null)
const messageLoading = ref(false)
const messages = ref([])
const inputMessage = ref('')
const aiTyping = ref(false)
const currentPhase = ref('')
const messageListRef = ref(null)
const agentData = ref({ profiling: null, planning: null, generating: null })
const planningMode = ref(false)

const phases = [
  { key: 'profiling', label: '了解学情' },
  { key: 'planning', label: '规划路径' },
  { key: 'generating', label: '生成资源' },
  { key: 'done', label: '完成' }
]
const phaseOrder = (key) => phases.findIndex(p => p.key === key)
const phaseLabel = (key) => phases.find(p => p.key === key)?.label || key

const initialSuggestions = [
  'Python 列表推导式怎么用',
  'Java 面向对象三大特性是什么',
  '什么是动态规划'
]

const formatTime = (timeString) => {
  if (!timeString) return ''
  const date = new Date(timeString)
  const now = new Date()
  const diff = now - date
  const day = 24 * 60 * 60 * 1000
  if (diff < day) return '今天'
  if (diff < 2 * day) return '昨天'
  if (diff < 7 * day) return Math.floor(diff / day) + '天前'
  return date.toLocaleDateString()
}

const renderContent = (content) => {
  if (!content) return ''
  let html = content
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/^### (.+)$/gm, '<h4>$1</h4>')
    .replace(/^## (.+)$/gm, '<h3>$1</h3>')
    .replace(/^# (.+)$/gm, '<h2>$1</h2>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br>')
  html = html.replace(/<li>([\s\S]*?)<\/li>/g, (m) => '<ul>' + m + '</ul>')
  return html
}

const loadSessions = async () => {
  if (!userId.value) return
  sessionLoading.value = true
  try {
    const res = await getSessions(userId.value)
    sessionList.value = res.data || []
    if (sessionList.value.length > 0 && !currentSessionId.value) {
      await handleSelectSession(sessionList.value[0].id)
    }
  } catch (e) {
    console.error('加载会话列表失败', e)
  } finally {
    sessionLoading.value = false
  }
}

const handleSelectSession = async (sessionId) => {
  currentSessionId.value = sessionId
  messageLoading.value = true
  messages.value = []
  try {
    const res = await getSessionMessages(sessionId)
    messages.value = (res.data || []).map(m => ({
      id: m.id,
      role: m.role,
      content: m.content,
      phase: m.phase
    }))
    const lastMsg = messages.value.filter(m => m.role === 'assistant').pop()
    currentPhase.value = lastMsg?.phase || ''
    await scrollToBottom()
  } catch (e) {
    console.error('加载消息失败', e)
  } finally {
    messageLoading.value = false
  }
}

const handleNewSession = async () => {
  try {
    const res = await createSession({ userId: userId.value, title: '新对话', status: 'active' })
    const newSession = res.data
    sessionList.value.unshift(newSession)
    currentSessionId.value = newSession.id
    messages.value = []
    currentPhase.value = ''
    ElMessage.success('已创建新对话')
  } catch (e) {
    ElMessage.error('创建对话失败')
  }
}

const handleDeleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定删除这条对话记录吗？', '提示', { type: 'warning' })
    await deleteSession(sessionId)
    sessionList.value = sessionList.value.filter(s => s.id !== sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
      if (sessionList.value.length > 0) {
        await handleSelectSession(sessionList.value[0].id)
      }
    }
    ElMessage.success('删除成功')
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const hasResults = computed(() =>
  agentData.value.profiling || agentData.value.planning || agentData.value.generating
)

const handleSend = async () => {
  const text = inputMessage.value.trim()
  if (!text || aiTyping.value) return
  await handleRagQuery(text)
}

const handleStartPlanning = async () => {
  const text = inputMessage.value.trim()
  if (!text) {
    ElMessage.info('请先描述你想学习的内容')
    return
  }
  if (aiTyping.value) return

  if (!currentSessionId.value) {
    await handleNewSession()
  }

  planningMode.value = true
  agentData.value = { profiling: null, planning: null, generating: null }

  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: '📋 ' + text,
    phase: null
  })
  inputMessage.value = ''
  await scrollToBottom()

  aiTyping.value = true
  try {
    const assistMsg = { id: Date.now() + 1, role: 'assistant', content: '', phase: '' }
    messages.value.push(assistMsg)

    const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
    const token = localStorage.getItem('token') || ''
    const response = await fetch(baseUrl + '/api/agent/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': token },
      body: JSON.stringify({
        userId: userId.value,
        sessionId: currentSessionId.value,
        message: text
      })
    })

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const data = line.substring(5).trim()
          if (data === '[DONE]') continue
          try {
            const parsed = JSON.parse(data)
            if (parsed.type === 'session' && parsed.sessionId) {
              currentSessionId.value = parsed.sessionId
            }
            if (parsed.type === 'message' && parsed.content) {
              assistMsg.content += parsed.content
              assistMsg.phase = parsed.phase || assistMsg.phase
              currentPhase.value = parsed.phase || currentPhase.value
              await scrollToBottom()
            }
            if (parsed.type === 'data') {
              if (parsed.phase && parsed.data) {
                agentData.value[parsed.phase] = parsed.data
              }
              currentPhase.value = parsed.phase || currentPhase.value
            }
            if (parsed.type === 'suggestions' && Array.isArray(parsed.suggestions)) {
              const last = messages.value.filter(m => m.role === 'assistant').pop()
              if (last) last.suggestions = parsed.suggestions
              await scrollToBottom()
            }
          } catch (e) {}
        }
      }
    }
    await scrollToBottom()
  } catch (e) {
    ElMessage.error('规划失败，请重试')
    messages.value = messages.value.filter(m => m.role !== 'assistant' || m.content)
  } finally {
    aiTyping.value = false
  }
}

const handleRagQuery = async (text) => {
  messages.value.push({ id: Date.now(), role: 'user', content: text, phase: null })
  inputMessage.value = ''
  await scrollToBottom()

  aiTyping.value = true
  const assistMsg = { id: Date.now() + 1, role: 'assistant', content: '', phase: 'rag', sources: [], _showSources: false }
  messages.value.push(assistMsg)
  try {
    const res = await ragQuery(text, null, 10)
    const data = res.data
    if (data) {
      assistMsg.content = data.answer || '暂无回答'
      assistMsg.sources = data.sources || []
    } else {
      assistMsg.content = '暂无回答'
    }
    await scrollToBottom()
  } catch (e) {
    assistMsg.content = '查询失败: ' + (e.message || '网络错误')
    ElMessage.error('RAG 查询失败')
  } finally {
    aiTyping.value = false
  }
}

const sendSuggestion = async (text) => {
  inputMessage.value = text
  await handleSend()
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.chat-page { height: 100vh; overflow: hidden; background: var(--bg-page); display: flex; flex-direction: column; }

.chat-container {
  flex: 1; display: flex; max-width: var(--content-max-xl); width: 100%;
  margin: 0 auto; padding: var(--space-md) var(--space-lg); gap: var(--space-lg);
  min-height: 0; overflow: hidden;
}

/* Sidebar */
.session-sidebar {
  width: 280px; background: var(--bg-card); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  display: flex; flex-direction: column; overflow: hidden;
}
.sidebar-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-md) var(--space-lg); border-bottom: 1px solid var(--border);
}
.sidebar-title { font-size: var(--fs-base); font-weight: var(--fw-semibold); color: var(--text-primary); }
.session-list { flex: 1; overflow-y: auto; padding: var(--space-xs); }
.session-item {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md); border-radius: var(--radius-md);
  cursor: pointer; transition: all var(--t-fast); position: relative;
}
.session-item:hover { background: var(--bg-hover); }
.session-item.active { background: var(--bg-active); }
.session-avatar {
  width: 32px; height: 32px; border-radius: var(--radius-full); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--primary), var(--primary-dark));
  color: #fff; font-size: var(--fs-sm); font-weight: var(--fw-semibold);
}
.session-info { flex: 1; min-width: 0; }
.session-title { font-size: var(--fs-base); color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.session-time { font-size: var(--fs-xs); color: var(--text-tertiary); margin-top: 2px; }
.session-delete { color: var(--text-tertiary); opacity: 0; transition: opacity var(--t-fast); cursor: pointer; }
.session-item:hover .session-delete { opacity: 1; }
.session-delete:hover { color: var(--danger); }

/* Main */
.chat-main {
  flex: 1; background: var(--bg-card); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  display: flex; flex-direction: column; overflow: hidden;
}

/* Phase bar */
.phase-bar {
  padding: var(--space-md) var(--space-lg);
  border-bottom: 1px solid var(--border); background: var(--bg-subtle);
}
.phase-inner { display: flex; align-items: center; max-width: 600px; margin: 0 auto; }
.phase-item { display: flex; align-items: center; flex: 1; }
.phase-dot {
  width: 28px; height: 28px; border-radius: var(--radius-full);
  background: var(--border); color: var(--text-tertiary);
  display: flex; align-items: center; justify-content: center;
  font-size: var(--fs-sm); font-weight: var(--fw-semibold); flex-shrink: 0;
  transition: all var(--t-normal);
}
.phase-item.active .phase-dot { background: var(--primary); color: #fff; box-shadow: 0 0 0 4px var(--primary-light); }
.phase-item.done .phase-dot { background: var(--success); color: #fff; }
.phase-label { font-size: var(--fs-sm); color: var(--text-tertiary); margin-left: var(--space-xs); white-space: nowrap; }
.phase-item.active .phase-label, .phase-item.done .phase-label { color: var(--text-primary); font-weight: var(--fw-medium); }
.phase-line { flex: 1; height: 2px; background: var(--border); margin: 0 var(--space-sm); }
.phase-item.done .phase-line { background: var(--success); }
.mode-indicator { display: flex; justify-content: flex-end; padding-top: var(--space-xs); }

/* Message list */
.message-list { flex: 1; overflow-y: auto; padding: var(--space-lg); }
.empty-chat { text-align: center; padding: var(--space-3xl) var(--space-lg); color: var(--text-tertiary); }
.empty-illustration { margin-bottom: var(--space-lg); }
.empty-chat h3 { margin: var(--space-md) 0 var(--space-xs); color: var(--text-primary); font-size: var(--fs-xl); }
.empty-chat p { margin-bottom: var(--space-lg); color: var(--text-secondary); }
.suggestion-chips { display: flex; flex-wrap: wrap; gap: var(--space-sm); justify-content: center; max-width: 600px; margin: 0 auto; }
.chip {
  padding: var(--space-sm) var(--space-md); display: inline-flex; align-items: center;
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: var(--radius-full); color: var(--text-secondary);
  cursor: pointer; font-size: var(--fs-base); transition: all var(--t-fast);
}
.chip:hover { background: var(--bg-active); border-color: var(--primary); color: var(--primary); }

.message-item { display: flex; gap: var(--space-sm); margin-bottom: var(--space-lg); }
.message-item.user { flex-direction: row-reverse; }
.msg-avatar {
  width: 36px; height: 36px; border-radius: var(--radius-full); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.msg-avatar.user { background: linear-gradient(135deg, var(--primary), var(--primary-dark)); color: #fff; }
.msg-avatar.assistant { background: linear-gradient(135deg, var(--success), #059669); color: #fff; }
.msg-content { max-width: 70%; }
.message-item.user .msg-content { display: flex; flex-direction: column; align-items: flex-end; }
.msg-meta { font-size: var(--fs-xs); color: var(--text-tertiary); margin-bottom: var(--space-xs); display: flex; gap: var(--space-sm); align-items: center; }
.msg-time { font-size: var(--fs-xs); color: var(--text-tertiary); }
.msg-bubble {
  padding: var(--space-sm) var(--space-md); border-radius: var(--radius-md);
  font-size: var(--fs-base); line-height: 1.6; word-break: break-word;
}
.message-item.user .msg-bubble { background: var(--primary); color: #fff; border-bottom-right-radius: var(--space-xs); }
.message-item.assistant .msg-bubble { background: var(--bg-subtle); color: var(--text-primary); border-bottom-left-radius: var(--space-xs); }
.msg-bubble :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: var(--space-md); border-radius: var(--radius-md); overflow-x: auto; margin: var(--space-sm) 0; font-size: var(--fs-sm); }
.msg-bubble :deep(code) { background: rgba(0,0,0,0.08); padding: 2px 6px; border-radius: 4px; font-family: 'Courier New', monospace; font-size: .9em; }
.message-item.user .msg-bubble :deep(code) { background: rgba(255,255,255,0.2); }
.msg-bubble :deep(h2), .msg-bubble :deep(h3), .msg-bubble :deep(h4) { margin: var(--space-md) 0 var(--space-xs); color: inherit; }
.msg-bubble :deep(ul) { margin: var(--space-xs) 0; padding-left: var(--space-lg); }
.msg-bubble :deep(li) { margin: var(--space-xs) 0; }
.msg-phase { margin-top: var(--space-xs); }
.msg-suggestions { display: flex; flex-wrap: wrap; gap: var(--space-xs); margin-top: var(--space-sm); }
.suggestion-chip {
  padding: var(--space-xs) var(--space-md); font-size: var(--fs-sm); white-space: nowrap;
  background: var(--bg-subtle); border: 1px solid var(--border);
  border-radius: var(--radius-full); color: var(--text-secondary);
  cursor: pointer; transition: all var(--t-fast); user-select: none;
}
.suggestion-chip:hover { background: var(--primary); border-color: var(--primary); color: #fff; }
.msg-sources { margin-top: var(--space-sm); border: 1px solid var(--border); border-radius: var(--radius-md); overflow: hidden; }
.sources-header { display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-xs) var(--space-sm); font-size: var(--fs-xs); color: var(--text-secondary); cursor: pointer; background: var(--bg-subtle); user-select: none; }
.sources-header .el-icon.rotated { transform: rotate(180deg); }
.sources-list { max-height: 200px; overflow-y: auto; }
.source-item { display: flex; gap: var(--space-xs); padding: var(--space-xs) var(--space-sm); border-top: 1px solid var(--border); }
.source-badge { width: 20px; height: 20px; border-radius: var(--radius-full); background: var(--primary); color: #fff; font-size: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.source-info { flex: 1; min-width: 0; }
.source-title { font-size: var(--fs-sm); font-weight: var(--fw-medium); color: var(--text-primary); display: block; }
.source-type { font-size: 10px; color: var(--text-tertiary); margin-left: var(--space-xs); }
.source-snippet { font-size: var(--fs-xs); color: var(--text-tertiary); margin-top: 2px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.msg-bubble.typing { display: flex; gap: 4px; align-items: center; padding: var(--space-md); }
.msg-bubble.typing .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--text-tertiary); animation: typing 1.4s infinite; }
.msg-bubble.typing .dot:nth-child(2) { animation-delay: 0.2s; }
.msg-bubble.typing .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes typing {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* Result cards */
.result-cards { display: flex; flex-direction: column; gap: var(--space-md); padding: 0 var(--space-lg) var(--space-lg); }
.result-card { background: var(--bg-card); border-radius: var(--radius-md); border: 1px solid var(--border); overflow: hidden; }
.result-card-header { display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-sm) var(--space-md); font-size: var(--fs-sm); font-weight: var(--fw-semibold); color: var(--text-primary); background: var(--bg-subtle); border-bottom: 1px solid var(--border); }
.result-card-body { padding: var(--space-sm) var(--space-md); }
.result-card-footer { padding: 0 var(--space-md) var(--space-sm); }
.resource-chip { display: flex; align-items: center; gap: var(--space-xs); padding: var(--space-xs) 0; font-size: var(--fs-sm); color: var(--text-secondary); }
.result-card .el-descriptions { --el-descriptions-item-bordered-label-background: var(--bg-subtle); }
.result-card .el-timeline { padding: 0; }
.result-card .el-timeline-item__timestamp { font-size: var(--fs-xs); color: var(--text-tertiary); }

/* Input area */
.input-area {
  padding: var(--space-sm) var(--space-lg);
  border-top: 1px solid var(--border); background: var(--bg-card);
}
.input-wrapper {
  display: flex; gap: var(--space-sm); align-items: flex-end;
}
.input-wrapper .el-input { flex: 1; }
.input-wrapper .el-input :deep(.el-textarea__inner) {
  border-radius: var(--radius-md); padding: 8px 12px; resize: none;
  min-height: 40px; line-height: 1.5; font-size: var(--fs-base);
}
.input-wrapper .el-input :deep(.el-textarea__inner)::placeholder {
  font-size: var(--fs-base);
}
.input-actions { display: flex; gap: var(--space-xs); align-items: center; flex-shrink: 0; }
.input-actions .el-button:not(.is-circle) { white-space: nowrap; }
.input-hint { font-size: 11px; color: var(--text-tertiary); margin: 2px 0 0; text-align: right; opacity: 0.6; }

@media (max-width: 768px) {
  .chat-container { flex-direction: column; padding: 0 var(--space-sm); height: calc(100vh - 64px - var(--space-md)); }
  .session-sidebar { width: 100%; max-height: 200px; }
  .chat-main { flex: 1; }
  .msg-content { max-width: 85%; }
}
</style>
