import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { navCopy } from "../content/uxCopy";

const Navbar = () => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="sticky top-0 z-30 border-b border-slate-800/80 bg-slate-950/85 backdrop-blur">
      <div className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3">
        <Link to="/" className="flex items-baseline gap-2 text-xl font-bold tracking-tight text-slate-100 sm:text-2xl">
          OwnDeck
          <span className="hidden text-xs font-semibold text-slate-400 sm:inline">{navCopy.brandTagline}</span>
        </Link>
        <nav className="flex items-center gap-2 text-sm sm:gap-4 sm:text-base">
          {user ? (
            <>
              <Link to="/" className="rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral">
                {navCopy.links.home}
              </Link>
              <Link
                to="/dashboard"
                className="rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral"
              >
                {navCopy.links.dashboard}
              </Link>
              <Link
                to="/upload"
                className="rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral"
              >
                {navCopy.links.upload}
              </Link>
              <a
                href="/dashboard#filters"
                className="hidden rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral sm:inline"
              >
                {navCopy.links.filters}
              </a>
              <div className="hidden text-sm text-slate-400 md:block">
                {navCopy.greetingPrefix},{" "}
                <span className="font-semibold text-slate-100">{user.name?.split(" ")?.[0] || "there"}</span>
              </div>
              <button
                onClick={() => {
                  logout();
                  navigate("/login");
                }}
                className="rounded-lg bg-gradient-to-r from-teal to-coral px-4 py-2 text-sm font-semibold text-white"
              >
                {navCopy.links.logout}
              </button>
            </>
          ) : (
            <>
              <a
                href="/#features"
                className="hidden rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral sm:inline"
              >
                {navCopy.links.features}
              </a>
              <a
                href="/#how"
                className="hidden rounded-lg px-2 py-1 font-medium text-slate-200 hover:bg-slate-900 hover:text-coral sm:inline"
              >
                {navCopy.links.howItWorks}
              </a>
              <Link to="/login" className="rounded-lg px-3 py-2 font-semibold text-slate-100 hover:bg-slate-900">
                {navCopy.links.login}
              </Link>
              <Link to="/signup" className="rounded-lg bg-gradient-to-r from-teal to-coral px-4 py-2 font-semibold text-white">
                {navCopy.links.signup}
              </Link>
            </>
          )}
        </nav>
      </div>
    </header>
  );
};

export default Navbar;
