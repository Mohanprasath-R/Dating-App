import { useEffect, useState } from 'react';
import { NavLink, Route, Routes } from 'react-router-dom';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '../firebase-config';
import DashboardPage from './routes/DashboardPage';
import UsersPage from './routes/UsersPage';
import ConversationPage from './routes/ConversationPage';
import ProfilePage from './routes/ProfilePage';
import RelationshipPage from './routes/RelationshipPage';
import LoginPage from './LoginPage';

function App() {
  const [currentUser, setCurrentUser] = useState(null);
  const [authLoading, setAuthLoading] = useState(true);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      setCurrentUser(user);
      setAuthLoading(false);
    }, () => {
      setAuthLoading(false);
    });

    return () => unsubscribe();
  }, []);

  if (authLoading) {
    return (
      <div className="h-screen min-h-screen overflow-hidden bg-[#09090B] text-slate-100">
        <div className="mx-auto flex h-screen min-h-screen max-w-[1600px] items-center justify-center px-6 py-12">
          <div className="glass-card max-w-xl p-10 text-center">
            <p className="text-sm uppercase tracking-[0.36em] text-pink-300/80">Loading</p>
            <h1 className="mt-6 text-3xl font-semibold text-white">Initializing Firebase auth...</h1>
            <p className="mt-4 text-slate-300">Please wait while we securely connect to your admin workspace.</p>
          </div>
        </div>
      </div>
    );
  }

  if (!currentUser) {
    return <LoginPage />;
  }

  return (
    <div className="h-screen min-h-screen w-screen overflow-hidden bg-[#09090B] text-slate-100">
      <div className="mx-auto grid h-full w-full max-w-[1600px] grid-cols-[280px_1fr] gap-6 px-4 py-6 sm:px-6 lg:px-8 xl:grid-cols-[320px_1fr]">
        <aside className="glass-card h-full flex flex-col rounded-[34px] border-white/10 bg-white/10 p-6 shadow-glow backdrop-blur-xl">
          <div className="space-y-4">
            <div>
              <p className="text-sm uppercase tracking-[0.35em] text-pink-300/75">Admin</p>
              <h1 className="mt-3 text-3xl font-semibold text-white">Dating HQ</h1>
              <p className="mt-2 text-sm text-slate-300">Live moderation and user insights.</p>
            </div>
            <div className="grid gap-3 rounded-[28px] bg-white/5 p-4">
              <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Connected</p>
              <p className="text-base font-semibold text-white">{currentUser.email}</p>
            </div>
          </div>

          <nav className="mt-10 flex flex-col gap-3">
            <NavLink 
              to="/" 
              end
              className={({ isActive }) => `rounded-3xl px-4 py-3 text-sm font-semibold transition ${isActive ? 'bg-gradient-to-r from-primary to-secondary text-white shadow-[0_16px_42px_rgba(255,77,141,0.23)]' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
            >
              Dashboard
            </NavLink>
            <NavLink 
              to="/users"
              className={({ isActive }) => `rounded-3xl px-4 py-3 text-sm font-semibold transition ${isActive ? 'bg-gradient-to-r from-primary to-secondary text-white shadow-[0_16px_42px_rgba(255,77,141,0.23)]' : 'text-slate-300 hover:bg-white/10 hover:text-white'}`}
            >
              Users
            </NavLink>
          </nav>
        </aside>

        <main className="flex h-full min-h-0 flex-col space-y-6 overflow-hidden">
          <header className="glass-card flex flex-wrap items-center justify-between gap-4 border-white/10 bg-white/10 p-6 shadow-glow backdrop-blur-xl">
            <div>
              <p className="text-xs uppercase tracking-[0.35em] text-pink-300/70">Operations</p>
              <h2 className="mt-3 text-3xl font-semibold text-white">Admin workspace</h2>
            </div>
            <div className="rounded-full bg-white/5 px-4 py-2 text-sm text-slate-300">Live Firestore moderation</div>
          </header>

          <div className="flex-1 min-h-0 overflow-auto">
            <Routes>
              <Route path="/" element={<DashboardPage />} />
              <Route path="/users" element={<UsersPage />} />
              <Route path="/profiles/:userId" element={<ProfilePage />} />
              <Route path="/profiles/:userId/likes" element={<RelationshipPage relType="likes" />} />
              <Route path="/profiles/:userId/dislikes" element={<RelationshipPage relType="dislikes" />} />
              <Route path="/profiles/:userId/blocked" element={<RelationshipPage relType="blocked" />} />
              <Route path="/conversation/:userId" element={<ConversationPage />} />
            </Routes>
          </div>
        </main>
      </div>
    </div>
  );
}

export default App;
