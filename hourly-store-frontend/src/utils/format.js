export function formatDateTime(isoOrLocal) {
  if (!isoOrLocal) return "—";
  const d = new Date(isoOrLocal);
  if (Number.isNaN(d.getTime())) return String(isoOrLocal);
  return d.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatMoney(n) {
  if (n == null || n === "") return "—";
  const x = Number(n);
  if (Number.isNaN(x)) return String(n);
  return x.toFixed(2);
}
