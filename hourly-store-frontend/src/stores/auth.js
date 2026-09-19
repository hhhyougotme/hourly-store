import { defineStore } from "pinia";
import { ref, computed } from "vue";
import client, { unwrap } from "@/api/client";

const TOKEN_KEY = "hourlystore_token";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || "");
  const userId = ref(Number(localStorage.getItem("hourlystore_userId")) || null);
  const nickname = ref(localStorage.getItem("hourlystore_nickname") || "");
  const role = ref(Number(localStorage.getItem("hourlystore_role")) || 0);

  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => role.value === 1);

  function persistSession(t, uid, name, r = 0) {
    token.value = t;
    userId.value = uid;
    nickname.value = name || "";
    role.value = r;
    if (t) localStorage.setItem(TOKEN_KEY, t);
    else localStorage.removeItem(TOKEN_KEY);
    if (uid != null) localStorage.setItem("hourlystore_userId", String(uid));
    else localStorage.removeItem("hourlystore_userId");
    if (name) localStorage.setItem("hourlystore_nickname", name);
    else localStorage.removeItem("hourlystore_nickname");
    localStorage.setItem("hourlystore_role", String(r));
  }

  async function login(account, password) {
    const data = await unwrap(client.post("/auth/login", { account, password }));
    persistSession(data.token, data.userId, data.nickname, data.role ?? 0);
    return data;
  }

  async function sendVerificationCode(payload) {
    return unwrap(client.post("/auth/verification-code", payload));
  }

  async function register(payload) {
    const data = await unwrap(client.post("/auth/register", payload));
    persistSession(data.token, data.userId, data.nickname, data.role ?? 0);
    return data;
  }

  function logout() {
    persistSession("", null, "", 0);
  }

  return {
    token,
    userId,
    nickname,
    role,
    isLoggedIn,
    isAdmin,
    login,
    sendVerificationCode,
    register,
    logout,
  };
});
