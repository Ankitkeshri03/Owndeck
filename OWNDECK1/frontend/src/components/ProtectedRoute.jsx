import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { messageLibrary } from "../content/uxCopy";

const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <div className="p-10 text-center text-slate-300">{messageLibrary.loading.auth}</div>;
  return user ? children : <Navigate to="/login" replace />;
};

export default ProtectedRoute;
