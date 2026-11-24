// 이미 로그인했으면 홈으로
export default defineNuxtRouteMiddleware(() => {
  if (useAuthToken().value) {
    return navigateTo('/')
  }
})
