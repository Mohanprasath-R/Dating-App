import { useEffect, useState } from 'react';
import { collection, onSnapshot } from 'firebase/firestore';
import { Link } from 'react-router-dom';
import { db } from '../../firebase-config';

function UsersPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const usersPerPage = 10;

  useEffect(() => {
    const unsubscribe = onSnapshot(collection(db, 'users'), (snapshot) => {
      const userList = snapshot.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));
      setUsers(userList);
      setLoading(false);
    }, (error) => {
      console.error('Failed to load users', error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, []);

  const indexOfLastUser = currentPage * usersPerPage;
  const indexOfFirstUser = indexOfLastUser - usersPerPage;
  const currentUsers = users.slice(indexOfFirstUser, indexOfLastUser);
  const totalPages = Math.ceil(users.length / usersPerPage);

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 rounded-[32px] border border-white/10 bg-white/5 p-6 shadow-glow backdrop-blur-xl">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-pink-300/75">Users</p>
          <h2 className="mt-2 text-3xl font-semibold text-white">User directory</h2>
          <p className="mt-2 text-sm text-slate-400">Browse all profiles and open the conversation stream.</p>
        </div>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          <div className="rounded-[28px] bg-slate-950/70 p-4 text-sm text-slate-300" style={{ boxShadow: '0 24px 80px rgba(15, 23, 42, 0.12)' }}>
            <p className="uppercase tracking-[0.3em] text-slate-500">Total users</p>
            <p className="mt-3 text-3xl font-semibold text-white">{users.length}</p>
          </div>
          <div className="rounded-[28px] bg-slate-950/70 p-4 text-sm text-slate-300" style={{ boxShadow: '0 24px 80px rgba(15, 23, 42, 0.12)' }}>
            <p className="uppercase tracking-[0.3em] text-slate-500">Active</p>
            <p className="mt-3 text-3xl font-semibold text-white">{users.filter((user) => user.account_status !== 'banned').length}</p>
          </div>
          <div className="rounded-[28px] bg-slate-950/70 p-4 text-sm text-slate-300" style={{ boxShadow: '0 24px 80px rgba(15, 23, 42, 0.12)' }}>
            <p className="uppercase tracking-[0.3em] text-slate-500">Banned</p>
            <p className="mt-3 text-3xl font-semibold text-white">{users.filter((user) => user.account_status === 'banned').length}</p>
          </div>
        </div>
      </div>

      <div className="glass-card p-6">
        {loading ? (
          <div className="space-y-4">
            <div className="h-5 w-1/3 rounded-full bg-slate-700/70 shimmer" />
            <div className="space-y-3">
              {[...Array(4)].map((_, idx) => (
                <div key={idx} className="h-24 rounded-[28px] bg-slate-950/70" />
              ))}
            </div>
          </div>
        ) : users.length === 0 ? (
          <p className="text-slate-300">No users found.</p>
        ) : (
          <>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm text-slate-300 border-separate border-spacing-y-2">
                <thead className="text-xs uppercase tracking-[0.2em] text-slate-500">
                  <tr>
                    <th scope="col" className="px-6 py-3 font-medium">ID</th>
                    <th scope="col" className="px-6 py-3 font-medium">Email</th>
                    <th scope="col" className="px-6 py-3 font-medium">Name</th>
                    <th scope="col" className="px-6 py-3 font-medium">Status</th>
                    <th scope="col" className="px-6 py-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {currentUsers.map((user) => (
                  <tr key={user.id} className="group bg-white/5 shadow-sm transition-all hover:bg-white/10">
                    <td className="whitespace-nowrap px-6 py-4 first:rounded-l-2xl">
                      <span className="rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-slate-200">
                        {user.id.slice(0, 8)}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">{user.email}</td>
                    <td className="whitespace-nowrap px-6 py-4 font-semibold text-white">
                      {user.first_name || 'Unnamed'} {user.last_name || ''}
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-wider ${
                        user.account_status === 'banned' 
                          ? 'bg-red-500/10 text-red-400 border border-red-500/20' 
                          : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      }`}>
                        {user.account_status || 'active'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right last:rounded-r-2xl">
                      <div className="flex justify-end gap-2">
                        <Link
                          className="inline-flex items-center justify-center rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-xs font-semibold text-white transition hover:border-primary/40 hover:bg-white/15"
                          to={`/profiles/${user.id}`}
                        >
                          Profile
                        </Link>
                        <Link
                          className="inline-flex items-center justify-center rounded-full bg-primary px-4 py-1.5 text-xs font-semibold text-white shadow-[0_8px_20px_rgba(255,77,141,0.2)] transition hover:-translate-y-0.5"
                          to={`/conversation/${user.id}`}
                        >Chat
                        </Link>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          
          <div className="mt-6 flex flex-wrap items-center justify-between gap-4 border-t border-white/10 pt-4">
            <div className="text-sm text-slate-400">
              Showing <span className="font-semibold text-white">{users.length > 0 ? indexOfFirstUser + 1 : 0}</span> to <span className="font-semibold text-white">{Math.min(indexOfLastUser, users.length)}</span> of <span className="font-semibold text-white">{users.length}</span> users
            </div>
            <div className="flex gap-2">
              <button
                onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                disabled={currentPage === 1}
                className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                Previous
              </button>
              <button
                onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                disabled={currentPage === totalPages || totalPages === 0}
                className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-semibold text-slate-300 transition hover:bg-white/10 hover:text-white disabled:cursor-not-allowed disabled:opacity-50"
              >
                Next
              </button>
            </div>
          </div>
          </>
        )}
      </div>
    </div>
  );
}

export default UsersPage;
