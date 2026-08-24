/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#050816",
        coral: "#7c3aed",
        sand: "#0f172a",
        teal: "#2563eb"
      },
      boxShadow: {
        neon: "0 18px 45px rgba(37, 99, 235, 0.25)"
      }
    }
  },
  plugins: []
};
