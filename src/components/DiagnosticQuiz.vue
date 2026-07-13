<template>
  <div class="diagnostic-quiz">
    <div class="quiz-header">
      <h3><el-icon><Notebook /></el-icon> 先了解一下你</h3>
      <p class="quiz-sub">回答几个问题，AI 将为你定制专属学习路径</p>
    </div>

    <div class="quiz-progress">
      <el-progress :percentage="(currentStep / totalSteps) * 100" :show-text="false" />
      <span class="progress-label">{{ currentStep }} / {{ totalSteps }}</span>
    </div>

    <transition name="fade-up" mode="out-in">
      <!-- Step 1: 基础水平 -->
      <div v-if="currentStep === 1" key="s1" class="step-card">
        <h4 class="step-question">你现在是什么水平？</h4>
        <div class="options">
          <div
            v-for="opt in levelOptions"
            :key="opt.value"
            class="option-card"
            :class="{ selected: answers.level === opt.value }"
            @click="answers.level = opt.value"
          >
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- Step 2: 学习目标 -->
      <div v-if="currentStep === 2" key="s2" class="step-card">
        <h4 class="step-question">你的学习目标是什么？</h4>
        <div class="options">
          <div
            v-for="opt in goalOptions"
            :key="opt.value"
            class="option-card"
            :class="{ selected: answers.goal === opt.value }"
            @click="answers.goal = opt.value"
          >
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- Step 3: 每周投入 -->
      <div v-if="currentStep === 3" key="s3" class="step-card">
        <h4 class="step-question">你每周能投入多少时间？</h4>
        <div class="options">
          <div
            v-for="opt in timeOptions"
            :key="opt.value"
            class="option-card"
            :class="{ selected: answers.time === opt.value }"
            @click="answers.time = opt.value"
          >
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>

      <!-- Step 4: 选择课程 -->
      <div v-if="currentStep === 4" key="s4" class="step-card">
        <h4 class="step-question">你想学哪门课程？</h4>
        <div class="options">
          <div
            v-for="c in courses"
            :key="c.id"
            class="option-card"
            :class="{ selected: answers.courseId === c.id }"
            @click="answers.courseId = c.id; answers.courseName = c.name"
          >
            <span class="option-label">{{ c.name }}</span>
          </div>
          <div v-if="!courses.length" class="no-courses">暂无可用课程</div>
        </div>
      </div>

      <!-- Step 5: 学习偏好 -->
      <div v-if="currentStep === 5" key="s5" class="step-card">
        <h4 class="step-question">你喜欢哪些学习方式？</h4>
        <div class="options multi">
          <div
            v-for="opt in styleOptions"
            :key="opt.value"
            class="option-card"
            :class="{ selected: answers.styles.includes(opt.value) }"
            @click="toggleStyle(opt.value)"
          >
            <el-icon class="check-icon" v-if="answers.styles.includes(opt.value)"><Check /></el-icon>
            <span class="option-label">{{ opt.label }}</span>
          </div>
        </div>
      </div>
    </transition>

    <div class="quiz-footer">
      <el-button v-if="currentStep > 1" text @click="prevStep">上一步</el-button>
      <div class="footer-right">
        <el-button
          v-if="currentStep < totalSteps"
          type="primary"
          :disabled="!canNext"
          @click="nextStep"
        >
          下一步
        </el-button>
        <el-button
          v-if="currentStep === totalSteps"
          type="primary"
          :loading="submitting"
          :disabled="!canNext"
          @click="submit"
        >
          提交并生成路径
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'

const emit = defineEmits(['complete', 'load-courses'])
const props = defineProps({
  courses: { type: Array, default: () => [] }
})

const totalSteps = 5
const currentStep = ref(1)
const submitting = ref(false)

const answers = reactive({
  level: '',
  goal: '',
  time: '',
  courseId: null,
  courseName: '',
  styles: []
})

const levelOptions = [
  { value: 'zero', label: '完全零基础（没写过代码）' },
  { value: 'basic', label: '有一点基础（学过一些概念）' },
  { value: 'forgot', label: '学过但忘了（需要重新捡起来）' },
  { value: 'experienced', label: '有一定经验（能写简单项目）' }
]

const goalOptions = [
  { value: 'overview', label: '了解基础知识' },
  { value: 'project', label: '能独立做项目' },
  { value: 'interview', label: '准备面试/考试' },
  { value: 'fill-gaps', label: '查漏补缺' },
  { value: 'deep', label: '系统深入学习' }
]

