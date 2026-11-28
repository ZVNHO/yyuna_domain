<script setup lang="ts">
// 로그인 만료 2분 전에 「로그인을 연장하시겠습니까?」를 띄우고, 시간이 다 되면 로그아웃한다
const WARN_SECONDS = 120

const { token, extendSession, expireSession, logout } = useAuth()
const { secondsLeft, label } = useSessionTimer()
const toast = useToast()
const extending = ref(false)

const open = computed(() => !!token.value && secondsLeft.value !== null && secondsLeft.value > 0 && secondsLeft.value <= WARN_SECONDS)

watch(secondsLeft, async (seconds) => {
  if (seconds !== 0 || !token.value) return
  await expireSession()
  toast.add({ title: '로그인 시간이 지나 로그아웃됐어요', description: '다시 로그인해 주세요.', color: 'warning', icon: 'i-lucide-timer-off' })
})

async function extend() {
  extending.value = true
  try {
    await extendSession()
    toast.add({ title: '로그인을 30분 연장했어요', color: 'success', icon: 'i-lucide-timer-reset' })
  } catch (error) {
    // 이미 만료됐으면(401) 로그인 화면으로
    if ((error as { statusCode?: number }).statusCode === 401) await expireSession()
    else toast.add({ title: apiErrorMessage(error, '연장하지 못했어요. 다시 시도해 주세요.'), color: 'error' })
  } finally {
    extending.value = false
  }
}
</script>

<template>
  <UModal
    :open="open"
    :dismissible="false"
    :close="false"
    title="로그인을 연장하시겠습니까?"
    :description="`보안을 위해 ${label} 뒤에 자동으로 로그아웃돼요.`"
  >
    <template #body>
      <div class="flex items-center gap-3">
        <UIcon
          name="i-lucide-timer"
          class="size-8 shrink-0 text-warning"
        />
        <p class="text-sm text-muted">
          계속 이용하려면 <b class="text-highlighted">연장하기</b>를 눌러 주세요. 지금부터 다시 30분 동안 로그인이 유지돼요.
        </p>
      </div>
    </template>
    <template #footer>
      <div class="flex w-full justify-end gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          label="로그아웃"
          @click="logout"
        />
        <UButton
          icon="i-lucide-timer-reset"
          :label="`연장하기 (${label})`"
          :loading="extending"
          autofocus
          @click="extend"
        />
      </div>
    </template>
  </UModal>
</template>
