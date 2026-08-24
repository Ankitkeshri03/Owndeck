import { Link } from "react-router-dom";
import {
  BellRing,
  FileText,
  LayoutDashboard,
  MessageSquareText,
  ScanText,
  ShieldCheck,
} from "lucide-react";
import { homeCopy } from "../content/uxCopy";

const benefitIcons = [ScanText, BellRing, MessageSquareText];
const featureIcons = [FileText, LayoutDashboard, ShieldCheck];
const featureIconClass = [
  "bg-violet-500/15 text-violet-200",
  "bg-blue-500/15 text-blue-200",
  "bg-indigo-500/15 text-indigo-200",
];

const HomePage = () => {
  return (
    <div className="mx-auto max-w-7xl px-4 py-14 sm:py-20">
      <section className="hero-grid rounded-3xl border border-slate-800 bg-slate-950/75 p-8 shadow-neon sm:p-12">
        <div className="grid gap-10 lg:grid-cols-[1.2fr_0.8fr] lg:items-center">
          <div>
            <p className="inline-flex rounded-full border border-violet-400/30 bg-violet-500/15 px-4 py-2 text-xs font-bold uppercase tracking-wider text-violet-200">
              {homeCopy.badge}
            </p>
            <h1 className="mt-5 text-4xl font-bold leading-tight text-slate-50 sm:text-6xl">{homeCopy.heroTitle}</h1>
            <p className="mt-6 max-w-2xl text-lg text-slate-300">{homeCopy.heroSubtitle}</p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link to="/signup" className="rounded-xl bg-gradient-to-r from-teal to-coral px-6 py-3 font-semibold text-white shadow-neon">
                {homeCopy.ctaPrimary}
              </Link>
              <Link to="/login" className="rounded-xl border border-slate-700 bg-slate-900/80 px-6 py-3 font-semibold text-slate-100 hover:bg-slate-800">
                {homeCopy.ctaSecondary}
              </Link>
              <a
                href="#features"
                className="rounded-xl border border-blue-400/30 bg-blue-500/10 px-6 py-3 font-semibold text-blue-200 hover:bg-blue-500/15"
              >
                {homeCopy.ctaTertiary}
              </a>
            </div>
          </div>

          <div className="panel p-6">
            <p className="text-sm font-semibold text-slate-200">{homeCopy.benefitsTitle}</p>
            <div className="mt-4 grid gap-3">
              {homeCopy.benefitCards.map((card, index) => {
                const Icon = benefitIcons[index];
                return (
                  <div key={card.title} className="flex items-start gap-3 rounded-xl border border-slate-800 bg-slate-950/65 p-4">
                    <div className="rounded-xl bg-violet-500/15 p-2 text-violet-300">
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="font-bold text-slate-100">{card.title}</p>
                      <p className="text-sm text-slate-400">{card.body}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </section>

      <section id="features" className="mt-10 sm:mt-14">
        <div className="mb-5 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="text-2xl font-bold text-slate-100 sm:text-3xl">{homeCopy.featuresHeading}</h2>
            <p className="mt-1 text-sm text-slate-400">{homeCopy.featuresSubheading}</p>
          </div>
          <Link to="/signup" className="rounded-xl bg-gradient-to-r from-coral to-teal px-5 py-2.5 text-sm font-semibold text-white">
            {homeCopy.featuresCta}
          </Link>
        </div>

        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {homeCopy.featurePanels.map((panel, index) => {
            const Icon = featureIcons[index];
            return (
              <div key={panel.title} className="panel p-5">
                <div className={`mb-3 inline-flex rounded-xl p-2 ${featureIconClass[index]}`}>
                  <Icon className="h-5 w-5" />
                </div>
                <p className="text-lg font-bold text-slate-100">{panel.title}</p>
                <p className="mt-1 text-sm text-slate-400">{panel.body}</p>
              </div>
            );
          })}
        </div>
      </section>

      <section id="how" className="mt-10 sm:mt-14">
        <div className="panel p-7 sm:p-8">
          <h2 className="text-2xl font-bold text-slate-100 sm:text-3xl">{homeCopy.howTitle}</h2>
          <p className="mt-2 max-w-3xl text-sm text-slate-400">{homeCopy.howSubtitle}</p>
          <div className="mt-6 grid gap-4 md:grid-cols-3">
            {homeCopy.howSteps.map((step, index) => (
              <div key={step.title} className="rounded-2xl border border-slate-800 bg-slate-950/70 p-5">
                <p className="text-xs font-bold uppercase tracking-wider text-slate-500">Step {index + 1}</p>
                <p className="mt-2 text-lg font-bold text-slate-100">{step.title}</p>
                <p className="mt-1 text-sm text-slate-400">{step.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      <section className="mt-10 sm:mt-14">
        <div className="rounded-3xl border border-slate-800 bg-slate-950/75 p-8 shadow-sm sm:p-10">
          <h2 className="text-2xl font-bold text-slate-100 sm:text-3xl">{homeCopy.footerTitle}</h2>
          <p className="mt-2 max-w-2xl text-sm text-slate-400">{homeCopy.footerSubtitle}</p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/signup" className="rounded-xl bg-gradient-to-r from-teal to-coral px-6 py-3 font-semibold text-white">
              {homeCopy.footerPrimary}
            </Link>
            <Link to="/login" className="rounded-xl border border-slate-700 bg-slate-900/80 px-6 py-3 font-semibold text-slate-100 hover:bg-slate-800">
              {homeCopy.footerSecondary}
            </Link>
          </div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
