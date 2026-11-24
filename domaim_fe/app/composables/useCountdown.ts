// 남은 시간을 1초마다 줄인다 (인증 코드 유효시간, 다시 보내기 대기시간 표시용)
export function useCountdown() {
  const remaining = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  function stop() {
    if (timer) clearInterval(timer)
    timer = undefined
  }

  function start(seconds: number) {
    stop()
    remaining.value = seconds
    timer = setInterval(() => {
      remaining.value = Math.max(0, remaining.value - 1)
      if (remaining.value === 0) stop()
    }, 1000)
  }

  // 9:05 형식
  const label = computed(() => `${Math.floor(remaining.value / 60)}:${String(remaining.value % 60).padStart(2, '0')}`)

  onBeforeUnmount(stop)

  return { remaining, label, start }
}
