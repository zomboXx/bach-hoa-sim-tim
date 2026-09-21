<script setup lang="ts">
import {
  computed,
  nextTick,
  onMounted,
  onUnmounted,
  reactive,
  ref,
  watch,
} from "vue";
import PixelPerson from "./PixelPerson.vue";
import {
  adjacent,
  blocked,
  pathTo,
  stations,
  type Point,
  type Station,
} from "./world";
defineProps<{ employeeName: string }>();
const emit = defineEmits<{ exit: [] }>();
type Stage =
  | "greet"
  | "milk"
  | "checkout"
  | "cooler"
  | "quarantine"
  | "mentor"
  | "complete";
type Choice = {
  text: string;
  right: boolean;
  feedback: string;
  effect?: () => void;
};
type Conversation = {
  speaker: string;
  heading: string;
  text: string;
  choices: Choice[];
  type?: "scan";
};
const stage = ref<Stage>("greet");
const player = reactive({ x: 10, y: 11, direction: "down" });
const intro = ref(true);
const exitPrompt = ref(false);
const dialogue = ref<Conversation | null>(null);
const feedback = ref("");
const correct = ref(false);
const picked = ref<Choice>();
const bag = reactive({ milk: 0, expired: 0 });
const scans = ref(0);
const errors = ref(0);
const steps = ref(0);
const path = ref<Point[]>([]);
const destination = ref<Station>();
const held = ref("");
const walking = ref(false);
const notice = ref("");
const gameRoot = ref<HTMLElement>();
const world = ref<HTMLElement>();
const firstChoice = ref<HTMLButtonElement[]>();
let ticker: ReturnType<typeof setInterval>;
let noticeTimer: ReturnType<typeof setTimeout>;
const goals: Record<
  Stage,
  {
    station: string;
    title: string;
    detail: string;
    verb: string;
    chapter: number;
  }
