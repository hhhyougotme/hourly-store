<script setup>
import { ref } from "vue";
import { useRouter, RouterLink } from "vue-router";
import { useAuthStore } from "@/stores/auth";

const auth = useAuthStore();
const router = useRouter();

const mode = ref("phone");
const phone = ref("");
const email = ref("");
const password = ref("");
const nickname = ref("");
const verificationCode = ref("");
const err = ref("");
const info = ref("");
const loading = ref(false);
const codeLoading = ref(false);

async function sendCode() {
  err.value = "";
  info.value = "";
  codeLoading.value = true;
  try {
    const payload =
      mode.value === "phone"
        ? { phone: phone.value.trim() }
        : { email: email.value.trim() };
    const res = await auth.sendVerificationCode(payload);
    info.value = res.demoCode
      ? `验证码已发送（演示环境验证码：${res.demoCode}，${res.expiresInMinutes} 分钟内有效）`
      : `验证码已发送，${res.expiresInMinutes} 分钟内有效`;
  } catch (e) {
    err.value = e.message || "发送失败";
  } finally {
    codeLoading.value = false;
  }
}

async function submit() {
  err.value = "";
  if (password.value.length < 6) {
    err.value = "密码至少 6 位";
    return;
  }
  if (!verificationCode.value.trim()) {
    err.value = "请输入验证码";
    return;
  }
  loading.value = true;
  try {
    const payload = {
      password: password.value,
      nickname: nickname.value.trim() || undefined,
      verificationCode: verificationCode.value.trim(),
    };
    if (mode.value === "phone") {
      payload.phone = phone.value.trim();
    } else {
      payload.email = email.value.trim();
    }
    await auth.register(payload);
    router.push("/");
  } catch (e) {
    err.value = e.message || "注册失败";
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="page-inner narrow">
    <div class="card form-card">
      <h2>注册</h2>
      <p class="muted">手机号或邮箱二选一，需验证码（存 Redis，有过期时间）</p>

      <div class="tabs">
        <button type="button" class="tab" :class="{ active: mode === 'phone' }" @click="mode = 'phone'">
          手机号
        </button>
        <button type="button" class="tab" :class="{ active: mode === 'email' }" @click="mode = 'email'">
          邮箱
        </button>
      </div>

      <form class="form" @submit.prevent="submit">
        <label v-if="mode === 'phone'">
          <span>手机号</span>
          <input v-model="phone" class="input" type="text" autocomplete="tel" required />
        </label>
        <label v-else>
          <span>邮箱</span>
          <input v-model="email" class="input" type="email" autocomplete="email" required />
        </label>
        <label>
          <span>验证码</span>
          <div class="code-row">
            <input v-model="verificationCode" class="input" type="text" maxlength="8" required />
            <button type="button" class="btn btn-outline" :disabled="codeLoading" @click="sendCode">
              {{ codeLoading ? "发送中…" : "获取验证码" }}
            </button>
          </div>
        </label>
        <label>
          <span>密码（6–64 位）</span>
          <input
            v-model="password"
            class="input"
            type="password"
            autocomplete="new-password"
            required
            minlength="6"
          />
        </label>
        <label>
          <span>昵称（可选）</span>
          <input v-model="nickname" class="input" type="text" maxlength="64" />
        </label>
        <p v-if="info" class="ok">{{ info }}</p>
        <p v-if="err" class="err-msg">{{ err }}</p>
        <button class="btn btn-primary full" type="submit" :disabled="loading">
          {{ loading ? "提交中…" : "注册并登录" }}
        </button>
      </form>
      <p class="foot muted">
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.narrow {
  max-width: 420px;
}
.form-card {
  padding: 1.75rem;
  margin-top: 1rem;
}
.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
}
.tab {
  flex: 1;
  padding: 0.45rem;
  border: 1px solid var(--fm-border);
  background: var(--fm-surface);
  border-radius: 8px;
  cursor: pointer;
}
.tab.active {
  background: var(--fm-accent);
  color: #fff;
  border-color: var(--fm-accent);
}
.code-row {
  display: flex;
  gap: 0.5rem;
}
.code-row .input {
  flex: 1;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 0.5rem;
}
label span {
  display: block;
  font-size: 0.875rem;
  font-weight: 600;
  margin-bottom: 0.35rem;
}
.full {
  width: 100%;
}
.foot {
  margin-top: 1.25rem;
  text-align: center;
}
.ok {
  color: #2a7d4a;
  font-size: 0.875rem;
  margin: 0;
}
</style>
