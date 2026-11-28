// 로그인 만료까지 남은 시간. 만료 시각(쿠키)과 현재 시각의 차이로 계산해서
// 백그라운드 탭에서 타이머가 느려져도 돌아오면 바로 정확한 값이 된다
export function useSessionTimer() {
  const expiresAt = useSessionExpiresAt()
  const now = ref(Date.now())
  let timer: ReturnType<typeof setInterval> | undefined

  onMounted(() => {
    timer = setInterval(() => (now.value = Date.now()), 1000)
  })
  onBeforeUnmount(() => clearInterval(timer))

  // 만료 시각을 모르면 null (로그인 전)
  const secondsLeft = computed(() => expiresAt.value ? Math.max(0, Math.ceil((expiresAt.value - now.value) / 1000)) : null)

  // 29:05 형식
  const label = computed(() => {
    const s = secondsLeft.value ?? 0
    return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`
  })

  return { secondsLeft, label }
}
