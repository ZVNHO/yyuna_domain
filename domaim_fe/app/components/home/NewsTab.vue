<script setup lang="ts">
interface NewsItem {
  id: string
  title: string
  summary: string
  link: string
  author: string
  publishedAt: string
}

interface NewsResponse {
  items: NewsItem[]
  fetchedAt: string
  source: string
}

const REFRESH_SECONDS = 60

const api = useApi()

// GeekNews 최신 글 (GET /news). 서버가 원본 피드를 1분마다 받아오고, 화면도 1분마다 다시 묻는다
const { data: news, status, error, refresh } = useLazyAsyncData('news', () => api<NewsResponse>('/news'), {
  server: false
})

const top = computed(() => news.value?.items[0])
const rest = computed(() => news.value?.items.slice(1) ?? [])

// 화면을 연 뒤 새로 올라온 글 표시
const seenIds = new Set<string>()
const newIds = ref(new Set<string>())
watch(news, (value) => {
  if (!value) return
  const fresh = value.items.filter(item => !seenIds.has(item.id)).map(item => item.id)
  if (seenIds.size > 0) newIds.value = new Set([...newIds.value, ...fresh])
  fresh.forEach(id => seenIds.add(id))
})

// 1초마다 "n분 전"과 새로고침 진행 막대를 갱신하고, 60초마다 새로 받아온다
const now = ref(Date.now())
const lastLoadedAt = ref(Date.now())
const secondsSinceUpdate = computed(() => Math.floor((now.value - lastLoadedAt.value) / 1000))
const refreshProgress = computed(() => Math.min(100, (secondsSinceUpdate.value / REFRESH_SECONDS) * 100))

watch(status, (value) => {
  if (value === 'success' || value === 'error') lastLoadedAt.value = Date.now()
})

let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  timer = setInterval(() => {
    now.value = Date.now()
    if (secondsSinceUpdate.value >= REFRESH_SECONDS && status.value !== 'pending') refresh()
  }, 1000)
})
onBeforeUnmount(() => clearInterval(timer))

function timeAgo(value: string) {
  const seconds = Math.max(0, Math.floor((now.value - new Date(value).getTime()) / 1000))
  if (seconds < 60) return '방금 전'
  if (seconds < 3600) return `${Math.floor(seconds / 60)}분 전`
  if (seconds < 86400) return `${Math.floor(seconds / 3600)}시간 전`
  return `${Math.floor(seconds / 86400)}일 전`
}
</script>

