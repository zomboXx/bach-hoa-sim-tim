<script setup lang="ts">
import { onMounted, onUnmounted, ref } from "vue";
import { apiMode, request } from "../server";
defineProps<{ employeeName: string }>();
const emit = defineEmits<{ exit: [] }>();
const upcoming = "Nội dung này đang được cập nhật, vui lòng thử lại sau";
const chapters = [
  {
    id: 1,
    icon: "◌",
    name: "Lời chào đầu tiên",
    topic: "Giao tiếp với khách",
    text: "Chào đón, lắng nghe nhu cầu và hướng dẫn khách trong cửa hàng.",
  },
  {
    id: 2,
    icon: "＋",
    name: "Đơn hàng đầu tiên",
    topic: "Bán hàng & hóa đơn",
    text: "Tư vấn sản phẩm, kiểm tra giỏ hàng và hoàn tất một lượt bán.",
  },
  {
    id: 3,
    icon: "▦",
    name: "Chăm chút từng kệ hàng",
    topic: "Trưng bày & hàng hóa",
    text: "Sắp xếp sản phẩm, kiểm tra nhãn giá và bổ sung hàng lên kệ.",
  },
  {
    id: 4,
    icon: "↓",
    name: "Chuyến hàng mới",
    topic: "Nhận hàng & nhập kho",
    text: "Đối chiếu số lượng, kiểm tra chất lượng và báo cáo chênh lệch.",
  },
  {
    id: 5,
    icon: "◷",
    name: "Đừng bỏ quên hạn dùng",
    topic: "Hàng cận hạn & bất thường",
    text: "Nhận diện, tách riêng và xử lý hàng hóa theo quy trình.",
  },
  {
    id: 6,
    icon: "♡",
    name: "Một tình huống khó",
    topic: "Chăm sóc khách hàng",
    text: "Bình tĩnh tiếp nhận phản hồi và nhờ người phụ trách hỗ trợ.",
  },
];
const stations = [
  ["mentor", "Gặp chị Linh", "Làm quen mentor và cách điều khiển"],
  ["checkout", "Quầy thu ngân", "Nơi đón khách và hoàn tất đơn hàng"],
  ["shelves", "Kệ hàng tiêu dùng", "Hiểu vị trí để hướng dẫn khách"],
  ["chilled", "Khu hàng lạnh", "Lưu ý bảo quản và hàng bất thường"],
  ["receiving", "Khu nhận hàng", "Điểm bắt đầu hành trình hàng hóa"],
  ["carts", "Giỏ hàng & lối vào", "Sẵn sàng đón khách vào cửa hàng"],
];
const modal = ref<HTMLDialogElement>();
const frame = ref<HTMLIFrameElement>();
const playing = ref(false);
const busy = ref(false);
const ready = ref(false);
const error = ref("");
const sessionId = ref("");
const visited = ref<string[]>([]);
const completed = ref(false);
const previous = ref(false);
let sequence: Promise<void> = Promise.resolve();
let pendingEvents: {
  type: "visit" | "orientation" | "complete";
  station?: string;
  answers?: { welcome: string; sensitive: string; delivery: string };
}[] = [];
const orientationDone = ref(false);
let loadTimer: ReturnType<typeof setTimeout>;
let disposed = false;
async function launch() {
  if (busy.value) return;
  busy.value = true;
  error.value = "";
  try {
    const check = await fetch("/training/index.html", { cache: "no-store" });
    if (!check.ok || !(await check.text()).includes("Engine"))
      throw Error(
        "Chưa có bản export Godot. Chạy scripts/export-training.ps1 rồi build lại PWA.",
      );
    sessionId.value = apiMode
      ? (
          await request<{ id: string }>("/training/sessions", "POST", {
            chapter: 0,
          })
        ).id
      : "local-demo";
    visited.value = [];
    completed.value = false;
    orientationDone.value = false;
    ready.value = false;
    pendingEvents = [];
    playing.value = true;
    loadTimer = setTimeout(() => {
      if (!ready.value)
        error.value =
          "Godot đang tải lâu hơn dự kiến. Kiểm tra WebGL 2 và mạng, hoặc trở lại để thử lại.";
    }, 45000);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    busy.value = false;
  }
}
async function flush() {
  while (pendingEvents.length && !disposed) {
    const event = pendingEvents[0];
    try {
      if (apiMode)
        await request(
          `/training/sessions/${sessionId.value}/${event.type === "visit" ? "events" : event.type}`,
          "POST",
          event.type === "visit"
            ? { station: event.station }
            : event.type === "orientation"
              ? event.answers
              : undefined,
        );
      pendingEvents.shift();
      if (event.type === "orientation") orientationDone.value = true;
      if (event.type === "complete") {
        completed.value = true;
        previous.value = true;
      }
      error.value = "";
    } catch (e) {
      error.value =
        (e as Error).message +
        " Tiến độ chưa lưu sẽ được thử lại khi em nhấn Lưu lại.";
      return;
    }
  }
}
function retry() {
  sequence = sequence.then(flush);
}
function onMessage(event: MessageEvent) {
  if (
    !playing.value ||
    event.origin !== location.origin ||
    event.source !== frame.value?.contentWindow ||
    event.data?.source !== "simtim-godot"
  )
    return;
  const payload = event.data;
  if (payload.type === "ready") {
    ready.value = true;
    error.value = "";
    clearTimeout(loadTimer);
  }
  if (
    payload.type === "visit" &&
    stations.some((s) => s[0] === payload.station) &&
    !visited.value.includes(payload.station)
  ) {
    visited.value.push(payload.station);
    pendingEvents.push({ type: "visit", station: payload.station });
    retry();
  }
  if (
    payload.type === "orientation" &&
    visited.value.length === 6 &&
    !orientationDone.value &&
    !pendingEvents.some((e) => e.type === "orientation") &&
    ["welcome", "sensitive", "delivery"].every(
      (k) => typeof payload.answers?.[k] === "string",
    )
  ) {
    pendingEvents.push({
      type: "orientation",
      answers: {
        welcome: payload.answers.welcome,
        sensitive: payload.answers.sensitive,
        delivery: payload.answers.delivery,
      },
    });
    retry();
  }
  if (
    payload.type === "complete" &&
    visited.value.length === 6 &&
    (orientationDone.value ||
      pendingEvents.some((e) => e.type === "orientation")) &&
    !completed.value &&
    !pendingEvents.some((e) => e.type === "complete")
  ) {
    pendingEvents.push({ type: "complete" });
    retry();
  }
}
async function leave() {
  await sequence;
  if (
    pendingEvents.length &&
    !confirm("Tiến độ chưa lưu hết. Em vẫn muốn rời chương?")
  )
    return;
  playing.value = false;
  clearTimeout(loadTimer);
  error.value = "";
}
function fullscreen() {
  frame.value?.requestFullscreen?.().catch(() => {
    error.value =
      "Trình duyệt không hỗ trợ toàn màn hình; thử xoay ngang điện thoại.";
  });
}
onMounted(async () => {
  window.addEventListener("message", onMessage);
  if (apiMode)
    try {
      previous.value = (
        await request<{ status: string }[]>("/training/sessions")
      ).some((s) => s.status === "COMPLETED");
    } catch {
      /* Launch reports authentication/network errors. */
    }
});
onUnmounted(() => {
  disposed = true;
  clearTimeout(loadTimer);
  window.removeEventListener("message", onMessage);
});
</script>

