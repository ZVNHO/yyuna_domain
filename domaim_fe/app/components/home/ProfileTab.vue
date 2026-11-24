<script setup lang="ts">
interface Profile {
  yuna: number
  age: number
  hobby: string
  job: string
}

const api = useApi()

// 내 소개 정보 (GET /demos/1)
const { data: profile, status, refresh } = useLazyAsyncData('profile', () => api<Profile>('/demos/1'), {
  server: false
})

// 나이는 생일(2003-10-28, 한국 시간 0시)부터 지금까지를 1초마다 다시 계산해 보여준다 (만 나이)
const BIRTH = { year: 2003, month: 10, day: 28 }
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  timer = setInterval(() => (now.value = Date.now()), 1000)
})
onBeforeUnmount(() => clearInterval(timer))

const age = computed(() => {
  // 보는 사람의 시간대와 관계없이 한국 시간(UTC+9) 기준으로 계산
  const kst = new Date(now.value + 9 * 60 * 60 * 1000)
  let years = kst.getUTCFullYear() - BIRTH.year
  let months = kst.getUTCMonth() + 1 - BIRTH.month
  let days = kst.getUTCDate() - BIRTH.day
  if (days < 0) {
    months -= 1
    days += new Date(Date.UTC(kst.getUTCFullYear(), kst.getUTCMonth(), 0)).getUTCDate() // 지난달 일수
  }
  if (months < 0) {
    years -= 1
    months += 12
  }
  const pad = (value: number) => String(value).padStart(2, '0')
  return {
    years,
    koreanYears: kst.getUTCFullYear() - BIRTH.year + 1, // 세는 나이
    // 태어난 날부터 센 전체 개월 수 (예: 277개월 26일 …)
    detail: `${years * 12 + months}개월 ${pad(days)}일 ${pad(kst.getUTCHours())}시간 ${pad(kst.getUTCMinutes())}분 ${pad(kst.getUTCSeconds())}초`
  }
})

const stats = computed(() => profile.value
  ? [
      { label: '나이', value: `만 ${age.value.years}살`, note: `세는 나이 ${age.value.koreanYears}살`, detail: age.value.detail, icon: 'i-lucide-cake' },
      { label: '취미', value: profile.value.hobby, icon: 'i-lucide-chef-hat' },
      { label: '직업', value: profile.value.job, icon: 'i-lucide-laptop' }
    ]
  : [])

const likes = ['사워도우', '버터 크루아상', '소금빵', 'Vue', 'Spring Boot', '말랑한 양']
</script>

<template>
  <div class="grid items-center gap-10 md:grid-cols-[320px_1fr]">
    <div class="relative mx-auto w-full max-w-xs">
      <div class="absolute -inset-3 rotate-3 rounded-3xl bg-amber-200/60 dark:bg-amber-500/15" />
      <img
        src="/images/sheep.png"
        alt="유나 프로필 사진"
        class="relative aspect-square w-full rounded-3xl object-cover bg-black shadow-xl"
      >
    </div>

    <div>
      <p class="text-sm font-semibold text-primary">
        ABOUT ME
      </p>
      <h2 class="mt-1 text-3xl font-bold text-highlighted">
        유나
      </h2>
      <p class="mt-3 leading-relaxed text-muted">
        버그를 잡다가 막히면 반죽을 치대는 개발자예요.
        코드도 빵도 기다림이 필요하다는 걸 배우는 중이에요.
        정성껏 발효시킨 빵처럼, 오래 봐도 좋은 서비스를 만들고 싶어요.
      </p>

      <div class="mt-6 grid gap-3 sm:grid-cols-3">
        <template v-if="status === 'pending'">
          <USkeleton v-for="i in 3" :key="i" class="h-20 rounded-2xl" />
        </template>

        <template v-else>
          <div
            v-for="stat in stats"
            :key="stat.label"
            class="rounded-2xl border border-default bg-elevated/50 p-4"
          >
            <div class="flex items-center gap-2 text-sm text-muted">
              <UIcon :name="stat.icon" class="size-4 text-primary" />
              {{ stat.label }}
            </div>
            <p class="mt-1 text-xl font-bold text-highlighted">
              {{ stat.value }}
            </p>
            <p v-if="'note' in stat" class="text-xs text-dimmed">
              {{ stat.note }}
            </p>
            <p v-if="'detail' in stat" class="mt-1 text-xs tabular-nums text-muted">
              {{ stat.detail }}
            </p>
          </div>
        </template>
      </div>

      <UAlert
        v-if="status === 'error'"
        class="mt-4"
        color="error"
        variant="subtle"
        icon="i-lucide-circle-alert"
        title="프로필을 불러오지 못했어요"
        description="백엔드가 실행 중인지, DB에 yuna = 1 데이터가 있는지 확인해주세요."
        :actions="[{ label: '다시 시도', color: 'error', variant: 'outline', onClick: () => refresh() }]"
      />

      <div class="mt-6">
        <p class="text-sm font-medium text-muted">
          좋아하는 것
        </p>
        <div class="mt-2 flex flex-wrap gap-2">
          <UBadge
            v-for="like in likes"
            :key="like"
            :label="like"
            color="neutral"
            variant="soft"
            class="rounded-full"
          />
        </div>
      </div>
    </div>
  </div>
</template>
