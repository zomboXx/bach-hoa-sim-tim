<script setup lang="ts">
import { nextTick, onUnmounted, ref } from "vue";
import type { IScannerControls } from "@zxing/browser";

defineProps<{ disabled?: boolean; feedback?: string }>();
const emit = defineEmits<{ scanned: [value: string] }>();
const open = ref(false);
const video = ref<HTMLVideoElement>();
const dialog = ref<HTMLDialogElement>();
const message = ref("");
const lastCode = ref("");
const armed = ref(true);
const starting = ref(false);
let controls: IScannerControls | undefined;
let revision = 0;

function stop() {
  revision++;
  controls?.stop();
  controls = undefined;
  const stream = video.value?.srcObject;
  if (stream instanceof MediaStream) stream.getTracks().forEach((track) => track.stop());
  if (video.value) video.value.srcObject = null;
  dialog.value?.close();
  open.value = false;
  starting.value = false;
}
async function start() {
  open.value = true;
  starting.value = true;
  armed.value = true;
  lastCode.value = "";
  message.value = "";
  const captured = ++revision;
  await nextTick();
  dialog.value?.showModal();
  if (!window.isSecureContext || !navigator.mediaDevices?.getUserMedia) {
    message.value = "Camera cần HTTPS (hoặc localhost). Hãy mở đường dẫn HTTPS trên điện thoại.";
    starting.value = false;
    return;
  }
  try {
    const { BrowserMultiFormatReader } = await import("@zxing/browser");
    if (captured !== revision) return;
    const reader = new BrowserMultiFormatReader();
    const scanner = await reader.decodeFromConstraints(
      { video: { facingMode: { ideal: "environment" }, width: { ideal: 1280 } }, audio: false },
      video.value!,
      (result) => {
        if (captured !== revision || !armed.value || !result) return;
        const value = result.getText().trim();
        if (!value) return;
        armed.value = false;
        lastCode.value = value;
        emit("scanned", value);
      },
    );
    if (captured !== revision) scanner.stop();
    else controls = scanner;
  } catch (e) {
    if (captured !== revision) return;
    const name = (e as Error).name;
    message.value =
      name === "NotAllowedError"
        ? "Bạn chưa cho phép dùng camera. Hãy cấp quyền camera trong trình duyệt rồi thử lại."
        : name === "NotFoundError"
          ? "Thiết bị không có camera phù hợp. Bạn vẫn có thể nhập mã bằng tay."
          : "Không mở được camera. Hãy đóng ứng dụng đang dùng camera và thử lại.";
  } finally {
    if (captured === revision) starting.value = false;
  }
}
function visibilityChanged() {
  if (document.hidden) stop();
}
document.addEventListener("visibilitychange", visibilityChanged);
onUnmounted(() => {
  stop();
  document.removeEventListener("visibilitychange", visibilityChanged);
});
</script>

<template>
  <button class="scanner-launch" :disabled="disabled" @click="start">▣ Quét mã bằng camera</button>
  <dialog
    v-if="open"
    ref="dialog"
    class="scanner-panel"
    aria-label="Quét mã sản phẩm"
    @cancel.prevent="stop"
  >
    <header>
      <h2>Quét mã sản phẩm</h2>
      <button aria-label="Đóng camera" @click="stop">✕</button>
    </header>
    <video ref="video" autoplay muted playsinline aria-label="Hình ảnh camera quét mã" />
    <p v-if="starting" role="status">Đang mở camera sau…</p>
    <p v-if="message" class="error" role="alert">{{ message }}</p>
    <p v-else-if="lastCode" role="status">
      Đã đọc mã: <strong>{{ lastCode }}</strong>
    </p>
    <p v-else>Đưa mã vạch vào giữa khung hình, giữ máy ổn định và đủ sáng.</p>
    <p v-if="lastCode && feedback" role="status">{{ feedback }}</p>
    <button
      v-if="lastCode"
      class="primary"
      @click="
        armed = true;
        lastCode = '';
      "
    >
      Quét tiếp
    </button>
    <button v-if="message && !starting" @click="stop">Đóng và nhập mã bằng tay</button>
    <small>Camera chỉ đọc mã trên thiết bị. Hình ảnh không gửi lên máy chủ.</small>
  </dialog>
</template>

<style scoped>
.scanner-launch {
  margin: 10px 0;
  padding: 12px 16px;
}
.scanner-panel::backdrop {
  background: #181325bc;
}
.scanner-panel {
  width: min(calc(100% - 32px), 540px);
  max-height: calc(100dvh - 32px);
  overflow: auto;
  margin: auto;
  border: 0;
  background: white;
  color: #262036;
  border-radius: 18px;
  padding: 20px;
  box-shadow: 0 16px 60px #0005;
  display: grid;
  gap: 12px;
}
.scanner-panel header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.scanner-panel h2 {
  margin: 0;
  font-size: 20px;
}
.scanner-panel video {
  width: 100%;
  max-height: 55vh;
  background: #15121e;
  border-radius: 12px;
}
.scanner-panel small {
  color: #655d73;
}
</style>
