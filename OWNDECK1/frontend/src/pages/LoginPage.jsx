import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { authCopy, messageLibrary } from "../content/uxCopy";

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const LoginPage = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    if (!form.email.trim()) return authCopy.validation.requiredEmail;
    if (!emailRegex.test(form.email.trim())) return authCopy.validation.invalidEmail;
    if (!form.password) return authCopy.validation.requiredPassword;
    return "";
  };

  const onSubmit = async (event) => {
    event.preventDefault();
    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      await login(form.email.trim(), form.password);
      navigate("/dashboard");
    } catch (err) {
      const fieldErrors = err.response?.data?.errors;
      const firstFieldError = fieldErrors ? Object.values(fieldErrors)[0] : "";
      setError(firstFieldError || err.response?.data?.message || err.message || messageLibrary.errors.authLoginFailed);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto mt-12 max-w-md panel p-8">
      <h1 className="mb-1 text-3xl font-bold text-slate-100">{authCopy.login.title}</h1>
      <p className="mb-6 text-sm text-slate-400">{authCopy.login.subtitle}</p>
      {error && <p className="mb-4 rounded border border-red-500/30 bg-red-500/15 p-2 text-sm text-red-200">{error}</p>}
      <form onSubmit={onSubmit} className="space-y-4">
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.login.emailLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.login.emailLabel}
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.login.passwordLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.login.passwordLabel}
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </label>
        <button disabled={isSubmitting} className="w-full rounded-lg bg-gradient-to-r from-coral to-teal px-4 py-3 font-semibold text-white disabled:opacity-60">
          {isSubmitting ? authCopy.login.loading : authCopy.login.submit}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-400">
        {authCopy.login.footerPrefix}{" "}
        <Link to="/signup" className="font-semibold text-teal">
          {authCopy.login.footerLink}
        </Link>
      </p>
    </div>
  );
};

export default LoginPage;
