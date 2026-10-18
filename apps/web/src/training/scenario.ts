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
  | "report_mentor"
  | "interact_hint";

export type TrainingTargetKey =
  | "customer_linh"
  | "shelf_milk"
  | "pos_scanner"
  | "pos_cash"
  | "cooler_display"
  | "quarantine_bin"
  | "mentor_mai"
  | "any";

export type TrainingChoiceKey =
  // Stage 1: greet
  | "greet_discount"
  | "greet_confirm_need"
  | "greet_self_service"
  // Stage 2: milk
  | "milk_pick_2_low_sugar"
  | "milk_pick_2_sweetened"
  | "milk_pick_1_low_sugar"
  // Stage 3: checkout
  | "scan_item_1"
  | "scan_item_2"
  | "tender_2k_change"
  | "tender_3k_receipt"
  | "tender_5k_no_receipt"
  // Stage 4: cooler
  | "cooler_discount"
  | "cooler_quarantine_expired"
  | "cooler_push_inside"
  // Stage 5: quarantine
  | "quarantine_label_expired"
  | "quarantine_mixed_with_new"
  // Stage 6: mentor
  | "mentor_full_report"
  | "mentor_report_nothing"
  // Hint / nav
  | "hint_ack";

export interface ScenarioChoiceDefinition {
  choiceKey: string;
  stage: TrainingStage;
  actionType: TrainingActionType | string;
  targetKey: TrainingTargetKey | string;
  isRight: boolean;
  textSnippet: string;
  feedback: string;
}

