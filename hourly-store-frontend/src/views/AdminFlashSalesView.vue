<script setup>
import { ref, onMounted } from "vue";
import client, { unwrap } from "@/api/client";

const merchants = ref([]);
const coupons = ref([]);
const products = ref([]);
const merchantId = ref("");
const form = ref({
  couponId: "",
  productId: "",
  title: "",
  stock: "50",
  beginTime: "",
  endTime: "",
  status: 1,
});
const msg = ref("");
const err = ref("");
const loading = ref(false);

function toIsoLocal(v) {
  if (!v) return v;
  return v.length === 16 ? `${v}:00` : v;
}

async function loadMerchants() {
  const page = await unwrap(client.get("/merchants", { params: { page: 1, pageSize: 50 } }));
  merchants.value = page.records ?? [];
}

async function loadCoupons() {
  if (!merchantId.value) {
    coupons.value = [];
    products.value = [];
    return;
  }
  coupons.value = await unwrap(
    client.get("/coupons", { params: { merchantId: merchantId.value } })
  );
  products.value = await unwrap(
    client.get("/products", { params: { merchantId: merchantId.value } })
  );
}

onMounted(loadMerchants);

async function submit() {
  err.value = "";
  msg.value = "";
  if (!form.value.couponId || !form.value.beginTime || !form.value.endTime) {
    err.value = "请填写优惠券与时间";
    return;
  }
  loading.value = true;
  try {
    const body = {
      couponId: Number(form.value.couponId),
      productId: form.value.productId ? Number(form.value.productId) : null,
      title: form.value.title || undefined,
      stock: Number(form.value.stock),
      beginTime: toIsoLocal(form.value.beginTime),
      endTime: toIsoLocal(form.value.endTime),
      status: Number(form.value.status),
    };
    await unwrap(client.post("/admin/flash-sales", body));
    msg.value = "闪购活动已创建";
  } catch (e) {
    err.value = e.message || "创建失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="page-inner narrow-wide">
    <h1 class="title">管理闪购活动</h1>
    <p class="muted sub">仅管理员可访问（需管理员账号登录）</p>

    <div class="card form-card">
      <form class="form" @submit.prevent="submit">
        <label>
          <span>商家</span>
          <select v-model="merchantId" class="input" @change="loadCoupons">
            <option value="">选择商家</option>
            <option v-for="m in merchants" :key="m.id" :value="String(m.id)">{{ m.name }}</option>
          </select>
        </label>
        <label>
          <span>优惠券</span>
          <select v-model="form.couponId" class="input" required>
            <option value="">选择优惠券</option>
            <option v-for="c in coupons" :key="c.id" :value="String(c.id)">{{ c.title }}</option>
          </select>
        </label>
        <label>
          <span>关联商品（可选）</span>
          <select v-model="form.productId" class="input">
            <option value="">不关联商品</option>
            <option v-for="p in products" :key="p.id" :value="String(p.id)">
              {{ p.name }}（¥{{ p.price }}）
            </option>
          </select>
        </label>
        <label>
          <span>活动标题</span>
          <input v-model="form.title" class="input" type="text" />
        </label>
        <label>
          <span>库存</span>
          <input v-model="form.stock" class="input" type="number" min="0" required />
        </label>
        <label>
          <span>开始时间</span>
          <input v-model="form.beginTime" class="input" type="datetime-local" required />
        </label>
        <label>
          <span>结束时间</span>
          <input v-model="form.endTime" class="input" type="datetime-local" required />
        </label>
        <p v-if="msg" class="ok">{{ msg }}</p>
        <p v-if="err" class="err-msg">{{ err }}</p>
        <button class="btn btn-primary" type="submit" :disabled="loading">
          {{ loading ? "提交中…" : "创建活动" }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.narrow-wide {
  max-width: 520px;
}
.title {
  margin: 0 0 0.25rem;
}
.sub {
  margin: 0 0 1rem;
}
.form-card {
  padding: 1.5rem;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
label span {
  display: block;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.35rem;
}
.ok {
  color: #2a7d4a;
}
</style>
