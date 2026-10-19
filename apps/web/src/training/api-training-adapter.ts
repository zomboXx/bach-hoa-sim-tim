export interface TrainingActionInput {
  actionType: string;
  targetKey: string;
  choiceKey: string;
}

export interface TrainingSavedResult {
  totalScore: number;
  passed: boolean;
  grade: "EXCELLENT" | "SATISFACTORY" | "NEEDS_PRACTICE";
  errorCount: number;
  completedAt: string;
}

export interface TrainingHistoryItem {
  sessionId: string;
  scenarioCode: string;
  scenarioVersion: number;
  attemptNumber: number;
  totalScore: number;
  passed: boolean;
  completedAt: string;
}

interface Scenario {
  id: string;
  code: string;
  version: number;
}

interface SessionStart {
  id: string;
  nextActionSequence: number;
  status: string;
}

interface ActionResponse {
  nextActionSequence: number;
  status: string;
  result?: TrainingSavedResult;
}

export class TrainingApiAdapter {
  constructor(private readonly fetchApi: (path: string, init?: RequestInit) => Promise<Response>) {}

  async retailScenario(): Promise<Scenario> {
    const scenarios = await this.read<Scenario[]>("/api/v1/training/scenarios");
    const scenario = scenarios.find((item) => item.code === "SCN-RETAIL-01" && item.version === 1);
    if (!scenario) throw new Error("Kịch bản đào tạo chưa được bật trên máy chủ.");
    return scenario;
  }

  start(scenarioId: string, key: string): Promise<SessionStart> {
    return this.read("/api/v1/training/sessions", {
      method: "POST",
      headers: { "Content-Type": "application/json", "Idempotency-Key": key },
      body: JSON.stringify({ scenarioId }),
    });
  }

  action(sessionId: string, actionSequence: number, action: TrainingActionInput) {
    return this.read<ActionResponse>(`/api/v1/training/sessions/${sessionId}/actions`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ actionSequence, ...action }),
    });
  }

  abandon(sessionId: string) {
    return this.read<{ id: string; status: string }>(
      `/api/v1/training/sessions/${sessionId}/abandon`,
      { method: "POST" },
    );
  }

  async ownResults(): Promise<TrainingHistoryItem[]> {
    const page = await this.read<{ items: TrainingHistoryItem[] }>(
      "/api/v1/training/results?scope=SELF&size=20",
    );
    return page.items;
  }

  private async read<T>(path: string, init?: RequestInit): Promise<T> {
    let response: Response;
    try {
      response = await this.fetchApi(path, { ...init, cache: "no-store" });
    } catch {
      throw new Error("Mất kết nối với máy chủ đào tạo. Kết quả hiện chưa được lưu.");
    }
    if (!response.ok) {
      const error = (await response.json().catch(() => undefined)) as
        { code?: string; message?: string } | undefined;
      throw new Error(error?.message || `Máy chủ đào tạo trả lỗi ${response.status}.`);
    }
    return (await response.json()) as T;
  }
}

/** One run keeps its own idempotency key and sequence across response-loss retries. */
export class TrainingRun {
  private readonly startKey = crypto.randomUUID();
  private sessionId?: string;
  private nextSequence = 1;
  private pending: TrainingActionInput[] = [];
  private flushing?: Promise<void>;
  private failed = false;
  private completed = false;

  constructor(
    private readonly api: TrainingApiAdapter,
    private readonly update: (
      message: string,
      result?: TrainingSavedResult,
      failed?: boolean,
    ) => void,
  ) {}

  start(): Promise<void> {
    return this.flush();
  }

  append(action: TrainingActionInput): void {
    if (this.completed) return;
    this.pending.push(action);
    this.update("Đang lưu tiến độ đào tạo…");
    if (!this.failed) void this.flush();
  }

  retry(): Promise<void> {
    this.failed = false;
    return this.flush();
  }

  async abandon(): Promise<void> {
    if (this.flushing) await this.flushing;
    if (this.sessionId && !this.completed) await this.api.abandon(this.sessionId);
  }

  private flush(): Promise<void> {
    if (this.flushing) return this.flushing;
    this.flushing = this.flushPending()
      .catch((error: unknown) => {
        this.failed = true;
        this.update((error as Error).message, undefined, true);
      })
      .finally(() => {
        this.flushing = undefined;
        if (this.pending.length && !this.failed && !this.completed) void this.flush();
      });
    return this.flushing;
  }

  private async flushPending(): Promise<void> {
    if (!this.sessionId) {
      this.update("Đang mở phiên đào tạo…");
      const scenario = await this.api.retailScenario();
      const session = await this.api.start(scenario.id, this.startKey);
      this.sessionId = session.id;
      this.nextSequence = session.nextActionSequence;
    }
    while (this.pending.length && !this.completed) {
      const action = this.pending[0];
      const response = await this.api.action(this.sessionId, this.nextSequence, action);
      this.pending.shift();
      this.nextSequence = response.nextActionSequence;
      if (response.result) {
        this.completed = true;
        this.update("Kết quả đã lưu trên máy chủ.", response.result);
      }
    }
    if (!this.pending.length && !this.completed) this.update("Tiến độ đã lưu trên máy chủ.");
  }
}
