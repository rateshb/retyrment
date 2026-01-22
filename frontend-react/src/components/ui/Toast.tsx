import { useEffect, useState } from 'react';
import { CheckCircle, XCircle, AlertCircle, Info, X } from 'lucide-react';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

interface Toast {
  id: string;
  message: string;
  type: ToastType;
}

let toastId = 0;
const listeners: Set<(toasts: Toast[]) => void> = new Set();
let toasts: Toast[] = [];

const notify = () => {
  listeners.forEach(listener => listener([...toasts]));
};

export const toast = {
  show: (message: string, type: ToastType = 'info') => {
    const id = String(++toastId);
    toasts.push({ id, message, type });
    notify();
    
    // Auto remove after 5 seconds
    setTimeout(() => {
      toast.dismiss(id);
    }, 5000);
  },
  success: (message: string) => toast.show(message, 'success'),
  error: (message: string) => toast.show(message, 'error'),
  warning: (message: string) => toast.show(message, 'warning'),
  info: (message: string) => toast.show(message, 'info'),
  dismiss: (id: string) => {
    toasts = toasts.filter(t => t.id !== id);
    notify();
  },
};

export function ToastContainer() {
  const [currentToasts, setCurrentToasts] = useState<Toast[]>([]);

  useEffect(() => {
    listeners.add(setCurrentToasts);
    return () => {
      listeners.delete(setCurrentToasts);
    };
  }, []);

  const icons = {
    success: <CheckCircle className="text-success-500" size={20} />,
    error: <XCircle className="text-danger-500" size={20} />,
    warning: <AlertCircle className="text-warning-500" size={20} />,
    info: <Info className="text-primary-500" size={20} />,
  };

  const backgrounds = {
    success: 'bg-success-50 border-success-200',
    error: 'bg-danger-50 border-danger-200',
    warning: 'bg-warning-50 border-warning-200',
    info: 'bg-primary-50 border-primary-200',
  };

  return (
    <div className="fixed bottom-4 right-4 z-50 flex flex-col gap-2">
      {currentToasts.map(t => (
        <div
          key={t.id}
          className={`flex items-center gap-3 px-4 py-3 rounded-lg border shadow-lg animate-slide-in ${backgrounds[t.type]}`}
        >
          {icons[t.type]}
          <span className="text-slate-700">{t.message}</span>
          <button
            onClick={() => toast.dismiss(t.id)}
            className="text-slate-400 hover:text-slate-600 ml-2"
          >
            <X size={16} />
          </button>
        </div>
      ))}
    </div>
  );
}
