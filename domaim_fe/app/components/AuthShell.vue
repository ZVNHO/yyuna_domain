<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-4 py-12 bg-linear-to-br from-amber-50 via-orange-50 to-rose-50 dark:from-stone-950 dark:via-stone-900 dark:to-stone-950">
    <div class="w-full max-w-md">
      <div class="flex flex-col items-center text-center mb-6">
        <img
          src="/images/sheep.png"
          alt="유나 프로필 사진"
          class="size-28 rounded-full object-cover bg-black ring-4 ring-white shadow-xl dark:ring-stone-800"
        >
        <p class="mt-4 text-sm font-medium text-primary">
          Yuna's Bakery Lab
        </p>
        <h1 class="mt-1 text-2xl font-bold text-highlighted">
          {{ title }}
        </h1>
        <p class="mt-1 text-sm text-muted">
          {{ description }}
        </p>
      </div>

      <UCard class="shadow-xl shadow-amber-900/5 rounded-2xl">
        <slot />
      </UCard>

      <div class="mt-6 text-center text-sm text-muted">
        <slot name="footer" />
      </div>
    </div>
  </div>
</template>
