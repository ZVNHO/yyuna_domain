// 로그인 토큰 (쿠키에 7일간 보관)
export function useAuthToken() {
  return useCookie<string | null>('token', { maxAge: 60 * 60 * 24 * 7, sameSite: 'lax' })
}

// 로그인 만료 시각 (밀리초). 새로고침해도 남도록 토큰과 같이 쿠키에 둔다
export function useSessionExpiresAt() {
  return useCookie<number | null>('sessionExpiresAt', { maxAge: 60 * 60 * 24 * 7, sameSite: 'lax' })
}

// Spring Boot API 호출: 로그인 토큰을 Authorization 헤더에 자동으로 붙인다
export function useApi() {
  const { public: { apiBase } } = useRuntimeConfig()
  const token = useAuthToken()

  return $fetch.create({
    baseURL: apiBase,
    onRequest({ options }) {
      if (token.value) {
        options.headers.set('Authorization', `Bearer ${token.value}`)
      }
    }
  })
}

// 회원가입·찾기 화면에서 쓰는 이메일 형식 검사 (백엔드와 같은 규칙)
export function isEmail(value: string | undefined) {
  return !!value && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim())
}

// 화면에 보여줄 때 이메일 일부를 가린다: yuna0314@gmail.com → yu******@gmail.com
export function maskEmail(email: string) {
  const [local = '', domain = ''] = email.trim().split('@')
  const visible = local.length <= 2 ? 1 : 2
  return `${local.slice(0, visible)}${'*'.repeat(Math.max(local.length - visible, 1))}@${domain}`
}

// 백엔드 에러 응답({ message })에서 메시지 꺼내기
export function apiErrorMessage(error: unknown, fallback: string) {
  return (error as { data?: { message?: string } })?.data?.message || fallback
}
