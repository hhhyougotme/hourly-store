<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import client, { unwrap } from "@/api/client";
import { useAuthStore } from "@/stores/auth";
import { formatDateTime, formatMoney } from "@/utils/format";

const router = useRouter();
const auth = useAuthStore();

const events = ref([]);
const loading = ref(true);
const err = ref("");
const orderBusy = ref(null);
const orderMsg = ref("");
const amountInput = ref({});

async function load() {
  loading.value = true;
  err.value = "";
  try {
    events.value = await unwrap(client.get("/flash-sales"));
  } catch (e) {
    err.value = e.message || "加载失败";
    events.value = [];
  } finally {
    loading.value = false;
  }
}

async function placeOrder(eventId) {
  if (!auth.isLoggedIn) {
    router.push({ name: "login", query: { redirect: router.currentRoute.value.fullPath } });
    return;
  }
  orderMsg.value = "";
  const raw = amountInput[eventId];
  let body = undefined;
  if (raw != null && String(raw).trim() !== "") {
    const n = Number(raw);
    if (Number.isNaN(n) || n <= 0) {
      orderMsg.value = "金额请输入正数";
      return;
    }
    body = { amount: n };
  }
  orderBusy.value = eventId;
  try {
    const res = await unwrap(client.post(`/flash-sales/${eventId}/orders`, body));
    if (res.status === "SUCCESS") {
      orderMsg.value = "下单成功";
      await load();
    } else {
      orderMsg.value = res.message || "下单失败";
    }
  } catch (e) {
    orderMsg.value = e.message || "下单失败";
  } finally {
    orderBusy.value = null;
  }
}

onMounted(load);
</script>

<template>
  <div class="page-inner">
    <h1 class="title">闪购活动</h1>
    <p class="muted sub">进行中的场次；登录后可下单（同一活动每位用户限一单）</p>

    <p v-if="orderMsg" :class="orderMsg.includes('成功') ? 'ok' : 'err-msg'">{{ orderMsg }}</p>
    <p v-if="err" class="err-msg">{{ err }}</p>
    <p v-else-if="loading" class="muted">加载中…</p>

    <ul v-else class="list">
      <li v-for="ev in events" :key="ev.id" class="card ev">
        <div class="ev-top">
          <h3>{{ ev.title || "闪购" }}</h3>
          <span class="stock muted">库存 {{ ev.stock }}</span>
        </div>
        <p class="muted small">
          {{ formatDateTime(ev.beginTime) }} — {{ formatDateTime(ev.endTime) }}
        </p>
        <p v-if="ev.productName" class="prod-line">
          抢购商品：<strong>{{ ev.productName }}</strong>
          <span v-if="ev.productPrice != null" class="muted"> · 标价 ¥{{ formatMoney(ev.productPrice) }}</span>
        </p>
        <p v-else class="muted small">本场未关联商品（仅优惠券活动）</p>
        <p class="muted small">关联优惠券 ID：{{ ev.couponId }}</p>
        <div class="row">
          <label class="amt">
            <span class="muted small">实付金额（可选，默认后端规则）</span>
            <input
              v-model="amountInput[ev.id]"
              class="input input-sm"
              type="text"
              inputmode="decimal"
              placeholder="例如 1.00"
            />
          </label>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="orderBusy === ev.id || (ev.stock != null && ev.stock <= 0)"
            @click="placeOrder(ev.id)"
          >
            {{
              ev.stock != null && ev.stock <= 0
                ? "已售罄"
                : orderBusy === ev.id
                  ? "提交中…"
                  : "立即抢购"
            }}
          </button>
        </div>
      </li>
    </ul>

    <p v-if="!loading && !err && events.length === 0" class="muted">暂无进行中的闪购</p>
  </div>
</template>

<style scoped>
.title {
  margin: 0 0 0.25rem;
  font-size: 1.5rem;
}
.sub {
  margin: 0 0 1rem;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.ev {
  padding: 1.15rem 1.25rem;
}
.ev-top {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 0.75rem;
}
.ev-top h3 {
  margin: 0;
  font-size: 1.1rem;
}
.stock {
  font-size: 0.85rem;
  white-space: nowrap;
}
.small {
  font-size: 0.8rem;
  margin: 0.35rem 0 0;
}
.row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 0.75rem;
  margin-top: 1rem;
}
.amt {
  flex: 1;
  min-width: 180px;
}
.amt span {
  display: block;
  margin-bottom: 0.25rem;
}
.input-sm {
  padding: 0.45rem 0.65rem;
  font-size: 0.9rem;
}
.ok {
  color: #2a7d4a;
  margin-bottom: 0.5rem;
}
.prod-line {
  font-size: 0.9rem;
  margin: 0.35rem 0 0;
}
</style>
