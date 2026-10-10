import { sameStocktakeDraft, type StocktakeQueueItem } from "../domain/stocktake";

const databaseName = "simtim-stocktake-queue-v1";
const storeName = "operations";

type StocktakeQueueDraft = Omit<
  StocktakeQueueItem,
  "clientOperationId" | "countedAt" | "status" | "conflictReason"
>;

export interface StocktakeOperationQueue {
  list(): Promise<StocktakeQueueItem[]>;
  enqueue(draft: StocktakeQueueDraft): Promise<StocktakeQueueItem>;
  markConflict(clientOperationId: string, reason: string): Promise<void>;
  remove(clientOperationId: string): Promise<void>;
}

export class IndexedDbStocktakeOperationQueue implements StocktakeOperationQueue {
  private readonly database = openDatabase();

  async list(): Promise<StocktakeQueueItem[]> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const request = database.transaction(storeName).objectStore(storeName).getAll();
      request.onsuccess = () => {
        const operations = (request.result as StocktakeQueueItem[]).sort((left, right) =>
          right.countedAt.localeCompare(left.countedAt),
        );
        resolve(operations);
      };
      request.onerror = () => reject(request.error);
    });
  }

  async enqueue(draft: StocktakeQueueDraft): Promise<StocktakeQueueItem> {
    const existing = (await this.list()).find((item) => sameStocktakeDraft(item, draft));
    if (existing) return existing;

    const operation: StocktakeQueueItem = {
      ...draft,
      clientOperationId: crypto.randomUUID(),
      countedAt: new Date().toISOString(),
      status: "PENDING",
    };
    await this.put(operation);
    return operation;
  }

  async markConflict(clientOperationId: string, reason: string): Promise<void> {
    const operation = (await this.list()).find(
      (candidate) => candidate.clientOperationId === clientOperationId,
    );
    if (!operation) return;
    await this.put({ ...operation, status: "CONFLICT", conflictReason: reason });
  }

  async remove(clientOperationId: string): Promise<void> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(storeName, "readwrite");
      transaction.objectStore(storeName).delete(clientOperationId);
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  }

  private async put(operation: StocktakeQueueItem): Promise<void> {
    const database = await this.database;
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(storeName, "readwrite");
      transaction.objectStore(storeName).put(structuredClone(operation));
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  }
}

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(databaseName, 1);
    request.onupgradeneeded = () => {
      if (!request.result.objectStoreNames.contains(storeName)) {
        request.result.createObjectStore(storeName, { keyPath: "clientOperationId" });
      }
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
    request.onblocked = () =>
      reject(new Error("Kho thao tác kiểm kê đang được mở ở phiên trình duyệt khác."));
  });
}
