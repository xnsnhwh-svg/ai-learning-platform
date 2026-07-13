<template>
  <div class="profile-page page-enter">
    <AppNavbar />
    <div class="page-container">
      <div class="page-header">
        <div class="header-icon"><el-icon :size="32"><User /></el-icon></div>
        <h1>用户学习画像</h1>
        <p>你的个性化学习分析报告</p>
      </div>

      <div v-loading="loading" class="profile-content">
        <div v-if="error" class="empty-state">
          <div class="empty-illustration">
            <svg width="80" height="80" viewBox="0 0 80 80" fill="none">
              <circle cx="40" cy="40" r="38" stroke="#e2e8f0" stroke-width="2" fill="#f8fafc"/>
              <circle cx="40" cy="30" r="12" fill="#dbeafe" stroke="#93c5fd" stroke-width="1.5"/>
              <path d="M24 56c0-8.8 7.2-16 16-16s16 7.2 16 16" stroke="#93c5fd" stroke-width="1.5" fill="none" stroke-linecap="round"/>
            </svg>
          </div>
          <h3>暂无画像数据</h3>
          <p>去「智能对话」页面开始学习，系统会自动为你生成画像</p>
          <el-button type="primary" round @click="$router.push('/chat')">开始对话</el-button>
        </div>

        <template v-else-if="profile">
          <div class="profile-grid">
            <div class="profile-card radar-card">
              <div class="card-header"><el-icon><TrendCharts /></el-icon>学习能力雷达图</div>
              <div ref="radarRef" class="chart-container"></div>
            </div>

            <div class="profile-card stats-card">
              <div class="card-header"><el-icon><InfoFilled /></el-icon>学习概况</div>
              <div class="stat-items">
                <div class="stat-item">
                  <div class="stat-left"><el-icon><Flag /></el-icon><span class="stat-label">学习目标</span></div>
                  <span class="stat-value">{{ profileData.goal || '未设定' }}</span>
                </div>
                <div class="stat-item">
                  <div class="stat-left"><el-icon><TrendCharts /></el-icon><span class="stat-label">当前水平</span></div>
                  <el-tag :type="levelType" size="large" round>{{ profileData.level || '未知' }}</el-tag>
                </div>
                <div class="stat-item">
                  <div class="stat-left"><el-icon><MagicStick /></el-icon><span class="stat-label">学习风格</span></div>
                  <span class="stat-value">{{ profileData.style || '未评估' }}</span>
                </div>
                <div class="stat-item">
                  <div class="stat-left"><el-icon><Clock /></el-icon><span class="stat-label">每周可用时间</span></div>
                  <span class="stat-value">{{ profileData.hoursPerWeek || '未知' }} 小时</span>
                </div>
              </div>
            </div>
          </div>

          <div class="profile-card tag-card">
            <div class="card-header"><el-icon><Collection /></el-icon>知识点掌握情况</div>
            <div class="tag-cloud" v-if="weakPoints.length">
              <div v-for="(wp, i) in weakPoints" :key="i" class="tag-item" :style="{ fontSize: tagSize(i), '--tag-color': tagColor(i) }">{{ wp }}</div>
            </div>
            <el-empty v-else description="暂无知识点数据" :image-size="40" />
          </div>

          <div class="profile-card insight-card">
            <div class="card-header"><el-icon><WarningFilled /></el-icon>学习建议</div>
            <div class="insights">
              <div class="insight-item" v-for="(tip, i) in learningTips" :key="i">
                <div class="insight-icon"><el-icon><Check /></el-icon></div>
                <span>{{ tip }}</span>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import AppNavbar from '../components/AppNavbar.vue'
import { useAuthStore } from '../stores/auth'
import { getUserProfile } from '../api/profile'
import * as echarts from 'echarts'

const authStore = useAuthStore()

const profile = ref(null)
const loading = ref(true)
const error = ref(false)
const radarRef = ref(null)

const profileData = computed(() => {
  if (!profile.value || !profile.value.dimensions) return {}
  const dims = profile.value.dimensions
  if (typeof dims === 'string') return JSON.parse(dims)
  return dims
})

const levelType = computed(() => {
  const level = profileData.value.level
  if (!level) return 'info'
  if (level.includes('零基础') || level.includes('初级')) return 'info'
  if (level.includes('中级') || level.includes('进阶')) return 'warning'
  if (level.includes('高级') || level.includes('精通')) return 'danger'
  return 'info'
})

const weakPoints = computed(() => {
  const wp = profileData.value.weakPoints
  if (Array.isArray(wp)) return wp
  const kps = profileData.value.knowledgePoints
  if (Array.isArray(kps)) return kps.filter(k => typeof k === 'string')
  return ['面向对象编程', '异常处理', '集合框架', '多线程', 'IO流', '数据库编程']
})

const learningTips = computed(() => {
  return [
    '建议每周保持至少 ' + (profileData.value.hoursPerWeek || '5') + ' 小时的学习时间',
    '结合理论学习和实践练习，效果更佳',
    '定期复习已学知识，巩固记忆',
    '尝试用学到的知识解决实际问题'
  ]
})

const tagSize = (i) => {
  const sizes = [14, 16, 18, 20, 22, 24]
  return sizes[i % sizes.length] + 'px'
}

