export interface Member {
  username: string
  name: string
  sessionSecondsLeft?: number // 로그인 만료까지 남은 초 (서버 기준 30분)
}

interface LoginResponse extends Member {
  token: string
  twoFactorRequired?: boolean
  challenge?: string
  channel?: 'sms' | 'email'
  sentTo?: string
}

export interface TwoFactorChallenge {
  challenge: string
  channel: 'sms' | 'email' // 휴대폰 번호가 있으면 문자, 없으면 이메일
  sentTo: string // 일부 가린 번호·주소
}

export function useAuth() {
  const api = useApi()
  const token = useAuthToken()
  const member = useState<Member | null>('member', () => null)
  const sessionExpiresAt = useSessionExpiresAt()

  // 서버가 알려준 남은 초로 만료 시각(ms)을 계산해 둔다 → SessionGuard가 2분 전에 연장 여부를 묻는다
  function setSession(seconds?: number) {
    sessionExpiresAt.value = seconds ? Date.now() + seconds * 1000 : null
  }

  function clearLogin() {
    token.value = null
    member.value = null
    sessionExpiresAt.value = null
  }

  function saveLogin(res: LoginResponse) {
    token.value = res.token
    member.value = { username: res.username, name: res.name }
    setSession(res.sessionSecondsLeft)
  }

  // 2단계 인증이 켜진 회원이면 로그인 대신 { challenge, channel, sentTo }를 돌려준다 → verifyLogin으로 마무리
  async function login(username: string, password: string): Promise<TwoFactorChallenge | null> {
    const res = await api<LoginResponse>('/auth/login', {
      method: 'POST',
      body: { username, password }
    })
    if (res.twoFactorRequired) return { challenge: res.challenge!, channel: res.channel ?? 'email', sentTo: res.sentTo! }
    saveLogin(res)
    return null
  }

  async function verifyLogin(challenge: string, code: string) {
    saveLogin(await api<LoginResponse>('/auth/login/verify', { method: 'POST', body: { challenge, code } }))
  }

  async function signup(body: { username: string, password: string, name: string, email: string, phone?: string, phoneVerificationToken?: string }) {
    await api('/auth/signup', { method: 'POST', body }) // 백엔드 실제 요청 코드
  }

  // 회원가입 휴대폰 문자 인증: 서버에 문자 설정이 있을 때만 켜진다
  async function getSignupOptions() {
    return api<{ phoneVerification: boolean }>('/auth/signup-options')
  }

  async function requestPhoneCode(phone: string) {
    return api<{ message: string }>('/auth/phone/request', { method: 'POST', body: { phone } })
  }

  async function verifyPhoneCode(phone: string, code: string) {
    const res = await api<{ verificationToken: string }>('/auth/phone/verify', { method: 'POST', body: { phone, code } })
    return res.verificationToken
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
      setSession(member.value.sessionSecondsLeft)
    } catch (error) {
      if ((error as { statusCode?: number }).statusCode === 401) clearLogin()
    }
    return member.value
  }

  async function logout() {
    try {
      await api('/auth/logout', { method: 'POST' })
    } catch {
      // 이미 만료된 토큰이어도 화면에서는 로그아웃 처리
    }
    clearLogin()
    await navigateTo('/login')
  }

  // 「로그인을 연장하시겠습니까?」 → 연장: 서버에서 지금부터 다시 30분
  async function extendSession() {
    const res = await api<Member>('/auth/session/extend', { method: 'POST' })
    setSession(res.sessionSecondsLeft)
  }

  // 시간이 다 지나면 서버 토큰도 이미 만료됐으니 화면만 로그아웃 상태로 바꾼다
  async function expireSession() {
    clearLogin()
    await navigateTo('/login')
  }

  return { token, member, sessionExpiresAt, extendSession, expireSession, login, verifyLogin, signup, getSignupOptions, requestPhoneCode, verifyPhoneCode, isUsernameAvailable, isEmailAvailable, findUsername, requestPasswordReset, resetPassword, fetchMe, logout }
}
