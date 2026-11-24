export interface Member {
  username: string
  name: string
}

interface LoginResponse extends Member {
  token: string
  twoFactorRequired?: boolean
  challenge?: string
  maskedEmail?: string
}

export interface TwoFactorChallenge {
  challenge: string
  maskedEmail: string
}

export function useAuth() {
  const api = useApi()
  const token = useAuthToken()
  const member = useState<Member | null>('member', () => null)

  function saveLogin(res: LoginResponse) {
    token.value = res.token
    member.value = { username: res.username, name: res.name }
  }

  // 2단계 인증이 켜진 회원이면 로그인 대신 { challenge, maskedEmail }을 돌려준다 → verifyLogin으로 마무리
  async function login(username: string, password: string): Promise<TwoFactorChallenge | null> {
    const res = await api<LoginResponse>('/auth/login', {
      method: 'POST',
      body: { username, password }
    })
    if (res.twoFactorRequired) return { challenge: res.challenge!, maskedEmail: res.maskedEmail! }
    saveLogin(res)
    return null
  }

  async function verifyLogin(challenge: string, code: string) {
    saveLogin(await api<LoginResponse>('/auth/login/verify', { method: 'POST', body: { challenge, code } }))
  }

  async function signup(body: { username: string, password: string, name: string, email: string }) {
    await api('/auth/signup', { method: 'POST', body }) // 백엔드 실제 요청 코드
  }

  // 회원가입 화면의 실시간 중복 확인
  async function isUsernameAvailable(username: string) {
    const res = await api<{ available: boolean }>('/auth/check-username', { query: { username } })
    return res.available
  }

  async function isEmailAvailable(email: string) {
    const res = await api<{ available: boolean }>('/auth/check-email', { query: { email } })
    return res.available
  }

  // 아이디 찾기·비밀번호 재설정: 서버는 가입 여부와 관계없이 같은 안내 메시지를 돌려준다
  async function findUsername(email: string) {
    return api<{ message: string }>('/auth/find-username', { method: 'POST', body: { email } })
  }

  async function requestPasswordReset(username: string, email: string) {
    return api<{ message: string }>('/auth/password-reset/request', { method: 'POST', body: { username, email } })
  }

  async function resetPassword(username: string, code: string, newPassword: string) {
    return api<{ message: string }>('/auth/password-reset/confirm', { method: 'POST', body: { username, code, newPassword } })
  }

  // 토큰으로 내 정보 조회. 토큰이 만료(401)됐으면 로그아웃 상태로 만든다
  async function fetchMe() {
    if (!token.value) return null
    try {
      member.value = await api<Member>('/auth/me')
    } catch (error) {
      if ((error as { statusCode?: number }).statusCode === 401) {
        token.value = null
        member.value = null
      }
    }
    return member.value
  }

  async function logout() {
    try {
      await api('/auth/logout', { method: 'POST' })
    } catch {
      // 이미 만료된 토큰이어도 화면에서는 로그아웃 처리
    }
    token.value = null
    member.value = null
    await navigateTo('/login')
  }

  return { token, member, login, verifyLogin, signup, isUsernameAvailable, isEmailAvailable, findUsername, requestPasswordReset, resetPassword, fetchMe, logout }
}
