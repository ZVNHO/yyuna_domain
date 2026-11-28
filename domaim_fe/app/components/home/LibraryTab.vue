<script setup lang="ts">
import type { Reply } from './ReplyThread.vue'

interface LibraryFile {
  id: number
  uploader: string
  name: string
  fileSize: number
  createdAt: string
  mine: boolean
  replies: Reply[]
}

const MAX_FILE_SIZE = 10 * 1024 * 1024

const api = useApi()
const toast = useToast()

// 자료 목록 (GET /library)
const { data: files, status, refresh } = useLazyAsyncData('library', () => api<LibraryFile[]>('/library'), {
  server: false,
  default: () => []
})

const { public: { apiBase } } = useRuntimeConfig()
const token = useAuthToken()

const file = ref<File | null>(null)

// 업로드 진행 상태: 진행률 → 서버 저장 중 → 완료/실패
interface UploadState {
  phase: 'uploading' | 'saving' | 'done' | 'error'
  name: string
  size: number
  progress: number
  message?: string
  finishedAt?: string
}
const upload = ref<UploadState | null>(null)
const uploading = computed(() => upload.value?.phase === 'uploading' || upload.value?.phase === 'saving')
const highlightIds = ref(new Set<number>()) // 방금 올린 파일을 목록에서 강조

// 페이지당 30건
const PAGE_SIZE = 30
const page = ref(1)
const pagedFiles = computed(() => files.value.slice((page.value - 1) * PAGE_SIZE, page.value * PAGE_SIZE))
// 페이지를 바꾸면 목록 맨 위로
const listTop = ref<HTMLElement | null>(null)
watch(page, () => listTop.value?.scrollIntoView({ behavior: 'smooth', block: 'start' }))
// 삭제로 마지막 페이지가 비면 앞 페이지로
watch(() => files.value.length, (length) => {
  page.value = Math.min(page.value, Math.max(1, Math.ceil(length / PAGE_SIZE)))
})

// $fetch는 업로드 진행률을 알려주지 않아서 XMLHttpRequest로 보낸다 (로그인 토큰은 직접 붙인다)
function sendWithProgress(target: File) {
  return new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.open('POST', `${apiBase}/library`)
    if (token.value) xhr.setRequestHeader('Authorization', `Bearer ${token.value}`)
    xhr.upload.onprogress = (event) => {
      if (!event.lengthComputable || !upload.value) return
      upload.value.progress = Math.round((event.loaded / event.total) * 100)
      if (upload.value.progress >= 100) upload.value.phase = 'saving' // 전송은 끝났고 서버가 저장하는 중
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) return resolve()
      let message = '파일 등록에 실패했습니다.'
      try {
        message = JSON.parse(xhr.responseText).message || message
      } catch {
        // 서버가 JSON이 아닌 응답을 준 경우 기본 문구 사용
      }
      reject(new Error(message))
    }
    xhr.onerror = () => reject(new Error('네트워크 오류로 업로드하지 못했어요.'))
    const body = new FormData()
    body.append('file', target)
    xhr.send(body)
  })
}

async function startUpload() {
  const target = file.value
  if (!target) return
  if (target.size > MAX_FILE_SIZE) {
    upload.value = { phase: 'error', name: target.name, size: target.size, progress: 0, message: '파일은 10MB까지 등록할 수 있습니다.' }
    return
  }
  upload.value = { phase: 'uploading', name: target.name, size: target.size, progress: 0 }
  const before = new Set(files.value.map(item => item.id))
  try {
    await sendWithProgress(target)
    await refresh()
    page.value = 1 // 새 파일은 맨 앞에 있으니 첫 페이지로
    upload.value = { ...upload.value!, phase: 'done', progress: 100, finishedAt: new Date().toISOString() }
    file.value = null
    highlightIds.value = new Set(files.value.filter(item => !before.has(item.id)).map(item => item.id))
  } catch (error) {
    upload.value = { ...upload.value!, phase: 'error', message: (error as Error).message }
  }
}

// 다른 파일을 고르면 이전 결과 카드는 닫는다
watch(file, (value) => {
  if (value && !uploading.value) upload.value = null
})

// 다운로드에도 로그인 토큰이 필요해서 Blob으로 받아 저장한다
async function download(target: LibraryFile) {
  try {
    const blob = await api<Blob>(`/library/${target.id}/download`, { responseType: 'blob' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = target.name
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '다운로드에 실패했습니다.'), color: 'error' })
  }
}

async function remove(target: LibraryFile) {
  try {
    await api(`/library/${target.id}`, { method: 'DELETE' })
    toast.add({ title: '삭제했어요.', color: 'success' })
    await refresh()
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '삭제에 실패했습니다.'), color: 'error' })
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>

