export type CatalogKind = "products" | "categories" | "units" | "suppliers";
export type CatalogStatus = "ACTIVE" | "INACTIVE";

export interface Category {
  id: string;
  parentId: string | null;
  code: string;
  name: string;
  status: CatalogStatus;
}

export interface Unit {
  id: string;
  code: string;
  name: string;
  precisionScale: number;
}

export interface Product {
  id: string;
  categoryId: string;
  baseUnitId: string;
  sku: string;
  name: string;
  salePrice: number | null;
  tracksExpiry: boolean;
  status: CatalogStatus;
  /** URL ảnh đại diện sản phẩm; undefined nếu chưa có ảnh. */
  imageUrl?: string | null;
}

export interface Supplier {
  id: string;
  code: string;
  name: string;
  phone: string | null;
  email: string | null;
  status: CatalogStatus;
}

export interface Barcode {
  id: string;
  barcode: string;
  isPrimary: boolean;
}

export interface CatalogRecords {
  products: Product;
  categories: Category;
  units: Unit;
  suppliers: Supplier;
}

export interface CatalogDrafts {
  products: Omit<Product, "id" | "imageUrl">;
  categories: Omit<Category, "id">;
  units: Omit<Unit, "id">;
  suppliers: Omit<Supplier, "id">;
}

type AuthorizedFetch = (path: string, init?: RequestInit) => Promise<Response>;

export class ApiCatalogAdapter {
  constructor(
    private readonly authorizedFetch: AuthorizedFetch,
    /**
     * Fetch không fire sự kiện logout khi nhận 401.
     * Dùng cho upload ảnh — khi endpoint chưa có trên server thì không đăng xuất.
     * Nếu không cung cấp, dùng authorizedFetch (giữ hành vi cũ).
     */
    private readonly silentFetch: AuthorizedFetch = authorizedFetch,
  ) {}

  list<K extends CatalogKind>(kind: K): Promise<CatalogRecords[K][]> {
    return this.request<CatalogRecords[K][]>(`/api/v1/${kind}`);
  }

  get<K extends CatalogKind>(kind: K, id: string): Promise<CatalogRecords[K]> {
    return this.request<CatalogRecords[K]>(`/api/v1/${kind}/${encodeURIComponent(id)}`);
  }

  save<K extends CatalogKind>(kind: K, id: string | undefined, draft: CatalogDrafts[K]) {
    return this.request<CatalogRecords[K]>(
      `/api/v1/${kind}${id ? `/${encodeURIComponent(id)}` : ""}`,
      { method: id ? "PUT" : "POST", body: JSON.stringify(draft) },
    );
  }

  remove(kind: CatalogKind, id: string): Promise<void> {
    return this.request<void>(`/api/v1/${kind}/${encodeURIComponent(id)}`, {
      method: "DELETE",
    });
  }

  listBarcodes(productId: string): Promise<Barcode[]> {
    return this.request<Barcode[]>(`/api/v1/products/${encodeURIComponent(productId)}/barcodes`);
  }

  getBarcode(productId: string, barcodeId: string): Promise<Barcode> {
    return this.request<Barcode>(
      `/api/v1/products/${encodeURIComponent(productId)}/barcodes/${encodeURIComponent(barcodeId)}`,
    );
  }

  saveBarcode(productId: string, barcodeId: string | undefined, draft: Omit<Barcode, "id">) {
    return this.request<Barcode>(
      `/api/v1/products/${encodeURIComponent(productId)}/barcodes${barcodeId ? `/${encodeURIComponent(barcodeId)}` : ""}`,
      { method: barcodeId ? "PUT" : "POST", body: JSON.stringify(draft) },
    );
  }

  removeBarcode(productId: string, barcodeId: string): Promise<void> {
    return this.request<void>(
      `/api/v1/products/${encodeURIComponent(productId)}/barcodes/${encodeURIComponent(barcodeId)}`,
      { method: "DELETE" },
    );
  }

  /**
   * Upload ảnh đại diện cho sản phẩm.
   * Gửi multipart/form-data — browser tự đặt Content-Type với boundary.
   * Trả về Product đã được cập nhật imageUrl.
   *
   * @param silentFetch - Nếu cung cấp, dùng fetch này thay vì authorizedFetch để
   *   tránh 401 gây ra sự kiện logout toàn cục (ví dụ khi endpoint chưa tồn tại trên server).
   */
  uploadImage(
    productId: string,
    file: File,
    silentFetch?: (path: string, init?: RequestInit) => Promise<Response>,
  ): Promise<Product> {
    const body = new FormData();
    body.append("image", file);
    const fetchFn = silentFetch ?? this.silentFetch;
    return this.requestMultipartWith<Product>(
      fetchFn,
      `/api/v1/products/${encodeURIComponent(productId)}/image`,
      { method: "PUT", body },
    );
  }

  /**
   * Xóa ảnh đại diện của sản phẩm.
   */
  deleteImage(productId: string): Promise<Product> {
    return this.request<Product>(`/api/v1/products/${encodeURIComponent(productId)}/image`, {
      method: "DELETE",
    });
  }

  private async request<T>(path: string, init: RequestInit = {}): Promise<T> {
    const response = await this.authorizedFetch(path, {
      ...init,
      cache: "no-store",
      headers: {
        ...(init.headers as Record<string, string> | undefined),
        "Content-Type": "application/json",
      },
    });
    return this.handleResponse<T>(response);
  }

  /** Gửi multipart/form-data (không đặt Content-Type để browser tự thêm boundary). */
  private async requestMultipart<T>(path: string, init: RequestInit = {}): Promise<T> {
    return this.requestMultipartWith<T>(this.authorizedFetch, path, init);
  }

  /** Giống requestMultipart nhưng dùng bất kỳ fetch function nào được truyền vào. */
  private async requestMultipartWith<T>(
    fetchFn: (path: string, init?: RequestInit) => Promise<Response>,
    path: string,
    init: RequestInit = {},
  ): Promise<T> {
    const response = await fetchFn(path, {
      ...init,
      cache: "no-store",
    });
    return this.handleResponse<T>(response);
  }

  private async handleResponse<T>(response: Response): Promise<T> {
    if (!response.ok) {
      let detail = `Yêu cầu không thành công (${response.status}).`;
      try {
        const body = (await response.json()) as {
          detail?: string;
          message?: string;
          errors?: Record<string, string>;
        };
        const fieldError = body.errors ? Object.entries(body.errors)[0] : undefined;
        detail = fieldError
          ? `${fieldError[0]}: ${fieldError[1]}`
          : body.detail || body.message || detail;
      } catch {
        // Keep the HTTP status for an empty or invalid error body.
      }
      throw new Error(detail);
    }
    if (response.status === 204) return undefined as T;
    return (await response.json()) as T;
  }
}
