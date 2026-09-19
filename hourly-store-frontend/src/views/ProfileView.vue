<script setup>
import { ref, onMounted } from "vue";
import client, { unwrap } from "@/api/client";
import { formatDateTime, formatMoney } from "@/utils/format";

const tab = ref("orders");
const orders = ref([]);
const claims = ref([]);
const loading = ref(true);
const err = ref("");

async function loadOrders() {
  orders.value = await unwrap(client.get("/me/orders"));
}

async function loadClaims() {
  claims.value = await unwrap(client.get("/me/coupon-claims"));
}

async function load() {
  loading.value = true;
  err.value = "";
  try {
    await Promise.all([loadOrders(), loadClaims()]);
  } catch (e) {
    err.value = e.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-inner">
    <h1 class="title">我的</h1>
    <div class="tabs">
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'orders' }"
        @click="tab = 'orders'"
      >
        我的订单
      </button>
      <button
        type="button"
        class="tab"
        :class="{ active: tab === 'claims' }"
        @click="tab = 'claims'"
      >
        领券记录
      </button>
      <button type="button" class="btn btn-outline btn-sm" :disabled="loading" @click="load">
        刷新
      </button>
    </div>

    <p v-if="err" class="err-msg">{{ err }}</p>
    <p v-else-if="loading" class="muted">加载中…</p>

    <template v-else>
      <section v-show="tab === 'orders'">
        <ul v-if="orders.length" class="list">
          <li v-for="(o, index) in orders" :key="o.id" class="card row">
            <div>
              <strong>订单 #{{ orders.length - index }}</strong>
              <p class="muted small">
                <template v-if="o.flashSaleEventId">闪购活动 {{ o.flashSaleEventId }}</template>
                <template v-else>商品订单</template>
                · 商户 {{ o.merchantId }}
                <template v-if="o.productId"> · 商品 #{{ o.productId }}</template>
                · 状态 {{ o.status }}
              </p>
              <p class="muted small">金额 ¥{{ formatMoney(o.amount) }} · {{ formatDateTime(o.createTime) }}</p>
            </div>
          </li>
        </ul>
        <p v-else class="muted">暂无订单</p>
      </section>

      <section v-show="tab === 'claims'">
        <ul v-if="claims.length" class="list">
          <li v-for="c in claims" :key="c.id" class="card row">
            <div>
              <strong>优惠券 #{{ c.couponId }}</strong>
              <p class="muted small">状态 {{ c.status }} · {{ formatDateTime(c.claimedAt) }}</p>
            </div>
          </li>
        </ul>
        <p v-else class="muted">暂无领券记录</p>
      </section>
    </template>
  </div>
</template>

<style scoped>
.title {
  margin: 0 0 1rem;
  font-size: 1.5rem;
}
.tabs {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
  margin-bottom: 1rem;
}
.tab {
  border: none;
  background: var(--fm-border);
  padding: 0.45rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.tab.active {
  background: var(--fm-accent);
  color: #fff;
}
.btn-sm {
  margin-left: auto;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}
.row {
  padding: 1rem 1.1rem;
}
.small {
  font-size: 0.8rem;
  margin: 0.25rem 0 0;
}
</style>
