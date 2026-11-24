<script setup lang="ts">
import type { FormError } from '@nuxt/ui'

definePageMeta({ middleware: 'guest' })

const { findUsername } = useAuth()
const toast = useToast()
const { remaining: resendLeft, start: startResendWait } = useCountdown() // 같은 메일을 연달아 보내지 않도록 60초 대기

const state = reactive({ email: '' })
const loading = ref(false)
const sentTo = ref('') // 보낸 이메일 (화면에는 일부를 가려서 표시)

function validate(s: Partial<typeof state>): FormError[] {
  return isEmail(s.email) ? [] : [{ name: 'email', message: '가입할 때 쓴 이메일을 입력해주세요.' }]
}

async function send() {
  loading.value = true
  try {
    await findUsername(state.email)
    sentTo.value = state.email.trim()
    startResendWait(60)
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '요청에 실패했습니다.'), color: 'error' })
  } finally {
    loading.value = false
  }
}

function useOtherEmail() {
  sentTo.value = ''
  state.email = ''
}
</script>

<template>
  <AuthShell title="계정 찾기" description="가입할 때 쓴 이메일로 아이디를 보내드려요">
    <FindAccountTabs />

    <UForm
      v-if="!sentTo"
      :state="state"
      :validate="validate"
      class="space-y-4"
      @submit="send"
    >
      <UFormField label="가입한 이메일" name="email">
        <UInput
          v-model="state.email"
          type="email"
          icon="i-lucide-mail"
          placeholder="you@example.com"
          autocomplete="email"
          size="lg"
          class="w-full"
          autofocus
        />
      </UFormField>

      <UButton
        type="submit"
        label="아이디 받기"
        size="lg"
        block
        :loading="loading"
      />
    </UForm>

    <div v-else class="space-y-5 text-center">
      <div class="mx-auto flex size-14 items-center justify-center rounded-full bg-primary/10 text-primary">
        <UIcon name="i-lucide-mail-check" class="size-7" />
      </div>
      <div>
        <p class="font-semibold text-highlighted">
          메일을 보냈어요
        </p>
        <p class="mt-1 text-sm text-muted">
          <b class="text-default">{{ maskEmail(sentTo) }}</b> 로 가입된 계정이 있으면<br>
          아이디를 담은 메일이 도착해요.
        </p>
      </div>

      <ul class="space-y-1 rounded-lg bg-elevated p-4 text-left text-sm text-muted">
        <li>· 메일이 안 보이면 <b class="text-default">스팸함</b>을 확인해 주세요.</li>
        <li>· 도착까지 몇 분 걸릴 수 있어요.</li>
        <li>· 이메일을 등록하지 않은 예전 계정은 찾을 수 없어요.</li>
      </ul>

      <UButton
        to="/login"
        label="로그인하러 가기"
        size="lg"
        block
      />
      <div class="flex justify-center gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          size="sm"
          :disabled="resendLeft > 0"
          :loading="loading"
          :label="resendLeft > 0 ? `다시 보내기 (${resendLeft}초)` : '다시 보내기'"
          @click="send"
        />
        <UButton
          color="neutral"
          variant="ghost"
          size="sm"
          label="다른 이메일로 찾기"
          @click="useOtherEmail"
        />
      </div>
    </div>

    <template #footer>
      아이디가 기억나셨나요?
      <ULink to="/login" class="font-semibold text-primary">
        로그인
      </ULink>
    </template>
  </AuthShell>
</template>
