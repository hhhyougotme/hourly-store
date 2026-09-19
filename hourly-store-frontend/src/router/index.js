import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const routes = [
  {
    path: "/",
    name: "home",
    component: () => import("@/views/HomeView.vue"),
  },
  {
    path: "/login",
    name: "login",
    component: () => import("@/views/LoginView.vue"),
    meta: { guestOnly: true },
  },
  {
    path: "/register",
    name: "register",
    component: () => import("@/views/RegisterView.vue"),
    meta: { guestOnly: true },
  },
  {
    path: "/merchants",
    name: "merchants",
    component: () => import("@/views/MerchantsView.vue"),
  },
  {
    path: "/merchants/:id",
    name: "merchant-detail",
    component: () => import("@/views/MerchantDetailView.vue"),
    props: true,
  },
  {
    path: "/products/:id",
    name: "product-detail",
    component: () => import("@/views/ProductDetailView.vue"),
    props: true,
  },
  {
    path: "/flash-sales",
    name: "flash-sales",
    component: () => import("@/views/FlashSalesView.vue"),
  },
  {
    path: "/profile",
    name: "profile",
    component: () => import("@/views/ProfileView.vue"),
    meta: { requiresAuth: true },
  },
  {
    path: "/admin/flash-sales",
    name: "admin-flash-sales",
    component: () => import("@/views/AdminFlashSalesView.vue"),
    meta: { requiresAuth: true, requiresAdmin: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { name: "home" };
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: "home" };
  }
  return true;
});

export default router;
