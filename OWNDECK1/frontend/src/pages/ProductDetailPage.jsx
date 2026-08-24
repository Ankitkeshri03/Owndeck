import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { Copy, Download, Loader2, WandSparkles } from "lucide-react";
import api from "../services/api";
import { messageLibrary, productCopy } from "../content/uxCopy";
import { formatDate } from "../utils/date";

const ProductDetailPage = () => {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [issueDescription, setIssueDescription] = useState("");
  const [complaint, setComplaint] = useState(null);
  const [generating, setGenerating] = useState(false);

  useEffect(() => {
    const loadProduct = async () => {
      setLoading(true);
      setError("");
      try {
        const { data } = await api.get(`/products/${id}`);
        setProduct(data);
      } catch (err) {
        setError(err.response?.data?.message || messageLibrary.errors.productLoadFailed);
      } finally {
        setLoading(false);
      }
    };

    loadProduct();
  }, [id]);

  const generateComplaint = async () => {
    if (!issueDescription.trim()) {
      setError(messageLibrary.errors.missingIssue);
      return;
    }

    setGenerating(true);
    setError("");
    try {
      const { data } = await api.post(`/ai/products/${id}/complaint`, {
        issueDescription: issueDescription.trim(),
      });
      setComplaint(data);
      setSuccess(messageLibrary.success.complaintGenerated);
    } catch (err) {
      setError(err.response?.data?.message || messageLibrary.errors.complaintFailed);
    } finally {
      setGenerating(false);
    }
  };

  const copyComplaint = async () => {
    if (!complaint) return;
    await navigator.clipboard.writeText(`${complaint.subject}\n\n${complaint.body}`);
    setSuccess(messageLibrary.success.complaintCopied);
  };

  if (loading) return <div className="p-8 text-sm text-slate-400">{productCopy.loading}</div>;
  if (!product) return <div className="p-8 text-sm text-red-300">{productCopy.notFound}</div>;

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      {error && <p className="mb-4 rounded border border-red-500/30 bg-red-500/15 p-3 text-sm text-red-200">{error}</p>}
      {success && <p className="mb-4 rounded border border-emerald-500/30 bg-emerald-500/15 p-3 text-sm text-emerald-200">{success}</p>}

      <div className="panel p-6">
        <h1 className="text-3xl font-bold text-slate-100">{product.productName}</h1>
        <p className="mt-2 inline-flex rounded-full border border-slate-700 bg-slate-900 px-3 py-1 text-xs font-semibold text-slate-300">
          {productCopy.statusLabel}: {product.status}
        </p>

        <div className="mt-4 grid gap-3 text-sm text-slate-300 md:grid-cols-2">
          <p>
            {productCopy.labels.description}: <strong>{product.titleDescription || "N/A"}</strong>
          </p>
          <p>
            {productCopy.labels.quantity}: <strong>{product.quantity}</strong>
          </p>
          <p>
            {productCopy.labels.purchaseDate}: <strong>{formatDate(product.purchaseDate)}</strong>
          </p>
          <p>
            {productCopy.labels.expiryDate}: <strong>{formatDate(product.expiryDate)}</strong>
          </p>
          <p>
            {productCopy.labels.warrantyDuration}: <strong>{product.warrantyDurationMonths} months</strong>
          </p>
          <p>
            {productCopy.labels.daysRemaining}: <strong>{product.daysRemaining}</strong>
          </p>
        </div>

        <div className="mt-4 flex flex-wrap gap-2">
          {product.invoiceUrl && (
            <a
              href={product.invoiceUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1 rounded-lg bg-teal px-4 py-2 text-sm font-semibold text-white"
            >
              <Download className="h-4 w-4" /> {productCopy.invoiceButtons.view}
            </a>
          )}
          <a
            href={`${import.meta.env.VITE_API_BASE_URL || "http://localhost:8081/api"}/products/${product.id}/invoice`}
            className="inline-flex items-center gap-1 rounded-lg bg-ink px-4 py-2 text-sm font-semibold text-white"
            target="_blank"
            rel="noreferrer"
          >
            <Download className="h-4 w-4" /> {productCopy.invoiceButtons.download}
          </a>
        </div>
      </div>

        <div className="mt-6 panel p-6">
        <h2 className="inline-flex items-center gap-2 text-xl font-bold text-slate-100">
          <WandSparkles className="h-5 w-5 text-coral" /> {productCopy.complaint.title}
        </h2>
        <p className="mt-1 text-sm text-slate-400">{productCopy.complaint.subtitle}</p>

        <label className="mt-4 block text-sm font-semibold text-slate-300">{productCopy.complaint.issueLabel}</label>
        <textarea
          rows="5"
          value={issueDescription}
          onChange={(e) => setIssueDescription(e.target.value)}
          placeholder="Example: I received the wrong product and want a replacement."
          className="input-shell mt-2 w-full rounded-lg p-3"
        />

        <div className="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            onClick={generateComplaint}
            disabled={generating}
            className="inline-flex items-center gap-2 rounded-lg bg-gradient-to-r from-coral to-teal px-4 py-2 font-semibold text-white disabled:opacity-60"
          >
            {generating && <Loader2 className="h-4 w-4 animate-spin" />}
            {generating ? productCopy.complaint.generating : productCopy.complaint.generate}
          </button>
          <button
            type="button"
            onClick={copyComplaint}
            disabled={!complaint}
            className="inline-flex items-center gap-2 rounded-lg bg-ink px-4 py-2 font-semibold text-white disabled:opacity-50"
          >
            <Copy className="h-4 w-4" /> {productCopy.complaint.copy}
          </button>
        </div>

        {complaint && (
          <div className="mt-4 space-y-2 rounded-lg border border-slate-800 bg-slate-950/70 p-4 text-sm text-slate-300">
            <p>
              <strong>{productCopy.complaint.outputSubject}:</strong> {complaint.subject}
            </p>
            <p className="whitespace-pre-wrap">
              <strong>{productCopy.complaint.outputBody}:</strong> {complaint.body}
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default ProductDetailPage;
