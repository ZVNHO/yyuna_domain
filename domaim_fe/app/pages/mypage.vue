<script setup lang="ts">
// 마이페이지: 이름·이메일·휴대폰·비밀번호 수정
// 이메일·휴대폰·비밀번호는 현재 비밀번호로 본인 확인, 새 이메일·번호는 인증 코드로 실제로 쓰는지 확인
definePageMeta({ middleware: 'auth' })

interface Account {
  username: string
  name: string
  email: string | null
  phone: string | null
  phoneVerification: boolean
}

const api = useApi()
const toast = useToast()
const { member, expireSession, requestPhoneCode, verifyPhoneCode } = useAuth()

const account = ref<Account | null>(null)
const loadError = ref(false)

function applyAccount(next: Account) {
  account.value = next
  if (member.value) member.value = { ...member.value, name: next.name } // 헤더 이름도 바로 바뀌게
}

onMounted(async () => {
  try {
    applyAccount(await api<Account>('/account'))
    nameInput.value = account.value!.name
  } catch (error) {
    if ((error as { statusCode?: number }).statusCode === 401) return await expireSession()
    loadError.value = true
  }
})

function success(title: string) {
  toast.add({ title, color: 'success', icon: 'i-lucide-circle-check' })
}

// ── 이름 ──
const nameInput = ref('')
const savingName = ref(false)
const nameChanged = computed(() => !!nameInput.value.trim() && nameInput.value.trim() !== account.value?.name)

async function saveName() {
  savingName.value = true
  try {
    applyAccount(await api<Account>('/account/name', { method: 'PUT', body: { name: nameInput.value } }))
    nameInput.value = account.value!.name
    success('이름을 바꿨어요.')
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '이름을 바꾸지 못했어요.'), color: 'error' })
  } finally {
    savingName.value = false
  }
}

// ── 이메일: 새 이메일 + 현재 비밀번호 → 새 주소로 코드 → 코드 입력 ──
const emailOpen = ref(false)
const email = reactive({ address: '', password: '', code: '' })
const emailSent = ref(false)
const emailBusy = ref(false)
const emailError = ref('')
const { remaining: emailCodeLeft, label: emailCodeLabel, start: startEmailTimer } = useCountdown() // 10분 (서버와 같음)
const { remaining: emailResendLeft, start: startEmailResend } = useCountdown() // 다시 받기 60초

function resetEmail() {
  emailOpen.value = false
  emailSent.value = false
  emailError.value = ''
  Object.assign(email, { address: '', password: '', code: '' })
}

async function sendEmailCode() {
  if (!isEmail(email.address)) return (emailError.value = '이메일을 올바르게 입력해주세요.')
  if (!email.password) return (emailError.value = '현재 비밀번호를 입력해주세요.')
  emailBusy.value = true
  emailError.value = ''
  try {
    await api('/account/email/request', { method: 'POST', body: { email: email.address, currentPassword: email.password } })
    emailSent.value = true
    email.code = ''
    startEmailTimer(600)
    startEmailResend(60)
  } catch (error) {
    emailError.value = apiErrorMessage(error, '인증 메일을 보내지 못했어요.')
  } finally {
    emailBusy.value = false
  }
}

async function confirmEmail() {
  if (!/^\d{6}$/.test(email.code.trim())) return (emailError.value = '메일로 받은 6자리 숫자를 입력해주세요.')
  emailBusy.value = true
  emailError.value = ''
  try {
    applyAccount(await api<Account>('/account/email/confirm', { method: 'POST', body: { code: email.code } }))
    resetEmail()
    success('이메일을 바꿨어요.')
  } catch (error) {
    emailError.value = apiErrorMessage(error, '이메일을 바꾸지 못했어요.')
  } finally {
    emailBusy.value = false
  }
}

// ── 휴대폰: 새 번호 문자 인증 → 현재 비밀번호 → 변경 ──
const phoneOpen = ref(false)
const phone = reactive({ number: '', code: '', password: '' })
const phoneSent = ref(false)
const phoneToken = ref('')
const phoneBusy = ref(false)
const phoneError = ref('')
const { remaining: phoneCodeLeft, label: phoneCodeLabel, start: startPhoneTimer } = useCountdown() // 3분 (서버와 같음)
const { remaining: phoneResendLeft, start: startPhoneResend } = useCountdown() // 다시 받기 60초
const phoneDigits = computed(() => phone.number.replace(/\D/g, ''))

// 인증 뒤에 번호를 고치면 다시 인증해야 한다
watch(() => phone.number, () => {
  phoneSent.value = false
  phoneToken.value = ''
  phone.code = ''
})