> = {
  greet: {
    station: "customer",
    title: "Đón vị khách đầu tiên",
    detail: "Đến gặp khách Linh ở cửa ra vào. Lắng nghe trước khi tư vấn.",
    verb: "Trò chuyện",
    chapter: 0,
  },
  milk: {
    station: "milk",
    title: "Tìm đúng món khách cần",
    detail: "Khách muốn 2 hộp sữa ít đường. Đến kệ sữa để chọn hàng.",
    verb: "Chọn sản phẩm",
    chapter: 1,
  },
  checkout: {
    station: "checkout",
    title: "Hoàn tất một đơn hàng",
    detail: "Mang 2 hộp sữa tới quầy. Quét hàng, nhận tiền và trả tiền thừa.",
    verb: "Dùng máy tính tiền",
    chapter: 1,
  },
  cooler: {
    station: "cooler",
    title: "Chăm sóc hàng trên kệ",
    detail: "Mai nhờ bạn kiểm tra tủ mát. Có một sản phẩm cần được xử lý.",
    verb: "Kiểm tra hàng",
    chapter: 2,
  },
  quarantine: {
    station: "quarantine",
    title: "Tách hàng không được bán",
    detail:
      "Mang hộp sữa chua hết hạn tới khu hàng cần xử lý, tách khỏi hàng bán.",
    verb: "Đặt vào khu xử lý",
    chapter: 2,
  },
  mentor: {
    station: "mentor",
    title: "Báo lại với Mentor Mai",
    detail: "Tới gặp Mai để bàn giao tình trạng hàng và kết thúc ca thực hành.",
    verb: "Báo cáo cuối ca",
    chapter: 2,
  },
  complete: {
    station: "mentor",
    title: "Bạn đã hoàn thành ca đầu tiên!",
    detail: "Giao tiếp, bán hàng và xử lý hàng hóa — bạn đã thử cả ba.",
    verb: "Trò chuyện",
    chapter: 3,
  },
};
const goal = computed(() => goals[stage.value]);
const nearby = computed(() => stations.find((s) => adjacent(player, s)));
const locked = computed(
  () =>
    intro.value ||
    !!dialogue.value ||
    exitPrompt.value ||
    stage.value === "complete",
);
const completed = computed(() =>
  stage.value === "greet"
    ? 0
    : ["milk", "checkout"].includes(stage.value)
      ? 1
      : stage.value === "complete"
        ? 3
        : 2,
);
const log = ref<string[]>([]);
function tell(s: string) {
  notice.value = s;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => (notice.value = ""), 4000);
}
function stop() {
  path.value = [];
  destination.value = undefined;
  held.value = "";
  walking.value = false;
}
async function focusWorld() {
  await nextTick();
  world.value?.focus({ preventScroll: true });
}
async function converse(c: Conversation) {
  stop();
  feedback.value = "";
  correct.value = false;
  picked.value = undefined;
  dialogue.value = c;
  await nextTick();
  firstChoice.value?.[0]?.focus({ preventScroll: true });
}
function choose(c: Choice) {
  if (correct.value) return;
  picked.value = c;
  feedback.value = c.feedback;
  correct.value = c.right;
  if (!c.right) errors.value++;
}
function finishReply() {
  picked.value?.effect?.();
  dialogue.value = null;
  feedback.value = "";
  correct.value = false;
  void focusWorld();
}
function advance(s: Stage, entry: string) {
  stage.value = s;
  log.value.unshift(entry);
}
function interact(s: Station) {
  if (locked.value) return;
  if (s.id !== goal.value.station) {
    void converse({
      speaker: s.type === "customer" ? "Khách Linh" : "Mentor Mai",
      heading: s.name,
      text:
        s.id === "snacks"
          ? "Kệ bánh kẹo có nhiều món ngon. Nhưng hãy giúp khách tìm đúng món họ cần trước nhé."
          : s.id === "mentor"
            ? "Chị ở đây nếu em cần hướng dẫn. " + goal.value.detail
            : "Mình sẽ quay lại đây khi cần. " + goal.value.detail,
      choices: [
        { text: "Mình hiểu rồi", right: true, feedback: goal.value.detail },
      ],
    });
    return;
  }
  if (stage.value === "greet")
    void converse({
      speaker: "Khách Linh",
      heading: "“Chào em, chị cần mua sữa…”",
      text: "Chị muốn mua hai hộp sữa, nhưng không thích ngọt lắm. Em giúp chị chọn được không?",
      choices: [
        {
          text: "Chị cứ lấy loại đang giảm giá đi ạ.",
          right: false,
          feedback:
            "Mai: Khuyến mãi chưa chắc phù hợp. Hãy hỏi và xác nhận nhu cầu của khách trước nhé.",
        },
        {
          text: "Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ? Em lấy giúp chị nhé.",
          right: true,
          feedback: "Linh: Đúng rồi, cảm ơn em! Chị chờ ở quầy nhé.",
          effect: () =>
            advance("milk", "Đã lắng nghe và xác nhận nhu cầu của khách."),
        },
        {
          text: "Sữa ở trên kệ đó chị tự tìm nhé.",
          right: false,
          feedback:
            "Mai: Khách đang cần trợ giúp. Một lời chào và hướng dẫn cụ thể sẽ làm khách thoải mái hơn.",
        },
      ],
    });
  else if (stage.value === "milk")
    void converse({
      speaker: employeeNameFallback(),
      heading: "Trước kệ sữa & đồ uống",
      text: "Hãy chọn hàng theo lời khách: 2 hộp sữa ít đường. Sản phẩm còn hạn, bao bì nguyên vẹn.",
      choices: [
        {
          text: "Lấy 2 hộp sữa ít đường · 8.500đ / hộp",
          right: true,
          feedback:
            "Bạn đặt 2 hộp sữa ít đường vào giỏ cầm tay. Tiếp theo: mang tới quầy thanh toán.",
          effect: () => {
            bag.milk = 2;
            advance("checkout", "Đã chọn đúng 2 hộp sữa ít đường, còn hạn.");
          },
        },
        {
          text: "Lấy 2 hộp sữa có đường · 8.000đ / hộp",
          right: false,
          feedback:
            "Mai: Loại này có đường, chưa đúng nhu cầu khách vừa xác nhận.",
        },
        {
          text: "Lấy 1 hộp sữa ít đường",
          right: false,
          feedback:
            "Mai: Khách cần hai hộp. Hãy kiểm tra cả loại hàng và số lượng nhé.",
        },
      ],
    });
  else if (stage.value === "checkout") {
    scans.value = 0;
    void converse({
      speaker: "Quầy thanh toán",
      heading: "Đưa hàng qua máy quét",
      text: "Hai hộp sữa ít đường đang ở trong giỏ của bạn. Quét đủ từng hộp để lên hóa đơn.",
      type: "scan",
      choices: [],
    });
  } else if (stage.value === "cooler")
    void converse({
      speaker: "Mentor Mai",
      heading: "Có gì không ổn trong tủ mát?",
      text: "Bạn thấy một hộp sữa chua hết hạn từ hôm qua. Bao bì vẫn nguyên vẹn. Bạn sẽ làm gì?",
      choices: [
        {
          text: "Giảm giá để bán nhanh trong hôm nay.",
          right: false,
          feedback:
            "Mai: Hàng đã hết hạn không được tiếp tục bán, kể cả giảm giá. Phải tách khỏi hàng còn bán được.",
        },
        {
          text: "Lấy hộp hết hạn khỏi tủ và mang tới khu hàng cần xử lý.",
          right: true,
          feedback:
            "Bạn lấy hộp sữa chua hết hạn khỏi tủ. Mang tới khu xử lý để tránh lẫn vào hàng bán, rồi báo quản lý.",
          effect: () => {
            bag.expired = 1;
            advance(
              "quarantine",
              "Đã nhận diện và lấy hàng hết hạn khỏi tủ mát.",
            );
          },
        },
        {
          text: "Đẩy hộp này vào phía trong tủ.",
          right: false,
          feedback:
            "Mai: Để lại trong tủ vẫn có nguy cơ khách lấy nhầm. Hãy tách hẳn sản phẩm khỏi khu bán hàng.",
        },
      ],
    });
  else if (stage.value === "quarantine")
    void converse({
      speaker: "Bạn",
      heading: "Khu hàng cần xử lý",
      text: "Hộp sữa chua này cần được tách riêng và ghi rõ tình trạng để quản lý xử lý tiếp.",
      choices: [
        {
          text: "Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”.",
          right: true,
          feedback:
            "Hàng đã được tách khỏi khu bán. Đến gặp Mai để báo lại và bàn giao tình trạng nhé.",
          effect: () => {
            bag.expired = 0;
            advance("mentor", "Đã tách hàng hết hạn và gắn nhãn không bán.");
          },
        },
        {
          text: "Đặt chung vào thùng hàng mới nhận.",
          right: false,
          feedback:
            "Mai: Hàng mới và hàng không đủ điều kiện bán phải tách riêng để tránh đưa nhầm trở lại kệ.",
        },
      ],
    });
  else if (stage.value === "mentor")
    void converse({
      speaker: "Mentor Mai",
      heading: "“Ca đầu tiên thế nào rồi em?”",
      text: "Hãy báo lại đúng việc bạn đã làm, để chị tiếp tục xử lý sản phẩm hết hạn.",
      choices: [
        {
          text: "Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn, đã tách và gắn nhãn ở khu xử lý.",
          right: true,
          feedback:
            "Mai: Tốt lắm! Em vừa thực hành giao tiếp, bán hàng và chăm sóc hàng hóa trong cùng một ca. Chị sẽ tiếp nhận phần xử lý tiếp theo.",
          effect: () =>
            advance("complete", "Đã báo cáo đầy đủ cho người phụ trách."),
        },
        {
          text: "Không có gì cần báo chị ạ.",
          right: false,
          feedback:
            "Mai: Hàng đã tách cần được bàn giao cho người phụ trách. Hãy nêu rõ loại hàng, số lượng và vị trí.",
        },
      ],
    });
}
function employeeNameFallback() {
  return "Bạn";
}
function scan() {
  if (scans.value >= 2) return;
  scans.value++;
}
function tender() {
  if (scans.value !== 2) return;
  void converse({
    speaker: "Khách Linh",
    heading: "“Chị gửi em 20.000 đồng.”",
    text: "Hóa đơn: 2 × 8.500đ = 17.000đ. Bạn trả lại khách bao nhiêu và nói gì?",
    choices: [
      {
        text: "Trả 2.000đ, cảm ơn khách.",
        right: false,
        feedback:
          "Mai: Hãy tính lại: 20.000 − 17.000 = 3.000 đồng. Đếm lại trước khi đưa khách.",
      },
      {
        text: "“Em gửi chị 3.000đ tiền thừa và hóa đơn. Cảm ơn chị, hẹn gặp lại!”",
        right: true,
        feedback:
          "Linh: Cảm ơn em nhé! Giao dịch đã hoàn tất, khách rất hài lòng. Mai nhờ bạn kiểm tra tủ mát trước khi kết thúc ca.",
        effect: () => {
          bag.milk = 0;
          advance(
            "cooler",
            "Đã quét đủ hàng, trả đúng 3.000đ và cảm ơn khách.",
          );
        },
      },
      {
        text: "Trả 5.000đ và bỏ qua hóa đơn.",
        right: false,
        feedback:
          "Mai: Tiền thừa chưa đúng. Đối chiếu số tiền khách đưa và tổng hóa đơn, rồi giao hóa đơn cho khách.",
      },
    ],
  });
}
function step(dx: number, dy: number) {
  if (locked.value) return;
  const next = { x: player.x + dx, y: player.y + dy };
  player.direction =
    dx < 0 ? "left" : dx > 0 ? "right" : dy < 0 ? "up" : "down";
  if (blocked(next)) {
    walking.value = false;
    return;
  }
  player.x = next.x;
  player.y = next.y;
  walking.value = true;
  steps.value++;
}
function tick() {
  if (locked.value) {
    walking.value = false;
    return;
  }
  if (held.value) {
    const d: Record<string, number[]> = {
      up: [0, -1],
      down: [0, 1],
      left: [-1, 0],
      right: [1, 0],
    };
    const [dx, dy] = d[held.value];
    step(dx, dy);
    return;
  }
  const next = path.value.shift();
  if (next) {
    step(next.x - player.x, next.y - player.y);
    return;
  }
  walking.value = false;
  if (destination.value) {
    const s = destination.value;
    destination.value = undefined;
    if (adjacent(player, s)) interact(s);
  }
}
function walkTo(s: Station) {
  if (locked.value) return;
  held.value = "";
  const result = pathTo(player, (p) => adjacent(p, s));
  if (!result) {
    tell("Chưa có lối đi tới đây.");
    return;
  }
  path.value = result;
  destination.value = s;
  world.value?.focus({ preventScroll: true });
}
function floorClick(e: MouseEvent) {
  if (locked.value) return;
  const svg = e.currentTarget as SVGSVGElement;
  const point = svg.createSVGPoint();
  point.x = e.clientX;
  point.y = e.clientY;
  const local = point.matrixTransform(svg.getScreenCTM()!.inverse());
  const target = { x: Math.floor(local.x / 40), y: Math.floor(local.y / 40) };
  if (blocked(target)) return;
  const result = pathTo(player, (p) => p.x === target.x && p.y === target.y);
  if (result) {
    held.value = "";
    destination.value = undefined;
    path.value = result;
  }
  void focusWorld();
}
const keyMap: Record<string, string> = {
  ArrowUp: "up",
  w: "up",
  W: "up",
  ArrowDown: "down",
  s: "down",
  S: "down",
  ArrowLeft: "left",
  a: "left",
  A: "left",
  ArrowRight: "right",
  d: "right",
  D: "right",
};
function keydown(e: KeyboardEvent) {
  if (
    e.key === "Tab" &&
    (intro.value || exitPrompt.value || stage.value === "complete")
  ) {
    const buttons = [
      ...gameRoot.value!.querySelectorAll<HTMLButtonElement>(
        ".game-overlay button",
      ),
    ];
    const first = buttons[0],
      last = buttons.at(-1);
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault();
      last?.focus();
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault();
      first?.focus();
    }
    return;
  }
  if (locked.value) return;
  if (keyMap[e.key]) {
    e.preventDefault();
    destination.value = undefined;
    path.value = [];
    held.value = keyMap[e.key];
    if (!e.repeat) {
      const map: Record<string, number[]> = {
        up: [0, -1],
        down: [0, 1],
        left: [-1, 0],
        right: [1, 0],
      };
      const [dx, dy] = map[held.value];
      step(dx, dy);
    }
  } else if (e.key.toLowerCase() === "e") {
    e.preventDefault();
    if (nearby.value) interact(nearby.value);
    else tell("Đến gần khách, kệ hàng hoặc quầy để tương tác.");
  }
}
function keyup(e: KeyboardEvent) {
  if (keyMap[e.key] === held.value) held.value = "";
}
function directionPress(d: string, e: PointerEvent) {
  if (locked.value) return;
  (e.currentTarget as HTMLElement).setPointerCapture(e.pointerId);
  path.value = [];
  destination.value = undefined;
  held.value = d;
}
function start() {
  intro.value = false;
  void focusWorld();
}
function leave() {
  stop();
  if (intro.value || stage.value === "complete") emit("exit");
  else exitPrompt.value = true;
}
function reset() {
  stop();
  stage.value = "greet";
  Object.assign(player, { x: 10, y: 11, direction: "down" });
  Object.assign(bag, { milk: 0, expired: 0 });
  scans.value = 0;
  steps.value = 0;
  errors.value = 0;
  log.value = [];
  dialogue.value = null;
  intro.value = true;
}
watch([intro, exitPrompt, stage], async () => {
  await nextTick();
  gameRoot.value
    ?.querySelector<HTMLButtonElement>(".game-overlay button")
    ?.focus({ preventScroll: true });
});
onMounted(() => {
  gameRoot.value
    ?.querySelector<HTMLButtonElement>(".game-overlay button")
    ?.focus({ preventScroll: true });
  ticker = setInterval(tick, 135);
  window.addEventListener("keyup", keyup);
  window.addEventListener("blur", stop);
});
onUnmounted(() => {
  clearInterval(ticker);
  clearTimeout(noticeTimer);
  window.removeEventListener("keyup", keyup);
  window.removeEventListener("blur", stop);
});
</script>

