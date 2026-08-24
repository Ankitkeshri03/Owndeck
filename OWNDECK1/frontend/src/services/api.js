import axios from "axios";
import { messageLibrary } from "../content/uxCopy";

const TOKEN_KEY = "wv_token";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api",
  timeout: 20000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.code === "ECONNABORTED") {
      error.message = messageLibrary.errors.timeout;
    } else if (!error.response) {
      error.message = messageLibrary.errors.network;
    } else if (error.response.status === 401) {
      // Token expired / invalid. Clear it and send user back to login.
      localStorage.removeItem(TOKEN_KEY);
      if (window.location.pathname !== "/login") {
        window.location.assign("/login");
      }
    } else if (!error.response.data?.message) {
      error.response.data = {
        ...error.response.data,
        message: messageLibrary.errors.generic,
      };
    }
    return Promise.reject(error);
  },
);

export default api;