<template>
  <div class="mx-auto max-w-2xl space-y-6">
    <div class="text-center">
      <h2 class="text-2xl font-bold text-highlighted">
        자료실
      </h2>
      <p class="mt-1 text-muted">
        레시피나 자료를 올리고 함께 나눠요 (파일당 10MB까지)
      </p>
    </div>

    <UCard class="rounded-2xl">
      <form
        class="space-y-3"
        @submit.prevent="startUpload"
      >
        <UFileUpload
          v-model="file"
          icon="i-lucide-upload"
          label="파일을 끌어다 놓거나 클릭해서 선택하세요"
          description="선택한 파일은 아래에 이름과 크기로 표시돼요"
          layout="list"
          class="w-full"
          :disabled="uploading"
        />

        <!-- 업로드 상태 -->
        <div
          v-if="upload"
          class="rounded-xl border p-3"
          :class="{
            'border-default bg-elevated/50': upload.phase === 'uploading' || upload.phase === 'saving',
            'border-success/40 bg-success/10': upload.phase === 'done',
            'border-error/40 bg-error/10': upload.phase === 'error'
          }"
          role="status"
          aria-live="polite"
        >
          <div class="flex items-center gap-3">
            <UIcon
              :name="upload.phase === 'done' ? 'i-lucide-circle-check' : upload.phase === 'error' ? 'i-lucide-circle-x' : 'i-lucide-loader-circle'"
              class="size-5 shrink-0"
              :class="{ 'animate-spin text-primary': uploading, 'text-success': upload.phase === 'done', 'text-error': upload.phase === 'error' }"
            />
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold text-highlighted">
                {{ upload.name }}
              </p>
              <p class="text-xs text-muted">
                {{ formatSize(upload.size) }} ·
                <template v-if="upload.phase === 'uploading'">
                  업로드 중 {{ upload.progress }}%
                </template>
                <template v-else-if="upload.phase === 'saving'">
                  서버에 저장하는 중…
                </template>
                <template v-else-if="upload.phase === 'done'">
                  업로드 완료 · {{ formatDate(upload.finishedAt!) }}
                </template>
                <template v-else>
                  {{ upload.message }}
                </template>
              </p>
            </div>
            <UButton
              v-if="upload.phase === 'error' && file"
              size="xs"
              color="error"
              variant="outline"
              label="다시 시도"
              @click="startUpload"
            />
            <UButton
              v-if="upload.phase === 'done' || upload.phase === 'error'"
              size="xs"
              color="neutral"
              variant="ghost"
              icon="i-lucide-x"
              aria-label="닫기"
              @click="upload = null"
            />
          </div>
          <UProgress
            v-if="upload.phase === 'uploading' || upload.phase === 'saving'"
            :model-value="upload.phase === 'saving' ? null : upload.progress"
            size="sm"
            class="mt-3"
          />
        </div>

        <div class="flex justify-end">
          <UButton
            type="submit"
            label="등록하기"
            icon="i-lucide-file-up"
            :loading="uploading"
            :disabled="!file"
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
        class="h-20 rounded-2xl"
      />
    </div>

    <UAlert
      v-else-if="status === 'error'"
      color="error"
      variant="subtle"
      icon="i-lucide-circle-alert"
      title="자료를 불러오지 못했어요"
      :actions="[{ label: '다시 시도', color: 'error', variant: 'outline', onClick: () => refresh() }]"
    />

    <div
      v-else-if="!files.length"
      class="rounded-2xl border border-dashed border-default py-12 text-center text-muted"
    >
      <UIcon
        name="i-lucide-folder-open"
        class="size-8 text-primary"
      />
      <p class="mt-2">
        아직 등록된 자료가 없어요. 첫 번째 파일을 올려주세요!
      </p>
    </div>

    <ul
      v-else
      ref="listTop"
      class="scroll-mt-24 space-y-3"
    >
      <li
        v-for="item in pagedFiles"
        :key="item.id"
        class="rounded-2xl border p-4 transition"
        :class="highlightIds.has(item.id) ? 'border-success/50 bg-success/10' : 'border-default bg-elevated/40'"
      >
        <p
          v-if="highlightIds.has(item.id)"
          class="mb-2 flex items-center gap-1 text-xs font-semibold text-success"
        >
          <UIcon
            name="i-lucide-sparkles"
            class="size-3.5"
          />
          방금 올린 파일
        </p>
        <div class="flex items-center gap-3">
          <div class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/15">
            <UIcon
              name="i-lucide-file-text"
              class="size-5 text-primary"
            />
          </div>
          <div class="min-w-0 flex-1">
            <p class="truncate font-semibold text-highlighted">
              {{ item.name }}
            </p>
            <p class="text-xs text-dimmed">
              {{ item.uploader }} · {{ formatSize(item.fileSize) }} · {{ formatDate(item.createdAt) }}
            </p>
          </div>
          <UButton
            color="neutral"
            variant="ghost"
            size="sm"
            icon="i-lucide-download"
            aria-label="다운로드"
            @click="download(item)"
          />
          <UButton
            v-if="item.mine"
            color="neutral"
            variant="ghost"
            size="sm"
            icon="i-lucide-trash-2"
            aria-label="삭제"
            @click="remove(item)"
          />
        </div>

        <HomeReplyThread
          class="mt-2 pl-13"
          :replies="item.replies"
          base-url="/library"
          :parent-id="item.id"
          @changed="refresh()"
        />
      </li>
    </ul>

    <div
      v-if="files.length > PAGE_SIZE"
      class="flex flex-col items-center gap-2"
    >
      <UPagination
        v-model:page="page"
        :total="files.length"
        :items-per-page="PAGE_SIZE"
      />
      <p class="text-xs text-dimmed">
        전체 {{ files.length }}건
      </p>
    </div>
  </div>
</template>
