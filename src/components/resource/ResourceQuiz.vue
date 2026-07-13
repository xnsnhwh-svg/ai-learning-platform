<template>
  <div class="resource-quiz">
    <h3>{{ title }}</h3>
    <div v-for="(q, i) in questions" :key="i" class="question-item">
      <p class="q-title">{{ i + 1 }}. {{ q.question }}</p>
      <div v-if="q.type === 'choice' && q.options">
        <div v-for="(opt, j) in q.options" :key="j" class="option" @click="selectAnswer(i, j)">
          <el-radio v-model="q.selected" :label="j" :disabled="q.submitted">
            {{ String.fromCharCode(65 + j) }}. {{ opt }}
          </el-radio>
        </div>
      </div>
      <div v-if="q.type === 'judge'">
        <el-radio-group v-model="q.selected" :disabled="q.submitted">
          <el-radio :label="0">正确</el-radio>
          <el-radio :label="1">错误</el-radio>
        </el-radio-group>
      </div>
      <div v-if="q.submitted" class="result" :class="q.correct ? 'correct' : 'wrong'">
        {{ q.correct ? '✓ 回答正确' : '✗ 回答错误' }}
        <span v-if="!q.correct">正确答案：{{ q.answer }}</span>
      </div>
    </div>
    <el-button v-if="!allSubmitted" type="primary" @click="submitAll" class="submit-btn">提交答案</el-button>
    <div v-else class="score">{{ correctCount }}/{{ questions.length }} 正确</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({ content: String, title: String })

const questions = ref([])
const allSubmitted = ref(false)

try {
  const parsed = JSON.parse(props.content || '[]')
  questions.value = (Array.isArray(parsed) ? parsed : [parsed]).map(q => ({
    ...q,
    selected: null,
    submitted: false,
    correct: false
  }))
} catch {
  questions.value = [{ question: props.content, type: 'choice', options: ['无法解析题目'], selected: null, submitted: false, correct: false }]
}

const correctCount = computed(() => questions.value.filter(q => q.correct).length)

const selectAnswer = (qi, oi) => {
  if (!questions.value[qi].submitted) questions.value[qi].selected = oi
}

const submitAll = () => {
  questions.value.forEach(q => {
    if (q.selected !== null && q.selected !== undefined) {
      q.submitted = true
      q.correct = String(q.selected) === String(q.answer)
    }
  })
  allSubmitted.value = questions.value.every(q => q.submitted)
}
</script>

<style scoped>
.resource-quiz { padding: var(--space-md); }
.resource-quiz h3 { margin-bottom: var(--space-md); color: var(--text-primary); font-size: var(--fs-lg); }
.question-item { margin-bottom: var(--space-lg); padding: var(--space-md); background: var(--bg-subtle); border-radius: var(--radius-md); }
.q-title { font-weight: var(--fw-medium); margin-bottom: var(--space-sm); color: var(--text-primary); }
.option { padding: var(--space-xs) 0; cursor: pointer; }
.result { margin-top: var(--space-xs); padding: 6px 12px; border-radius: var(--radius-sm); font-size: var(--fs-sm); }
.result.correct { background: var(--success-light, #d1fae5); color: var(--success); }
.result.wrong { background: var(--danger-light, #fee2e2); color: var(--danger); }
.submit-btn { margin-top: var(--space-sm); }
.score { margin-top: var(--space-md); font-size: var(--fs-lg); font-weight: var(--fw-semibold); color: var(--primary); text-align: center; }
</style>
