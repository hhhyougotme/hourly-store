<script setup>
import { ref, onMounted, computed } from "vue";
import { useRouter, RouterLink } from "vue-router";
import client, { unwrap } from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { formatDateTime, formatMoney } from "@/utils/format";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const auth = useAuthStore();

const merchant = ref(null);
const coupons = ref([]);
const products = ref([]);
const loading = ref(true);
const err = ref("");
const claimBusy = ref(null);
const claimMsg = ref("");
const productMsg = ref("");
const productBusy = ref(false);
const newProduct = ref({
  name: "",
  description: "",
  price: "",
  stock: "0",
  imageUrl: "",
});

const merchantIdNum = computed(() => Number(props.id));

async function load() {
  loading.value = true;
  err.value = "";
  try {
    merchant.value = await unwrap(client.get(`/merchants/${props.id}`));
    coupons.value = await unwrap(
      client.get("/coupons", {
        params: { merchantId: merchantIdNum.value, activeOnly: true },
      })
    );
  } catch (e) {
    err.value = e.message || "加载失败";
    loading.value = false;
    return;
  }
  try {
    products.value = await unwrap(
      client.get("/products", { params: { merchantId: merchantIdNum.value } })
    );
  } catch {
    products.value = [];
  } finally {
    loading.value = false;
  }
}

