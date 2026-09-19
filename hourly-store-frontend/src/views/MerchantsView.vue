<script setup>
import { ref, onMounted, watch } from "vue";
import { RouterLink } from "vue-router";
import client, { unwrap } from "@/api/client";

const types = ref([]);
const merchants = ref([]);
const typeId = ref(null);
const page = ref(1);
const pageSize = 10;
const total = ref(0);
const totalPages = ref(0);
const loading = ref(true);
const err = ref("");

async function loadTypes() {
  types.value = await unwrap(client.get("/merchant-types"));
}

async function loadMerchants() {
  loading.value = true;
  err.value = "";
  try {
    const params = { page: page.value, pageSize };
    if (typeId.value != null) params.merchantTypeId = typeId.value;
    const data = await unwrap(client.get("/merchants", { params }));
    merchants.value = data.records ?? [];
    total.value = data.total ?? 0;
    totalPages.value = data.totalPages ?? 0;
  } catch (e) {
    err.value = e.message || "加载失败";
    merchants.value = [];
    total.value = 0;
    totalPages.value = 0;
  } finally {
    loading.value = false;
  }
}

function setType(id) {
  typeId.value = id;
  page.value = 1;
}

function goPage(p) {
  if (p < 1 || (totalPages.value > 0 && p > totalPages.value)) return;
  page.value = p;
  loadMerchants();
}

onMounted(async () => {
  try {
    await loadTypes();
  } catch (e) {
    err.value = e.message || "加载失败";
    loading.value = false;
    return;
  }
  await loadMerchants();
});

watch(typeId, () => loadMerchants());
</script>

<template>
  <div class="page-inner">
    <h1 class="title">商家</h1>
    <p class="muted sub">选择分类筛选，点击进入店铺领券</p>

    <div v-if="types.length" class="chips">
      <button
        type="button"
        class="chip"
        :class="{ active: typeId === null }"
        @click="setType(null)"
      >
        全部
      </button>
      <button
        v-for="t in types"
        :key="t.id"
        type="button"
        class="chip"
        :class="{ active: typeId === t.id }"
        @click="setType(t.id)"
      >
        {{ t.name }}
      </button>
    </div>

    <p v-if="err" class="err-msg">{{ err }}</p>
    <p v-else-if="loading" class="muted">加载中…</p>

    <ul v-else class="list">
      <li v-for="m in merchants" :key="m.id" class="card item">
        <RouterLink :to="`/merchants/${m.id}`" class="item-link">
          <div class="item-main">
            <h3>{{ m.name }}</h3>
            <p class="muted addr">{{ m.address || "地址未填" }}</p>
            <div class="meta">
              <span v-if="m.score != null">评分 {{ m.score }}</span>
              <span v-if="m.averagePrice != null">人均 ¥{{ m.averagePrice }}</span>
            </div>
          </div>
          <span class="chev">›</span>
        </RouterLink>
      </li>
    </ul>

    <p v-if="!loading && !err && merchants.length === 0" class="muted">该分类下暂无商家</p>

    <nav v-if="!loading && !err && totalPages > 1" class="pager" aria-label="分页">
      <button type="button" class="btn btn-outline btn-sm" :disabled="page <= 1" @click="goPage(page - 1)">
        上一页
      </button>
      <span class="pager-info muted">第 {{ page }} / {{ totalPages }} 页（共 {{ total }} 家）</span>
      <button
        type="button"
        class="btn btn-outline btn-sm"
        :disabled="page >= totalPages"
        @click="goPage(page + 1)"
      >
        下一页
      </button>
    </nav>
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
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-bottom: 1.25rem;
}
.chip {
  border: 1px solid var(--fm-border);
  background: var(--fm-surface);
  padding: 0.4rem 0.9rem;
  border-radius: 999px;
  font-size: 0.875rem;
  cursor: pointer;
}
.chip.active {
  background: var(--fm-accent);
  color: #fff;
  border-color: var(--fm-accent);
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.item {
  padding: 0;
}
.item-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.15rem;
  color: inherit;
  text-decoration: none;
}
.item-link:hover {
  background: rgba(29, 53, 87, 0.04);
  text-decoration: none;
}
.item h3 {
  margin: 0 0 0.35rem;
  font-size: 1.05rem;
}
.addr {
  margin: 0 0 0.5rem;
  font-size: 0.875rem;
}
.meta {
  display: flex;
  gap: 1rem;
  font-size: 0.8rem;
  color: var(--fm-muted);
}
.chev {
  font-size: 1.5rem;
  color: var(--fm-border);
  font-weight: 300;
}
.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  margin-top: 1.25rem;
  flex-wrap: wrap;
}
.pager-info {
  font-size: 0.875rem;
}
</style>