<template>
  <div class="mx-auto max-w-4xl space-y-6">
    <!-- 헤더 -->
    <section class="relative overflow-hidden rounded-3xl border border-default bg-linear-to-br from-amber-50 via-orange-50 to-rose-50 p-6 sm:p-8 dark:from-stone-900 dark:via-stone-900 dark:to-stone-950">
      <div class="pointer-events-none absolute -top-20 -right-16 size-64 rounded-full bg-amber-200/50 blur-3xl dark:bg-amber-500/10" />
      <div class="relative flex flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <UBadge
            label="GeekNews 실시간"
            icon="i-lucide-newspaper"
            variant="subtle"
            class="rounded-full"
          />
          <h2 class="mt-3 text-3xl font-extrabold tracking-tight text-highlighted">
            개발 뉴스
          </h2>
          <p class="mt-1 text-muted">
            개발 · 기술 · 스타트업 소식을 1분마다 새로 가져와요
          </p>
        </div>

        <div class="flex items-center gap-2">
          <span class="inline-flex items-center gap-2 rounded-full bg-default/80 px-3 py-1.5 text-xs font-medium text-muted ring-1 ring-default backdrop-blur">
            <span class="relative flex size-2">
              <span class="absolute inline-flex size-full animate-ping rounded-full bg-success opacity-60" />
              <span class="relative inline-flex size-2 rounded-full bg-success" />
            </span>
            <b class="text-success">LIVE</b>
            {{ status === 'pending' ? '불러오는 중…' : `${secondsSinceUpdate}초 전 업데이트` }}
          </span>
          <UButton
            color="neutral"
            variant="outline"
            size="sm"
            icon="i-lucide-refresh-cw"
            aria-label="지금 새로고침"
            class="rounded-full bg-default/80"
            :loading="status === 'pending'"
            @click="refresh()"
          />
        </div>
      </div>

      <!-- 다음 자동 새로고침까지 -->
      <div class="relative mt-6 h-1 overflow-hidden rounded-full bg-default/70" aria-hidden="true">
        <div class="h-full rounded-full bg-primary transition-[width] duration-1000 ease-linear" :style="{ width: `${refreshProgress}%` }" />
      </div>
    </section>

    <!-- 불러오는 중 -->
    <div v-if="status === 'pending' && !news" class="space-y-3">
      <USkeleton class="h-40 rounded-3xl" />
      <div class="grid gap-3 md:grid-cols-2">
        <USkeleton v-for="i in 6" :key="i" class="h-32 rounded-2xl" />
      </div>
    </div>

    <UAlert
      v-else-if="error && !news"
      color="error"
      variant="subtle"
      icon="i-lucide-circle-alert"
      :title="apiErrorMessage(error, '뉴스를 불러오지 못했어요')"
      :actions="[{ label: '다시 시도', color: 'error', variant: 'outline', onClick: () => refresh() }]"
    />

    <template v-else-if="news">
      <!-- 톱 뉴스 -->
      <a
        v-if="top"
        :href="top.link"
        target="_blank"
        rel="noopener noreferrer"
        class="group block rounded-3xl border border-default bg-default p-6 shadow-sm transition hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-lg sm:p-7"
      >
        <p class="flex items-center gap-1.5 text-xs font-semibold text-primary">
          <UIcon name="i-lucide-flame" class="size-4" />
          지금 가장 새로운 소식
          <UBadge v-if="newIds.has(top.id)" label="NEW" size="sm" class="ml-1" />
        </p>
        <h3 class="mt-2 text-xl leading-snug font-bold text-highlighted group-hover:text-primary sm:text-2xl">
          {{ top.title }}
        </h3>
        <p v-if="top.summary" class="mt-3 line-clamp-3 leading-relaxed text-muted">
          {{ top.summary }}
        </p>
        <div class="mt-4 flex items-center gap-2 text-sm text-dimmed">
          <UAvatar :alt="top.author || 'GeekNews'" size="2xs" class="bg-primary/15 text-primary" />
          <span class="font-medium text-muted">{{ top.author || 'GeekNews' }}</span>
          <span aria-hidden="true">·</span>
          <span>{{ timeAgo(top.publishedAt) }}</span>
          <span class="ml-auto inline-flex items-center gap-1 font-medium text-primary opacity-0 transition group-hover:opacity-100">
            원문 보기
            <UIcon name="i-lucide-arrow-up-right" class="size-4" />
          </span>
        </div>
      </a>

      <!-- 나머지 뉴스 -->
      <ul class="grid gap-3 md:grid-cols-2">
        <li v-for="(item, index) in rest" :key="item.id">
          <a
            :href="item.link"
            target="_blank"
            rel="noopener noreferrer"
            class="group flex h-full flex-col rounded-2xl border border-default bg-elevated/40 p-4 transition hover:-translate-y-0.5 hover:border-primary/40 hover:bg-default hover:shadow-md"
          >
            <div class="flex items-start gap-3">
              <span class="flex size-7 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold tabular-nums text-primary">
                {{ index + 2 }}
              </span>
              <h3 class="line-clamp-2 min-w-0 flex-1 leading-snug font-semibold text-highlighted group-hover:text-primary">
                {{ item.title }}
              </h3>
              <UBadge v-if="newIds.has(item.id)" label="NEW" size="sm" class="shrink-0" />
            </div>
            <p v-if="item.summary" class="mt-2 line-clamp-2 pl-10 text-sm leading-relaxed text-muted">
              {{ item.summary }}
            </p>
            <div class="mt-auto flex items-center gap-1.5 pt-3 pl-10 text-xs text-dimmed">
              <span class="font-medium text-muted">{{ item.author || 'GeekNews' }}</span>
              <span aria-hidden="true">·</span>
              <span>{{ timeAgo(item.publishedAt) }}</span>
              <UIcon name="i-lucide-arrow-up-right" class="ml-auto size-4 opacity-0 transition group-hover:opacity-100 group-hover:text-primary" />
            </div>
          </a>
        </li>
      </ul>
    </template>

    <p class="text-center text-xs text-dimmed">
      출처
      <ULink :to="news?.source ?? 'https://news.hada.io'" target="_blank" class="font-medium underline underline-offset-2">
        GeekNews (news.hada.io)
      </ULink>
      · 카드를 누르면 원문이 새 탭에서 열려요
    </p>
  </div>
</template>
