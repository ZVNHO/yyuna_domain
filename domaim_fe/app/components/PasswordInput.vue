<script setup lang="ts">
// 보기/숨기기 버튼과 Caps Lock 안내가 있는 비밀번호 입력칸
defineOptions({ inheritAttrs: false }) // autofocus 등은 바깥 div가 아니라 입력칸으로 넘긴다

withDefaults(defineProps<{
  placeholder?: string
  autocomplete?: string
  icon?: string
}>(), {
  placeholder: '비밀번호',
  autocomplete: 'current-password',
  icon: 'i-lucide-lock'
})

const model = defineModel<string>({ default: '' })
const show = ref(false)
const capsLock = ref(false)

function checkCapsLock(event: KeyboardEvent) {
  capsLock.value = event.getModifierState?.('CapsLock') ?? false
}
</script>

<template>
  <div>
    <UInput
      v-bind="$attrs"
      v-model="model"
      :type="show ? 'text' : 'password'"
      :icon="icon"
      :placeholder="placeholder"
      :autocomplete="autocomplete"
      size="lg"
      class="w-full"
      :ui="{ trailing: 'pe-1' }"
      @keydown="checkCapsLock"
      @keyup="checkCapsLock"
      @blur="capsLock = false"
    >
      <template #trailing>
        <UButton
          color="neutral"
          variant="link"
          size="sm"
          :icon="show ? 'i-lucide-eye-off' : 'i-lucide-eye'"
          :aria-label="show ? '비밀번호 숨기기' : '비밀번호 보기'"
          @click="show = !show"
        />
      </template>
    </UInput>
    <p v-if="capsLock" class="mt-1.5 flex items-center gap-1 text-xs text-warning">
      <UIcon name="i-lucide-arrow-big-up" class="size-3.5" />
      Caps Lock이 켜져 있어요
    </p>
  </div>
</template>