<template>
  <section ref="gameRoot" class="training-game" @keydown="keydown">
    <header
      class="game-header"
      :inert="intro || exitPrompt || stage === 'complete'"
    >
      <button @click="leave" class="game-exit">
        ← <span>Về cửa hàng</span>
      </button>
      <div class="game-wordmark">
        <img src="/icon.svg" alt="" />
        <div>SIM TÍM <small>HỌC VIỆC PHIÊU LƯU KÝ</small></div>
      </div>
      <div class="game-player-badge">
        <span>✦</span>
        <div>{{ employeeName }}<small>Nhân viên tập sự</small></div>
      </div>
    </header>
    <div
      class="game-content"
      :inert="intro || exitPrompt || stage === 'complete'"
    >
      <div class="game-heading">
        <div>
          <p>CA THỰC HÀNH 01 <span>•</span> 5–8 PHÚT</p>
          <h1>Ca đầu tiên ở Sim Tím<span>✧</span></h1>
        </div>
        <span class="sandbox-badge">◈ Không gian thực hành riêng</span>
      </div>
      <div class="game-layout">
        <div class="world-column">
          <div class="world-topline">
            <span><i></i> CỬA HÀNG SIM TÍM</span
            ><span>↗ Nhấp vào nơi muốn đến</span>
          </div>
          <div
            ref="world"
            tabindex="0"
            class="world-viewport"
            aria-label="Cửa hàng 2D. Dùng WASD hoặc phím mũi tên để di chuyển, E để tương tác."
          >
            <svg
              class="store-world"
              viewBox="0 0 800 560"
              @click="floorClick"
              role="group"
              aria-label="Bản đồ cửa hàng tương tác"
            >
              <defs>
                <pattern
                  id="floorPattern"
                  width="40"
                  height="40"
                  patternUnits="userSpaceOnUse"
                >
                  <rect width="40" height="40" fill="#ebe0c9" />
                  <path
                    d="M0 40V0H40"
                    fill="none"
                    stroke="#d5c6b4"
                    stroke-width="1"
                  />
                  <path d="M6 6h2v2H6" fill="#d8cbb8" />
                </pattern>
                <pattern
                  id="rugPattern"
                  width="10"
                  height="10"
                  patternUnits="userSpaceOnUse"
                >
                  <rect width="10" height="10" fill="#96749f" />
                  <path d="M0 0h10v1H0" fill="#a48aab" />
                </pattern>
              </defs>
              <rect width="800" height="560" fill="#614b70" />
              <rect
                x="18"
                y="65"
                width="764"
                height="480"
                rx="3"
                fill="#4a3b53"
              />
              <rect
                x="30"
                y="74"
                width="740"
                height="456"
                fill="url(#floorPattern)"
              />
              <rect x="30" y="65" width="740" height="24" fill="#ae95b6" />
              <rect x="30" y="82" width="740" height="9" fill="#8e7598" />
              <rect
                x="46"
                y="14"
                width="168"
                height="42"
                fill="#493253"
                stroke="#caaa80"
                stroke-width="3"
              />
              <text
                x="130"
                y="42"
                fill="#f7e3bb"
                text-anchor="middle"
                class="world-store-sign"
              >
                SIM TÍM
              </text>
              <text
                x="480"
                y="42"
                text-anchor="middle"
                fill="#d8c4e1"
                class="world-wall-copy"
              >
                MỖI NGÀY MỘT CHÚT TỰ TIN HƠN
              </text>
              <g transform="translate(708 13)">
                <circle
                  cx="18"
                  cy="20"
                  r="17"
                  fill="#f2e6ca"
                  stroke="#b599ba"
                  stroke-width="3"
                />
                <path
                  d="M18 10v10l8 5"
                  stroke="#806484"
                  stroke-width="2"
                  fill="none"
                />
              </g>
              <rect
                x="555"
                y="434"
                width="160"
                height="91"
                fill="url(#rugPattern)"
              />
              <rect x="555" y="523" width="160" height="17" fill="#c5b5c7" />
              <text
                x="635"
                y="498"
                text-anchor="middle"
                fill="#e9d8eb"
                class="world-floor-label"
              >
                CHÀO MỪNG BẠN
              </text>
              <path
                d="M187 105v165M187 379v110M427 105v384"
                stroke="#cab799"
                stroke-width="2"
                stroke-dasharray="4 8"
                opacity=".6"
              />
              <g
                v-for="s in stations.filter(
                  (s) => !['mentor', 'customer'].includes(s.id),
                )"
                :key="s.id"
                :transform="`translate(${s.x * 40},${s.y * 40})`"
                :class="[
                  'world-station',
                  { target: goal.station === s.id && !intro, disabled: locked },
                ]"
                role="button"
                :tabindex="locked ? -1 : 0"
                :aria-label="'Đi đến ' + s.name"
                @click.stop="walkTo(s)"
                @keydown.enter.stop="walkTo(s)"
                @keydown.space.prevent.stop="walkTo(s)"
              >
                <rect
                  x="-8"
                  y="-9"
                  :width="s.w * 40 + 16"
                  :height="s.h * 40 + 20"
                  fill="transparent"
                />
                <rect
                  class="station-glow"
                  x="-7"
                  y="-7"
                  :width="s.w * 40 + 14"
                  :height="s.h * 40 + 14"
                  rx="5"
                  fill="none"
                  stroke="#e5a35f"
                  stroke-width="3"
                  stroke-dasharray="7 5"
                />
                <rect
                  x="7"
                  y="8"
                  :width="s.w * 40"
                  :height="s.h * 40"
                  rx="2"
                  fill="#584454"
                  opacity=".2"
                />
                <template v-if="s.type === 'shelf'">
                  <rect :width="s.w * 40" :height="s.h * 40" fill="#866878" />
                  <rect
                    x="4"
                    y="3"
                    :width="s.w * 40 - 8"
                    :height="s.h * 40 - 9"
                    fill="#d0b18a"
                  />
                  <rect
                    x="8"
                    y="5"
                    :width="s.w * 40 - 16"
                    height="64"
                    fill="#d8c69e"
                  />
                  <g
                    v-for="r in 2"
                    :transform="`translate(12,${r === 1 ? 6 : 39})`"
                  >
                    <g
                      v-for="n in 6"
                      :transform="`translate(${(n - 1) * 16},0)`"
                    >
                      <rect
                        width="11"
                        height="24"
                        :fill="
                          s.id === 'milk'
                            ? ['#e9f0dd', '#b8d4d0', '#f2dcb7'][n % 3]
                            : ['#c57e58', '#e5b668', '#9e7a99'][n % 3]
                        "
                      />
                      <path
                        v-if="s.id === 'milk'"
                        d="M2 0h7v-3H2z"
                        fill="#709caf"
                      />
                      <rect
                        x="2"
                        y="9"
                        width="7"
                        height="8"
                        fill="#fff7d4"
                        opacity=".7"
                      />
                    </g>
                  </g>
                  <path
                    d="M4 35h112M4 69h112"
                    stroke="#947b67"
                    stroke-width="6"
                  />
                  <rect x="38" y="75" width="44" height="12" fill="#fff0ce" />
                  <text x="60" y="83" class="shelf-price" text-anchor="middle">
                    {{ s.id === "milk" ? "8.500đ" : "BÁNH KẸO" }}
                  </text>
                </template>
                <template v-if="s.type === 'cooler'">
                  <rect width="160" height="80" fill="#7a8c92" />
                  <rect x="4" y="4" width="152" height="64" fill="#b0d3d3" />
                  <rect x="9" y="9" width="68" height="53" fill="#c1dfd9" />
                  <rect x="83" y="9" width="68" height="53" fill="#c1dfd9" />
                  <g v-for="n in 7" :transform="`translate(${n * 18 - 5},29)`">
                    <path
                      d="M0 0h12l-2 21H2z"
                      :fill="
                        n === 3 &&
                        stage !== 'quarantine' &&
                        stage !== 'mentor' &&
                        stage !== 'complete'
                          ? '#e5a0a0'
                          : '#f5efcb'
                      "
                    />
                    <rect x="-1" y="-3" width="14" height="5" fill="#9375a6" />
                  </g>
                  <path
                    d="M77 5v58M6 64h148"
                    stroke="#7d9da3"
                    stroke-width="4"
                  />
                  <rect x="68" y="25" width="3" height="15" fill="#637a80" />
                  <rect x="89" y="25" width="3" height="15" fill="#637a80" />
                  <rect x="4" y="69" width="152" height="7" fill="#586c77" />
                </template>
                <template v-if="s.type === 'checkout'">
                  <rect width="160" height="80" fill="#77586d" />
                  <rect x="3" y="4" width="154" height="57" fill="#c1a085" />
                  <rect x="10" y="10" width="61" height="36" fill="#8d7693" />
                  <rect x="17" y="16" width="46" height="21" fill="#b0d6b9" />
                  <path d="M35 45h11v10H24v-5h11z" fill="#63546d" />
                  <rect
                    x="95"
                    y="13"
                    width="49"
                    height="36"
                    rx="3"
                    fill="#8c8885"
                  />
                  <path d="M101 27h34" stroke="#d78b7b" stroke-width="3" />
                  <rect x="9" y="65" width="139" height="11" fill="#a98778" />
                  <text x="108" y="75" class="shelf-price" fill="#fff3d6">
                    THU NGÂN
                  </text>
                </template>
                <template v-if="s.type === 'quarantine'">
                  <rect width="120" height="80" fill="#947666" />
                  <rect x="5" y="5" width="110" height="65" fill="#bfa079" />
                  <rect
                    x="15"
                    y="12"
                    width="90"
                    height="45"
                    fill="#d3b898"
                    stroke="#a8876d"
                    stroke-width="4"
                  />
                  <path d="M45 18h30v26H45z" fill="#e9d1a7" />
                  <text
                    x="60"
                    y="38"
                    text-anchor="middle"
                    fill="#c18062"
                    font-size="23"
                    font-weight="bold"
                  >
                    !
                  </text>
                  <rect x="6" y="64" width="107" height="12" fill="#ecd9aa" />
                  <text x="60" y="73" text-anchor="middle" class="shelf-price">
                    TÁCH RIÊNG · KHÔNG BÁN
                  </text>
                </template>
                <text
                  :x="s.w * 20"
                  y="-15"
                  text-anchor="middle"
                  class="station-name"
                >
                  {{ s.name }}
                </text>
                <g
                  v-if="goal.station === s.id && !intro"
                  :transform="`translate(${s.w * 20},-41)`"
                >
                  <path d="M-10-11h20v19H3l-3 5-3-5h-7z" fill="#f2bc73" />
                  <text
                    text-anchor="middle"
                    y="3"
                    fill="#664d52"
                    font-size="13"
                    font-weight="bold"
                  >
                    !
                  </text>
                </g>
              </g>
              <g
                v-for="s in stations.filter((s) =>
                  ['mentor', 'customer'].includes(s.id),
                )"
                :key="s.id"
                :transform="`translate(${s.x * 40},${s.y * 40})`"
                :class="['world-npc', { target: goal.station === s.id }]"
                role="button"
                :tabindex="locked ? -1 : 0"
                :aria-label="'Đi đến ' + s.name"
                @click.stop="walkTo(s)"
                @keydown.enter.stop="walkTo(s)"
                @keydown.space.prevent.stop="walkTo(s)"
              >
                <rect
                  x="-23"
                  y="-36"
                  width="86"
                  height="90"
                  fill="transparent"
                />
                <ellipse
                  class="npc-ring"
                  cx="20"
                  cy="35"
                  rx="25"
                  ry="10"
                  fill="none"
                  stroke="#da9f5c"
                  stroke-width="2"
                  stroke-dasharray="5 4"
                />
                <PixelPerson :kind="s.type" />
                <text x="20" y="54" text-anchor="middle" class="npc-name">
                  {{ s.name }}
                </text>
                <g
                  v-if="goal.station === s.id && !intro"
                  transform="translate(20,-39)"
                >
                  <path d="M-11-14h22v22H3L0 13-3 8h-8z" fill="#ffce86" />
                  <text
                    y="3"
                    text-anchor="middle"
                    fill="#815a45"
                    font-size="17"
                    font-weight="bold"
                  >
                    !
                  </text>
                </g>
              </g>
              <g v-if="path.length" pointer-events="none">
                <circle
                  v-for="(p, i) in path.filter((_, i) => i % 2 === 0)"
                  :key="i"
                  :cx="p.x * 40 + 20"
                  :cy="p.y * 40 + 22"
                  r="3"
                  fill="#a28aab"
                  opacity=".6"
                />
              </g>
              <g
                :transform="`translate(${player.x * 40},${player.y * 40})`"
                class="world-player"
                :data-x="player.x"
                :data-y="player.y"
                :data-stage="stage"
                aria-label="Nhân vật của bạn"
                pointer-events="none"
              >
                <ellipse
                  cx="20"
                  cy="36"
                  rx="21"
                  ry="8"
                  fill="none"
                  stroke="#8b63a6"
                  stroke-width="2"
                />
                <PixelPerson :walking="walking" :direction="player.direction" />
                <g v-if="bag.milk || bag.expired" transform="translate(32,10)">
                  <rect
                    width="13"
                    height="17"
                    :fill="bag.expired ? '#dd9880' : '#eedac1'"
                    stroke="#86654f"
                    stroke-width="2"
                  />
                  <path
                    d="M3 0v-5h7v5"
                    fill="none"
                    stroke="#86654f"
                    stroke-width="2"
                  />
                </g>
                <text x="20" y="53" text-anchor="middle" class="player-name">
                  BẠN
                </text>
              </g>
              <g transform="translate(43,440)">
                <rect x="12" y="18" width="26" height="36" fill="#ae7c5d" />
                <path
                  d="M12 19V-5H0v-16h18V-8h8v-30h16v23h10V2H35v17z"
                  fill="#7c9b74"
                />
                <path d="M18 16V-5h9v21" stroke="#5d7d65" stroke-width="4" />
              </g>
            </svg>
            <div
              v-if="!intro && !dialogue && stage !== 'complete'"
              class="in-world-hint"
              aria-live="polite"
            >
              <span v-if="destination">Đang đi tới {{ destination.name }}…</span
              ><span v-else-if="nearby"
                ><kbd>E</kbd>
                {{
                  nearby.id === goal.station
                    ? goal.verb
                    : "Tương tác với " + nearby.name
                }}</span
              ><span v-else>Di chuyển đến điểm có dấu <b>!</b></span>
            </div>
            <div v-if="notice" class="world-notice" role="status">
              {{ notice }}
            </div>
          </div>
          <div class="game-controls">
            <div class="keyboard-legend">
              <span
                ><kbd>W</kbd><kbd>A</kbd><kbd>S</kbd><kbd>D</kbd> Di
                chuyển</span
              ><span><kbd>E</kbd> Tương tác</span>
            </div>
            <div class="touch-pad" aria-label="Điều khiển di chuyển">
              <button
                v-for="[d, label, arrow] in [
                  ['up', 'Đi lên', '↑'],
                  ['left', 'Đi trái', '←'],
                  ['down', 'Đi xuống', '↓'],
                  ['right', 'Đi phải', '→'],
                ]"
                :key="d"
                :aria-label="label"
                :class="'pad-' + d"
                :disabled="locked"
                @pointerdown.prevent="directionPress(d, $event)"
                @pointerup="held = ''"
                @pointercancel="held = ''"
                @lostpointercapture="held = ''"
              >
                {{ arrow }}
              </button>
            </div>
            <button
              class="interact-button"
              :disabled="!nearby || locked"
              @click="nearby && interact(nearby)"
            >
              <kbd>E</kbd> Tương tác
            </button>
          </div>
        </div>
        <aside class="quest-panel">
          <div class="quest-heading">
            <span>NHẬT KÝ CA LÀM</span><b>{{ completed }} / 3</b>
          </div>
          <div class="quest-progress">
            <i :style="{ width: (completed / 3) * 100 + '%' }"></i>
          </div>
          <div
            v-for="(q, i) in [
              ['Giao tiếp với khách', 'Chào hỏi, lắng nghe, tư vấn'],
              ['Bán hàng tại quầy', 'Chọn hàng, quét mã, trả tiền'],
              ['Xử lý hàng hóa', 'Kiểm tra hạn, tách hàng, báo cáo'],
            ]"
            :key="q[0]"
            :class="[
              'quest-item',
              { done: completed > i, current: goal.chapter === i },
            ]"
          >
            <span>{{
              completed > i ? "✓" : String(i + 1).padStart(2, "0")
            }}</span>
            <div>
              <b>{{ q[0] }}</b
              ><small>{{ q[1] }}</small>
            </div>
          </div>
          <div class="current-quest">
            <span>MỤC TIÊU TIẾP THEO</span>
            <h2>{{ goal.title }}</h2>
            <p>{{ goal.detail }}</p>
            <button
              :disabled="locked"
              @click="walkTo(stations.find((s) => s.id === goal.station)!)"
            >
              Dẫn đường tới mục tiêu <span>↗</span>
            </button>
          </div>
          <div class="bag-panel">
            <h3>
              ♧ Giỏ cầm tay <small>{{ bag.milk + bag.expired }} món</small>
            </h3>
            <p v-if="!bag.milk && !bag.expired">Chưa mang theo sản phẩm.</p>
            <p v-if="bag.milk">
              <span>▣</span> Sữa ít đường <b>× {{ bag.milk }}</b>
            </p>
            <p v-if="bag.expired"><span>!</span> Sữa chua hết hạn <b>× 1</b></p>
          </div>
          <div class="mentor-note">
            <img src="/mentor.png" alt="Mentor Mai" />
            <p>
              “Cứ thử nhé! Nếu làm chưa đúng, chị sẽ hướng dẫn em ngay.”<b
                >— Mentor Mai</b
              >
            </p>
          </div>
        </aside>
      </div>
      <div
        v-if="dialogue"
        class="conversation"
        role="region"
        aria-label="Hội thoại trong trò chơi"
      >
        <div class="conversation-avatar">
          <img
            v-if="dialogue.speaker === 'Mentor Mai'"
            src="/mentor.png"
            alt="Mentor Mai"
          /><svg v-else viewBox="-5 -27 50 70">
            <PixelPerson
              :kind="dialogue.speaker === 'Khách Linh' ? 'customer' : 'player'"
            />
          </svg>
        </div>
        <div class="conversation-content">
          <span class="speaker">{{ dialogue.speaker }}</span>
          <h2>{{ dialogue.heading }}</h2>
          <p>{{ dialogue.text }}</p>
          <template v-if="dialogue.type === 'scan'"
            ><div class="scan-items">
              <button
                v-for="n in 2"
                :key="n"
                :disabled="scans !== n - 1"
                @click="scan"
                :aria-label="'Quét hộp sữa ' + n"
              >
                <span>▣</span>{{ scans >= n ? "✓ Đã quét" : "Quét hộp sữa " + n
                }}<b>8.500đ</b>
              </button>
            </div>
            <div class="scan-total">
              <span>{{ scans }} / 2 sản phẩm</span
              ><b>{{ (scans * 8500).toLocaleString("vi-VN") }}đ</b
              ><button :disabled="scans !== 2" @click="tender">
                Nhận tiền từ khách →
              </button>
            </div></template
          >
          <div v-else class="dialogue-choices">
            <button
              v-for="(c, i) in dialogue.choices"
              ref="firstChoice"
              :key="c.text"
              :disabled="correct"
              @click="choose(c)"
            >
              <span>{{ i + 1 }}</span
              >{{ c.text }}
            </button>
          </div>
          <p
            v-if="feedback"
            :class="['choice-feedback', correct ? 'right' : 'retry']"
            role="status"
          >
            {{ correct ? "✓" : "↺" }} {{ feedback }}
          </p>
          <button v-if="correct" class="dialogue-next" @click="finishReply">
            {{
              stage === "mentor" ? "Kết thúc ca" : "Tiếp tục trong cửa hàng"
            }}
            →</button
          ><button
            v-else
            class="conversation-back"
            @click="
              dialogue = null;
              focusWorld();
            "
          >
            Quay lại cửa hàng
          </button>
        </div>
      </div>
      <p class="game-footnote">
        Bản chơi thử 2D trên trình duyệt · Dữ liệu phiên riêng · Tích hợp Godot
        và lưu kết quả qua backend ở giai đoạn sau.
      </p>
    </div>
    <div v-if="intro" class="game-overlay">
      <section
        class="shift-intro"
        role="dialog"
        aria-modal="true"
        aria-labelledby="intro-heading"
      >
        <span class="intro-kicker">CHÀO MỪNG TÂN BINH</span>
        <div class="intro-art">
          <img src="/mentor.png" alt="Mentor Mai chào bạn" />
          <div>Chị Mai đây.<br />Cùng lên ca nhé! <span>✦</span></div>
        </div>
        <h2 id="intro-heading">Bạn là nhân viên trong<br />cửa hàng này.</h2>
        <p>
          Đi gặp khách, lấy hàng và thanh toán tại quầy.<br />Cuối ca, cùng Mai
          xử lý một tình huống trên kệ.
        </p>
        <div class="intro-skills">
          <span>♡ Giao tiếp</span><span>▤ Bán hàng</span><span>▦ Hàng hóa</span>
        </div>
        <button class="start-shift" @click="start">
          Bắt đầu ca thực hành <span>→</span></button
        ><small
          >Di chuyển: WASD / phím mũi tên · Tương tác: E<br />Hoặc nhấp / chạm
          vào khách và đồ vật để đi tới.</small
        ><button class="intro-back" @click="emit('exit')">
          Về giao diện quản lý
        </button>
      </section>
    </div>
    <div v-if="exitPrompt" class="game-overlay">
      <section
        class="leave-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="exit-heading"
      >
        <h2 id="exit-heading">Rời ca thực hành?</h2>
        <p>
          Tiến độ của lần chơi này sẽ được đặt lại. Dữ liệu cửa hàng không bị
          ảnh hưởng.
        </p>
        <button class="start-shift" @click="emit('exit')">Rời ca</button
        ><button
          class="intro-back"
          @click="
            exitPrompt = false;
            focusWorld();
          "
        >
          Tiếp tục chơi
        </button>
      </section>
    </div>
    <div v-if="stage === 'complete'" class="game-overlay">
      <section
        class="shift-complete"
        role="dialog"
        aria-modal="true"
        aria-labelledby="complete-heading"
      >
        <div class="complete-emblem">✦</div>
        <span class="intro-kicker">CA THỰC HÀNH HOÀN TẤT</span>
        <h2 id="complete-heading">Một ca làm, ba kỹ năng.</h2>
        <p>
          Bạn đã trực tiếp chăm khách, bán hàng và xử lý hàng trên kệ trong cửa
          hàng 2D.
        </p>
        <div class="complete-skills">
          <span>✓ Giao tiếp & tư vấn</span><span>✓ Thanh toán & tiền thừa</span
          ><span>✓ Hàng hết hạn & bàn giao</span>
        </div>
        <p class="practice-summary">
          {{ steps }} bước chân ·
          {{
            errors
              ? `${errors} lần được mentor hướng dẫn lại`
              : "Hoàn thành đúng ngay từ lần đầu"
          }}
        </p>
        <div class="complete-history">
          <p v-for="entry in [...log].reverse()" :key="entry">✓ {{ entry }}</p>
        </div>
        <button class="start-shift" @click="emit('exit')">
          Trở về cửa hàng →</button
        ><button class="intro-back" @click="reset">Chơi lại ca này</button>
      </section>
    </div>
  </section>
</template>

<style src="./training.css"></style>
