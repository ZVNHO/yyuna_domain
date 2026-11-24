<script setup lang="ts">
import type { FormError } from '@nuxt/ui'
import type { Ref } from 'vue'

definePageMeta({ middleware: 'guest' })

const { signup, isUsernameAvailable, isEmailAvailable } = useAuth()
const toast = useToast()

const state = reactive({ username: '', password: '', passwordConfirm: '', name: '', email: '' }) // 5개의 값을 하나로 묶는다
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
  loading.value = true
  try {
    await signup({ name: state.name, username: state.username, email: state.email, password: state.password }) // signin 호출
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
