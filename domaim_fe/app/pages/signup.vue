<script setup lang="ts">
import type { FormError } from '@nuxt/ui'
import type { Ref } from 'vue'

definePageMeta({ middleware: 'guest' })

const { signup, isUsernameAvailable, isEmailAvailable, getSignupOptions, requestPhoneCode, verifyPhoneCode } = useAuth()
const toast = useToast()

const state = reactive({ username: '', password: '', passwordConfirm: '', name: '', email: '', phone: '' }) // 6개의 값을 하나로 묶는다
// v-model은 입력칸과 값을 양방향으로 연결해요.
const loading = ref(false)

// 아이디·이메일 실시간 중복 확인
type Availability = 'idle' | 'checking' | 'available' | 'taken'
const usernameStatus = ref<Availability>('idle')
const emailStatus = ref<Availability>('idle')

// 입력을 멈추고 0.4초 뒤에 서버에 확인한다. 그사이 값이 또 바뀌면 이전 결과는 버린다
function watchAvailability(source: () => string, status: Ref<Availability>, isValid: (value: string) => boolean, check: (value: string) => Promise<boolean>) {
  let timer: ReturnType<typeof setTimeout> | undefined
  watch(source, (value) => {
    clearTimeout(timer)
    status.value = 'idle'
    if (!isValid(value)) return
    status.value = 'checking'
    timer = setTimeout(async () => {
      try {
        const available = await check(value.trim())
        if (source() === value) status.value = available ? 'available' : 'taken'
      } catch {
        if (source() === value) status.value = 'idle' // 확인 실패 시 가입 버튼에서 서버가 다시 검사한다
      }
    }, 400)
  })
}

watchAvailability(() => state.username, usernameStatus, value => !!value.trim(), isUsernameAvailable)
watchAvailability(() => state.email, emailStatus, isEmail, isEmailAvailable)

// 휴대폰 문자 인증 (서버에 문자 설정이 있을 때만 칸이 보인다)
// 인증번호 받기 → 6자리 입력 → 확인 → 받은 토큰을 가입 요청에 함께 보낸다
const phoneRequired = ref(false)
onMounted(async () => {
  try {
    phoneRequired.value = (await getSignupOptions()).phoneVerification
  } catch {
    // 옵션을 못 받으면 휴대폰 칸 없이 진행 (필요하면 서버가 가입 때 알려준다)
  }
})

const phoneCode = ref('')
const phoneCodeSent = ref(false)
const phoneToken = ref('') // 인증을 마치면 채워진다
const sendingPhoneCode = ref(false)
const verifyingPhone = ref(false)
const phoneError = ref('')
const { remaining: phoneCodeLeft, label: phoneCodeLeftLabel, start: startPhoneTimer } = useCountdown() // 인증번호 3분 (서버와 같음)
const { remaining: phoneResendLeft, start: startPhoneResendWait } = useCountdown() // 다시 받기 60초 (서버와 같음)

const phoneDigits = computed(() => state.phone.replace(/\D/g, ''))
const phoneValid = computed(() => /^01[016789]\d{7,8}$/.test(phoneDigits.value))

// 인증을 마친 뒤 번호를 바꾸면 다시 인증해야 한다
watch(() => state.phone, () => {
  phoneToken.value = ''
  phoneCodeSent.value = false
  phoneCode.value = ''
  phoneError.value = ''
})

async function sendPhoneCode() {
  if (!phoneValid.value) {
    phoneError.value = '휴대폰 번호를 올바르게 입력해주세요. (예: 010-1234-5678)'
    return
  }
  sendingPhoneCode.value = true
  phoneError.value = ''
  try {
    await requestPhoneCode(phoneDigits.value)
    phoneCodeSent.value = true
    phoneCode.value = ''
    startPhoneTimer(180)
    startPhoneResendWait(60)
  } catch (error) {
    phoneError.value = apiErrorMessage(error, '인증번호를 보내지 못했어요.')
  } finally {
    sendingPhoneCode.value = false
  }
}