const timeOptions = [
  { value: 'lt2', label: '< 2 小时（碎片化学习）' },
  { value: '2-5', label: '2-5 小时（有空就学）' },
  { value: '5-10', label: '5-10 小时（比较规律）' },
  { value: 'gt10', label: '10+ 小时（全力投入）' }
]

const styleOptions = [
  { value: 'document', label: '图文文档' },
  { value: 'video', label: '视频讲解' },
  { value: 'quiz', label: '练习题/测验' },
  { value: 'project', label: '动手做项目' },
  { value: 'mindmap', label: '思维导图' }
]

const stepValid = computed(() => {
  switch (currentStep.value) {
    case 1: return !!answers.level
    case 2: return !!answers.goal
    case 3: return !!answers.time
    case 4: return !!answers.courseId
    case 5: return answers.styles.length > 0
    default: return false
  }
})

const canNext = computed(() => stepValid.value)

const toggleStyle = (value) => {
  const idx = answers.styles.indexOf(value)
  if (idx >= 0) {
    answers.styles.splice(idx, 1)
  } else {
    answers.styles.push(value)
  }
}

const nextStep = () => {
  if (canNext.value && currentStep.value < totalSteps) {
    currentStep.value++
  }
}

const prevStep = () => {
  if (currentStep.value > 1) {
    currentStep.value--
  }
}

const buildDiagnosticText = () => {
  const levelMap = { zero: '零基础', basic: '有基础', forgot: '学过忘了', experienced: '有经验' }
  const goalMap = { overview: '了解基础', project: '做项目', interview: '面试', 'fill-gaps': '查漏补缺', deep: '深入学习' }
  const timeMap = { lt2: '<2h/周', '2-5': '2-5h/周', '5-10': '5-10h/周', gt10: '10h+/周' }
  const styleMap = { document: '文档', video: '视频', quiz: '练习', project: '项目', mindmap: '导图' }
  return `基础水平：${levelMap[answers.level] || answers.level}。` +
    `学习目标：${goalMap[answers.goal] || answers.goal}。` +
    `每周时间：${timeMap[answers.time] || answers.time}。` +
    `课程：${answers.courseName}。` +
    `学习偏好：${answers.styles.map(s => styleMap[s] || s).join('、')}。`
}

const submit = async () => {
  submitting.value = true
  try {
    emit('complete', {
      diagnosticText: buildDiagnosticText(),
      answers: { ...answers }
    })
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  emit('load-courses')
})
</script>

<style scoped>
.diagnostic-quiz {
  max-width: 640px;
  margin: 0 auto;
  padding: var(--space-lg);
}

.quiz-header { text-align: center; margin-bottom: var(--space-lg); }
.quiz-header h3 { font-size: var(--fs-xl); color: var(--text-primary); display: flex; align-items: center; justify-content: center; gap: var(--space-xs); margin: 0 0 var(--space-xs); }
.quiz-sub { color: var(--text-secondary); font-size: var(--fs-sm); margin: 0; }

.quiz-progress { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-xl); }
.progress-label { font-size: var(--fs-sm); color: var(--text-tertiary); white-space: nowrap; min-width: 36px; text-align: right; }

.step-question { font-size: var(--fs-lg); color: var(--text-primary); margin: 0 0 var(--space-lg); text-align: center; }

.options { display: flex; flex-direction: column; gap: var(--space-sm); }
.options.multi { flex-direction: row; flex-wrap: wrap; }
.options.multi .option-card { flex: 1; min-width: 120px; }

.option-card {
  padding: var(--space-md) var(--space-lg);
  background: var(--bg-subtle);
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--t-fast);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.option-card:hover { background: var(--bg-active); border-color: var(--primary-light); }
.option-card.selected { background: var(--bg-active); border-color: var(--primary); }

.option-label { font-size: var(--fs-base); color: var(--text-primary); }
.check-icon { color: var(--primary); font-size: 16px; }

.no-courses { text-align: center; padding: var(--space-xl); color: var(--text-tertiary); }

.quiz-footer { display: flex; justify-content: space-between; align-items: center; margin-top: var(--space-xl); padding-top: var(--space-md); border-top: 1px solid var(--border); }
.footer-right { margin-left: auto; }

/* Transition */
.fade-up-enter-active, .fade-up-leave-active {
  transition: all 0.25s ease;
}
.fade-up-enter-from { opacity: 0; transform: translateY(12px); }
.fade-up-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
