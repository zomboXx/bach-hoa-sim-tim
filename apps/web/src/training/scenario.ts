// Training Scenario definitions and Evaluation Oracle (TRN-02 / TRN-03)
// Data isolation rule: No operational state, sales invoices, or inventory records are modified.

export const REQUIRED_CHECKOUT_SCAN_KEYS = ["scan_item_1", "scan_item_2"] as const;
export const REQUIRED_CHECKOUT_TENDER_KEY = "tender_3k_receipt" as const;

export class TrainingCatalogValidationError extends Error {
  constructor(
    public readonly code: "UNKNOWN_CATALOG_VERSION" | "MISSING_CHOICE_KEY" | "UNKNOWN_CHOICE_KEY",
    message: string,
  ) {
    super(message);
    this.name = "TrainingCatalogValidationError";
  }
}

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

export interface CatalogChoiceDefinition {
  choiceKey: string;
  stage: string;
  actionType?: string;
  targetKey?: string;
  isRight: boolean;
  textSnippet?: string;
  feedback?: string;
}

export type ScenarioChoiceDefinition = CatalogChoiceDefinition;

export interface TrainingCatalog {
  version: string;
  requiredStages: readonly string[];
  choicesByKey:
    ReadonlyMap<string, CatalogChoiceDefinition> | Record<string, CatalogChoiceDefinition>;
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

export interface TrainingActionEvent {
  choiceKey?: string;
  // Các trường dưới đây do client gửi lên sẽ bị BỎ QUA hoàn toàn trên đường authoritative:
  isRight?: boolean;
  stage?: string;
  actionType?: string;
  choiceText?: string;
  targetKey?: string;
  feedback?: string;
  sequence?: number;
  timestamp?: number;
  scenarioVersion?: string;
}

export type TrainingActionRecord = TrainingActionEvent & {
  sequence: number;
  stage: TrainingStage;
  actionType: TrainingActionType | string;
  choiceText: string;
  isRight: boolean;
  feedback: string;
  timestamp: number;
};

export interface EvaluateTrainingSessionInput {
  catalogVersion?: string;
  actions: readonly TrainingActionEvent[];
  // BỎ QUA trên đường authoritative:
  completedStages?: readonly string[];
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

export interface AuthoritativeEvaluationResult {
  score: number;
  passed: boolean;
  rating: "EXCELLENT" | "GOOD" | "NEEDS_IMPROVEMENT";
  completedStages: string[];
  rightCount: number;
  totalActions: number;
  // Trường tương thích giao diện PWA:
  errors: number;
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

export const REQUIRED_STAGES: readonly TrainingStage[] = [
  "greet",
  "milk",
  "checkout",
  "cooler",
  "quarantine",
  "mentor",
];

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

/**
 * Scenario Choice Mapping Table for SCN-RETAIL-01 (version 1.0.0).
 * Used by frontend and backend (TRN-03) to independently determine right/wrong choices
 * without trusting client-supplied boolean flags.
 */
export const SCENARIO_CHOICE_MAP_V1: Record<string, CatalogChoiceDefinition> = {
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

export const SCENARIO_CATALOG_V1: TrainingCatalog = {
  version: "1.0.0",
  requiredStages: REQUIRED_STAGES,
  choicesByKey: SCENARIO_CHOICE_MAP_V1,
};

export const SCENARIO_CATALOG_BY_VERSION: Record<string, TrainingCatalog> = {
  "1.0.0": SCENARIO_CATALOG_V1,
  v1: SCENARIO_CATALOG_V1,
};

function catalogFor(versionOrCatalog: TrainingCatalog | string): TrainingCatalog {
  if (typeof versionOrCatalog !== "string") return versionOrCatalog;
  const catalog = Object.hasOwn(SCENARIO_CATALOG_BY_VERSION, versionOrCatalog)
    ? SCENARIO_CATALOG_BY_VERSION[versionOrCatalog]
    : undefined;
  if (!catalog) {
    throw new TrainingCatalogValidationError(
      "UNKNOWN_CATALOG_VERSION",
      `Unsupported catalogVersion "${versionOrCatalog}".`,
    );
  }
  return catalog;
}

function getCatalogChoice(
  catalog: TrainingCatalog,
  choiceKey: string,
): CatalogChoiceDefinition | undefined {
  if (catalog.choicesByKey instanceof Map) {
    return catalog.choicesByKey.get(choiceKey);
  }
  return Object.hasOwn(catalog.choicesByKey, choiceKey)
    ? (catalog.choicesByKey as Record<string, CatalogChoiceDefinition>)[choiceKey]
    : undefined;
}

/**
 * Tra cứu và xác thực choiceKey từ catalog của server.
 * Tuyệt đối không fallback về dữ liệu tự khai của client.
 */
export function resolveCatalogChoiceAuthoritative(
  action: TrainingActionEvent,
  catalog: TrainingCatalog = SCENARIO_CATALOG_V1,
): CatalogChoiceDefinition {
  const rawKey = typeof action.choiceKey === "string" ? action.choiceKey.trim() : "";
  if (!rawKey) {
    throw new TrainingCatalogValidationError(
      "MISSING_CHOICE_KEY",
      "Action event is missing required choiceKey for authoritative evaluation.",
    );
  }

  const catalogChoice = getCatalogChoice(catalog, rawKey);
  if (!catalogChoice) {
    throw new TrainingCatalogValidationError(
      "UNKNOWN_CHOICE_KEY",
      `Unknown choiceKey "${rawKey}" in catalog version "${catalog.version}".`,
    );
  }

  return catalogChoice;
}

/**
 * Authoritative: Đúng/sai chỉ suy ra từ catalog trên server.
 * Không bao giờ rơi về action.isRight.
 */
export function resolveActionRight(
  action: TrainingActionEvent,
  catalogOrVersion: TrainingCatalog | string = SCENARIO_CATALOG_V1,
): boolean {
  const catalog = catalogFor(catalogOrVersion);
  const catalogChoice = resolveCatalogChoiceAuthoritative(action, catalog);
  return catalogChoice.isRight;
}

/**
 * Authoritative: Tiến độ hoàn thành các stage chỉ suy ra từ catalogChoice.
 * Riêng bước checkout yêu cầu đủ cả scan_item_1, scan_item_2 và tender_3k_receipt.
 */
export function deriveCompletedStages(
  actions: readonly TrainingActionEvent[],
  catalogOrVersion: TrainingCatalog | string = SCENARIO_CATALOG_V1,
): string[] {
  const catalog = catalogFor(catalogOrVersion);

  const completedStages: string[] = [];
  const scannedCheckoutKeys = new Set<string>();

  for (const action of actions) {
    const choice = resolveCatalogChoiceAuthoritative(action, catalog);
    const expectedStage = catalog.requiredStages[completedStages.length];
    if (!choice.isRight || choice.choiceKey === "hint_ack" || choice.stage !== expectedStage) {
      continue;
    }

    if (choice.stage === "checkout") {
      if (
        REQUIRED_CHECKOUT_SCAN_KEYS.includes(
          choice.choiceKey as (typeof REQUIRED_CHECKOUT_SCAN_KEYS)[number],
        )
      ) {
        scannedCheckoutKeys.add(choice.choiceKey);
      } else if (
        choice.choiceKey === REQUIRED_CHECKOUT_TENDER_KEY &&
        REQUIRED_CHECKOUT_SCAN_KEYS.every((reqKey) => scannedCheckoutKeys.has(reqKey))
      ) {
        completedStages.push("checkout");
      }
    } else {
      completedStages.push(choice.stage);
    }
  }

  return completedStages;
}

/**
 * Đường chấm Authoritative dùng cho TRN-03 / lưu kết quả trên server.
 * Bỏ qua hoàn toàn input.completedStages, action.isRight, action.stage, action.actionType hay choiceText.
 */
export function evaluateTrainingSession(
  inputOrActions: EvaluateTrainingSessionInput | readonly TrainingActionEvent[],
  catalogOrStages?: TrainingCatalog | readonly string[],
): AuthoritativeEvaluationResult {
  const input: EvaluateTrainingSessionInput =
    "actions" in inputOrActions
      ? (inputOrActions as EvaluateTrainingSessionInput)
      : { actions: inputOrActions };

  const catalog: TrainingCatalog =
    catalogOrStages && "choicesByKey" in catalogOrStages ? catalogOrStages : SCENARIO_CATALOG_V1;

  if (input.catalogVersion !== undefined && input.catalogVersion !== catalog.version) {
    throw new TrainingCatalogValidationError(
      "UNKNOWN_CATALOG_VERSION",
      `Unsupported catalogVersion "${input.catalogVersion}". Expected "${catalog.version}".`,
    );
  }

  if (!Array.isArray(input.actions) || input.actions.length === 0) {
    return {
      score: 0,
      passed: false,
      rating: "NEEDS_IMPROVEMENT" as const,
      completedStages: [] as string[],
      rightCount: 0,
      totalActions: 0,
      errors: 0,
      grade: "NEEDS_PRACTICE" as const,
      gradeLabel: "Cần luyện tập thêm",
      completedSteps: 0,
      totalRequiredSteps: catalog.requiredStages.length,
      summaryText: "0 lần được mentor hướng dẫn lại",
    };
  }

  // Xác thực toàn bộ chuỗi sự kiện với catalog trước khi tính điểm
  const resolvedChoices = input.actions.map((action) =>
    resolveCatalogChoiceAuthoritative(action, catalog),
  );

  const seenCorrect = new Set<string>();
  const scoreableChoices = resolvedChoices.filter((choice) => {
    if (choice.choiceKey === "hint_ack") return false;
    if (!choice.isRight) return true;
    if (seenCorrect.has(choice.choiceKey)) return false;
    seenCorrect.add(choice.choiceKey);
    return true;
  });
  const rightCount = scoreableChoices.filter((choice) => choice.isRight).length;
  const completedStages = deriveCompletedStages(input.actions, catalog);
  const allStagesCompleted = catalog.requiredStages.every((stage) =>
    completedStages.includes(stage),
  );

  const accuracyRatio = scoreableChoices.length ? rightCount / scoreableChoices.length : 0;
  const stageRatio =
    catalog.requiredStages.length > 0 ? completedStages.length / catalog.requiredStages.length : 0;

  // Điểm số và trạng thái pass phụ thuộc chặt vào việc hoàn thành đủ các bước từ catalog server
  const rawScore = Math.round(accuracyRatio * 100);
  const score = allStagesCompleted ? rawScore : Math.min(rawScore, Math.round(stageRatio * 80));
  const passed = allStagesCompleted && score >= 80;

  const rating =
    passed && score >= 95
      ? ("EXCELLENT" as const)
      : passed
        ? ("GOOD" as const)
        : ("NEEDS_IMPROVEMENT" as const);

  const errors = scoreableChoices.length - rightCount;
  const grade: TrainingGrade =
    rating === "EXCELLENT" ? "EXCELLENT" : rating === "GOOD" ? "SATISFACTORY" : "NEEDS_PRACTICE";
  const gradeLabel =
    grade === "EXCELLENT"
      ? "Xuất sắc (Đúng ngay từ lần đầu)"
      : grade === "SATISFACTORY"
        ? "Đạt yêu cầu"
        : "Cần luyện tập thêm";
  const summaryText =
    errors === 0 ? "Hoàn thành đúng ngay từ lần đầu" : `${errors} lần được mentor hướng dẫn lại`;

  return {
    score,
    passed,
    rating,
    completedStages,
    rightCount,
    totalActions: scoreableChoices.length,
    errors,
    grade,
    gradeLabel,
    completedSteps: completedStages.length,
    totalRequiredSteps: catalog.requiredStages.length,
    summaryText,
  };
}

/**
 * Hàm suy diễn tiến trình hoàn thành cho luồng mô phỏng trình duyệt legacy.
 */
function deriveLegacyCompletedStages(actions: TrainingActionRecord[]): TrainingStage[] {
  const completed: TrainingStage[] = [];
  let scanCount = 0;

  for (const a of actions) {
    const isRight =
      a.choiceKey && SCENARIO_CHOICE_MAP_V1[a.choiceKey]
        ? SCENARIO_CHOICE_MAP_V1[a.choiceKey].isRight
        : a.isRight;
    if (!isRight) continue;

    // Stage 1: greet
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

    // Stage 2: milk
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

    // Stage 3: checkout
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

    // Stage 4: cooler
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

    // Stage 5: quarantine
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

    // Stage 6: mentor
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
 * CHỈ DÙNG CHO LEGACY BROWSER PREVIEW (Không dùng để chấm/lưu kết quả trên server).
 * Giữ nguyên logic tính điểm trừ dần phục vụ trải nghiệm người học tại trình duyệt.
 */
export function evaluateLegacyBrowserSimulation(
  inputOrActions: EvaluateTrainingSessionInput | TrainingActionRecord[],
  completedStages?: TrainingStage[],
): TrainingEvaluationResult {
  const actions: TrainingActionRecord[] = Array.isArray(inputOrActions)
    ? inputOrActions
    : (inputOrActions.actions as TrainingActionRecord[]);

  const errors = actions.filter((a) => {
    if (a.choiceKey && SCENARIO_CHOICE_MAP_V1[a.choiceKey]) {
      return !SCENARIO_CHOICE_MAP_V1[a.choiceKey].isRight;
    }
    return !a.isRight;
  }).length;

  const score = Math.max(0, BASE_SCORE - errors * PENALTY_PER_ERROR);
  const effectiveStages = completedStages ?? deriveLegacyCompletedStages(actions);
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

/**
 * Alias cho evaluateLegacyBrowserSimulation phục vụ tương thích hợp đồng.
 */
export const evaluateLegacyBrowserTrainingSession = evaluateLegacyBrowserSimulation;
