<script setup lang="ts">
// 아이디 찾기 ↔ 비밀번호 찾기를 한 번에 오갈 수 있는 탭
const route = useRoute()

const tabs = [
  { label: '아이디 찾기', to: '/find-id' },
  { label: '비밀번호 찾기', to: '/reset-password' }
]
</script>

<template>
  <nav aria-label="계정 찾기" class="mb-6 grid grid-cols-2 gap-1 rounded-lg bg-elevated p-1 text-sm font-medium">
    <ULink
      v-for="tab in tabs"
      :key="tab.to"
      :to="tab.to"
      :aria-current="route.path === tab.to ? 'page' : undefined"
      class="rounded-md py-2 text-center transition"
      :class="route.path === tab.to ? 'bg-default text-highlighted shadow-sm' : 'text-muted hover:text-default'"
    >
      {{ tab.label }}
    </ULink>
  </nav>
</template>
