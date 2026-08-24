import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { authCopy, messageLibrary } from "../content/uxCopy";

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const SignupPage = () => {
  const { signup } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ name: "", email: "", password: "", phone: "" });
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const validate = () => {
    if (!form.name.trim()) return authCopy.validation.requiredName;
    if (!form.email.trim()) return authCopy.validation.requiredEmail;
    if (!emailRegex.test(form.email.trim())) return authCopy.validation.invalidEmail;
    if (!form.password) return authCopy.validation.requiredPassword;
    if (form.password.length < 6) return authCopy.validation.shortPassword;
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
      await signup({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password,
        phone: form.phone.trim(),
      });
      navigate("/dashboard");
    } catch (err) {
      const fieldErrors = err.response?.data?.errors;
      const firstFieldError = fieldErrors ? Object.values(fieldErrors)[0] : "";
      setError(firstFieldError || err.response?.data?.message || err.message || messageLibrary.errors.authSignupFailed);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="mx-auto mt-10 max-w-md panel p-8">
      <h1 className="mb-1 text-3xl font-bold text-slate-100">{authCopy.signup.title}</h1>
      <p className="mb-6 text-sm text-slate-400">{authCopy.signup.subtitle}</p>
      {error && <p className="mb-4 rounded border border-red-500/30 bg-red-500/15 p-2 text-sm text-red-200">{error}</p>}
      <form onSubmit={onSubmit} className="space-y-4">
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.signup.nameLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.signup.nameLabel}
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            required
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.signup.emailLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.signup.emailLabel}
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            required
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.signup.phoneLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.signup.phoneLabel}
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
          />
        </label>
        <label className="block">
          <span className="mb-1 block text-sm font-medium text-slate-300">{authCopy.signup.passwordLabel}</span>
          <input
            className="input-shell w-full rounded-lg p-3"
            placeholder={authCopy.signup.passwordLabel}
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            required
          />
        </label>
        <button disabled={isSubmitting} className="w-full rounded-lg bg-gradient-to-r from-coral to-teal px-4 py-3 font-semibold text-white disabled:opacity-60">
          {isSubmitting ? authCopy.signup.loading : authCopy.signup.submit}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-400">
        {authCopy.signup.footerPrefix}{" "}
        <Link to="/login" className="font-semibold text-teal">
          {authCopy.signup.footerLink}
        </Link>
      </p>
    </div>
  );
};

export default SignupPage;
