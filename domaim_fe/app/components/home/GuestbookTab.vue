<script setup lang="ts">
import type { Reply } from './ReplyThread.vue'

interface GuestbookEntry extends Reply {
  replies: Reply[]
}

const api = useApi()
const toast = useToast()

// 방명록 목록 (GET /guestbook)
const { data: entries, status, refresh } = useLazyAsyncData('guestbook', () => api<GuestbookEntry[]>('/guestbook'), {
  server: false,
  default: () => []
})

// 페이지당 30건
const PAGE_SIZE = 30
const page = ref(1)
const pagedEntries = computed(() => entries.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))
// 페이지를 바꾸면 목록 맨 위로
const listTop = ref<HTMLElement | null>(null)
watch(page, () => listTop.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
// 삭제로 마지막 페이지가 비면 앞 페이지로
watch(() => entries.value.length, (length) => {
  page.value = Math.min(page.value, Math.max(1, Math.ceil(length / PAGE_SIZE)))
})

const content = ref('')
const posting = ref(false)

async function submit() {
  if (!content.value.trim()) return
  posting.value = true
  try {
    await api('/guestbook', { method: 'POST', body: { content: content.value } })
    content.value = ''
    toast.add({ title: '방명록을 남겼어요!', color: 'success' })
    await refresh()
    page.value = 1 // 새 글은 맨 앞에 있으니 첫 페이지로
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '등록에 실패했습니다.'), color: 'error' })
  } finally {
    posting.value = false
  }
}

// 본인 글 수정
const editingId = ref<number | null>(null)
const editText = ref('')
const saving = ref(false)

function startEdit(entry: GuestbookEntry) {
  editingId.value = entry.id
  editText.value = entry.content
}

async function saveEdit(entry: GuestbookEntry) {
  if (!editText.value.trim()) return
  saving.value = true
  try {
    await api(`/guestbook/${entry.id}`, { method: 'PUT', body: { content: editText.value } })
    editingId.value = null
    toast.add({ title: '수정했어요.', color: 'success' })
    await refresh()
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '수정에 실패했습니다.'), color: 'error' })
  } finally {
    saving.value = false
  }
}

async function remove(entry: GuestbookEntry) {
  try {
    await api(`/guestbook/${entry.id}`, { method: 'DELETE' })
    toast.add({ title: '삭제했어요.', color: 'success' })
    await refresh()
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '삭제에 실패했습니다.'), color: 'error' })
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>

<template>
  <div class="mx-auto max-w-2xl space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-highlighted">
        방명록
      </h2>
      <p class="mt-1 text-muted">
        좋아하는 빵이나 응원 한마디를 남겨주세요 🐑
      </p>
    </div>

    <UCard class="rounded-2xl">
      <form
        class="space-y-3"
        @submit.prevent="submit"
      >
        <UTextarea
          v-model="content"
          :rows="3"
          :maxlength="500"
          autoresize
          placeholder="오늘 먹은 빵은 무엇인가요?"
          class="w-full"
        />
        <div class="flex items-center justify-between">
          <span class="text-xs text-dimmed">{{ content.length }} / 500</span>
          <UButton
            type="submit"
            label="남기기"
            icon="i-lucide-send"
            :loading="posting"
            :disabled="!content.trim()"
          />
        </div>
      </form>
    </UCard>

    <div
      v-if="status === 'pending'"
      class="space-y-3"
    >
      <USkeleton
        v-for="i in 3"
        :key="i"
        class="h-24 rounded-2xl"
      />
    </div>

    <UAlert
      v-else-if="status === 'error'"
      color="error"
      variant="subtle"
      icon="i-lucide-circle-alert"
      title="방명록을 불러오지 못했어요"
      :actions="[{ label: '다시 시도', color: 'error', variant: 'outline', onClick: () => refresh() }]"
    />

    <div
      v-else-if="!entries.length"
      class="rounded-2xl border border-dashed border-default py-12 text-center text-muted"
    >
      <UIcon
        name="i-lucide-message-circle-heart"
        class="size-8 text-primary"
      />
      <p class="mt-2">
        아직 방명록이 없어요. 첫 번째 글을 남겨주세요!
      </p>
    </div>

    <ul
      v-else
      ref="listTop"
      class="scroll-mt-24 space-y-3"
    >
      <li
        v-for="entry in pagedEntries"
        :key="entry.id"
        class="flex gap-3 rounded-2xl border border-default bg-elevated/40 p-4"
      >
        <UAvatar
          :alt="entry.writer"
          size="md"
          class="bg-primary/15 text-primary"
        />
        <div class="min-w-0 flex-1">
          <div class="flex items-center justify-between gap-2">
            <p class="text-sm">
              <span class="font-semibold text-highlighted">{{ entry.writer }}</span>
              <span class="ml-2 text-xs text-dimmed">{{ formatDate(entry.createdAt) }}</span>
              <span
                v-if="entry.edited"
                class="ml-1 text-xs text-dimmed"
              >(수정됨)</span>
            </p>
            <div
              v-if="entry.mine && editingId !== entry.id"
              class="flex"
            >
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-pencil"
                aria-label="수정"
                @click="startEdit(entry)"
              />
              <UButton
                color="neutral"
                variant="ghost"
                size="xs"
                icon="i-lucide-trash-2"
                aria-label="삭제"
                @click="remove(entry)"
              />
            </div>
          </div>

          <form
            v-if="editingId === entry.id"
            class="mt-2 space-y-2"
            @submit.prevent="saveEdit(entry)"
          >
            <UTextarea
              v-model="editText"
              :rows="3"
              :maxlength="500"
              autoresize
              autofocus
              class="w-full"
            />
            <div class="flex items-center justify-between">
              <span class="text-xs text-dimmed">{{ editText.length }} / 500</span>
              <div class="flex gap-2">
                <UButton
                  color="neutral"
                  variant="ghost"
                  size="sm"
                  label="취소"
                  @click="editingId = null"
                />
                <UButton
                  type="submit"
                  size="sm"
                  label="저장"
                  :loading="saving"
                  :disabled="!editText.trim()"
                />
              </div>
            </div>
          </form>
          <p
            v-else
            class="mt-1 whitespace-pre-line break-words text-default"
          >
            {{ entry.content }}
          </p>

          <HomeReplyThread
            :replies="entry.replies"
            base-url="/guestbook"
            :parent-id="entry.id"
            nested
            editable
            @changed="refresh()"
          />
        </div>
      </li>
    </ul>

    <div
      v-if="entries.length > PAGE_SIZE"
      class="flex flex-col items-center gap-2"
    >
      <UPagination
        v-model:page="page"
        :total="entries.length"
        :items-per-page="PAGE_SIZE"
      />
      <p class="text-xs text-dimmed">
        전체 {{ entries.length }}건
      </p>
    </div>
  </div>
</template>
