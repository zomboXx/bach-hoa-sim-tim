import { ref } from "vue";
import { SaleApiError } from "../domain/sale";

export interface SharedCart {
  id: string;
  version: number;
  generation: number;
  items: Array<{ productId: string; productName: string; quantity: string }>;
}
interface Adjustment {
  operationId: string;
  generation: number;
  productId: string;
  delta: string;
}
type FetchApi = (path: string, init?: RequestInit) => Promise<Response>;

/** Each request is authorized by the API; no bearer tokens or carts in shared browser storage. */
export function useSharedPosCart(
  fetchApi: FetchApi,
  onChange: (cart: SharedCart) => void,
  onError: (message: string) => void,
) {
  const ready = ref(false);
  const saving = ref(false);
  const error = ref("");
  const pending = ref<Adjustment>();
  const snapshot = ref<SharedCart>();
  let scope = "";
  let active = false;
  let revision = 0;
  let etag: string | undefined;
  let timer: ReturnType<typeof setTimeout> | undefined;
  let reading: AbortController | undefined;
  const key = () => "simtim-pos-adjustment:" + scope;

  function accept(cart: SharedCart) {
    if (snapshot.value && cart.version <= snapshot.value.version) return;
    snapshot.value = cart;
    onChange(cart);
  }

  async function failure(res: Response): Promise<never> {
    const body = (await res.json().catch(() => ({}))) as { code?: string; message?: string };
    throw new SaleApiError(
      body.code ?? "CART_ERROR",
      body.message ?? "Không đồng bộ được giỏ hàng.",
      res.status,
    );
  }

  async function refresh() {
    if (!active || reading) return;
    const captured = revision;
    const controller = new AbortController();
    reading = controller;
    const timeout = setTimeout(() => controller.abort(), 6000);
    try {
      const res = await fetchApi("/api/v1/sales/cart", {
        cache: "no-store",
        headers: etag ? { "If-None-Match": etag } : {},
        signal: controller.signal,
      });
      if (captured !== revision) return;
      if (res.status !== 304) {
        if (!res.ok) await failure(res);
        const cart = (await res.json()) as SharedCart;
        if (captured !== revision) return;
        if (!snapshot.value || cart.version >= snapshot.value.version) {
          accept(cart);
          etag = res.headers.get("ETag") ?? undefined;
        }
      }
      ready.value = true;
      if (!pending.value) error.value = "";
    } catch {
      if (captured === revision) {
        ready.value = false;
        error.value = "Mất kết nối giỏ chung. Đang thử kết nối lại…";
      }
    } finally {
      clearTimeout(timeout);
      if (reading === controller) reading = undefined;
    }
  }

  async function poll() {
    const captured = revision;
    await refresh();
    if (!active || captured !== revision) return;
    timer = setTimeout(() => void poll(), ready.value ? 500 : 2500);
  }

  function stop() {
    active = false;
    revision++;
    ready.value = false;
    clearTimeout(timer);
    reading?.abort();
    reading = undefined;
  }

  function start(nextScope: string) {
    if (active && nextScope === scope) return;
    stop();
    if (nextScope !== scope) {
      scope = nextScope;
      snapshot.value = undefined;
      pending.value = undefined;
      etag = undefined;
    }
    snapshot.value = undefined;
    etag = undefined;
    saving.value = false;
    error.value = "";
    try {
      const saved = sessionStorage.getItem(key());
      if (saved) pending.value = JSON.parse(saved) as Adjustment;
    } catch {
      sessionStorage.removeItem(key());
    }
    active = true;
    void poll();
  }

  async function send(adjustment: Adjustment) {
    if (!active || saving.value) return false;
    const captured = revision;
    saving.value = true;
    pending.value = adjustment;
    try {
      sessionStorage.setItem(key(), JSON.stringify(adjustment));
    } catch {
      saving.value = false;
      pending.value = undefined;
      error.value = "Hãy cho phép lưu trữ trong trình duyệt để thử lại thao tác an toàn.";
      onError(error.value);
      return false;
    }
    try {
      const res = await fetchApi("/api/v1/sales/cart/operations", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(adjustment),
        signal: AbortSignal.timeout(6000),
      });
      if (captured !== revision) return false;
      if (!res.ok) await failure(res);
      const cart = (await res.json()) as SharedCart;
      if (captured !== revision) return false;
      pending.value = undefined;
      sessionStorage.removeItem(key());
      error.value = "";
      accept(cart);
      ready.value = true;
      return true;
    } catch (e) {
      if (captured !== revision) return false;
      if (e instanceof SaleApiError && e.status >= 400 && e.status < 500) {
        pending.value = undefined;
        sessionStorage.removeItem(key());
        error.value = e.message;
        onError(e.message);
        await refresh();
      } else {
        error.value = "Chưa rõ thao tác đã lưu hay chưa. Bấm thử lại để dùng đúng mã thao tác.";
      }
      return false;
    } finally {
      if (captured === revision) saving.value = false;
    }
  }

  async function adjust(productId: string, delta: number) {
    if (!ready.value || !snapshot.value || pending.value || saving.value) return false;
    return send({
      operationId: crypto.randomUUID(),
      generation: snapshot.value.generation,
      productId,
      delta: String(delta),
    });
  }
  async function retry() {
    if (pending.value) await send(pending.value);
    else await refresh();
  }
  return { ready, saving, error, pending, snapshot, start, stop, refresh, adjust, retry };
}