function resetPhone() {
  phoneOpen.value = false
  phoneError.value = ''
  Object.assign(phone, { number: '', code: '', password: '' })
}

async function sendPhoneCode() {
  if (!/^01[016789]\d{7,8}$/.test(phoneDigits.value)) return (phoneError.value = '휴대폰 번호를 올바르게 입력해주세요. (예: 010-1234-5678)')
  phoneBusy.value = true
  phoneError.value = ''
  try {
    await requestPhoneCode(phoneDigits.value)
    phoneSent.value = true
    phone.code = ''
    startPhoneTimer(180)
    startPhoneResend(60)
  } catch (error) {
    phoneError.value = apiErrorMessage(error, '인증번호를 보내지 못했어요.')
  } finally {
    phoneBusy.value = false
  }
}

async function confirmPhoneCode() {
  if (!/^\d{6}$/.test(phone.code.trim())) return (phoneError.value = '문자로 받은 6자리 숫자를 입력해주세요.')
  phoneBusy.value = true
  phoneError.value = ''
  try {
    phoneToken.value = await verifyPhoneCode(phoneDigits.value, phone.code.trim())
  } catch (error) {
    phoneError.value = apiErrorMessage(error, '인증에 실패했어요.')
  } finally {
    phoneBusy.value = false
  }
}

async function savePhone() {
  if (!phone.password) return (phoneError.value = '현재 비밀번호를 입력해주세요.')
  phoneBusy.value = true
  phoneError.value = ''
  try {
    applyAccount(await api<Account>('/account/phone', {
      method: 'PUT',
      body: { phone: phoneDigits.value, verificationToken: phoneToken.value, currentPassword: phone.password }
    }))
    resetPhone()
    success('휴대폰 번호를 바꿨어요. 다음 로그인부터 이 번호로 인증번호가 가요.')
  } catch (error) {
    phoneError.value = apiErrorMessage(error, '휴대폰 번호를 바꾸지 못했어요.')
  } finally {
    phoneBusy.value = false
  }
}

// ── 비밀번호 ──
const pw = reactive({ current: '', next: '', confirm: '' })
const pwBusy = ref(false)
const pwError = ref('')
const pwLongEnough = computed(() => pw.next.length >= 4)
const pwMatches = computed(() => !!pw.confirm && pw.next === pw.confirm)

