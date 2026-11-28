<script setup lang="ts">
import type { TabsItem } from '@nuxt/ui'

definePageMeta({ middleware: 'auth' })

const { token, member, fetchMe, logout, extendSession } = useAuth()
const { secondsLeft: sessionLeft, label: sessionLabel } = useSessionTimer()
const toast = useToast()

// 헤더의 남은 시간을 누르면 바로 30분 연장
async function extendNow() {
  try {
    await extendSession()
    toast.add({ title: '로그인을 30분 연장했어요', color: 'success', icon: 'i-lucide-timer-reset' })
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '연장하지 못했어요.'), color: 'error' })
  }
}

// 토큰이 만료됐으면 fetchMe가 토큰을 지우므로 로그인 페이지로 보낸다
onMounted(async () => {
  await fetchMe()
  if (!token.value) await navigateTo('/login')
})

// GNB 메뉴. 선택한 메뉴의 value가 active에 담기고, 본문에서 그 내용을 보여준다
const items = [
  { label: '내 소개', icon: 'i-lucide-smile', value: 'profile' },
  { label: '제빵 이야기', icon: 'i-lucide-croissant', value: 'baking' },
  { label: '방명록', icon: 'i-lucide-message-circle-heart', value: 'guestbook' },
  { label: '자료실', icon: 'i-lucide-folder-open', value: 'library' },
  { label: '개발 뉴스', icon: 'i-lucide-newspaper', value: 'news' }
] satisfies TabsItem[]

const active = ref('profile')

// 로고를 누르면 첫 화면으로: 첫 메뉴(내 소개)로 돌아가고 맨 위로 스크롤
function goHome() {
  active.value = 'profile'
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

const githubUrl = 'https://github.com/ZVNHO'
</script>

<template>
  <div class="min-h-screen bg-default">
    <!-- 상단 헤더 -->
    <header class="sticky top-0 z-40 border-b border-default bg-default/80 backdrop-blur">
      <UContainer class="flex flex-wrap items-center justify-between gap-x-4">
        <NuxtLink
          to="/"
          aria-label="첫 화면으로"
          class="flex h-16 items-center gap-2 transition hover:opacity-80"
          @click="goHome"
        >
          <img
            src="/images/sheep.png"
            alt=""
            class="size-9 rounded-full object-cover bg-black"
          >
          <span class="font-bold text-highlighted">Yuna's Bakery Lab</span>
        </NuxtLink>

        <!-- GNB: 넓은 화면에서는 로고 옆, 좁은 화면에서는 헤더 둘째 줄 -->
        <UTabs
          v-model="active"
          :items="items"
          :content="false"
          variant="link"
          class="order-last w-full lg:order-none lg:w-auto"
          :ui="{ list: 'border-none', leadingIcon: 'hidden xl:inline-flex' }"
        />

        <div class="flex items-center gap-2">
          <span
            v-if="member"
            class="hidden xl:inline text-sm text-muted"
          >
            <b class="text-highlighted">{{ member.name }}</b>님 반가워요
          </span>
          <UTooltip
            v-if="sessionLeft !== null"
            text="눌러서 로그인 30분 연장"
          >
            <UButton
              color="neutral"
              variant="ghost"
              icon="i-lucide-timer"
              :label="sessionLabel"
              :aria-label="`로그인 남은 시간 ${sessionLabel}, 눌러서 연장`"
              class="tabular-nums"
              :class="sessionLeft <= 120 ? 'text-warning' : ''"
              @click="extendNow"
            />
          </UTooltip>
          <UButton
            :to="githubUrl"
            target="_blank"
            color="neutral"
            variant="ghost"
            icon="i-simple-icons-github"
            aria-label="GitHub"
          />
          <UColorModeButton />
          <UButton
            color="neutral"
            variant="ghost"
            icon="i-lucide-log-out"
            label="로그아웃"
            @click="logout"
          />
        </div>
      </UContainer>
    </header>

    <!-- 히어로 -->
    <section class="relative overflow-hidden bg-linear-to-br from-amber-50 via-orange-50 to-rose-50 dark:from-stone-900 dark:via-stone-900 dark:to-stone-950">
      <div class="pointer-events-none absolute -top-24 -right-24 size-80 rounded-full bg-amber-200/40 blur-3xl dark:bg-amber-500/10" />
      <UContainer class="relative flex flex-col-reverse items-center gap-8 py-14 md:flex-row md:justify-between md:py-20">
        <div class="text-center md:text-left">
          <UBadge
            label="Baker × Developer"
            variant="subtle"
            size="lg"
            icon="i-lucide-sparkles"
            class="rounded-full"
          />
          <h1 class="mt-4 text-3xl font-extrabold leading-tight text-highlighted sm:text-5xl">
            안녕하세요,<br>
            빵 굽는 개발자 <span class="text-primary">유나</span>예요
          </h1>
          <p class="mt-4 max-w-md text-muted">
            낮에는 코드를 굽고, 주말에는 반죽을 빚어요.<br class="hidden sm:inline">
            제 이야기와 좋아하는 빵들을 소개할게요.
          </p>
        </div>

        <div class="relative shrink-0">
          <div class="absolute inset-0 rounded-full bg-amber-300/50 blur-2xl dark:bg-amber-400/20" />
          <img
            src="/images/sheep.png"
            alt="유나 프로필 사진"
            class="relative size-48 rounded-full object-cover bg-black ring-8 ring-white shadow-2xl sm:size-60 dark:ring-stone-800"
          >
        </div>
      </UContainer>
    </section>

    <!-- GNB에서 선택한 메뉴 내용 -->
    <UContainer class="py-10">
      <HomeProfileTab v-if="active === 'profile'" />
      <HomeBakingTab v-else-if="active === 'baking'" />
      <HomeGuestbookTab v-else-if="active === 'guestbook'" />
      <HomeLibraryTab v-else-if="active === 'library'" />
      <HomeNewsTab v-else-if="active === 'news'" />
    </UContainer>

    <footer class="border-t border-default py-6 text-center text-sm text-muted">
      © 2026 Yuna's Bakery Lab · 오늘도 맛있게 🍞 ·
      <ULink
        :to="githubUrl"
        target="_blank"
        class="font-medium text-primary"
      >
        GitHub
      </ULink>
    </footer>
  </div>
</template>