const tagColor = (i) => {
  const colors = ['#5B7B8A', '#6B8F71', '#C4904A', '#C2453D', '#8C8680', '#C4704A']
  return colors[i % colors.length]
}

const loadProfile = async () => {
  loading.value = true
  try {
    const res = await getUserProfile(authStore.userId)
    profile.value = res.data
    if (!profile.value) {
      error.value = true
      return
    }
    error.value = false
    await nextTick()
    renderRadar()
  } catch (e) {
    console.error('加载画像失败', e)
    error.value = true
  } finally {
    loading.value = false
  }
}

const renderRadar = () => {
  if (!radarRef.value) return
  const chart = echarts.init(radarRef.value)
  const pd = profileData.value
  const indicators = [
    { name: '基础知识', max: 100 },
    { name: '编程能力', max: 100 },
    { name: '逻辑思维', max: 100 },
    { name: '学习效率', max: 100 },
    { name: '坚持度', max: 100 },
    { name: '实践能力', max: 100 }
  ]
  const values = [
    pd.baseKnowledge || pd.foundation || 60,
    pd.programming || pd.coding || 50,
    pd.logicThinking || pd.logic || 70,
    pd.learningEfficiency || pd.efficiency || 55,
    pd.perseverance || 65,
    pd.practice || pd.handsOn || 45
  ]
  chart.setOption({
    radar: { indicator: indicators, shape: 'circle' },
    series: [{
      type: 'radar', data: [{ value: values, name: '当前水平', areaStyle: { opacity: 0.2 } }],
      lineStyle: { color: '#5B7B8A', width: 2 },
      areaStyle: { color: '#5B7B8A', opacity: 0.15 },
      itemStyle: { color: '#5B7B8A' }
    }],
    tooltip: { trigger: 'item' }
  })
  window.addEventListener('resize', () => chart.resize())
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page { min-height: 100vh; background: var(--bg-page); }

.page-container { max-width: var(--content-max); margin: 0 auto; padding: var(--space-2xl) var(--space-lg); }
.page-header { text-align: center; margin-bottom: var(--space-2xl); }
.header-icon {
  width: 56px; height: 56px; border-radius: var(--radius-full); margin: 0 auto var(--space-md);
  display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, var(--primary-light), var(--primary)); color: #fff;
}
.page-header h1 { font-size: var(--fs-2xl); color: var(--text-primary); margin-bottom: var(--space-xs); }
.page-header p { color: var(--text-secondary); }

/* Empty state */
.empty-state { text-align: center; padding: var(--space-3xl) var(--space-lg); background: var(--bg-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-sm); border: 1px solid var(--border); }
.empty-illustration { margin-bottom: var(--space-lg); }
.empty-state h3 { margin: var(--space-md) 0 var(--space-xs); color: var(--text-primary); }
.empty-state p { margin-bottom: var(--space-lg); color: var(--text-secondary); }

/* Cards */
.profile-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-lg); margin-bottom: var(--space-lg); }
.profile-card {
  background: var(--bg-card); border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm); border: 1px solid var(--border);
  padding: var(--space-lg); transition: box-shadow var(--t-normal);
}
.profile-card:hover { box-shadow: var(--shadow-md); }
.card-header {
  display: flex; align-items: center; gap: var(--space-xs);
  font-size: var(--fs-base); font-weight: var(--fw-semibold); color: var(--text-primary);
  margin-bottom: var(--space-md);
}
.card-header .el-icon { color: var(--primary); font-size: var(--fs-lg); }
.chart-container { width: 100%; height: 320px; }

/* Stats */
.stat-items { display: flex; flex-direction: column; }
.stat-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: var(--space-md) 0; border-bottom: 1px solid var(--border);
}
.stat-item:last-child { border-bottom: none; }
.stat-left { display: flex; align-items: center; gap: var(--space-xs); }
.stat-left .el-icon { color: var(--text-tertiary); font-size: var(--fs-base); }
.stat-label { color: var(--text-secondary); font-size: var(--fs-base); }
.stat-value { color: var(--text-primary); font-weight: var(--fw-medium); }

/* Tag cloud */
.tag-card { margin-bottom: var(--space-lg); }
.tag-cloud { display: flex; flex-wrap: wrap; gap: var(--space-sm); padding: var(--space-sm) 0; }
.tag-item {
  padding: var(--space-xs) var(--space-md); border-radius: var(--radius-full);
  cursor: default; transition: all var(--t-fast);
  background: var(--bg-subtle); border: 1px solid transparent;
  color: var(--tag-color, var(--primary));
}
.tag-item:hover { transform: scale(1.08); border-color: var(--tag-color, var(--primary)); background: transparent; }

/* Insights */
.insights { display: flex; flex-direction: column; gap: var(--space-sm); }
.insight-item {
  display: flex; align-items: center; gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: linear-gradient(135deg, #ecfdf5, #f0fdf4);
  border-radius: var(--radius-md); color: var(--text-primary);
  border: 1px solid rgba(16,185,129,0.15);
}
.insight-icon {
  width: 24px; height: 24px; border-radius: var(--radius-full); flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
  background: var(--success); color: #fff;
}
.insight-icon .el-icon { font-size: 14px; }

@media (max-width: 768px) {
  .profile-grid { grid-template-columns: 1fr; }
}
</style>
