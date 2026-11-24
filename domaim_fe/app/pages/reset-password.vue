<script setup lang="ts">
import type { FormError } from '@nuxt/ui'

definePageMeta({ middleware: 'guest' })

const { requestPasswordReset, resetPassword } = useAuth()
const toast = useToast()
const { remaining: codeLeft, label: codeLeftLabel, start: startCodeTimer } = useCountdown() // 코드 유효시간 10분 (서버와 같음)
const { remaining: resendLeft, start: startResendWait } = useCountdown() // 다시 받기 60초 대기 (서버와 같음)

// 1단계: 아이디+이메일로 인증 코드 받기 → 2단계: 코드+새 비밀번호 입력
const step = ref<'request' | 'confirm'>('request')
const loading = ref(false)

const requestState = reactive({ username: '', email: '' })
const confirmState = reactive({ code: '', password: '', passwordConfirm: '' })

const steps = [
  { key: 'request', label: '본인 확인' },
  { key: 'confirm', label: '새 비밀번호' }
] as const

const passwordMatches = computed(() => !!confirmState.passwordConfirm && confirmState.password === confirmState.passwordConfirm)

function validateRequest(s: Partial<typeof requestState>): FormError[] {
  const errors: FormError[] = []
  if (!s.username) errors.push({ name: 'username', message: '아이디를 입력해주세요.' })
  if (!isEmail(s.email)) errors.push({ name: 'email', message: '가입할 때 쓴 이메일을 입력해주세요.' })
  return errors
}

function validateConfirm(s: Partial<typeof confirmState>): FormError[] {
  const errors: FormError[] = []
  if (!/^\d{6}$/.test(s.code?.trim() ?? '')) errors.push({ name: 'code', message: '메일로 받은 6자리 숫자를 입력해주세요.' })
  if (!s.password || s.password.length < 4) errors.push({ name: 'password', message: '비밀번호는 4자 이상이어야 해요.' })
  if (s.password !== s.passwordConfirm) errors.push({ name: 'passwordConfirm', message: '비밀번호가 일치하지 않아요.' })
  return errors
}

async function sendCode() {
  loading.value = true
  try {
    await requestPasswordReset(requestState.username, requestState.email)
    step.value = 'confirm'
    confirmState.code = ''
    startCodeTimer(600)
    startResendWait(60)
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '요청에 실패했습니다.'), color: 'error' })
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  loading.value = true
  try {
    const res = await resetPassword(requestState.username, confirmState.code, confirmState.password)
    toast.add({ title: res.message, color: 'success' })
    await navigateTo({ path: '/login', query: { username: requestState.username } })
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '비밀번호 변경에 실패했습니다.'), color: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="계정 찾기" description="이메일로 받은 인증 코드로 새 비밀번호를 설정해요">
    <FindAccountTabs />

    <!-- 진행 단계 -->
    <ol class="mb-6 flex items-center gap-2 text-xs font-medium">
      <template v-for="(item, index) in steps" :key="item.key">
        <li v-if="index > 0" class="h-px flex-1 bg-accented" aria-hidden="true" />
        <li
          class="flex items-center gap-1.5"
          :class="step === item.key ? 'text-primary' : 'text-muted'"
          :aria-current="step === item.key ? 'step' : undefined"
        >
          <span
            class="flex size-5 items-center justify-center rounded-full text-[11px]"
            :class="step === item.key ? 'bg-primary text-inverted' : 'bg-elevated'"
          >
            <UIcon v-if="step === 'confirm' && item.key === 'request'" name="i-lucide-check" class="size-3" />
            <template v-else>{{ index + 1 }}</template>
          </span>
          {{ item.label }}
        </li>
      </template>
    </ol>

    <UForm
      v-if="step === 'request'"
      :state="requestState"
      :validate="validateRequest"
      class="space-y-4"
      @submit="sendCode"
    >
      <UFormField label="아이디" name="username">
        <UInput
          v-model="requestState.username"
          icon="i-lucide-user"
          placeholder="아이디"
          autocomplete="username"
          size="lg"
          class="w-full"
          autofocus
        />
      </UFormField>

      <UFormField label="가입한 이메일" name="email">
        <UInput
          v-model="requestState.email"
          type="email"
          icon="i-lucide-mail"
          placeholder="you@example.com"
          autocomplete="email"
          size="lg"
          class="w-full"
        />
      </UFormField>

      <UButton
        type="submit"
        label="인증 코드 받기"
        size="lg"
        block
        :loading="loading"
      />
    </UForm>

    <UForm
      v-else
      :state="confirmState"
      :validate="validateConfirm"
      class="space-y-4"
      @submit="onConfirm"
    >
      <UAlert
        :color="codeLeft > 0 ? 'success' : 'warning'"
        variant="subtle"
        :icon="codeLeft > 0 ? 'i-lucide-mail-check' : 'i-lucide-timer-off'"
        :title="codeLeft > 0 ? `${maskEmail(requestState.email)} 로 인증 코드를 보냈어요` : '인증 코드가 만료됐어요'"
        :description="codeLeft > 0 ? '아이디와 이메일이 맞으면 메일이 도착해요. 스팸함도 확인해 주세요.' : '아래 「코드 다시 받기」를 눌러 새 코드를 받아주세요.'"
      />

      <UFormField label="인증 코드" name="code">
        <UInput
          v-model="confirmState.code"
          icon="i-lucide-key-round"
          placeholder="6자리 숫자"
          inputmode="numeric"
          autocomplete="one-time-code"
          :maxlength="6"
          size="lg"
          class="w-full"
          autofocus
        >
          <template #trailing>
            <span class="text-sm tabular-nums" :class="codeLeft > 60 ? 'text-primary' : 'text-error'">
              {{ codeLeftLabel }}
            </span>
          </template>
        </UInput>
        <template #help>
          메일이 안 왔나요?
          <UButton
            variant="link"
            size="xs"
            class="px-1"
            :disabled="resendLeft > 0 || loading"
            :label="resendLeft > 0 ? `코드 다시 받기 (${resendLeft}초)` : '코드 다시 받기'"
            @click="sendCode"
          />
        </template>
      </UFormField>

      <UFormField label="새 비밀번호" name="password">
        <PasswordInput v-model="confirmState.password" placeholder="4자 이상" autocomplete="new-password" />
      </UFormField>

      <UFormField label="새 비밀번호 확인" name="passwordConfirm">
        <PasswordInput
          v-model="confirmState.passwordConfirm"
          placeholder="비밀번호 다시 입력"
          autocomplete="new-password"
          icon="i-lucide-lock-keyhole"
        />
        <template v-if="passwordMatches" #help>
          <span class="flex items-center gap-1 text-success">
            <UIcon name="i-lucide-circle-check" class="size-3.5" />
            비밀번호가 일치해요.
          </span>
        </template>
      </UFormField>

      <UButton
        type="submit"
        label="비밀번호 변경"
        size="lg"
        block
        :loading="loading"
        :disabled="codeLeft === 0"
      />
      <UButton
        color="neutral"
        variant="ghost"
        size="sm"
        label="아이디·이메일 다시 입력"
        block
        @click="step = 'request'"
      />
    </UForm>

    <template #footer>
      비밀번호가 기억나셨나요?
      <ULink to="/login" class="font-semibold text-primary">
        로그인
      </ULink>
    </template>
  </AuthShell>
</template>
