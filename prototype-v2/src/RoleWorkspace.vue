<script setup lang="ts">
import { computed } from "vue";
import { roleNames, type User, type State, type Count } from "./api";
const props = defineProps<{
  route: string;
  user: User;
  state: State;
  busy: boolean;
}>();
defineEmits<{ go: [string]; approve: [Count] }>();
const money = (n: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(
    n,
  );
const revenue = computed(() =>
  props.state.invoices.reduce((s, i) => s + i.total, 0),
);
const requests = computed(() =>
  props.state.counts.filter((c) => c.status === "REVIEW"),
);
const roles = [
  [
    "Admin / Chủ cửa hàng",
    "Quyền cao nhất; cấu hình, báo cáo tổng và quản lý chuỗi theo định hướng.",
  ],
  [
    "Kế toán",
    "Dòng tiền, thu chi, công nợ và đối soát; không vận hành nhân sự hoặc duyệt thay quản lý.",
  ],
  [
    "Quản lý cửa hàng",
    "Chốt duyệt hủy hóa đơn, trả/hoàn tiền, chênh lệch kiểm kho và mở/đóng ca.",
  ],
  [
    "Sales / Cashier",
    "Lập đơn, quét/nhập mã vạch, tính tiền POS; xem hóa đơn của mình. Không tự duyệt ngoại lệ.",
  ],
  [
    "Warehouse",
    "Lập phiếu nhập, xuất, điều chuyển; không bán hàng hoặc phê duyệt điều chỉnh tồn.",
  ],
];
</script>
<template>
  <div class="role-space">
    <div class="page-head">
      <div>
        <p class="eyebrow">{{ roleNames[user.role] }} · CỬA HÀNG MẪU</p>
        <h1>
          {{
            route === "finance"
              ? "Không gian kế toán"
              : route === "approvals"
                ? "Mọi ngoại lệ, đúng người duyệt."
                : route === "system"
                  ? "Không gian chủ cửa hàng"
                  : "Chứng từ hàng hóa"
          }}
        </h1>
        <p>
          {{
            route === "approvals"
              ? "Nhân viên không được tự thực hiện thay các quyết định nhạy cảm."
              : "Phạm vi truy cập được kiểm tra tại máy chủ, không chỉ ở menu."
          }}
        </p>
      </div>
    </div>
    <template v-if="route === 'finance'">
      <div class="role-metrics">
        <article class="card">
          <small>DOANH SỐ HÓA ĐƠN ĐÃ GHI NHẬN</small>
          <h2>{{ money(revenue) }}</h2>
          <p>
            {{ state.invoices.length }} hóa đơn · Không phải lợi nhuận hay số dư
            két
          </p>
        </article>
        <article class="card">
          <small>DỮ LIỆU HIỆN CÓ</small>
          <h2>01 cửa hàng mẫu</h2>
          <p>Chưa có sổ thu chi, công nợ hoặc số liệu đối soát.</p>
        </article>
      </div>
      <button class="secondary" @click="$emit('go', 'invoices')">
        Tra cứu hóa đơn →
      </button>
      <section class="card scope-note">
        <h2>Phạm vi kế toán đang phát triển</h2>
        <p>
          Thu chi · Công nợ · Đối soát két tiền. Chưa có thao tác ghi sổ trong
          bản này; không dùng doanh số hóa đơn thay cho báo cáo tài chính.
        </p>
        <p>
          Kế toán không được tạo đơn bán, nhận hàng, sửa danh mục, duyệt kiểm kê
          hoặc can thiệp vận hành nhân sự.
        </p>
      </section>
    </template>
    <template v-else-if="route === 'approvals'">
      <section class="card scope-note">
        <h2>Chênh lệch kiểm kho · {{ requests.length }} phiếu chờ duyệt</h2>
        <p>
          Chỉ điều chỉnh tồn sau khi người có quyền duyệt. Nếu dữ liệu lô đã
          thay đổi, phiếu chuyển sang xung đột để kiểm lại.
        </p>
        <div v-if="!requests.length" class="empty">
          Chưa có phiếu cần duyệt.
        </div>
        <article v-for="c in requests" :key="c.id" class="approval-item">
          <div>
            <b
              >{{
                state.products.find((p) => p.id === c.productId)?.name ||
                c.productId
              }}
              · {{ c.batchId }}</b
            >
            <p>
              Hệ thống {{ c.expected }} → thực tế {{ c.actual }} ·
              {{ c.note || "Không có ghi chú" }}
            </p>
          </div>
          <button class="primary" :disabled="busy" @click="$emit('approve', c)">
            Duyệt điều chỉnh tồn
          </button>
        </article>
        <button class="secondary" @click="$emit('go', 'count')">
          Xem toàn bộ phiếu kiểm kê →
        </button>
      </section>
      <div class="role-metrics">
        <article
          v-for="label in [
            'Hủy hóa đơn',
            'Trả hàng / hoàn tiền',
            'Mở / đóng ca',
          ]"
          :key="label"
          class="card"
        >
          <span class="status amber">Chưa triển khai</span>
          <h2>{{ label }}</h2>
          <p>
            Yêu cầu nghiệp vụ đã ghi nhận: quản lý cửa hàng là chốt duyệt. Chưa
            có API thực thi; không tự thay đổi tiền hay chứng từ.
          </p>
        </article>
      </div>
    </template>
    <template v-else-if="route === 'warehouse_orders'">
      <div class="role-metrics">
        <article class="card">
          <span class="status">Đang hoạt động</span>
          <h2>Phiếu nhập hàng</h2>
          <p>Kiểm nhận, ghi số lượng và hạn dùng theo lô.</p>
          <button class="primary" @click="$emit('go', 'receive')">
            Tạo phiếu nhập →
          </button>
        </article>
        <article
          v-for="label in ['Phiếu xuất hàng', 'Phiếu điều chuyển']"
          :key="label"
          class="card"
        >
          <span class="status amber">Đang phát triển</span>
          <h2>{{ label }}</h2>
          <p>Nghiệp vụ thuộc nhân viên kho. Chưa mở ghi nhận trong bản này.</p>
          <button disabled>Chưa khả dụng</button>
        </article>
      </div>
    </template>
    <template v-else>
      <section class="card scope-note">
        <h2>5 vai trò · Quyền tách biệt</h2>
        <div v-for="r in roles" :key="r[0]" class="approval-item">
          <b>{{ r[0] }}</b>
          <p>{{ r[1] }}</p>
        </div>
      </section>
      <section class="card scope-note">
        <h2>Giới hạn triển khai hiện tại</h2>
        <p>
          Hiện vận hành một cửa hàng mẫu. Quản lý chuỗi, cấu hình dịch vụ và
          quản lý tài khoản chưa có chức năng ghi. Admin có quyền cao nhất trên
          các API đã triển khai; không phải SaaS đa doanh nghiệp hoàn chỉnh.
        </p>
      </section>
    </template>
    <aside class="role-training">
      <img src="/mentor.png" alt="Chị Linh" />
      <div>
        <b>Mới vào đội? Bắt đầu ở Chapter 0.</b>
        <p>Khám phá cửa hàng 2D, nhận biết khu vực và người cần nhờ hỗ trợ.</p>
      </div>
      <button class="secondary" @click="$emit('go', 'training')">
        Học cùng mentor ↗
      </button>
    </aside>
  </div>
