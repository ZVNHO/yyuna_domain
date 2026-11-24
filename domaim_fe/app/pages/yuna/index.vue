<script setup lang="ts">
import type { TableColumn } from '@nuxt/ui'

interface Yuna {
  yuna: number
  age: number
  hobby: string
  job: string
}

const { public: { apiBase } } = useRuntimeConfig()
const toast = useToast()

// 목록 조회 (GET /demos)
const { data: rows, refresh } = await useFetch<Yuna[]>('/demos', {
  baseURL: apiBase,
  default: () => [],
  server: false
})

// Grid: Nuxt UI 기본 테이블
const columns: TableColumn<Yuna>[] = [
  { accessorKey: 'yuna', header: '번호' },
  { accessorKey: 'age', header: '나이' },
  { accessorKey: 'hobby', header: '취미' },
  { accessorKey: 'job', header: '직업' },
  { id: 'actions', header: '' }
]

// 등록/수정 폼
const form = reactive<Yuna>({ yuna: 0, age: 0, hobby: '', job: '' })
const editing = ref(false)

function edit(row: Yuna) {
  Object.assign(form, row)
  editing.value = true
}

function reset() {
  Object.assign(form, { yuna: 0, age: 0, hobby: '', job: '' })
  editing.value = false
}

async function save() {
  try {
    if (editing.value) {
      const { yuna, ...body } = form
      await $fetch(`/demos/${yuna}`, { baseURL: apiBase, method: 'PUT', body })
    } else {
      await $fetch('/demos', { baseURL: apiBase, method: 'POST', body: form })
    }
    toast.add({ title: '저장되었습니다.', color: 'success' })
    reset()
    await refresh()
  } catch {
    toast.add({ title: '저장에 실패했습니다.', color: 'error' })
  }
}

async function remove(row: Yuna) {
  try {
    await $fetch(`/demos/${row.yuna}`, { baseURL: apiBase, method: 'DELETE' })
    toast.add({ title: '삭제되었습니다.', color: 'success' })
    await refresh()
  } catch {
    toast.add({ title: '삭제에 실패했습니다.', color: 'error' })
  }
}

// Charts: ApexCharts
const chartOptions = computed(() => ({
  chart: { id: 'yuna-age', toolbar: { show: false } },
  xaxis: { categories: rows.value.map(r => String(r.yuna)) },
  dataLabels: { enabled: false }
}))
const chartSeries = computed(() => [
  { name: '나이', data: rows.value.map(r => r.age) }
])
</script>

<template>
  <UContainer class="py-8 space-y-6">
    <h1 class="text-2xl font-bold">
      유나 관리
    </h1>

    <div class="grid gap-6 lg:grid-cols-3">
      <UCard class="lg:col-span-2">
        <template #header>
          목록
        </template>

        <UTable :data="rows" :columns="columns">
          <template #actions-cell="{ row }">
            <div class="flex justify-end gap-2">
              <UButton
                size="xs"
                variant="subtle"
                label="수정"
                @click="edit(row.original)"
              />
              <UButton
                size="xs"
                color="error"
                variant="subtle"
                label="삭제"
                @click="remove(row.original)"
              />
            </div>
          </template>
        </UTable>
      </UCard>

      <UCard>
        <template #header>
          {{ editing ? '수정' : '등록' }}
        </template>

        <UForm :state="form" class="space-y-4" @submit="save">
          <UFormField label="번호" name="yuna">
            <UInputNumber v-model="form.yuna" :disabled="editing" class="w-full" />
          </UFormField>
          <UFormField label="나이" name="age">
            <UInputNumber v-model="form.age" class="w-full" />
          </UFormField>
          <UFormField label="취미" name="hobby">
            <UInput v-model="form.hobby" class="w-full" />
          </UFormField>
          <UFormField label="직업" name="job">
            <UInput v-model="form.job" class="w-full" />
          </UFormField>

          <div class="flex gap-2">
            <UButton type="submit" :label="editing ? '수정' : '등록'" />
            <UButton
              v-if="editing"
              variant="ghost"
              label="취소"
              @click="reset"
            />
          </div>
        </UForm>
      </UCard>
    </div>

    <UCard>
      <template #header>
        나이 차트
      </template>

      <ClientOnly>
        <apexchart
          type="bar"
          height="300"
          :options="chartOptions"
          :series="chartSeries"
        />
      </ClientOnly>
    </UCard>
  </UContainer>
</template>
