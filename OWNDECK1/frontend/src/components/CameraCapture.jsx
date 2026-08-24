import { useEffect, useRef, useState } from "react";
import { Camera, CameraOff, CheckCircle2, RefreshCcw, ScanLine } from "lucide-react";

const CameraCapture = ({ onCapture, disabled }) => {
  const [cameraOn, setCameraOn] = useState(false);
  const [starting, setStarting] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [previewUrl, setPreviewUrl] = useState("");
  const [capturedFileName, setCapturedFileName] = useState("");
  const videoRef = useRef(null);
  const streamRef = useRef(null);

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
      }
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const stopCamera = () => {
    if (streamRef.current) {
      streamRef.current.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    }
    setCameraOn(false);
  };

  const startCamera = async () => {
    if (disabled) return;
    setStarting(true);
    setCameraError("");

    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError("Camera is not available here. Use HTTPS or localhost, or try the Take Photo file option.");
      setStarting(false);
      return;
    }

    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: { ideal: "environment" },
          width: { ideal: 1280 },
          height: { ideal: 720 },
        },
        audio: false,
      });

      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
      setCameraOn(true);
    } catch (_err) {
      setCameraError("Camera access failed. Check browser camera permission, use HTTPS or localhost, or try the Take Photo file option.");
    } finally {
      setStarting(false);
    }
  };

  const capturePhoto = () => {
    if (!videoRef.current) return;

    const canvas = document.createElement("canvas");
    const width = videoRef.current.videoWidth || 1280;
    const height = videoRef.current.videoHeight || 720;

    canvas.width = width;
    canvas.height = height;

    const ctx = canvas.getContext("2d");
    ctx.drawImage(videoRef.current, 0, 0, width, height);

    canvas.toBlob(
      (blob) => {
        if (!blob) return;

        if (previewUrl) {
          URL.revokeObjectURL(previewUrl);
        }

        const capturedFile = new File([blob], `invoice-${Date.now()}.png`, {
          type: "image/png",
        });

        const nextPreview = URL.createObjectURL(blob);
        setCapturedFileName(capturedFile.name);
        setPreviewUrl(nextPreview);
        onCapture(capturedFile, nextPreview);
        stopCamera();
      },
      "image/png",
      0.92,
    );
  };

  const retake = () => {
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      setPreviewUrl("");
    }
    setCapturedFileName("");
    onCapture(null, "");
    startCamera();
  };

  return (
    <div className="panel p-4">
      <div className="mb-3 flex items-center justify-between">
        <p className="text-sm font-semibold text-slate-100">Capture Invoice</p>
        <ScanLine className="h-4 w-4 text-slate-500" />
      </div>

      <div className="mx-auto w-full max-w-[290px] overflow-hidden rounded-xl border border-slate-800 bg-black/90 shadow-inner">
        {cameraOn ? (
          <video ref={videoRef} autoPlay muted playsInline className="h-48 w-full object-cover" />
        ) : previewUrl ? (
          <img src={previewUrl} alt="Captured invoice" className="h-48 w-full object-contain bg-slate-950" />
        ) : (
          <div className="flex h-48 items-center justify-center text-center text-xs text-slate-300">
            Start camera to capture invoice
          </div>
        )}
      </div>

      {capturedFileName && (
        <p className="mt-2 text-xs text-slate-400">Captured: {capturedFileName}</p>
      )}

      <div className="mt-3 grid grid-cols-2 gap-2">
        <button
          type="button"
          onClick={startCamera}
          disabled={disabled || cameraOn || starting}
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-ink px-3 py-2 text-xs font-semibold text-white disabled:opacity-50"
        >
          <Camera className="h-3.5 w-3.5" /> {starting ? "Starting..." : "Start"}
        </button>
        <button
          type="button"
          onClick={capturePhoto}
          disabled={disabled || !cameraOn}
          className="inline-flex items-center justify-center gap-2 rounded-lg bg-teal px-3 py-2 text-xs font-semibold text-white disabled:opacity-50"
        >
          <CheckCircle2 className="h-3.5 w-3.5" /> Capture
        </button>
        <button
          type="button"
          onClick={retake}
          disabled={disabled}
          className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold text-slate-200 disabled:opacity-50"
        >
          <RefreshCcw className="h-3.5 w-3.5" /> Retake
        </button>
        <button
          type="button"
          onClick={stopCamera}
          disabled={disabled || !cameraOn}
          className="inline-flex items-center justify-center gap-2 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-semibold text-slate-200 disabled:opacity-50"
        >
          <CameraOff className="h-3.5 w-3.5" /> Stop
        </button>
      </div>

      {cameraError && (
        <p className="mt-3 rounded-lg border border-red-500/30 bg-red-500/15 p-2 text-xs text-red-200">{cameraError}</p>
      )}
    </div>
  );
};

export default CameraCapture;
