// 로그인하지 않았으면 로그인 페이지로
export default defineNuxtRouteMiddleware(() => {
  if (!useAuthToken().value) {
    return navigateTo('/login')
  }
})