</template>
<style scoped>
.role-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
  gap: 18px;
  margin-bottom: 22px;
}
.role-metrics .card,
.scope-note {
  padding: 24px;
}
.role-metrics small {
  font-size: 10px;
  letter-spacing: 1px;
  color: #9682a2;
}
.role-metrics h2 {
  margin: 18px 0;
  font-size: 25px;
}
.role-space p {
  font-size: 13px;
  line-height: 1.75;
  color: #8a7b95;
}
.scope-note {
  margin: 22px 0;
}
.scope-note h2 {
  font-size: 21px;
}
.approval-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 22px;
  padding: 18px 0;
  border-top: 1px solid #eee6f3;
}
.approval-item > b {
  min-width: 180px;
}
.role-training {
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 24px;
  border-radius: 14px;
  background: #eee5f6;
  margin-top: 28px;
}
.role-training img {
  width: 58px;
  height: 68px;
  border-radius: 50%;
  object-fit: cover;
  object-position: top;
}
.role-training button {
  margin-left: auto;
}
.role-training p {
  margin-bottom: 0;
}
@media (max-width: 650px) {
  .role-metrics {
    grid-template-columns: 1fr;
  }
  .approval-item {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
  .role-training {
    flex-wrap: wrap;
  }
  .role-training > div {
    flex: 1;
    min-width: 170px;
  }
  .role-training button {
    margin: 0;
  }
}
</style>
