<script setup>
import { RouterLink } from "vue-router";
</script>

<template>
  <div class="page-inner">
    <section class="hero card">
      <div class="hero-text">
        <h1>附近好店 · 限时闪购</h1>
        <p class="muted">
          浏览商家与优惠券，参与闪购活动下单。登录后可领券、查看我的订单与领券记录。
        </p>
        <div class="cta">
          <RouterLink class="btn btn-primary" to="/merchants">逛商家</RouterLink>
          <RouterLink class="btn btn-outline" to="/flash-sales">看闪购</RouterLink>
        </div>
      </div>
    </section>
    <section class="grid">
      <RouterLink to="/merchants" class="tile card">
        <h3>商家</h3>
        <p class="muted">按分类筛选，查看地址与人均</p>
      </RouterLink>
      <RouterLink to="/flash-sales" class="tile card">
        <h3>闪购</h3>
        <p class="muted">进行中的活动，一键下单</p>
      </RouterLink>
      <RouterLink to="/profile" class="tile card">
        <h3>我的</h3>
        <p class="muted">订单与已领优惠券</p>
      </RouterLink>
    </section>
  </div>
</template>

<style scoped>
.hero {
  padding: 2rem 1.75rem;
  margin-bottom: 1.5rem;
  background: linear-gradient(135deg, #fff5f5 0%, #fff 50%, #f0f4fa 100%);
}
.hero h1 {
  margin: 0 0 0.75rem;
  font-size: 1.75rem;
  color: var(--fm-accent);
}
.cta {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
  margin-top: 1.25rem;
}
.cta .btn {
  text-decoration: none;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 1rem;
}
.tile {
  padding: 1.25rem;
  text-decoration: none;
  color: inherit;
  transition: transform 0.15s, box-shadow 0.15s;
}
.tile:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 28px rgba(26, 29, 36, 0.12);
  text-decoration: none;
}
.tile h3 {
  margin: 0 0 0.35rem;
  color: var(--fm-brand);
}
</style>
