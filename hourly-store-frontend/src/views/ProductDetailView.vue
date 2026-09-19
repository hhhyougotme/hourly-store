<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import client, { unwrap } from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { formatMoney } from "@/utils/format";

const props = defineProps({
  id: { type: String, required: true },
});

const router = useRouter();
const auth = useAuthStore();

const product = ref(null);
const loading = ref(true);
const err = ref("");
const quantity = ref(1);
const orderBusy = ref(false);
const orderMsg = ref("");

const maxQty = computed(() => {
  const s = product.value?.stock;
  return s != null && s > 0 ? s : 1;
});

const canBuy = computed(() => {
  const p = product.value;
  return p && p.status === 1 && (p.stock == null || p.stock > 0);
});

async function load() {
  loading.value = true;
  err.value = "";
  orderMsg.value = "";
  try {
    product.value = await unwrap(client.get(`/products/${props.id}`));
    quantity.value = 1;
  } catch (e) {
    err.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

async function placeOrder() {
  if (!auth.isLoggedIn) {
    router.push({ name: "login", query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  if (!canBuy.value) {
    orderMsg.value = "商品已售罄或下架";
    return;
  }
  const qty = Number(quantity.value);
  if (!Number.isInteger(qty) || qty < 1 || qty > maxQty.value) {
    orderMsg.value = `数量须为 1～${maxQty.value} 的整数`;
    return;
  }
  orderBusy.value = true;
  orderMsg.value = "";
  try {
    const res = await unwrap(
      client.post(`/products/${props.id}/orders`, { quantity: qty })
    );
    if (res.status === "SUCCESS") {
      orderMsg.value = "下单成功，可在「我的」查看订单";
      await load();
    } else {
      orderMsg.value = res.message || "下单失败";
    }
  } catch (e) {
    orderMsg.value = e.message || "下单失败";
  } finally {
    orderBusy.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-inner narrow">
    <button type="button" class="back muted" @click="router.back()">← 返回</button>

    <p v-if="loading" class="muted">加载中…</p>
    <p v-else-if="err" class="err-msg">{{ err }}</p>

    <article v-else-if="product" class="card detail">
      <div v-if="product.imageUrl" class="img-wrap">
        <img :src="product.imageUrl" :alt="product.name" />
      </div>
      <h1>{{ product.name }}</h1>
      <p class="price">¥{{ formatMoney(product.price) }}</p>
      <p v-if="product.description" class="desc">{{ product.description }}</p>
      <p class="muted small">库存 {{ product.stock ?? 0 }}</p>

      <section v-if="canBuy" class="buy-box">
        <label class="qty-label">
          <span>数量</span>
          <input
            v-model.number="quantity"
            class="input qty-input"
            type="number"
            min="1"
            :max="maxQty"
          />
        </label>
        <p class="line-total muted">
          合计 ¥{{ formatMoney((product.price || 0) * (quantity || 1)) }}
        </p>
        <button
          type="button"
          class="btn btn-primary btn-block"
          :disabled="orderBusy"
          @click="placeOrder"
        >
          {{ orderBusy ? "提交中…" : "立即购买" }}
        </button>
      </section>
      <p v-else class="err-msg">该商品已售罄或下架</p>

      <p v-if="orderMsg" :class="orderMsg.includes('成功') ? 'ok' : 'err-msg'">{{ orderMsg }}</p>

      <button type="button" class="btn btn-outline btn-block shop-link" @click="router.push(`/merchants/${product.merchantId}`)">
        进入店铺
      </button>
    </article>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 520px;
}
.back {
  border: none;
  background: none;
  cursor: pointer;
  padding: 0 0 1rem;
  font-size: 0.9rem;
}
.detail {
  padding: 1.5rem;
}
.img-wrap {
  margin: -1.5rem -1.5rem 1rem;
  max-height: 220px;
  overflow: hidden;
  background: var(--fm-bg);
}
.img-wrap img {
  width: 100%;
  display: block;
  object-fit: cover;
}
.detail h1 {
  margin: 0 0 0.5rem;
  font-size: 1.35rem;
}
.price {
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--fm-brand);
  margin: 0 0 0.75rem;
}
.desc {
  margin: 0 0 1rem;
  line-height: 1.55;
}
.small {
  font-size: 0.85rem;
  margin-bottom: 1rem;
}
.buy-box {
  margin: 1rem 0;
  padding-top: 1rem;
  border-top: 1px solid var(--fm-border);
}
.qty-label {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 0.5rem;
}
.qty-label span {
  font-weight: 600;
  font-size: 0.875rem;
}
.qty-input {
  width: 5rem;
}
.line-total {
  font-size: 0.9rem;
  margin: 0 0 0.75rem;
}
.btn-block {
  width: 100%;
  margin-bottom: 0.5rem;
}
.shop-link {
  margin-top: 0.5rem;
}
.ok {
  color: #2a7d4a;
  margin: 0.5rem 0 0;
}
</style>
