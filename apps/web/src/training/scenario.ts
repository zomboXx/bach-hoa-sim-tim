// Training Scenario definitions and Evaluation Oracle (TRN-02)
// Data isolation rule: No operational state, sales invoices, or inventory records are modified.

export type TrainingStage =
  "greet" | "milk" | "checkout" | "cooler" | "quarantine" | "mentor" | "complete";

export type TrainingActionType =
  | "greet_customer"
  | "pick_product"
  | "scan_item"
  | "tender_payment"
  | "inspect_cooler"
  | "quarantine_product"
  | "report_mentor";

export interface TrainingChoice {
  id?: string;
  text: string;
  right: boolean;
  feedback: string;
  effect?: () => void;
}

export interface TrainingGoal {
  station: string;
  title: string;
  detail: string;
  verb: string;
  chapter: number;
}

export interface TrainingActionRecord {
  sequence: number;
  stage: TrainingStage;
  actionType: TrainingActionType | string;
  choiceText: string;
  isRight: boolean;
  feedback: string;
  timestamp: number;
}

export type TrainingGrade = "EXCELLENT" | "SATISFACTORY" | "NEEDS_PRACTICE";

export interface TrainingEvaluationResult {
  score: number;
  errors: number;
  passed: boolean;
  grade: TrainingGrade;
  gradeLabel: string;
  completedSteps: number;
  totalRequiredSteps: number;
  summaryText: string;
}

export const SCENARIO_ID = "SCN-RETAIL-01";
export const SCENARIO_TITLE = "Ca đầu tiên ở Sim Tím";
export const TOTAL_REQUIRED_STEPS = 6;
export const BASE_SCORE = 100;
export const PENALTY_PER_ERROR = 10;

export const DEFAULT_GOALS: Record<TrainingStage, TrainingGoal> = {
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
    detail: "Mang hộp sữa chua hết hạn tới khu hàng cần xử lý, tách khỏi hàng bán.",
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

export const REQUIRED_STAGES: TrainingStage[] = [
  "greet",
  "milk",
  "checkout",
  "cooler",
  "quarantine",
  "mentor",
];

/**
 * Deterministic scoring oracle shared between browser client, browser tests,
 * and future backend evaluation in TRN-03.
 *
 * Scoring logic:
 * - Base score: 100 points
 * - Each wrong action choice incurs a 10-point penalty
 * - Grade EXCELLENT: 100 points (0 errors)
 * - Grade SATISFACTORY: 70-90 points (1-3 errors)
 * - Grade NEEDS_PRACTICE: < 70 points (> 3 errors)
 */
export function evaluateTrainingSession(
  actions: TrainingActionRecord[],
  completedStages: TrainingStage[],
): TrainingEvaluationResult {
  const errors = actions.filter((a) => !a.isRight).length;
  const score = Math.max(0, BASE_SCORE - errors * PENALTY_PER_ERROR);
  const completedSteps = REQUIRED_STAGES.filter((s) => completedStages.includes(s)).length;
  const passed = completedSteps === REQUIRED_STAGES.length;

  let grade: TrainingGrade;
  let gradeLabel: string;

  if (errors === 0 && passed) {
    grade = "EXCELLENT";
    gradeLabel = "Xuất sắc (Đúng ngay từ lần đầu)";
  } else if (errors <= 3 && passed) {
    grade = "SATISFACTORY";
    gradeLabel = "Đạt yêu cầu";
  } else {
    grade = "NEEDS_PRACTICE";
    gradeLabel = "Cần luyện tập thêm";
  }

  const summaryText =
    errors === 0 ? "Hoàn thành đúng ngay từ lần đầu" : `${errors} lần được mentor hướng dẫn lại`;

  return {
    score,
    errors,
    passed,
    grade,
    gradeLabel,
    completedSteps,
    totalRequiredSteps: TOTAL_REQUIRED_STEPS,
    summaryText,
  };
}
