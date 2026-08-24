import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  AlertTriangle,
  Archive,
  Bell,
  CheckCircle2,
  Clock3,
  Plus,
  RefreshCcw,
  Search,
  X,
} from "lucide-react";
import api from "../services/api";
import { dashboardCopy, messageLibrary } from "../content/uxCopy";
import { formatDate } from "../utils/date";

const ALERT_POLL_MS = 60_000;

const badgeClass = {
  Active: "border border-emerald-500/20 bg-emerald-500/10 text-emerald-200",
  "Expiring Soon": "border border-amber-500/20 bg-amber-500/10 text-amber-200",
  Expired: "border border-red-500/20 bg-red-500/10 text-red-200",
};

const summaryCardClass = {
  total: "panel",
  active: "border border-emerald-500/20 bg-emerald-500/10",
  expiring: "border border-amber-500/20 bg-amber-500/10",
  expired: "border border-red-500/20 bg-red-500/10",
};

const getAlertStorageKey = (alert) =>
  `owndeck-alert:${alert.productId}:${alert.alertType}:${alert.expiryDate}`;

const buildAlertLine = (alert) =>
  dashboardCopy.alertsModal.line
    .replace("{message}", alert.message)
    .replace("{date}", formatDate(alert.expiryDate));

const DashboardPage = () => {
  const [products, setProducts] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [showAlerts, setShowAlerts] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [query, setQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [checkingAlerts, setCheckingAlerts] = useState(false);
  const [lastAlertSync, setLastAlertSync] = useState("");

  const markAlertHandled = (alert) => {
    if (typeof window === "undefined") return;
    localStorage.setItem(getAlertStorageKey(alert), new Date().toISOString());
  };

  const processAlerts = (nextAlerts, { forceOpen = false } = {}) => {
    setAlerts(nextAlerts);
    setLastAlertSync(new Date().toLocaleTimeString([], {
      hour: "2-digit",
      minute: "2-digit",
    }));

    if (typeof window === "undefined") {
      return;
    }

    const unseenAlerts = nextAlerts.filter((alert) => !localStorage.getItem(getAlertStorageKey(alert)));
    if (forceOpen && nextAlerts.length > 0) {
      setShowAlerts(true);
    } else if (unseenAlerts.length > 0) {
      setShowAlerts(true);
    }

    unseenAlerts.forEach((alert) => {
      markAlertHandled(alert);
    });
  };

  const fetchAlerts = async ({ silent = false } = {}) => {
    if (!silent) {
      setCheckingAlerts(true);
    }

    try {
      const { data } = await api.get("/products/alerts");
      processAlerts(data || [], { forceOpen: !silent });
    } catch (err) {
      if (!silent) {
        setError(err.response?.data?.message || messageLibrary.errors.dashboardLoadFailed);
      }
    } finally {
      if (!silent) {
        setCheckingAlerts(false);
      }
    }
  };

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError("");
      try {
        const [productsRes, alertsRes] = await Promise.all([
          api.get("/products"),
          api.get("/products/alerts"),
        ]);
        setProducts(productsRes.data || []);
        processAlerts(alertsRes.data || []);
      } catch (err) {
        setError(err.response?.data?.message || messageLibrary.errors.dashboardLoadFailed);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  useEffect(() => {
    const timer = window.setInterval(() => {
      fetchAlerts({ silent: true });
    }, ALERT_POLL_MS);

    return () => window.clearInterval(timer);
  }, []);

  const summary = useMemo(() => {
    const total = products.length;
    const active = products.filter((item) => item.status === "Active").length;
    const expiring = products.filter((item) => item.status === "Expiring Soon").length;
    const expired = products.filter((item) => item.status === "Expired").length;
    return { total, active, expiring, expired };
  }, [products]);

  const visibleProducts = useMemo(() => {
    const q = query.trim().toLowerCase();
    return products.filter((item) => {
      const matchesStatus = statusFilter === "all" || item.status === statusFilter;
      const matchesQuery =
        !q ||
        item.productName?.toLowerCase().includes(q) ||
        item.titleDescription?.toLowerCase().includes(q);
      return matchesStatus && matchesQuery;
    });
  }, [products, query, statusFilter]);

  if (loading) {
    return <div className="mx-auto max-w-6xl px-4 py-10 text-sm text-slate-400">{dashboardCopy.loading}</div>;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8">
      {showAlerts && (
        <div className="fixed inset-0 z-40 flex items-center justify-center bg-slate-950/70 p-4 backdrop-blur-sm">
          <div className="w-full max-w-xl rounded-2xl border border-slate-800 bg-slate-950 p-6 shadow-xl">
            <div className="mb-3 flex items-center justify-between">
              <h2 className="text-xl font-bold text-slate-100">{dashboardCopy.alertsModal.title}</h2>
              <button
                type="button"
                onClick={() => setShowAlerts(false)}
                className="rounded-full p-1 text-slate-400 hover:bg-slate-900"
                aria-label={dashboardCopy.alertsModal.close}
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="space-y-3">
              {alerts.length === 0 && (
                <div className="rounded-xl border border-slate-800 bg-slate-900 p-3 text-sm text-slate-300">
                  {dashboardCopy.alertsModal.empty}
                </div>
              )}
              {alerts.map((alert) => (
                <div
                  key={`${alert.productId}-${alert.alertType}`}
                  className="rounded-xl border border-blue-500/20 bg-blue-500/10 p-3"
                >
                  <p className="text-sm font-semibold text-blue-100">{alert.productName}</p>
                  <p className="text-sm text-slate-300">{buildAlertLine(alert)}</p>
                </div>
              ))}
            </div>
            <button
              type="button"
              onClick={() => setShowAlerts(false)}
              className="mt-4 rounded-lg bg-gradient-to-r from-teal to-coral px-4 py-2 text-sm font-semibold text-white"
            >
              {dashboardCopy.alertsModal.close}
            </button>
          </div>
        </div>
      )}

      <div className="mb-6 flex items-center justify-between gap-3">
        <div>
          <h1 className="text-3xl font-bold text-slate-100">{dashboardCopy.heading}</h1>
          <p className="mt-1 text-sm text-slate-400">{dashboardCopy.subheading}</p>
        </div>
        <Link
          to="/upload"
          className="inline-flex items-center gap-2 rounded-lg bg-gradient-to-r from-teal to-coral px-4 py-2 font-semibold text-white"
        >
          <Plus className="h-4 w-4" /> {dashboardCopy.uploadCta}
        </Link>
      </div>

      <div className="mb-6 grid gap-4 lg:grid-cols-[1.3fr_0.7fr]">
        <div className="panel p-5">
          <div className="flex items-start justify-between gap-4">
            <div>
              <p className="text-base font-bold text-slate-100">{dashboardCopy.alertsPanel.title}</p>
              <p className="mt-1 text-sm text-slate-400">{dashboardCopy.alertsPanel.subtitle}</p>
            </div>
            <span className="inline-flex items-center gap-2 rounded-full border border-violet-500/20 bg-violet-500/10 px-3 py-1 text-xs font-semibold text-violet-200">
              <Bell className="h-3.5 w-3.5" />
              {dashboardCopy.alertsPanel.activeBadge.replace("{count}", String(alerts.length))}
            </span>
          </div>

          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              onClick={() => fetchAlerts()}
              disabled={checkingAlerts}
              className="inline-flex items-center gap-2 rounded-xl border border-slate-700 bg-slate-900 px-4 py-2 text-sm font-semibold text-slate-200 disabled:opacity-60"
            >
              <RefreshCcw className={`h-4 w-4 ${checkingAlerts ? "animate-spin" : ""}`} />
              {dashboardCopy.alertsPanel.refresh}
            </button>
          </div>

          <div className="mt-3 space-y-2 text-sm">
            <p className="text-emerald-200">{dashboardCopy.alertsPanel.enabled}</p>
            <p className="text-slate-500">{dashboardCopy.alertsPanel.pollNote}</p>
            {lastAlertSync && <p className="text-slate-500">Last alert check: {lastAlertSync}</p>}
          </div>
        </div>

        <div className="panel p-5">
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-slate-500">Welcome back</p>
          <p className="mt-2 text-2xl font-bold text-slate-100">Reminder-focused warranty tracking</p>
          <p className="mt-2 text-sm text-slate-400">
            OwnDeck shows reminder popups directly inside the dashboard, so warranty alerts stay in the browser and
            never rely on email delivery.
          </p>
        </div>
      </div>

      {error && <p className="mb-4 rounded-lg border border-red-500/30 bg-red-500/15 p-3 text-sm text-red-200">{error}</p>}

      <div className="mb-6 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <div className={`${summaryCardClass.total} p-4`}>
          <Archive className="h-4 w-4 text-slate-500" />
          <p className="text-xs uppercase tracking-wide text-slate-500">{dashboardCopy.summary.total}</p>
          <p className="mt-2 text-2xl font-bold text-slate-100">{summary.total}</p>
        </div>
        <div className={`${summaryCardClass.active} rounded-xl p-4 shadow-sm`}>
          <CheckCircle2 className="h-4 w-4 text-emerald-200" />
          <p className="text-xs uppercase tracking-wide text-emerald-200">{dashboardCopy.summary.active}</p>
          <p className="mt-2 text-2xl font-bold text-emerald-100">{summary.active}</p>
        </div>
        <div className={`${summaryCardClass.expiring} rounded-xl p-4 shadow-sm`}>
          <Clock3 className="h-4 w-4 text-amber-200" />
          <p className="text-xs uppercase tracking-wide text-amber-200">{dashboardCopy.summary.expiringSoon}</p>
          <p className="mt-2 text-2xl font-bold text-amber-100">{summary.expiring}</p>
        </div>
        <div className={`${summaryCardClass.expired} rounded-xl p-4 shadow-sm`}>
          <AlertTriangle className="h-4 w-4 text-red-200" />
          <p className="text-xs uppercase tracking-wide text-red-200">{dashboardCopy.summary.expired}</p>
          <p className="mt-2 text-2xl font-bold text-red-100">{summary.expired}</p>
        </div>
      </div>

      <div id="filters" className="panel mb-6 grid gap-3 p-4 md:grid-cols-[1fr_auto_auto]">
        <label className="inline-flex items-center gap-2 rounded-xl border border-slate-800 bg-slate-950/70 px-3">
          <Search className="h-4 w-4 text-slate-500" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder={dashboardCopy.filters.searchPlaceholder}
            className="w-full bg-transparent py-2 text-sm text-slate-100 outline-none placeholder:text-slate-500"
          />
        </label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="input-shell rounded-xl px-3 py-2 text-sm"
        >
          <option value="all">{dashboardCopy.filters.allStatus}</option>
          <option value="Active">{dashboardCopy.filters.active}</option>
          <option value="Expiring Soon">{dashboardCopy.filters.expiringSoon}</option>
          <option value="Expired">{dashboardCopy.filters.expired}</option>
        </select>
        <button
          type="button"
          onClick={() => setStatusFilter("Expiring Soon")}
          className="rounded-xl border border-amber-500/20 bg-amber-500/10 px-3 py-2 text-sm font-semibold text-amber-200"
        >
          {dashboardCopy.filters.quickFilter}
        </button>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {visibleProducts.map((product, index) => (
          <Link
            key={product.id}
            to={`/products/${product.id}`}
            className="stagger-in rounded-2xl border border-slate-800 bg-slate-950/75 p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-neon"
            style={{ animationDelay: `${Math.min(index * 30, 220)}ms` }}
          >
            <div className="mb-2 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-100">{product.productName}</h2>
              <span className={`rounded-full px-3 py-1 text-xs font-semibold ${badgeClass[product.status] || "border border-slate-700 bg-slate-900 text-slate-300"}`}>
                {product.status}
              </span>
            </div>
            <p className="text-sm text-slate-400">{dashboardCopy.card.description}: {product.titleDescription || "N/A"}</p>
            <p className="text-sm text-slate-400">{dashboardCopy.card.quantity}: {product.quantity}</p>
            <p className="text-sm text-slate-400">{dashboardCopy.card.purchaseDate}: {formatDate(product.purchaseDate)}</p>
            <p className="text-sm text-slate-400">{dashboardCopy.card.expiryDate}: {formatDate(product.expiryDate)}</p>
            <p className="mt-2 text-sm font-semibold text-blue-200">{dashboardCopy.card.daysRemaining}: {product.daysRemaining}</p>
          </Link>
        ))}
      </div>

      {visibleProducts.length === 0 && (
        <div className="mt-10 rounded-xl border border-dashed border-slate-700 bg-slate-950/55 p-8 text-center text-slate-300">
          {products.length === 0 ? messageLibrary.empty.dashboard : dashboardCopy.emptyState}
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