<template>
  <div class="academy">
    <header class="academy-header">
      <button class="academy-back" @click="playing ? leave() : emit('exit')">
        ← {{ playing ? "Chọn chương" : "Về cửa hàng" }}
      </button>
      <a
        class="academy-brand"
        href="#"
        @click.prevent="playing ? leave() : undefined"
        ><img src="/icon.svg" alt="" /> sim tím <span>ACADEMY</span></a
      >
      <span class="academy-employee"
        >{{ employeeName }}
        <span class="academy-avatar">{{ employeeName[0] }}</span></span
      >
    </header>
    <main v-if="!playing" class="academy-lobby">
      <section class="academy-intro">
        <div>
          <p class="academy-eyebrow">HỌC VIỆC QUA TRẢI NGHIỆM</p>
          <h1>Mỗi ngày, tự tin hơn<br />một chút.</h1>
          <p>
            Vào vai nhân viên mới, gặp gỡ khách hàng và học cách<br
              class="desktop-break"
            />
            chăm sóc cửa hàng cùng chị Linh.
          </p>
        </div>
        <div class="academy-mentor">
          <img src="/mentor.png" alt="Chị Linh, mentor hướng dẫn" />
          <div>
            <b>“Cứ thử nhé, chị ở đây!”</b>
            <p>
              Không ảnh hưởng đơn hàng hay tồn kho thật.<br />Một không gian an
              toàn để làm quen.
            </p>
          </div>
        </div>
      </section>
      <p v-if="error" class="academy-error" role="alert">{{ error }}</p>
      <section class="chapter-zero">
        <div class="chapter-zero-copy">
          <span class="academy-tag"
            >CHAPTER 0 <i></i>
            {{ previous ? "ĐÃ HOÀN THÀNH" : "SẴN SÀNG KHÁM PHÁ" }}</span
          >
          <h2>Ngày đầu tiên<br />tại Sim Tím.</h2>
          <p>
            Mặc đồng phục, bước vào cửa hàng 2D và đi một vòng cùng mentor. Làm
            quen 6 khu vực trước khi bắt đầu ca làm việc đầu tiên.
          </p>
          <div class="chapter-meta">
            <span>◷ 5–8 phút</span><span>⌘ Di chuyển & tương tác</span>
          </div>
          <button class="academy-launch" :disabled="busy" @click="launch">
            {{
              busy
                ? "Đang chuẩn bị…"
                : previous
                  ? "Khám phá lại cửa hàng"
                  : "Bắt đầu Chapter 0"
            }}
            <span>↗</span></button
          ><small>Godot 4 · Map cửa hàng · Không phát sinh giao dịch</small>
        </div>
        <div class="chapter-zero-map">
          <img
            src="/training-map.png"
            alt="Bản đồ mô phỏng cửa hàng Sim Tím với quầy thu ngân và các dãy kệ hàng"
          />
          <div class="map-caption">
            <span class="map-dot"></span> CỬA HÀNG MÔ PHỎNG <b>01</b>
          </div>
          <div class="map-sticker">
            ✦<br /><span>Hẹn gặp em<br />ở cửa hàng!</span>
          </div>
        </div>
      </section>
      <div class="chapters-heading">
        <div>
          <p class="academy-eyebrow">HÀNH TRÌNH PHÍA TRƯỚC</p>
          <h2>Từng kỹ năng. Từng chương.</h2>
        </div>
        <span>01 chương mở / 07 chương</span>
      </div>
      <section class="chapters-grid">
        <button
          v-for="chapter in chapters"
          :key="chapter.id"
          class="chapter-card"
          @click="modal?.showModal()"
        >
          <div class="chapter-card-top">
            <span class="chapter-icon">{{ chapter.icon }}</span
            ><span>CHAPTER {{ String(chapter.id).padStart(2, "0") }}</span
            ><span>↗</span>
          </div>
          <p class="chapter-topic">{{ chapter.topic }}</p>
          <h3>{{ chapter.name }}</h3>
          <p>{{ chapter.text }}</p>
          <span class="chapter-status"><i></i> Đang phát triển</span>
        </button>
      </section>
      <footer class="academy-footer">
        Thực hành trong mô phỏng. Tự tin ngoài cửa hàng.<span
          >SIM TÍM · LEARN BY DOING</span
        >
      </footer>
    </main>
    <main v-else class="academy-play">
      <div class="game-heading">
        <div>
          <p class="academy-eyebrow">CHAPTER 0 · LÀM QUEN CỬA HÀNG</p>
          <h1>Ngày đầu tiên tại Sim Tím</h1>
        </div>
        <button class="academy-back" @click="fullscreen">
          ⛶ Toàn màn hình
        </button>
      </div>
      <div v-if="error" class="academy-error" role="alert">
        {{ error }} <button @click="retry">Lưu lại</button>
      </div>
      <div class="game-layout">
        <div class="game-stage">
          <iframe
            ref="frame"
            src="/training/index.html"
            title="Chapter 0 · Trò chơi Godot 2D"
            allow="fullscreen; gamepad; autoplay"
            allowfullscreen
          ></iframe>
          <div v-if="!ready" class="game-loading">
            Đang mở cửa hàng 2D…<small
              >Lần đầu cần tải bản Godot Web. Vui lòng chờ.</small
            >
          </div>
        </div>
        <aside class="tour-guide">
          <span class="academy-tag">NHIỆM VỤ LÀM QUEN</span>
          <h2>Một vòng cửa hàng</h2>
          <p>
            Đi tới điểm sáng, nhấn <kbd>E</kbd> và đọc lời hướng dẫn từ mentor.
          </p>
          <ol>
            <li
              v-for="(s, i) in stations"
              :key="s[0]"
              :class="{ visited: visited.includes(s[0]) }"
            >
              <span>{{ visited.includes(s[0]) ? "✓" : i + 1 }}</span>
              <div>
                <b>{{ s[1] }}</b
                ><small>{{ s[2] }}</small>
              </div>
            </li>
          </ol>
          <div class="tour-progress">
            <span :style="{ width: `${(visited.length / 6) * 100}%` }"></span>
          </div>
          <b>{{ visited.length }} / 6 điểm đã khám phá</b>
          <p>
            {{
              orientationDone
                ? "✓ Đã xác nhận cùng mentor"
                : "Bước cuối: trả lời 3 câu làm quen cùng mentor trong game."
            }}
          </p>
          <p class="game-saved" v-if="completed" role="status">
            ✓ Hoàn thành Chapter 0.<br />{{
              apiMode
                ? "Kết quả đã lưu trên máy chủ."
                : "Kết quả chỉ thuộc phiên demo này."
            }}
          </p>
        </aside>
      </div>
      <div class="game-help">
        <span
          ><kbd>W A S D</kbd> / <kbd>↑ ← ↓ →</kbd> Di chuyển &nbsp;
          <kbd>E</kbd> Tương tác</span
        ><span
          >Điện thoại: dùng nút trong game · Xoay ngang để nhìn rộng hơn</span
        >
      </div>
    </main>
    <dialog ref="modal" class="chapter-modal">
      <span>✦</span>
      <h2>Hẹn em ở chương tiếp theo</h2>
      <p>{{ upcoming }}</p>
      <button class="academy-launch" @click="modal?.close()">Đã hiểu</button>
    </dialog>
  </div>
