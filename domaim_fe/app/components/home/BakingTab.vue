<script setup lang="ts">
const breads = [
  { emoji: '🥖', name: '바게트', description: '밀가루, 물, 소금, 이스트만으로 만드는 바삭한 프랑스 빵. 칼집(쿠프)이 예쁘게 터지면 성공!' },
  { emoji: '🍞', name: '깜빠뉴', description: '천연 발효종으로 오래 발효시켜 은은한 산미와 쫄깃한 속살이 매력적인 시골빵.' },
  { emoji: '🌾', name: '호밀빵', description: '호밀과 씨앗을 듬뿍 넣어 고소하고 묵직해요. 얇게 썰어 치즈와 함께 먹으면 최고.' },
  { emoji: '🥐', name: '크루아상', description: '버터를 반죽 사이에 접어 수십 겹의 결을 만든 빵. 인내심 테스트 1순위예요.' }
]

const steps = [
  { title: '계량', description: '재료를 그램 단위로 정확하게. 제빵은 과학이에요.', icon: 'i-lucide-scale' },
  { title: '반죽', description: '글루텐이 얇은 막을 만들 때까지 치대요.', icon: 'i-lucide-hand' },
  { title: '1차 발효', description: '따뜻한 곳에서 반죽이 2배가 될 때까지 기다려요.', icon: 'i-lucide-timer' },
  { title: '성형', description: '가스를 빼고 원하는 모양으로 빚어요.', icon: 'i-lucide-shapes' },
  { title: '2차 발효', description: '모양을 잡은 뒤 한 번 더 부풀려요.', icon: 'i-lucide-cloud' },
  { title: '굽기', description: '예열한 오븐에서 노릇노릇하게. 집 안 가득 빵 냄새!', icon: 'i-lucide-flame' }
]
</script>

<template>
  <div class="space-y-12">
    <!-- 대표 사진 -->
    <div class="relative overflow-hidden rounded-3xl shadow-xl">
      <img
        src="/images/bread.png"
        alt="바구니에 담긴 여러 가지 빵"
        class="h-72 w-full object-cover sm:h-96"
      >
      <div class="absolute inset-0 bg-linear-to-t from-black/70 via-black/20 to-transparent" />
      <div class="absolute bottom-0 p-6 sm:p-10">
        <p class="text-sm font-semibold text-amber-300">
          BAKING STORY
        </p>
        <h2 class="mt-1 text-2xl font-bold text-white sm:text-4xl">
          오늘도 반죽 중입니다
        </h2>
        <p class="mt-2 max-w-lg text-sm text-white/80 sm:text-base">
          밀가루와 물, 소금, 그리고 시간. 단순한 재료로 매번 다른 빵이 나오는 게 베이킹의 매력이에요.
        </p>
      </div>
    </div>

    <!-- 좋아하는 빵 -->
    <section>
      <h3 class="text-xl font-bold text-highlighted">
        제가 좋아하는 빵
      </h3>
      <div class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div
          v-for="bread in breads"
          :key="bread.name"
          class="group rounded-2xl border border-default bg-elevated/50 p-5 transition hover:-translate-y-1 hover:shadow-lg"
        >
          <div class="flex size-12 items-center justify-center rounded-xl bg-amber-100 text-2xl dark:bg-amber-500/15">
            {{ bread.emoji }}
          </div>
          <p class="mt-4 font-semibold text-highlighted">
            {{ bread.name }}
          </p>
          <p class="mt-1 text-sm leading-relaxed text-muted">
            {{ bread.description }}
          </p>
        </div>
      </div>
    </section>

    <!-- 제빵 과정 -->
    <section>
      <h3 class="text-xl font-bold text-highlighted">
        빵이 만들어지는 과정
      </h3>
      <ol class="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <li
          v-for="(step, index) in steps"
          :key="step.title"
          class="flex gap-4 rounded-2xl border border-default p-5"
        >
          <div class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary text-inverted font-bold">
            {{ index + 1 }}
          </div>
          <div>
            <p class="flex items-center gap-1.5 font-semibold text-highlighted">
              <UIcon :name="step.icon" class="size-4 text-primary" />
              {{ step.title }}
            </p>
            <p class="mt-1 text-sm text-muted">
              {{ step.description }}
            </p>
          </div>
        </li>
      </ol>
    </section>
  </div>
</template>
