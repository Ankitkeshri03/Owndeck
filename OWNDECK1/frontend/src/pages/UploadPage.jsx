import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { FileText, Loader2, UploadCloud } from "lucide-react";
import CameraCapture from "../components/CameraCapture";
import api from "../services/api";
import { messageLibrary, uploadCopy } from "../content/uxCopy";
import { formatDate } from "../utils/date";

const MAX_FILE_BYTES = 10 * 1024 * 1024;
const ACCEPTED_MIME_TYPES = new Set([
  "image/jpeg",
  "image/png",
  "image/webp",
  "application/pdf",
]);

const initialManualDetails = {
  productName: "",
  titleDescription: "",
  quantity: 1,
  purchaseDate: "",
  warrantyDurationMonths: 12,
  expiryDate: "",
};

const UploadPage = () => {
  const [file, setFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState("");
  const [manualDetails, setManualDetails] = useState(initialManualDetails);
  const [uploadedProduct, setUploadedProduct] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const validateFile = (selectedFile) => {
    if (!selectedFile) return { ok: false, message: messageLibrary.errors.noFile };
    if (selectedFile.size > MAX_FILE_BYTES) {
      return { ok: false, message: messageLibrary.errors.fileTooLarge };
    }
    if (selectedFile.type && !ACCEPTED_MIME_TYPES.has(selectedFile.type)) {
      return { ok: false, message: messageLibrary.errors.invalidFileType };
    }
    return { ok: true, message: "" };
  };

  const onFileChange = (selectedFile) => {
    const validation = validateFile(selectedFile);
    if (!validation.ok) {
      setError(validation.message);
      setFile(null);
      setUploadedProduct(null);
      if (previewUrl) URL.revokeObjectURL(previewUrl);
      setPreviewUrl("");
      return;
    }

    setFile(selectedFile);
    setUploadedProduct(null);
    setError("");
    setSuccess(messageLibrary.success.invoiceSelected);

    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl(selectedFile.type.startsWith("image/") ? URL.createObjectURL(selectedFile) : "");
  };

  const onManualChange = (key, value) => {
    setManualDetails((prev) => ({ ...prev, [key]: value }));
  };

  const validateManualDetails = () => {
    if (Number(manualDetails.quantity) < 1) {
      return messageLibrary.errors.invalidQuantity;
    }
    if (Number(manualDetails.warrantyDurationMonths) < 0) {
      return messageLibrary.errors.invalidWarrantyMonths;
    }
    return "";
  };

  const submit = async (event) => {
    event.preventDefault();
    if (!file) {
      setError(messageLibrary.errors.noFile);
      return;
    }

    const validation = validateFile(file);
    if (!validation.ok) {
      setError(validation.message);
      return;
    }

    const manualValidationError = validateManualDetails();
    if (manualValidationError) {
      setError(manualValidationError);
      return;
    }

    const formData = new FormData();
    formData.append("file", file);
    Object.entries(manualDetails).forEach(([key, value]) => {
      if (value !== "" && value !== null && value !== undefined) {
        formData.append(key, value);
      }
    });

    setLoading(true);
    setError("");
    setSuccess("");

    try {
      const { data } = await api.post("/products/upload", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });
      setUploadedProduct(data);
      setSuccess(messageLibrary.success.invoiceUploaded);
    } catch (err) {
      setError(err.response?.data?.message || messageLibrary.errors.uploadFailed);
    } finally {
      setLoading(false);
    }
  };

  const reset = () => {
    if (previewUrl) URL.revokeObjectURL(previewUrl);
    setPreviewUrl("");
    setFile(null);
    setUploadedProduct(null);
    setManualDetails(initialManualDetails);
    setError("");
    setSuccess("");
  };

  return (
    <div className="mx-auto mt-6 max-w-7xl px-4 pb-10">
      <div className="hero-grid rounded-3xl border border-slate-800 bg-slate-950/75 p-6 shadow-sm">
        <h1 className="text-3xl font-bold tracking-tight text-slate-100 sm:text-4xl">{uploadCopy.pageTitle}</h1>
        <p className="mt-2 max-w-3xl text-sm text-slate-400">{uploadCopy.pageSubtitle}</p>
      </div>

      <div className="mt-6 grid gap-6 lg:grid-cols-5">
        <section className="panel p-5 lg:col-span-3">
          <h2 className="text-xl font-bold text-slate-100">{uploadCopy.sectionTitle}</h2>
          <p className="mt-1 text-sm text-slate-400">{uploadCopy.sectionSubtitle}</p>

          <form onSubmit={submit} className="mt-4 space-y-4">
            <label className="block rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm">
              <span className="inline-flex items-center gap-2 font-semibold text-slate-200">
                <FileText className="h-4 w-4" /> {uploadCopy.chooseFile}
              </span>
              <input
                type="file"
                accept="image/*,.pdf"
                onChange={(e) => onFileChange(e.target.files?.[0] || null)}
                className="mt-2 w-full"
              />
            </label>

            <label className="block rounded-xl border border-slate-800 bg-slate-950/60 p-3 text-sm">
              <span className="inline-flex items-center gap-2 font-semibold text-slate-200">
                <FileText className="h-4 w-4" /> {uploadCopy.takePhoto}
              </span>
              <input
                type="file"
                accept="image/*"
                capture="environment"
                onChange={(e) => onFileChange(e.target.files?.[0] || null)}
                className="mt-2 w-full"
              />
              <p className="mt-2 text-xs text-slate-500">{uploadCopy.takePhotoHint}</p>
            </label>

            <CameraCapture onCapture={onFileChange} disabled={loading} />

            {file && (
              <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-3 text-sm">
                <p className="font-semibold text-slate-200">{uploadCopy.selected}</p>
                <p className="mt-1 text-slate-400">
                  {file.name} - {(file.size / (1024 * 1024)).toFixed(2)} MB
                </p>
                {previewUrl && (
                  <img
                    src={previewUrl}
                    alt="Invoice preview"
                    className="mt-3 w-full rounded-lg border border-slate-800 object-contain"
                  />
                )}
                {file.type === "application/pdf" && (
                  <p className="mt-2 text-xs text-slate-500">{uploadCopy.pdfPreviewNote}</p>
                )}
              </div>
            )}

            <div className="rounded-xl border border-slate-800 bg-slate-950/70 p-3">
              <p className="text-sm font-semibold text-slate-200">{uploadCopy.manualTitle}</p>
              <div className="mt-3 grid gap-2 sm:grid-cols-2">
                <input
                  placeholder={uploadCopy.manualFields.productName}
                  value={manualDetails.productName}
                  onChange={(e) => onManualChange("productName", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                />
                <input
                  placeholder={uploadCopy.manualFields.description}
                  value={manualDetails.titleDescription}
                  onChange={(e) => onManualChange("titleDescription", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                />
                <input
                  type="number"
                  min="1"
                  placeholder={uploadCopy.manualFields.quantity}
                  value={manualDetails.quantity}
                  onChange={(e) => onManualChange("quantity", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                />
                <input
                  type="date"
                  value={manualDetails.purchaseDate}
                  onChange={(e) => onManualChange("purchaseDate", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                  title={uploadCopy.manualFields.purchaseDate}
                />
                <input
                  type="number"
                  min="0"
                  max="240"
                  placeholder={uploadCopy.manualFields.warrantyMonths}
                  value={manualDetails.warrantyDurationMonths}
                  onChange={(e) => onManualChange("warrantyDurationMonths", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                />
                <input
                  type="date"
                  value={manualDetails.expiryDate}
                  onChange={(e) => onManualChange("expiryDate", e.target.value)}
                  className="input-shell rounded-lg p-2 text-sm"
                  title={uploadCopy.manualFields.expiryDate}
                />
              </div>
            </div>

            {error && <p className="rounded border border-red-500/30 bg-red-500/15 p-2 text-sm text-red-200">{error}</p>}
            {success && <p className="rounded border border-emerald-500/30 bg-emerald-500/15 p-2 text-sm text-emerald-200">{success}</p>}

            <div className="flex flex-wrap gap-2">
              <button
                disabled={loading || !file}
                className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-coral to-orange-500 px-5 py-3 font-semibold text-white transition hover:opacity-90 disabled:opacity-60"
              >
                {loading && <Loader2 className="h-4 w-4 animate-spin" />}
                <UploadCloud className="h-4 w-4" />
                {loading ? uploadCopy.submitting : uploadCopy.submit}
              </button>
              <button
                type="button"
                onClick={reset}
                className="rounded-xl border border-slate-700 bg-slate-900 px-5 py-3 text-sm font-semibold text-slate-200"
              >
                {uploadCopy.reset}
              </button>
            </div>
          </form>
        </section>

        <section className="panel p-5 lg:col-span-2">
          <h2 className="text-xl font-bold text-slate-100">{uploadCopy.extractedTitle}</h2>
          {!uploadedProduct && (
            <div className="mt-4 rounded-xl border border-dashed border-slate-700 p-4 text-sm text-slate-500">
              {uploadCopy.extractedEmpty}
            </div>
          )}

          {uploadedProduct && (
            <div className="mt-4 space-y-2 rounded-xl border border-slate-800 bg-slate-950/70 p-4 text-sm text-slate-300">
              <p><strong>{uploadCopy.detailLabels.name}:</strong> {uploadedProduct.productName}</p>
              <p><strong>{uploadCopy.detailLabels.description}:</strong> {uploadedProduct.titleDescription || "N/A"}</p>
              <p><strong>{uploadCopy.detailLabels.quantity}:</strong> {uploadedProduct.quantity}</p>
              <p><strong>{uploadCopy.detailLabels.purchaseDate}:</strong> {formatDate(uploadedProduct.purchaseDate)}</p>
              <p><strong>{uploadCopy.detailLabels.expiryDate}:</strong> {formatDate(uploadedProduct.expiryDate)}</p>
              <p><strong>{uploadCopy.detailLabels.status}:</strong> {uploadedProduct.status}</p>
              <p><strong>{uploadCopy.detailLabels.daysRemaining}:</strong> {uploadedProduct.daysRemaining}</p>
              <div className="pt-2">
                <button
                  type="button"
                  onClick={() => navigate(`/products/${uploadedProduct.id}`)}
                  className="rounded-lg bg-ink px-4 py-2 text-xs font-semibold text-white"
                >
                  {uploadCopy.openDetail}
                </button>
              </div>
            </div>
          )}
        </section>
      </div>
    </div>
  );
};

export default UploadPage;