async function savePassword() {
  if (!pw.current) return (pwError.value = '현재 비밀번호를 입력해주세요.')
  if (!pwLongEnough.value) return (pwError.value = '새 비밀번호는 4자 이상이어야 해요.')
  if (!pwMatches.value) return (pwError.value = '새 비밀번호가 일치하지 않아요.')
  pwBusy.value = true
  pwError.value = ''
  try {
    await api('/account/password', { method: 'PUT', body: { currentPassword: pw.current, newPassword: pw.next } })
    Object.assign(pw, { current: '', next: '', confirm: '' })
    success('비밀번호를 바꿨어요.')
  } catch (error) {
    pwError.value = apiErrorMessage(error, '비밀번호를 바꾸지 못했어요.')
  } finally {
    pwBusy.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-linear-to-br from-amber-50 via-orange-50 to-rose-50 px-4 py-10 dark:from-stone-950 dark:via-stone-900 dark:to-stone-950">
    <div class="mx-auto w-full max-w-xl space-y-5">
      <div class="flex items-center justify-between">
        <UButton
          to="/"
          color="neutral"
          variant="ghost"
          icon="i-lucide-arrow-left"
          label="홈으로"
        />
        <UColorModeButton />
      </div>

      <div class="text-center">
        <UIcon
          name="i-lucide-circle-user-round"
          class="size-12 text-primary"
        />
        <h1 class="mt-2 text-2xl font-bold text-highlighted">
          마이페이지
        </h1>
        <p class="mt-1 text-sm text-muted">
          내 정보를 확인하고 바꿀 수 있어요
        </p>
      </div>

      <UAlert
        v-if="loadError"
        color="error"
        variant="subtle"
        icon="i-lucide-circle-alert"
        title="내 정보를 불러오지 못했어요"
        description="잠시 후 새로고침해 주세요."
      />

      <div
        v-else-if="!account"
        class="space-y-4"
      >
        <USkeleton
          v-for="i in 3"
          :key="i"
          class="h-32 rounded-2xl"
        />
      </div>

      <template v-else>
        <!-- 기본 정보 -->
        <UCard class="rounded-2xl">
          <template #header>
            <h2 class="flex items-center gap-2 font-semibold text-highlighted">
              <UIcon
                name="i-lucide-id-card"
                class="size-5 text-primary"
              />
              기본 정보
            </h2>
          </template>
          <div class="space-y-4">
            <UFormField
              label="아이디"
              help="아이디는 바꿀 수 없어요."
            >
              <UInput
                :model-value="account.username"
                icon="i-lucide-user"
                size="lg"
                class="w-full"
                disabled
              />
            </UFormField>
            <form
              class="flex items-end gap-2"
              @submit.prevent="saveName"
            >
              <UFormField
                label="이름"
                class="flex-1"
              >
                <UInput
                  v-model="nameInput"
                  icon="i-lucide-smile"
                  :maxlength="50"
                  autocomplete="name"
                  size="lg"
                  class="w-full"
                />
              </UFormField>
              <UButton
                type="submit"
                label="저장"
                size="lg"
                :loading="savingName"
                :disabled="!nameChanged"
              />
            </form>
          </div>
        </UCard>

        <!-- 이메일 -->
        <UCard class="rounded-2xl">
          <template #header>
            <div class="flex items-center justify-between gap-2">
              <div class="min-w-0">
                <h2 class="flex items-center gap-2 font-semibold text-highlighted">
                  <UIcon
                    name="i-lucide-mail"
                    class="size-5 text-primary"
                  />
                  이메일
                </h2>
                <p class="mt-1 truncate text-sm text-muted">
                  {{ account.email ?? '등록된 이메일이 없어요' }}
                </p>
              </div>
              <UButton
                v-if="!emailOpen"
                color="neutral"
                variant="outline"
                :label="account.email ? '변경' : '등록'"
                @click="emailOpen = true"
              />
            </div>
          </template>
          <p
            v-if="!emailOpen"
            class="text-xs text-dimmed"
          >
            아이디 찾기, 비밀번호 찾기, 로그인 인증(휴대폰이 없을 때)에 쓰여요.
          </p>
          <div
            v-else
            class="space-y-4"
          >
            <UFormField label="새 이메일">
              <UInput
                v-model="email.address"
                type="email"
                icon="i-lucide-at-sign"
                placeholder="new@example.com"
                autocomplete="email"
                size="lg"
                class="w-full"
                :disabled="emailSent"
              />
            </UFormField>
            <UFormField
              v-if="!emailSent"
              label="현재 비밀번호"
              help="본인 확인을 위해 필요해요."
            >
              <PasswordInput v-model="email.password" />
            </UFormField>
            <UFormField
              v-else
              label="인증 코드"
              :help="`${email.address} 로 보낸 6자리 코드를 입력해 주세요.`"
            >
              <UInput
                v-model="email.code"
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
                  <span
                    class="text-sm tabular-nums"
                    :class="emailCodeLeft > 60 ? 'text-primary' : 'text-error'"
                  >{{ emailCodeLabel }}</span>
                </template>
              </UInput>
            </UFormField>
            <p
              v-if="emailError"
              class="text-sm text-error"
            >
              {{ emailError }}
            </p>
            <div class="flex flex-wrap justify-end gap-2">
              <UButton
                color="neutral"
                variant="ghost"
                label="취소"
                @click="resetEmail"
              />
              <UButton
                v-if="emailSent"
                color="neutral"
                variant="outline"
                :label="emailResendLeft > 0 ? `다시 받기 (${emailResendLeft}초)` : '다른 주소 / 다시 받기'"
                :disabled="emailResendLeft > 0 || emailBusy"
                @click="emailSent = false"
              />
              <UButton
                v-if="!emailSent"
                label="인증 코드 받기"
                icon="i-lucide-send"
                :loading="emailBusy"
                @click="sendEmailCode"
              />
              <UButton
                v-else
                label="변경하기"
                icon="i-lucide-check"
                :loading="emailBusy"
                :disabled="emailCodeLeft === 0"
                @click="confirmEmail"
              />
            </div>
          </div>
        </UCard>

        <!-- 휴대폰 (서버에 문자 설정이 있을 때만) -->
        <UCard
          v-if="account.phoneVerification"
          class="rounded-2xl"
        >
          <template #header>
            <div class="flex items-center justify-between gap-2">
              <div>
                <h2 class="flex items-center gap-2 font-semibold text-highlighted">
                  <UIcon
                    name="i-lucide-smartphone"
                    class="size-5 text-primary"
                  />
                  휴대폰 번호
                </h2>
                <p class="mt-1 text-sm text-muted">
                  {{ account.phone ?? '등록된 번호가 없어요' }}
                </p>
              </div>
              <UButton
                v-if="!phoneOpen"
                color="neutral"
                variant="outline"
                :label="account.phone ? '변경' : '등록'"
                @click="phoneOpen = true"
              />
            </div>
          </template>
          <p
            v-if="!phoneOpen"
            class="text-xs text-dimmed"
          >
            번호가 있으면 로그인 인증번호를 문자로 받아요.
          </p>
          <div
            v-else
            class="space-y-4"
          >
            <UFormField label="새 휴대폰 번호">
              <div class="flex gap-2">
                <UInput
                  v-model="phone.number"
                  type="tel"
                  icon="i-lucide-smartphone"
                  placeholder="010-1234-5678"
                  autocomplete="tel"
                  inputmode="tel"
                  size="lg"
                  class="flex-1"
                  :disabled="!!phoneToken"
                />
                <UButton
                  v-if="!phoneToken"
                  color="neutral"
                  variant="outline"
                  size="lg"
                  :label="phoneSent ? (phoneResendLeft > 0 ? `다시 받기 (${phoneResendLeft}초)` : '다시 받기') : '인증번호 받기'"
                  :loading="phoneBusy && !phoneSent"
                  :disabled="phoneResendLeft > 0"
                  @click="sendPhoneCode"
                />
              </div>
            </UFormField>
            <UFormField
              v-if="phoneSent && !phoneToken"
              label="인증번호"
            >
              <div class="flex gap-2">
                <UInput
                  v-model="phone.code"
                  icon="i-lucide-key-round"
                  placeholder="6자리 숫자"
                  inputmode="numeric"
                  autocomplete="one-time-code"
                  :maxlength="6"
                  size="lg"
                  class="flex-1"
                  autofocus
                >
                  <template #trailing>
                    <span
                      class="text-sm tabular-nums"
                      :class="phoneCodeLeft > 60 ? 'text-primary' : 'text-error'"
                    >{{ phoneCodeLabel }}</span>
                  </template>
                </UInput>
                <UButton
                  size="lg"
                  label="확인"
                  :loading="phoneBusy"
                  :disabled="phoneCodeLeft === 0"
                  @click="confirmPhoneCode"
                />
              </div>
            </UFormField>
            <template v-if="phoneToken">
              <p class="flex items-center gap-1 text-sm text-success">
                <UIcon
                  name="i-lucide-circle-check"
                  class="size-4"
                />
                번호 인증을 마쳤어요.
              </p>
              <UFormField
                label="현재 비밀번호"
                help="본인 확인을 위해 필요해요."
              >
                <PasswordInput v-model="phone.password" />
              </UFormField>
            </template>
            <p
              v-if="phoneError"
              class="text-sm text-error"
            >
              {{ phoneError }}
            </p>
            <div class="flex justify-end gap-2">
              <UButton
                color="neutral"
                variant="ghost"
                label="취소"
                @click="resetPhone"
              />
              <UButton
                v-if="phoneToken"
                label="변경하기"
                icon="i-lucide-check"
                :loading="phoneBusy"
                @click="savePhone"
              />
            </div>
          </div>
        </UCard>

        <!-- 비밀번호 -->
        <UCard class="rounded-2xl">
          <template #header>
            <h2 class="flex items-center gap-2 font-semibold text-highlighted">
              <UIcon
                name="i-lucide-lock-keyhole"
                class="size-5 text-primary"
              />
              비밀번호 변경
            </h2>
          </template>
          <form
            class="space-y-4"
            @submit.prevent="savePassword"
          >
            <UFormField label="현재 비밀번호">
              <PasswordInput v-model="pw.current" />
            </UFormField>
            <UFormField label="새 비밀번호">
              <PasswordInput
                v-model="pw.next"
                placeholder="새 비밀번호 (4자 이상)"
                autocomplete="new-password"
              />
              <template #help>
                <span
                  v-if="pw.next"
                  :class="pwLongEnough ? 'text-success' : 'text-error'"
                >{{ pwLongEnough ? '✓ 4자 이상' : '4자 이상 입력해 주세요' }}</span>
              </template>
            </UFormField>
            <UFormField label="새 비밀번호 확인">
              <PasswordInput
                v-model="pw.confirm"
                placeholder="새 비밀번호 한 번 더"
                autocomplete="new-password"
              />
              <template #help>
                <span
                  v-if="pw.confirm"
                  :class="pwMatches ? 'text-success' : 'text-error'"
                >{{ pwMatches ? '✓ 일치해요' : '일치하지 않아요' }}</span>
              </template>
            </UFormField>
            <p
              v-if="pwError"
              class="text-sm text-error"
            >
              {{ pwError }}
            </p>
            <div class="flex justify-end">
              <UButton
                type="submit"
                label="비밀번호 변경"
                icon="i-lucide-check"
                :loading="pwBusy"
                :disabled="!pw.current || !pw.next || !pw.confirm"
              />
            </div>
          </form>
        </UCard>
      </template>
    </div>
  </div>
</template>
