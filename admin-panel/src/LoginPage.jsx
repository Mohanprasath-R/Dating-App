import { useState } from 'react';
import { motion } from 'framer-motion';
import { Eye, EyeOff } from 'lucide-react';
import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../firebase-config';

function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleLogin = async (event) => {
    event.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (loginError) {
      setError(loginError.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-screen overflow-hidden bg-[#09090B] px-4 py-10 text-slate-100 flex items-center justify-center">
      <div className="relative w-full max-w-md">
        <div className="absolute inset-0 -z-10 rounded-[32px] bg-gradient-to-br from-primary/15 via-secondary/10 to-accent/10 blur-3xl" />
        <motion.div
          initial={{ opacity: 0, y: 18 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8, ease: 'easeOut' }}
          className="relative overflow-hidden rounded-[32px] border border-white/10 bg-slate-950/80 p-8 shadow-[0_30px_90px_rgba(15,23,42,0.35)] backdrop-blur-xl"
        >
          <div className="mb-8 space-y-4 text-center">
            <p className="text-xs uppercase tracking-[0.35em] text-pink-300/75">Admin portal</p>
            <h1 className="text-4xl font-semibold text-white">Simple sign in</h1>
            <p className="mx-auto max-w-xs text-sm leading-6 text-slate-400">
              A clean and stylish login experience for the admin dashboard.
            </p>
          </div>

          <form onSubmit={handleLogin} className="space-y-5">
            <div className="grid gap-2 text-left">
              <label className="text-sm font-medium text-slate-200">Email</label>
              <input
                className="rounded-[20px] border border-white/10 bg-slate-900/80 px-4 py-3 text-sm text-slate-100 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/30"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@example.com"
                required
              />
            </div>

            <div className="grid gap-2 text-left">
              <label className="text-sm font-medium text-slate-200">Password</label>
              <div className="relative">
                <input
                  className="w-full rounded-[20px] border border-white/10 bg-slate-900/80 pr-14 pl-4 py-3 text-sm text-slate-100 outline-none transition focus:border-primary focus:ring-2 focus:ring-primary/30"
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((prev) => !prev)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 inline-flex h-9 w-9 items-center justify-center rounded-full bg-white/5 text-slate-200 transition hover:bg-white/10"
                >
                  {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </div>

            {error && (
              <div className="rounded-3xl bg-red-500/10 px-4 py-3 text-sm text-red-200">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="inline-flex w-full items-center justify-center p-4 rounded-full bg-[rgb(255,77,141)] hover:bg-[rgba(255,77,141,0.28)] transition duration-300 hover:-translate-y-0.5 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <div className="mt-6 rounded-[24px] border border-white/10 bg-white/5 p-4 text-center text-sm text-slate-300">
            Secure access only. Keep your credentials private.
          </div>
        </motion.div>
      </div>
    </div>
  );
}

export default LoginPage;
