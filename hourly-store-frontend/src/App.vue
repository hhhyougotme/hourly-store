<script setup>
import { RouterLink, RouterView } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useRouter } from "vue-router";

const auth = useAuthStore();
const router = useRouter();

function onLogout() {
  auth.logout();
  router.push({ name: "home" });
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink to="/" class="logo">Hourly Store</RouterLink>
      <nav class="nav">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/merchants">商家</RouterLink>
        <RouterLink to="/flash-sales">闪购</RouterLink>
        <RouterLink v-if="auth.isLoggedIn" to="/profile">我的</RouterLink>
        <RouterLink v-if="auth.isAdmin" to="/admin/flash-sales">管理闪购</RouterLink>
        <template v-if="!auth.isLoggedIn">
          <RouterLink to="/login">登录</RouterLink>
          <RouterLink to="/register" class="nav-cta">注册</RouterLink>
        </template>
        <template v-else>
          <span class="hi muted">Hi，{{ auth.nickname || "用户" }}</span>
          <button type="button" class="btn btn-outline btn-sm" @click="onLogout">
            退出
          </button>
        </template>
      </nav>
    </div>
  </header>
  <main class="main">
    <RouterView />
  </main>
  <footer class="footer muted">
    <div class="topbar-inner">Hourly Store · Flash-sale & coupons demo</div>
  </footer>
</template>

<style scoped>
.topbar {
  background: var(--fm-surface);
  border-bottom: 1px solid var(--fm-border);
  position: sticky;
  top: 0;
  z-index: 50;
}
.topbar-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0.85rem 1.5rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
}
.logo {
  font-weight: 800;
  font-size: 1.35rem;
  color: var(--fm-brand);
  text-decoration: none;
}
.logo:hover {
  text-decoration: none;
  color: var(--fm-brand-dark);
}
.nav {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex-wrap: wrap;
}
.nav a {
  font-weight: 500;
  color: var(--fm-text);
}
.nav a.router-link-active {
  color: var(--fm-brand);
}
.nav-cta {
  padding: 0.35rem 0.75rem;
  background: var(--fm-accent);
  color: #fff !important;
  border-radius: 8px;
  text-decoration: none !important;
}
.nav-cta:hover {
  opacity: 0.92;
  text-decoration: none !important;
}
.hi {
  font-size: 0.9rem;
}
.btn-sm {
  padding: 0.35rem 0.75rem;
  font-size: 0.8rem;
}
.main {
  flex: 1;
}
.footer {
  padding: 1rem 0;
  text-align: center;
  border-top: 1px solid var(--fm-border);
  background: var(--fm-surface);
}
</style>