</template>

<style scoped>
.academy {
  min-height: 100vh;
  background: #f7f5fa;
  color: #30223e;
  font-family: inherit;
}
.academy button {
  font: inherit;
  cursor: pointer;
}
.academy-header {
  height: 86px;
  background: #fff;
  border-bottom: 1px solid #e9e3ef;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4.5vw;
  gap: 20px;
}
.academy-back {
  border: 1px solid #e4ddec;
  border-radius: 10px;
  padding: 11px 16px;
  color: #51405f;
  background: #fff;
}
.academy-brand {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 24px;
  font-weight: 800;
  color: #684299;
  text-decoration: none;
}
.academy-brand img {
  width: 32px;
}
.academy-brand > span {
  border-left: 1px solid #e5deed;
  padding-left: 14px;
  margin-left: 6px;
  font-size: 10px;
  letter-spacing: 2px;
  color: #8c7b9d;
}
.academy-employee {
  display: flex;
  align-items: center;
  gap: 13px;
  font-size: 13px;
}
.academy-avatar {
  display: grid;
  place-items: center;
  background: #eee5fb;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  font-weight: 700;
}
.academy-lobby {
  max-width: 1300px;
  margin: auto;
  padding: 45px 36px 22px;
}
.academy-intro {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 32px;
}
.academy-eyebrow {
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 2px;
  color: #8861ae;
  margin: 0 0 13px;
}
.academy-intro h1 {
  font-size: 46px;
  line-height: 1.14;
  letter-spacing: -1.7px;
  margin: 0 0 18px;
}
.academy-intro p:not(.academy-eyebrow) {
  color: #82768d;
  font-size: 14px;
  line-height: 1.8;
}
.academy-mentor {
  display: flex;
  align-items: center;
  gap: 18px;
  max-width: 470px;
  padding: 18px 22px;
  background: #efebf6;
  border-radius: 20px;
}
.academy-mentor img {
  width: 86px;
  height: 98px;
  object-fit: cover;
  object-position: 50% 22%;
  border-radius: 50% 50% 42% 42%;
  background: #decdf2;
}
.academy-mentor b {
  font-size: 17px;
}
.academy-mentor p {
  margin-bottom: 0;
  font-size: 12px !important;
}
.chapter-zero {
  display: grid;
  grid-template-columns: 42% 58%;
  overflow: hidden;
  border-radius: 22px;
  background: #302043;
  box-shadow: 0 12px 38px #32204615;
  min-height: 370px;
}
.chapter-zero-copy {
  padding: 36px 38px;
  color: #fff;
  z-index: 1;
}
.academy-tag {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 1.2px;
  color: #cdb4ef;
}
.academy-tag i,
.map-dot {
  width: 6px;
  height: 6px;
  background: #91d9b0;
  border-radius: 50%;
  display: inline-block;
}
.chapter-zero h2 {
  font-size: 37px;
  line-height: 1.15;
  letter-spacing: -0.7px;
  margin: 19px 0 14px;
}
.chapter-zero-copy > p {
  font-size: 13px;
  line-height: 1.8;
  color: #c6bad4;
  max-width: 360px;
}
.chapter-meta {
  display: flex;
  gap: 20px;
  font-size: 11px;
  color: #c5b6d4;
  margin: 22px 0;
}
.academy-launch {
  background: #c1a0f0;
  border: 0;
  border-radius: 10px;
  padding: 14px 18px;
  color: #322143;
  font-weight: 800 !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 36px;
}
.academy-launch:disabled {
  opacity: 0.6;
}
.chapter-zero-copy > small {
  display: block;
  font-size: 9px;
  letter-spacing: 0.2px;
  color: #aa97be;
  margin-top: 13px;
}
.chapter-zero-map {
  position: relative;
  background: #211a28;
  overflow: hidden;
  min-height: 310px;
}
.chapter-zero-map > img {
  width: 130%;
  height: 100%;
  object-fit: cover;
  object-position: 65%;
  image-rendering: pixelated;
  opacity: 0.95;
  transform: scale(1.06);
}
.map-caption {
  position: absolute;
  top: 22px;
  left: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #30243adb;
  padding: 11px 15px;
  border: 1px solid #9b83b340;
  border-radius: 9px;
  font-size: 9px;
  letter-spacing: 1.5px;
  color: #eee4fa;
}
.map-caption b {
  margin-left: auto;
}
.map-sticker {
  position: absolute;
  bottom: 28px;
  right: 30px;
  background: #f4db87;
  border-radius: 50%;
  height: 110px;
  width: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transform: rotate(9deg);
  font-size: 27px;
  line-height: 1;
}
.map-sticker span {
  font-size: 12px;
  font-weight: 700;
  text-align: center;
  line-height: 1.3;
  margin-top: 5px;
}
.chapters-heading {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 42px 0 21px;
}
.chapters-heading h2 {
  font-size: 24px;
  margin: 0;
  letter-spacing: -0.6px;
}
.chapters-heading .academy-eyebrow {
  margin-bottom: 8px;
}
.chapters-heading > span {
  font-size: 11px;
  color: #93839e;
}
.chapters-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}
.chapter-card {
  text-align: left;
  background: #fff;
  border: 1px solid #e9e2ef;
  border-radius: 15px;
  padding: 23px;
  color: inherit;
  transition:
    transform 0.18s,
    border-color 0.18s;
}
.chapter-card:hover {
  transform: translateY(-4px);
  border-color: #b594da;
}
.chapter-card:focus-visible,
.academy button:focus-visible {
  outline: 3px solid #a17cc6;
  outline-offset: 3px;
}
.chapter-card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 9px;
  letter-spacing: 1px;
  color: #9a8ca6;
}
.chapter-card-top > span:last-child {
  margin-left: auto;
  font-size: 18px;
}
.chapter-icon {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  background: #f1ebf9;
  color: #9d78c2;
  font-size: 23px;
  letter-spacing: 0;
  border-radius: 10px;
}
.chapter-topic {
  font-size: 10px !important;
  color: #9677b3 !important;
  margin: 23px 0 8px !important;
}
.chapter-card h3 {
  font-size: 18px;
  margin: 0 0 10px;
}
.chapter-card p {
  font-size: 12px;
  line-height: 1.7;
  color: #918397;
}
.chapter-status {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 10px;
  color: #a295ad;
  margin-top: 22px;
  padding-top: 15px;
  border-top: 1px solid #f2edf6;
}
.chapter-status i {
  width: 5px;
  height: 5px;
  background: #b9a8ca;
  border-radius: 50%;
}
.academy-footer {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #a798b2;
  margin: 32px 0 0;
  padding: 20px 0;
  border-top: 1px solid #e8e1ed;
}
.academy-footer span {
  font-size: 9px;
  letter-spacing: 1.5px;
}
.chapter-modal {
  border: 0;
  padding: 34px;
  border-radius: 18px;
  max-width: min(440px, 90vw);
  color: #392546;
  text-align: center;
}
.chapter-modal::backdrop {
  background: #23172f88;
  backdrop-filter: blur(4px);
}
.chapter-modal > span {
  font-size: 38px;
  color: #9463bb;
}
.chapter-modal h2 {
  font-size: 21px;
}
.chapter-modal p {
  line-height: 1.7;
  color: #897693;
}
.chapter-modal button {
  width: 100%;
  justify-content: center;
  margin-top: 24px;
}
.academy-error {
  padding: 14px 18px;
  background: #fff1e8;
  border: 1px solid #efc9ac;
  color: #975a37;
  border-radius: 10px;
  font-size: 13px;
  margin-bottom: 18px;
}
.academy-error button {
  background: #fff;
  border: 1px solid #dfbba5;
  border-radius: 6px;
  padding: 7px 12px;
}
.academy-play {
  max-width: 1580px;
  padding: 28px 30px;
  margin: auto;
}
.game-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 20px;
}
.game-heading h1 {
  font-size: 27px;
  margin: 0;
}
.game-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 270px;
  gap: 20px;
}
.game-stage {
  background: #211929;
  border-radius: 15px;
  overflow: hidden;
  position: relative;
  align-self: start;
}
.game-stage iframe {
  display: block;
  width: 100%;
  aspect-ratio: 1120/620;
  border: 0;
}
.game-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 14px;
  background: #2e223c;
  color: white;
  pointer-events: none;
}
.game-loading small {
  font-size: 11px;
  color: #bda4d1;
}
.tour-guide {
  background: white;
  border: 1px solid #e9e0f1;
  border-radius: 15px;
  padding: 22px;
}
.tour-guide .academy-tag {
  color: #9c71bd;
}
.tour-guide h2 {
  font-size: 20px;
  margin: 14px 0 8px;
}
.tour-guide > p {
  font-size: 12px;
  line-height: 1.6;
  color: #93829f;
}
.tour-guide ol {
  padding: 0;
  list-style: none;
  margin: 24px 0;
}
.tour-guide li {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}
.tour-guide li > span {
  flex-shrink: 0;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f1edf5;
  color: #a695b4;
  font-size: 11px;
}
.tour-guide li.visited > span {
  background: #e0f4e8;
  color: #438c64;
}
.tour-guide li b {
  font-size: 12px;
}
.tour-guide li small {
  font-size: 10px;
  color: #a28eaf;
  display: block;
  line-height: 1.5;
  margin-top: 4px;
}
.tour-progress {
  height: 5px;
  border-radius: 9px;
  background: #f0e8f7;
  margin: 20px 0 10px;
  overflow: hidden;
}
.tour-progress span {
  display: block;
  height: 100%;
  background: #a780d0;
  transition: width 0.3s;
}
.tour-guide > b {
  font-size: 10px;
  color: #9c83ae;
}
.game-saved {
  color: #398259 !important;
  background: #eef9f2;
  padding: 12px;
  border-radius: 8px;
}
.game-help {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  color: #96849f;
  font-size: 10px;
  margin-top: 16px;
}
.academy kbd {
  background: #ece5f3;
  border: 1px solid #ded0ea;
  border-radius: 4px;
  padding: 2px 5px;
  font-family: inherit;
}
@media (min-width: 1600px) {
  .game-stage iframe {
    aspect-ratio: 1120/700;
  }
}
@media (max-width: 1050px) {
  .academy-intro h1 {
    font-size: 38px;
  }
  .academy-mentor {
    max-width: 350px;
    padding: 16px;
  }
  .academy-mentor img {
    width: 65px;
    height: 80px;
  }
  .chapter-zero-copy {
    padding: 28px;
  }
  .chapter-zero h2 {
    font-size: 32px;
  }
  .game-layout {
    grid-template-columns: 1fr;
  }
  .tour-guide ol {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 10px;
  }
  .tour-guide li {
    margin-bottom: 5px;
  }
  .tour-guide {
    padding: 20px;
  }
  .tour-guide > p {
    margin: 8px 0;
  }
  .tour-progress {
    margin-top: 0;
  }
  .game-help {
    flex-wrap: wrap;
  }
}
@media (max-width: 700px) {
  .academy-header {
    height: 70px;
    padding: 0 18px;
  }
  .academy-brand {
    font-size: 20px;
  }
  .academy-brand > span,
  .academy-employee {
    display: none;
  }
  .academy-back {
    font-size: 11px !important;
    padding: 9px 12px;
  }
  .academy-lobby {
    padding: 28px 18px 10px;
  }
  .academy-intro {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
  .academy-intro h1 {
    font-size: 37px;
  }
  .academy-mentor {
    max-width: none;
    width: 100%;
    box-sizing: border-box;
  }
  .academy-mentor img {
    width: 55px;
    height: 65px;
  }
  .academy-mentor b {
    font-size: 14px;
  }
  .academy-mentor p {
    font-size: 10px !important;
  }
  .chapter-zero {
    grid-template-columns: 1fr;
  }
  .chapter-zero-copy {
    padding: 26px;
  }
  .chapter-zero h2 {
    font-size: 33px;
  }
  .chapter-zero-copy > p {
    max-width: none;
  }
  .chapter-zero-map {
    min-height: 230px;
    height: 230px;
    grid-row: 1;
  }
  .chapter-zero-map > img {
    width: 100%;
    object-fit: cover;
    object-position: 50%;
  }
  .map-sticker {
    height: 82px;
    width: 82px;
    right: 18px;
    bottom: 14px;
  }
  .map-sticker span {
    font-size: 10px;
  }
  .chapter-zero-copy .academy-launch {
    width: 100%;
  }
  .chapters-grid {
    grid-template-columns: 1fr;
  }
  .chapters-heading {
    align-items: flex-start;
    gap: 10px;
  }
  .chapters-heading h2 {
    font-size: 21px;
  }
  .chapters-heading > span {
    font-size: 9px;
    max-width: 70px;
    text-align: right;
  }
  .chapter-card {
    padding: 21px;
  }
  .chapter-topic {
    margin-top: 18px !important;
  }
  .academy-footer {
    line-height: 1.7;
  }
  .academy-footer span {
    display: none;
  }
  .academy-play {
    padding: 20px 12px;
  }
  .game-heading h1 {
    font-size: 20px;
  }
  .game-heading .academy-eyebrow {
    font-size: 8px;
  }
  .game-stage {
    border-radius: 8px;
  }
  .game-stage iframe {
    aspect-ratio: 1.25;
  }
  .tour-guide ol {
    grid-template-columns: repeat(2, 1fr);
  }
  .game-loading {
    font-size: 13px;
  }
  .game-loading small {
    font-size: 9px;
  }
  .game-help {
    line-height: 1.8;
  }
  .desktop-break {
    display: none;
  }
}
</style>
