import axios from "axios";
import { useAuthStore } from "@/stores/auth";

const client = axios.create({
  baseURL: "/api",
  timeout: 20000,
  headers: { "Content-Type": "application/json" },
});

client.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

/** 后端统一 Result：code === 0 成功 */
export async function unwrap(promise) {
  const { data } = await promise;
  if (data.code !== 0) {
    const err = new Error(data.message || "请求失败");
    err.code = data.code;
    throw err;
  }
  return data.data;
}

export default client;