async function confirmPhoneCode() {
  if (!/^\d{6}$/.test(phoneCode.value.trim())) {
    phoneError.value = '문자로 받은 6자리 숫자를 입력해주세요.'
    return
  }
  verifyingPhone.value = true
  phoneError.value = ''
  try {
    phoneToken.value = await verifyPhoneCode(phoneDigits.value, phoneCode.value.trim())
  } catch (error) {
    phoneError.value = apiErrorMessage(error, '인증에 실패했어요.')
  } finally {
    verifyingPhone.value = false
  }
}

const passwordLongEnough = computed(() => state.password.length >= 4)
const passwordMatches = computed(() => !!state.passwordConfirm && state.password === state.passwordConfirm)

function validate(s: Partial<typeof state>): FormError[] {
  const errors: FormError[] = []
  if (!s.username) errors.push({ name: 'username', message: '아이디를 입력해주세요.' })
  if (!s.password || s.password.length < 4) errors.push({ name: 'password', message: '비밀번호는 4자 이상이어야 해요.' })
  if (s.password !== s.passwordConfirm) errors.push({ name: 'passwordConfirm', message: '비밀번호가 일치하지 않아요.' })
  if (!s.name) errors.push({ name: 'name', message: '이름을 입력해주세요.' })
  if (!isEmail(s.email)) errors.push({ name: 'email', message: '이메일을 올바르게 입력해주세요.' })
  return errors
}

