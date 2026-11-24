<script setup lang="ts">
export interface Reply {
  id: number
  writer: string
  content: string
  createdAt: string
  mine: boolean
  parentReplyId?: number | null // 답글에 단 답글이면 원래 답글 id (방명록만)
  parentWriter?: string | null
  edited?: boolean
}

// baseUrl: 방명록은 /guestbook, 자료실은 /library
// nested: 답글에 다시 답글 달기, editable: 본인 답글 수정 (방명록만 켠다)
const props = withDefaults(defineProps<{
  replies: Reply[]
  baseUrl: string
  parentId: number
  nested?: boolean
  editable?: boolean
}>(), {
  nested: false,
  editable: false
})

const emit = defineEmits<{
  changed: []
}>()

const api = useApi()
const toast = useToast()

const open = ref(false) // 글에 바로 다는 답글 입력창
const replyTo = ref<Reply | null>(null) // 답글에 다는 답글 입력창 (그 답글 바로 아래에 열림)
const content = ref('')
const posting = ref(false)

const editingId = ref<number | null>(null)
const editText = ref('')
const saving = ref(false)

// 답글의 답글은 원래 답글 바로 아래에 오도록 순서를 맞춘다 (들여쓰기는 한 단계까지만)
const ordered = computed(() => {
  if (!props.nested) return props.replies.map(reply => ({ reply, depth: 0 }))
  const ids = new Set(props.replies.map(reply => reply.id))
  const children = new Map<number | null, Reply[]>()
  for (const reply of props.replies) {
    const key = reply.parentReplyId && ids.has(reply.parentReplyId) ? reply.parentReplyId : null
    children.set(key, [...(children.get(key) ?? []), reply])
  }
  const result: { reply: Reply, depth: number }[] = []
  const walk = (parent: number | null, depth: number) => {
    for (const reply of children.get(parent) ?? []) {
      result.push({ reply, depth })
      walk(reply.id, depth + 1)
    }
  }
  walk(null, 0)
  return result
})

function toggle() {
  open.value = !open.value
  replyTo.value = null
  content.value = ''
}

function startReplyTo(reply: Reply) {
  replyTo.value = replyTo.value?.id === reply.id ? null : reply
  open.value = false
  content.value = ''
}

function closeComposer() {
  open.value = false
  replyTo.value = null
  content.value = ''
}

async function submit() {
  if (!content.value.trim()) return
  posting.value = true
  try {
    await api(`${props.baseUrl}/${props.parentId}/replies`, {
      method: 'POST',
      body: { content: content.value, parentReplyId: replyTo.value?.id ?? null }
    })
    closeComposer()
    emit('changed')
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '답글 등록에 실패했습니다.'), color: 'error' })
  } finally {
    posting.value = false
  }
}

function startEdit(reply: Reply) {
  editingId.value = reply.id
  editText.value = reply.content
}

async function saveEdit(reply: Reply) {
  if (!editText.value.trim()) return
  saving.value = true
  try {
    await api(`${props.baseUrl}/replies/${reply.id}`, { method: 'PUT', body: { content: editText.value } })
    editingId.value = null
    emit('changed')
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '답글 수정에 실패했습니다.'), color: 'error' })
  } finally {
    saving.value = false
  }
}

async function remove(reply: Reply) {
  try {
    await api(`${props.baseUrl}/replies/${reply.id}`, { method: 'DELETE' })
    emit('changed')
  } catch (error) {
    toast.add({ title: apiErrorMessage(error, '답글 삭제에 실패했습니다.'), color: 'error' })
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' })
}
</script>

<template>
  <div>
    <UButton
      class="mt-1 -ml-2"
      color="neutral"
      variant="ghost"
      size="xs"
      icon="i-lucide-reply"
      :label="replies.length ? `답글 ${replies.length}` : '답글'"
      @click="toggle"
    />

    <ul v-if="replies.length" class="mt-2 space-y-2 border-l-2 border-default pl-3">
      <li v-for="{ reply, depth } in ordered" :key="reply.id" :class="{ 'ml-6': depth > 0 }">
        <div class="flex items-center justify-between gap-2">
          <p class="text-sm">
            <UIcon v-if="depth > 0" name="i-lucide-corner-down-right" class="mr-1 size-3.5 align-middle text-dimmed" />
            <span class="font-semibold text-highlighted">{{ reply.writer }}</span>
            <span class="ml-2 text-xs text-dimmed">{{ formatDate(reply.createdAt) }}</span>
            <span v-if="reply.edited" class="ml-1 text-xs text-dimmed">(수정됨)</span>
          </p>
          <div v-if="reply.mine && editingId !== reply.id" class="flex">
            <UButton
              v-if="editable"
              color="neutral"
              variant="ghost"
              size="xs"
              icon="i-lucide-pencil"
              aria-label="답글 수정"
              @click="startEdit(reply)"
            />
            <UButton
              color="neutral"
              variant="ghost"
              size="xs"
              icon="i-lucide-trash-2"
              aria-label="답글 삭제"
              @click="remove(reply)"
            />
          </div>
        </div>

        <!-- 수정 중 -->
        <form v-if="editingId === reply.id" class="mt-1 space-y-2" @submit.prevent="saveEdit(reply)">
          <UTextarea
            v-model="editText"
            :rows="2"
            :maxlength="500"
            autoresize
            autofocus
            class="w-full"
          />
          <div class="flex justify-end gap-2">
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
        </form>

        <template v-else>
          <p class="whitespace-pre-line break-words text-sm text-default">
            <span v-if="reply.parentWriter" class="mr-1 font-semibold text-primary">@{{ reply.parentWriter }}</span>{{ reply.content }}
          </p>
          <UButton
            v-if="nested"
            class="-ml-2"
            color="neutral"
            variant="link"
            size="xs"
            label="답글 달기"
            @click="startReplyTo(reply)"
          />
        </template>

        <!-- 이 답글에 다는 답글 입력창 -->
        <form v-if="replyTo?.id === reply.id" class="mt-2 space-y-2" @submit.prevent="submit">
          <UTextarea
            v-model="content"
            :rows="2"
            :maxlength="500"
            autoresize
            autofocus
            :placeholder="`@${reply.writer}님에게 답글`"
            class="w-full"
          />
          <div class="flex justify-end gap-2">
            <UButton
              color="neutral"
              variant="ghost"
              size="sm"
              label="취소"
              @click="closeComposer"
            />
            <UButton
              type="submit"
              size="sm"
              label="등록"
              :loading="posting"
              :disabled="!content.trim()"
            />
          </div>
        </form>
      </li>
    </ul>

    <!-- 글에 바로 다는 답글 입력창 -->
    <form
      v-if="open"
      class="mt-3 space-y-2"
      @submit.prevent="submit"
    >
      <UTextarea
        v-model="content"
        :rows="2"
        :maxlength="500"
        autoresize
        placeholder="답글을 입력해주세요"
        class="w-full"
      />
      <div class="flex justify-end gap-2">
        <UButton
          color="neutral"
          variant="ghost"
          size="sm"
          label="취소"
          @click="toggle"
        />
        <UButton
          type="submit"
          size="sm"
          label="등록"
          :loading="posting"
          :disabled="!content.trim()"
        />
      </div>
    </form>
  </div>
</template>
