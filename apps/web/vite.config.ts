import { defineConfig, loadEnv } from "vite";
import vue from "@vitejs/plugin-vue";
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "API_PROXY_");
  const proxy = {
    "/api": {
      target: env.API_PROXY_TARGET || "http://127.0.0.1:8080",
      changeOrigin: true,
    },
  };
  return {
    plugins: [vue()],
    server: { port: 5174, proxy },
    preview: { port: 4174, proxy },
  };
});
