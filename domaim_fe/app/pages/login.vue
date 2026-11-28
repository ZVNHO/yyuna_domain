<script setup lang="ts">
import type { FormError } from '@nuxt/ui'
import type { TwoFactorChallenge } from '~/composables/useAuth'

definePageMeta({ middleware: 'guest' })

const { login, verifyLogin } = useAuth()
const toast = useToast()
const route = useRoute()

// 2단계 인증: 서버가 코드를 메일로 보냈다고 하면 코드 입력 화면으로 바뀐다
const twoFactor = ref<TwoFactorChallenge | null>(null)
const code = ref('')
const { remaining: codeLeft, label: codeLeftLabel, start: startCodeTimer } = useCountdown() // 코드 유효시간 5분 (서버와 같음)
const { remaining: resendLeft, start: startResendWait } = useCountdown() // 다시 받기 60초 대기 (서버와 같음)

// 회원가입·비밀번호 재설정 직후에는 아이디를 미리 채워준다 (/login?username=...)
const state = reactive({ username: typeof route.query.username === 'string' ? route.query.username : '', password: '' })
const loading = ref(false)
const errorMessage = ref('') // 로그인 실패 안내는 토스트 대신 폼 안에 계속 보여준다

watch(() => [state.username, state.password, code.value], () => {
  errorMessage.value = ''
})

// validate()  아이디와 비밀번호가 비어 있는지 확인
function validate(s: Partial<typeof state>): FormError[] {
  const errors: FormError[] = []
  if (!s.username) errors.push({ name: 'username', message: '아이디를 입력해주세요.' })
  if (!s.password) errors.push({ name: 'password', message: '비밀번호를 입력해주세요.' })
  return errors
}

async function welcome() {
  toast.add({ title: '환영해요!', color: 'success', icon: 'i-lucide-croissant' })
  await navigateTo('/')
}

async function onSubmit() {
  loading.value = true
  try {
    const challenge = await login(state.username, state.password) // login()을 호출해요.
    if (!challenge) return await welcome()
    // 같은 challenge면 서버가 1분 안의 재요청이라 메일을 다시 보내지 않은 것 → 타이머 유지
    if (challenge.challenge !== twoFactor.value?.challenge) {
      startCodeTimer(300)
      startResendWait(60)
      code.value = ''
    }
    twoFactor.value = challenge
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '로그인에 실패했습니다.')
  } finally {
    loading.value = false
  }
}

async function onVerify() {
  if (!/^\d{6}$/.test(code.value.trim())) {
    errorMessage.value = '메일로 받은 6자리 숫자를 입력해주세요.'
    return
  }
  loading.value = true
  try {
    await verifyLogin(twoFactor.value!.challenge, code.value)
    await welcome()
  } catch (error) {
    errorMessage.value = apiErrorMessage(error, '인증에 실패했습니다.')
  } finally {
    loading.value = false
  }
}

function backToPassword() {
  twoFactor.value = null
  state.password = ''
}
</script>

<template>
  <AuthShell title="로그인" description="갓 구운 빵 냄새가 나는 곳으로 들어오세요">
    <!-- 2단계: 문자(휴대폰 번호가 있는 회원) 또는 이메일 인증 코드 -->
    <form v-if="twoFactor" class="space-y-4" @submit.prevent="onVerify">
      <UAlert
        :color="codeLeft > 0 ? 'success' : 'warning'"
        variant="subtle"
        :icon="codeLeft > 0 ? (twoFactor.channel === 'sms' ? 'i-lucide-smartphone' : 'i-lucide-shield-check') : 'i-lucide-timer-off'"
        :title="codeLeft > 0 ? `${twoFactor.sentTo} 로 인증번호를 보냈어요` : '인증번호가 만료됐어요'"
        :description="codeLeft > 0
          ? `계정 보호를 위해 ${twoFactor.channel === 'sms' ? '문자' : '메일'}로 받은 6자리 번호를 입력해 주세요.`
          : '「코드 다시 받기」를 눌러 새 번호를 받아주세요.'"
      />

      <UFormField label="인증 코드" :error="errorMessage || undefined">
        <UInput
          v-model="code"
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
            @click="onSubmit"
          />
        </template>
      </UFormField>

      <UButton
        type="submit"
        label="인증하고 로그인"
        size="lg"
        block
        :loading="loading"
        :disabled="codeLeft === 0"
      />
      <UButton
        color="neutral"
        variant="ghost"
        size="sm"
        label="다른 계정으로 로그인"
        block
        @click="backToPassword"
      />
    </form>

    <UForm
      v-else
      :state="state"
      :validate="validate"
      class="space-y-4"
      @submit="onSubmit"
    >
      <UFormField label="아이디" name="username">
        <UInput
          v-model="state.username"
          icon="i-lucide-user"
          placeholder="아이디"
          autocomplete="username"
          size="lg"
          class="w-full"
          :autofocus="!state.username"
        />
      </UFormField>

      <UFormField label="비밀번호" name="password">
        <PasswordInput v-model="state.password" :autofocus="!!state.username" />
      </UFormField>

      <UAlert
        v-if="errorMessage && !twoFactor"
        color="error"
        variant="subtle"
        icon="i-lucide-circle-alert"
        :title="errorMessage"
        description="아이디나 비밀번호가 기억나지 않으면 아래에서 찾을 수 있어요."
      />

      <UButton
        type="submit"
        label="로그인"
        size="lg"
        block
        :loading="loading"
      />
    </UForm>

    <nav aria-label="계정 도움" class="mt-5 flex items-center justify-center text-sm text-muted">
      <ULink to="/find-id" class="px-3 hover:text-primary">
        아이디 찾기
      </ULink>
      <span class="h-3 w-px bg-accented" aria-hidden="true" />
      <ULink to="/reset-password" class="px-3 hover:text-primary">
        비밀번호 찾기
      </ULink>
      <span class="h-3 w-px bg-accented" aria-hidden="true" />
      <ULink to="/signup" class="px-3 font-semibold text-primary">
        회원가입
      </ULink>
    </nav>
  </AuthShell>
</template>