async function onSubmit() {
  if (usernameStatus.value === 'taken' || emailStatus.value === 'taken') return // 입력칸에 이미 안내가 떠 있다
  if (phoneRequired.value && !phoneToken.value) {
    phoneError.value = '휴대폰 번호 인증을 먼저 완료해 주세요.'
    return
  }
  loading.value = true
  try {
    await signup({
      name: state.name,
      username: state.username,
      email: state.email,
      password: state.password,
      ...(phoneRequired.value ? { phone: phoneDigits.value, phoneVerificationToken: phoneToken.value } : {})
    }) // signin 호출
    // 서버 응답이 언제 올지 몰라서 응답을 기다린 뒤 다음 코드를 실행해야 하기 때문
    toast.add({ title: '회원가입이 완료되었어요. 로그인해주세요!', color: 'success' })
    await navigateTo({ path: '/login', query: { username: state.username } })
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '회원가입에 실패했습니다.'), color: 'error' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <AuthShell title="회원가입" description="유나의 빵집 친구가 되어주세요">
    <!-- 폼 제출 실행 -->
    <UForm
      :state="state"
      :validate="validate"
      class="space-y-4"
      @submit="onSubmit"
    >
      <UFormField
        label="아이디"
        name="username"
        required
        :error="usernameStatus === 'taken' ? '이미 사용 중인 아이디예요.' : undefined"
      >
        <UInput
          v-model="state.username"
          icon="i-lucide-user"
          placeholder="아이디"
          autocomplete="username"
          size="lg"
          class="w-full"
          autofocus
          :loading="usernameStatus === 'checking'"
        />
        <template v-if="usernameStatus === 'available'" #help>
          <span class="flex items-center gap-1 text-success">
            <UIcon name="i-lucide-circle-check" class="size-3.5" />
            사용할 수 있는 아이디예요.
          </span>
        </template>
      </UFormField>

      <UFormField label="비밀번호" name="password" required>
        <PasswordInput v-model="state.password" placeholder="4자 이상" autocomplete="new-password" />
        <template #help>
          <span class="flex items-center gap-1" :class="passwordLongEnough ? 'text-success' : 'text-muted'">
            <UIcon :name="passwordLongEnough ? 'i-lucide-circle-check' : 'i-lucide-circle'" class="size-3.5" />
            4자 이상
          </span>
        </template>
      </UFormField>

      <UFormField label="비밀번호 확인" name="passwordConfirm" required>
        <PasswordInput
          v-model="state.passwordConfirm"
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

      <UFormField label="이름" name="name" required>
        <UInput
          v-model="state.name"
          icon="i-lucide-smile"
          placeholder="홍길동"
          autocomplete="name"
          size="lg"
          class="w-full"
        />
      </UFormField>

      <UFormField
        label="이메일"
        name="email"
        required
        :error="emailStatus === 'taken' ? '이미 가입된 이메일이에요. 아이디 찾기를 이용해 주세요.' : undefined"
      >
        <UInput
          v-model="state.email"
          type="email"
          icon="i-lucide-mail"
          placeholder="you@example.com"
          autocomplete="email"
          size="lg"
          class="w-full"
          :loading="emailStatus === 'checking'"
        />
        <template #help>
          <span v-if="emailStatus === 'available'" class="flex items-center gap-1 text-success">
            <UIcon name="i-lucide-circle-check" class="size-3.5" />
            사용할 수 있는 이메일이에요.
          </span>
          <span v-else>아이디·비밀번호를 잊었을 때 이 이메일로 찾을 수 있어요.</span>
        </template>
      </UFormField>

      <!-- 휴대폰 문자 인증: 로그인할 때 이 번호로 인증번호가 와요 -->
      <UFormField
        v-if="phoneRequired"
        label="휴대폰 번호"
        required
        :error="phoneError || undefined"
      >
        <div class="flex gap-2">
          <UInput
            v-model="state.phone"
            type="tel"
            inputmode="numeric"
            icon="i-lucide-smartphone"
            placeholder="010-1234-5678"
            autocomplete="tel"
            size="lg"
            class="min-w-0 flex-1"
            :disabled="!!phoneToken"
          />
          <UButton
            v-if="!phoneToken"
            size="lg"
            color="neutral"
            variant="outline"
            class="shrink-0"
            :loading="sendingPhoneCode"
            :disabled="!phoneValid || phoneResendLeft > 0"
            :label="phoneResendLeft > 0 ? `다시 받기 (${phoneResendLeft}초)` : (phoneCodeSent ? '다시 받기' : '인증번호 받기')"
            @click="sendPhoneCode"
          />
          <UButton
            v-else
            size="lg"
            color="neutral"
            variant="ghost"
            class="shrink-0"
            label="번호 변경"
            @click="state.phone = ''"
          />
        </div>

        <!-- 인증번호 입력 -->
        <div v-if="phoneCodeSent && !phoneToken" class="mt-2 flex gap-2">
          <UInput
            v-model="phoneCode"
            inputmode="numeric"
            autocomplete="one-time-code"
            :maxlength="6"
            icon="i-lucide-key-round"
            placeholder="인증번호 6자리"
            size="lg"
            class="min-w-0 flex-1"
            autofocus
            @keydown.enter.prevent="confirmPhoneCode"
          >
            <template #trailing>
              <span class="text-sm tabular-nums" :class="phoneCodeLeft > 30 ? 'text-primary' : 'text-error'">
                {{ phoneCodeLeftLabel }}
              </span>
            </template>
          </UInput>
          <UButton
            size="lg"
            class="shrink-0"
            label="확인"
            :loading="verifyingPhone"
            :disabled="phoneCodeLeft === 0"
            @click="confirmPhoneCode"
          />
        </div>

        <template #help>
          <span v-if="phoneToken" class="flex items-center gap-1 text-success">
            <UIcon name="i-lucide-circle-check" class="size-3.5" />
            휴대폰 번호 인증을 마쳤어요.
          </span>
          <span v-else-if="phoneCodeSent && phoneCodeLeft === 0">인증번호가 만료됐어요. 「다시 받기」를 눌러주세요.</span>
          <span v-else-if="phoneCodeSent">문자로 받은 6자리 번호를 3분 안에 입력해 주세요.</span>
          <span v-else>로그인할 때 이 번호로 인증번호 문자가 와요.</span>
        </template>
      </UFormField>

      <!-- 버튼클릭으로 폼제출 -->
      <UButton
        type="submit"
        label="가입하기"
        size="lg"
        block
        :loading="loading"
      />
    </UForm>

    <template #footer>
      이미 계정이 있으신가요?
      <ULink to="/login" class="font-semibold text-primary">
        로그인
      </ULink>
    </template>
  </AuthShell>
</template>