export interface TrainingChoice {
  id?: string;
  choiceKey?: string;
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
  targetKey?: TrainingTargetKey | string;
  choiceKey?: TrainingChoiceKey | string;
  choiceText: string;
  isRight: boolean;
  feedback: string;
  timestamp: number;
  scenarioVersion?: string;
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
export const SCENARIO_VERSION = "1.0.0";
export const SCENARIO_TITLE = "Ca đầu tiên ở Sim Tím";
export const TOTAL_REQUIRED_STEPS = 6;
export const BASE_SCORE = 100;
export const PENALTY_PER_ERROR = 10;

/**
 * Scenario Choice Mapping Table for SCN-RETAIL-01 (version 1.0.0).
 * Used by frontend and backend (TRN-03) to independently determine right/wrong choices
 * without trusting client-supplied boolean flags.
 */
export const SCENARIO_CHOICE_MAP_V1: Record<string, ScenarioChoiceDefinition> = {
  greet_discount: {
    choiceKey: "greet_discount",
    stage: "greet",
    actionType: "greet_customer",
    targetKey: "customer_linh",
    isRight: false,
    textSnippet: "Chị cứ lấy loại đang giảm giá đi ạ.",
    feedback: "Mai: Khuyến mãi chưa chắc phù hợp. Hãy hỏi và xác nhận nhu cầu của khách trước nhé.",
  },
  greet_confirm_need: {
    choiceKey: "greet_confirm_need",
    stage: "greet",
    actionType: "greet_customer",
    targetKey: "customer_linh",
    isRight: true,
    textSnippet: "Dạ, chị muốn 2 hộp sữa ít đường đúng không ạ? Em lấy giúp chị nhé.",
    feedback: "Linh: Đúng rồi, cảm ơn em! Chị chờ ở quầy nhé.",
  },
  greet_self_service: {
    choiceKey: "greet_self_service",
    stage: "greet",
    actionType: "greet_customer",
    targetKey: "customer_linh",
    isRight: false,
    textSnippet: "Sữa ở trên kệ đó chị tự tìm nhé.",
    feedback:
      "Mai: Khách đang cần trợ giúp. Một lời chào và hướng dẫn cụ thể sẽ làm khách thoải mái hơn.",
  },
  milk_pick_2_low_sugar: {
    choiceKey: "milk_pick_2_low_sugar",
    stage: "milk",
    actionType: "pick_product",
    targetKey: "shelf_milk",
    isRight: true,
    textSnippet: "Lấy 2 hộp sữa ít đường · 8.500đ / hộp",
    feedback: "Bạn đặt 2 hộp sữa ít đường vào giỏ cầm tay. Tiếp theo: mang tới quầy thanh toán.",
  },
  milk_pick_2_sweetened: {
    choiceKey: "milk_pick_2_sweetened",
    stage: "milk",
    actionType: "pick_product",
    targetKey: "shelf_milk",
    isRight: false,
    textSnippet: "Lấy 2 hộp sữa có đường · 8.000đ / hộp",
    feedback: "Mai: Loại này có đường, chưa đúng nhu cầu khách vừa xác nhận.",
  },
  milk_pick_1_low_sugar: {
    choiceKey: "milk_pick_1_low_sugar",
    stage: "milk",
    actionType: "pick_product",
    targetKey: "shelf_milk",
    isRight: false,
    textSnippet: "Lấy 1 hộp sữa ít đường",
    feedback: "Mai: Khách cần hai hộp. Hãy kiểm tra cả loại hàng và số lượng nhé.",
  },
  scan_item_1: {
    choiceKey: "scan_item_1",
    stage: "checkout",
    actionType: "scan_item",
    targetKey: "pos_scanner",
    isRight: true,
    textSnippet: "Quét hộp sữa 1",
    feedback: "Đã quét hộp sữa 1",
  },
  scan_item_2: {
    choiceKey: "scan_item_2",
    stage: "checkout",
    actionType: "scan_item",
    targetKey: "pos_scanner",
    isRight: true,
    textSnippet: "Quét hộp sữa 2",
    feedback: "Đã quét hộp sữa 2",
  },
  tender_2k_change: {
    choiceKey: "tender_2k_change",
    stage: "checkout",
    actionType: "tender_payment",
    targetKey: "pos_cash",
    isRight: false,
    textSnippet: "Trả 2.000đ, cảm ơn khách.",
    feedback: "Mai: Hãy tính lại: 20.000 − 17.000 = 3.000 đồng. Đếm lại trước khi đưa khách.",
  },
  tender_3k_receipt: {
    choiceKey: "tender_3k_receipt",
    stage: "checkout",
    actionType: "tender_payment",
    targetKey: "pos_cash",
    isRight: true,
    textSnippet: "“Em gửi chị 3.000đ tiền thừa và hóa đơn. Cảm ơn chị, hẹn gặp lại!”",
    feedback:
      "Linh: Cảm ơn em nhé! Giao dịch đã hoàn tất, khách rất hài lòng. Mai nhờ bạn kiểm tra tủ mát trước khi kết thúc ca.",
  },
  tender_5k_no_receipt: {
    choiceKey: "tender_5k_no_receipt",
    stage: "checkout",
    actionType: "tender_payment",
    targetKey: "pos_cash",
    isRight: false,
    textSnippet: "Trả 5.000đ và bỏ qua hóa đơn.",
    feedback:
      "Mai: Tiền thừa chưa đúng. Đối chiếu số tiền khách đưa và tổng hóa đơn, rồi giao hóa đơn cho khách.",
  },
  cooler_discount: {
    choiceKey: "cooler_discount",
    stage: "cooler",
    actionType: "inspect_cooler",
    targetKey: "cooler_display",
    isRight: false,
    textSnippet: "Giảm giá để bán nhanh trong hôm nay.",
    feedback:
      "Mai: Hàng đã hết hạn không được tiếp tục bán, kể cả giảm giá. Phải tách khỏi hàng còn bán được.",
  },
  cooler_quarantine_expired: {
    choiceKey: "cooler_quarantine_expired",
    stage: "cooler",
    actionType: "inspect_cooler",
    targetKey: "cooler_display",
    isRight: true,
    textSnippet: "Lấy hộp hết hạn khỏi tủ và mang tới khu hàng cần xử lý.",
    feedback:
      "Bạn lấy hộp sữa chua hết hạn khỏi tủ. Mang tới khu xử lý để tránh lẫn vào hàng bán, rồi báo quản lý.",
  },
  cooler_push_inside: {
    choiceKey: "cooler_push_inside",
    stage: "cooler",
    actionType: "inspect_cooler",
    targetKey: "cooler_display",
    isRight: false,
    textSnippet: "Đẩy hộp này vào phía trong tủ.",
    feedback:
      "Mai: Để lại trong tủ vẫn có nguy cơ khách lấy nhầm. Hãy tách hẳn sản phẩm khỏi khu bán hàng.",
  },
  quarantine_label_expired: {
    choiceKey: "quarantine_label_expired",
    stage: "quarantine",
    actionType: "quarantine_product",
    targetKey: "quarantine_bin",
    isRight: true,
    textSnippet: "Đặt vào khay riêng, gắn nhãn “Hết hạn — không bán”.",
    feedback: "Hàng đã được tách khỏi khu bán. Đến gặp Mai để báo lại và bàn giao tình trạng nhé.",
  },
  quarantine_mixed_with_new: {
    choiceKey: "quarantine_mixed_with_new",
    stage: "quarantine",
    actionType: "quarantine_product",
    targetKey: "quarantine_bin",
    isRight: false,
    textSnippet: "Đặt chung vào thùng hàng mới nhận.",
    feedback:
      "Mai: Hàng mới và hàng không đủ điều kiện bán phải tách riêng để tránh đưa nhầm trở lại kệ.",
  },
  mentor_full_report: {
    choiceKey: "mentor_full_report",
    stage: "mentor",
    actionType: "report_mentor",
    targetKey: "mentor_mai",
    isRight: true,
    textSnippet:
      "Em đã bán đúng hàng cho khách; có 1 hộp sữa chua hết hạn, đã tách và gắn nhãn ở khu xử lý.",
    feedback:
      "Mai: Tốt lắm! Em vừa thực hành giao tiếp, bán hàng và chăm sóc hàng hóa trong cùng một ca. Chị sẽ tiếp nhận phần xử lý tiếp theo.",
  },
  mentor_report_nothing: {
    choiceKey: "mentor_report_nothing",
    stage: "mentor",
    actionType: "report_mentor",
    targetKey: "mentor_mai",
    isRight: false,
    textSnippet: "Không có gì cần báo chị ạ.",
    feedback:
      "Mai: Hàng đã tách cần được bàn giao cho người phụ trách. Hãy nêu rõ loại hàng, số lượng và vị trí.",
  },
  hint_ack: {
    choiceKey: "hint_ack",
    stage: "greet",
    actionType: "interact_hint",
    targetKey: "any",
    isRight: true,
    textSnippet: "Mình hiểu rồi",
    feedback: "Đã xác nhận hướng dẫn mục tiêu",
  },
};

export const SCENARIO_CATALOG_BY_VERSION: Record<
  string,
  Record<string, ScenarioChoiceDefinition>
> = {
  "1.0.0": SCENARIO_CHOICE_MAP_V1,
};

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
 * Resolves whether an action was correct using the authoritative scenario catalog for the given version.
 * If choiceKey exists in catalog, its catalog value takes precedence over client-supplied `isRight`.
 * If choiceKey is absent (legacy test cases), fallback to client-supplied `isRight`.
 */
export function resolveActionRight(
  action: TrainingActionRecord,
  version: string = action.scenarioVersion || SCENARIO_VERSION,
): boolean {
  const catalog = SCENARIO_CATALOG_BY_VERSION[version] ?? SCENARIO_CHOICE_MAP_V1;
  if (action.choiceKey && catalog[action.choiceKey]) {
    return catalog[action.choiceKey].isRight;
  }
  return action.isRight;
}

/**
 * Deduces completed training stages strictly from the action events sequence.
 * Enforces sequential stage progression (cannot skip preceding required stage).
 * A stage is marked completed only when its required correct action(s) are present in the stream.
 */
export function deriveCompletedStages(
  actions: TrainingActionRecord[],
  version: string = SCENARIO_VERSION,
): TrainingStage[] {
  const completed: TrainingStage[] = [];
  let scanCount = 0;

  for (const a of actions) {
    const isRight = resolveActionRight(a, version);
    if (!isRight) continue;

    // Stage 1: greet (Greeting & confirming customer need)
    if (!completed.includes("greet")) {
      if (
        a.choiceKey === "greet_confirm_need" ||
        ((a.actionType === "greet_customer" || a.stage === "greet") &&
          (a.choiceText.includes("ít đường") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "greet" || a.stage === "greet"))
      ) {
        completed.push("greet");
      }
      continue;
    }

    // Stage 2: milk (Picking correct product & quantity)
    if (!completed.includes("milk")) {
      if (
        a.choiceKey === "milk_pick_2_low_sugar" ||
        ((a.actionType === "pick_product" || a.stage === "milk") &&
          (a.choiceText.includes("ít đường") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "milk" || a.stage === "milk"))
      ) {
        completed.push("milk");
      }
      continue;
    }

    // Stage 3: checkout (Scanning 2 items + tender payment with exact change)
    if (!completed.includes("checkout")) {
      if (a.actionType === "scan_item" || a.choiceKey?.startsWith("scan_item_")) {
        scanCount++;
      }
      const isTenderSuccess =
        a.choiceKey === "tender_3k_receipt" ||
        ((a.actionType === "tender_payment" || a.stage === "checkout") &&
          (a.choiceText.includes("3.000đ") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "checkout" || a.stage === "checkout"));

      if (isTenderSuccess && (scanCount >= 2 || !a.choiceKey)) {
        completed.push("checkout");
      }
      continue;
    }

    // Stage 4: cooler (Inspecting cooler & separating expired item)
    if (!completed.includes("cooler")) {
      if (
        a.choiceKey === "cooler_quarantine_expired" ||
        ((a.actionType === "inspect_cooler" || a.stage === "cooler") &&
          (a.choiceText.includes("hết hạn") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "cooler" || a.stage === "cooler"))
      ) {
        completed.push("cooler");
      }
      continue;
    }

    // Stage 5: quarantine (Placing item in quarantine tray with label)
    if (!completed.includes("quarantine")) {
      if (
        a.choiceKey === "quarantine_label_expired" ||
        ((a.actionType === "quarantine_product" || a.stage === "quarantine") &&
          (a.choiceText.includes("khay riêng") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "quarantine" || a.stage === "quarantine"))
      ) {
        completed.push("quarantine");
      }
      continue;
    }

    // Stage 6: mentor (Full handover report to Mentor Mai)
    if (!completed.includes("mentor")) {
      if (
        a.choiceKey === "mentor_full_report" ||
        ((a.actionType === "report_mentor" || a.stage === "mentor") &&
          (a.choiceText.includes("bán đúng") || a.choiceText === "Lựa chọn đúng")) ||
        (!a.choiceKey && (a.actionType === "mentor" || a.stage === "mentor"))
      ) {
        completed.push("mentor");
      }
      continue;
    }
  }

  return completed;
}

/**
 * Deterministic scoring oracle shared between browser client, browser tests,
 * and future backend evaluation in TRN-03.
 *
 * Scoring logic:
 * - Base score: 100 points
 * - Each wrong action choice incurs a 10-point penalty (resolved via version catalog)
 * - Stage completion is deduced directly from event stream when completedStages is omitted
 * - Grade EXCELLENT: 100 points (0 errors, 6/6 steps)
 * - Grade SATISFACTORY: 70-90 points (1-3 errors, 6/6 steps)
 * - Grade NEEDS_PRACTICE: < 70 points (> 3 errors or incomplete)
 */
export function evaluateTrainingSession(
  actions: TrainingActionRecord[],
  completedStages?: TrainingStage[],
  scenarioVersion: string = SCENARIO_VERSION,
): TrainingEvaluationResult {
  const errors = actions.filter((a) => !resolveActionRight(a, scenarioVersion)).length;
  const score = Math.max(0, BASE_SCORE - errors * PENALTY_PER_ERROR);
  const effectiveStages = completedStages ?? deriveCompletedStages(actions, scenarioVersion);
  const completedSteps = REQUIRED_STAGES.filter((s) => effectiveStages.includes(s)).length;
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