async function submitProduct() {
  if (!auth.isLoggedIn) {
    router.push({ name: "login", query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  productMsg.value = "";
  const price = Number(newProduct.value.price);
  const stock = Number(newProduct.value.stock);
  if (!newProduct.value.name.trim()) {
    productMsg.value = "请填写商品名称";
    return;
  }
  if (Number.isNaN(price) || price <= 0) {
    productMsg.value = "价格须大于 0";
    return;
  }
  if (Number.isNaN(stock) || stock < 0) {
    productMsg.value = "库存须为 ≥0 的整数";
    return;
  }
  productBusy.value = true;
  try {
    await unwrap(
      client.post("/products", {
        merchantId: merchantIdNum.value,
        name: newProduct.value.name.trim(),
        description: newProduct.value.description.trim() || undefined,
        price,
        stock: Math.floor(stock),
        imageUrl: newProduct.value.imageUrl.trim() || undefined,
      })
    );
    productMsg.value = "商品已上架";
    newProduct.value = { name: "", description: "", price: "", stock: "0", imageUrl: "" };
    await load();
  } catch (e) {
    productMsg.value = e.message || "添加失败";
  } finally {
    productBusy.value = false;
  }
}

async function claim(couponId) {
  if (!auth.isLoggedIn) {
    router.push({ name: "login", query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  claimMsg.value = "";
  claimBusy.value = couponId;
  try {
    await unwrap(client.post(`/coupons/${couponId}/claim`));
    claimMsg.value = "领取成功";
    await load();
  } catch (e) {
    claimMsg.value = e.message || "领取失败";
  } finally {
    claimBusy.value = null;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-inner">
    <button type="button" class="back muted" @click="router.push('/merchants')">← 返回商家列表</button>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="err" class="err-msg">{{ err }}</p>

    <template v-else-if="merchant">
      <header class="head card">
        <h1>{{ merchant.name }}</h1>
        <p class="muted">{{ merchant.address || "地址未填" }}</p>
        <section v-if="merchant.serviceDescription" class="service-desc">
          <h2 class="service-title">服务说明</h2>
          <p>{{ merchant.serviceDescription }}</p>
        </section>
        <div class="meta">
          <span v-if="merchant.score != null">评分 {{ merchant.score }}</span>
          <span v-if="merchant.averagePrice != null">人均 ¥{{ merchant.averagePrice }}</span>
        </div>
      </header>

      <h2 class="sec-title">店内商品</h2>
      <ul v-if="products.length" class="prod-list">
        <li v-for="p in products" :key="p.id" class="card prod">
          <RouterLink :to="`/products/${p.id}`" class="prod-link">
            <div>
              <strong>{{ p.name }}</strong>
              <p class="muted small">¥{{ formatMoney(p.price) }} · 库存 {{ p.stock }}</p>
            </div>
            <span class="chev">›</span>
          </RouterLink>
        </li>
      </ul>
      <p v-else class="muted">暂无在售商品</p>

      <div v-if="auth.isLoggedIn" class="card add-prod">
        <h3 class="add-title">上架商品（演示：任意登录用户可添加）</h3>
        <p v-if="productMsg" :class="productMsg.includes('上架') ? 'ok' : 'err-msg'">{{ productMsg }}</p>
        <div class="grid-form">
          <label>
            <span>名称</span>
            <input v-model="newProduct.name" class="input" type="text" />
          </label>
          <label>
            <span>价格</span>
            <input v-model="newProduct.price" class="input" type="text" inputmode="decimal" placeholder="如 19.9" />
          </label>
          <label>
            <span>库存</span>
            <input v-model="newProduct.stock" class="input" type="number" min="0" />
          </label>
          <label class="full">
            <span>简介（可选）</span>
            <input v-model="newProduct.description" class="input" type="text" />
          </label>
          <label class="full">
            <span>图片 URL（可选）</span>
            <input v-model="newProduct.imageUrl" class="input" type="url" placeholder="https://..." />
          </label>
        </div>
        <button type="button" class="btn btn-primary" :disabled="productBusy" @click="submitProduct">
          {{ productBusy ? "提交中…" : "提交上架" }}
        </button>
      </div>
      <p v-else class="muted login-hint">登录后可在此店铺上架商品（演示接口）</p>

      <h2 class="sec-title sec-gap">可领优惠券</h2>
      <p v-if="claimMsg" :class="claimMsg.includes('成功') ? 'ok' : 'err-msg'">{{ claimMsg }}</p>

      <ul v-if="coupons.length" class="coupon-list">
        <li v-for="c in coupons" :key="c.id" class="card coupon">
          <div class="c-body">
            <h3>{{ c.title }}</h3>
            <p class="muted small">
              库存 {{ c.stockRemain }} / {{ c.stockTotal }} ·
              {{ formatDateTime(c.beginTime) }} — {{ formatDateTime(c.endTime) }}
            </p>
          </div>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="claimBusy === c.id || (c.stockRemain != null && c.stockRemain <= 0)"
            @click="claim(c.id)"
          >
            {{
              c.stockRemain != null && c.stockRemain <= 0
                ? "已抢光"
                : claimBusy === c.id
                  ? "领取中…"
                  : "领取"
            }}
          </button>
        </li>
      </ul>
      <p v-else class="muted">当前没有可领取的券（需在有效期内且仍有库存）</p>
    </template>
  </div>
</template>

<style scoped>
.back {
  border: none;
  background: none;
  cursor: pointer;
  padding: 0 0 1rem;
  font-size: 0.9rem;
}
.head {
  padding: 1.25rem 1.5rem;
  margin-bottom: 1.25rem;
}
.head h1 {
  margin: 0 0 0.35rem;
  font-size: 1.35rem;
}
.service-desc {
  margin-top: 0.85rem;
  padding-top: 0.85rem;
  border-top: 1px solid var(--fm-border);
}
.service-title {
  margin: 0 0 0.35rem;
  font-size: 0.95rem;
  font-weight: 600;
}
.service-desc p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.5;
  color: var(--fm-text);
}
.meta {
  display: flex;
  gap: 1rem;
  margin-top: 0.75rem;
  font-size: 0.875rem;
  color: var(--fm-muted);
}
.sec-title {
  font-size: 1.1rem;
  margin: 0 0 0.75rem;
}
.coupon-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.coupon {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1rem 1.15rem;
}
.c-body h3 {
  margin: 0 0 0.35rem;
  font-size: 1rem;
}
.small {
  font-size: 0.8rem;
  margin: 0;
}
.ok {
  color: #2a7d4a;
  font-size: 0.875rem;
  margin-bottom: 0.5rem;
}
.prod-list {
  list-style: none;
  margin: 0 0 1.25rem;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.prod {
  padding: 0;
}
.prod-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  color: inherit;
  text-decoration: none;
}
.prod-link:hover {
  background: rgba(29, 53, 87, 0.04);
  text-decoration: none;
}
.chev {
  color: var(--fm-muted);
  font-size: 1.25rem;
}
.add-prod {
  padding: 1rem 1.15rem;
  margin-bottom: 1.5rem;
}
.add-title {
  margin: 0 0 0.75rem;
  font-size: 1rem;
}
.grid-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.65rem 1rem;
  margin-bottom: 0.75rem;
}
.grid-form label span {
  display: block;
  font-size: 0.8rem;
  font-weight: 600;
  margin-bottom: 0.25rem;
}
.grid-form .full {
  grid-column: 1 / -1;
}
.login-hint {
  margin-bottom: 1.25rem;
}
.sec-gap {
  margin-top: 0.25rem;
}
</style>
