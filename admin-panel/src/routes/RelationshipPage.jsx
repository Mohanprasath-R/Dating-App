import { useEffect, useState } from 'react';
import { collection, doc, getDoc, onSnapshot, query, where } from 'firebase/firestore';
import { Link, useParams } from 'react-router-dom';
import { db } from '../../firebase-config';

const relationshipCollections = {
  likes: 'liked_profiles',
  dislikes: 'disliked_profiles',
  blocked: 'blocked_users',
};

const relationshipConfig = {
  likes: {
    label: 'Liked profiles',
    emptyLabel: 'No liked profiles found.',
    buttonLabel: 'View all likes',
    fieldNames: ['likes', 'likedUsers', 'liked', 'liked_user_ids', 'liked_profiles', 'liked_people'],
  },
  dislikes: {
    label: 'Disliked profiles',
    emptyLabel: 'No disliked profiles found.',
    buttonLabel: 'View all dislikes',
    fieldNames: ['dislikes', 'dislikedUsers', 'disliked', 'disliked_user_ids', 'disliked_profiles', 'disliked_people'],
  },
  blocked: {
    label: 'Blocked users',
    emptyLabel: 'No blocked users found.',
    buttonLabel: 'View blocked users',
    fieldNames: ['blocked', 'blockedUsers', 'blocked_user_ids', 'blocked_profiles', 'blocked_people'],
  },
};

function RelationshipPage({ relType }) {
  const { userId } = useParams();
  const [user, setUser] = useState(null);
  const [profiles, setProfiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const relationship = relationshipConfig[relType];

  useEffect(() => {
    if (!userId || !relationship) {
      setLoading(false);
      return;
    }

    const userDoc = doc(db, 'users', userId);
    const relationshipCollection = relationshipCollections[relType];
    const relationshipQuery = query(collection(db, relationshipCollection), where('fromUserId', '==', userId));

    const unsubscribeUser = onSnapshot(userDoc, (snapshot) => {
      if (!snapshot.exists()) {
        setUser(null);
        setError('User not found');
        setLoading(false);
      } else {
        setUser({ id: snapshot.id, ...snapshot.data() });
        setError(null);
      }
    }, (listenError) => {
      console.error('Failed to load user profile', listenError);
      setError(listenError.message || 'Failed to load user profile');
    });

    const unsubscribeRelationships = onSnapshot(
      relationshipQuery,
      async (snapshot) => {
        const relationshipDocs = snapshot.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));
        const ids = relationshipDocs
          .map((item) => item.toUserId || item.to_user_id || item.toId || item.to || null)
          .filter(Boolean);

        if (ids.length === 0) {
          setProfiles([]);
          setLoading(false);
          return;
        }

        const loadedProfiles = await Promise.all(
          ids.map(async (profileId) => {
            const profileSnap = await getDoc(doc(db, 'users', profileId));
            return profileSnap.exists() ? { id: profileSnap.id, ...profileSnap.data() } : null;
          })
        );

        setProfiles(loadedProfiles.filter(Boolean));
        setLoading(false);
      },
      (listenError) => {
        console.error('Failed to load relationship list', listenError);
        setError(listenError.message || 'Failed to load list');
        setLoading(false);
      }
    );

    return () => {
      unsubscribeUser();
      unsubscribeRelationships();
    };
  }, [userId, relType, relationship]);

  if (!relationship) {
    return (
      <div className="glass-card p-6">
        <p className="text-sm text-slate-300">Invalid relationship type.</p>
      </div>
    );
  }

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      {/* Premium Header */}
      <div className="relative rounded-[32px] border border-white/10 bg-slate-950/40 p-6 sm:p-8 backdrop-blur-xl overflow-hidden shadow-xl">
        <div className="absolute top-0 left-0 w-full h-32 bg-gradient-to-r from-pink-500/20 via-purple-500/20 to-indigo-500/20 blur-3xl opacity-50 pointer-events-none" />
        
        <div className="relative z-10 flex flex-col gap-6 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex items-center gap-5 sm:gap-6">
              <div className="flex h-16 w-16 sm:h-20 sm:w-20 shrink-0 items-center justify-center rounded-[24px] bg-gradient-to-br from-pink-500 to-purple-600 text-2xl sm:text-3xl font-bold text-white shadow-lg border border-white/20 overflow-hidden relative">
                  {user?.profile_image || user?.profile_picture || user?.photoURL || user?.avatar || user?.image_url ? (
                      <img src={user.profile_image || user.profile_picture || user.photoURL || user.avatar || user.image_url} alt="User" className="h-full w-full object-cover absolute inset-0" />
                  ) : (
                      <span>{user ? (user.first_name ? user.first_name[0] : user.name ? user.name[0] : 'U') : 'U'}</span>
                  )}
              </div>
              <div>
                <p className="text-[10px] sm:text-xs uppercase tracking-[0.3em] text-pink-300/80 font-medium mb-1 drop-shadow-sm">{relationship.label}</p>
                <h2 className="text-2xl sm:text-3xl font-bold text-white tracking-tight drop-shadow-md">{user ? `${user.first_name || user.name || 'User'} ${user.last_name || ''}`.trim() : 'Profile list'}</h2>
                <p className="mt-1 text-xs sm:text-sm text-slate-400">Browse the users this profile has marked.</p>
              </div>
            </div>
            <div className="flex flex-wrap gap-3">
              <Link className="inline-flex items-center justify-center rounded-full border border-white/10 bg-white/5 px-5 py-2.5 text-sm font-semibold text-slate-200 transition hover:bg-white/10 hover:text-white shadow-sm" to={`/profiles/${userId}`}>
                Back to profile
              </Link>
            </div>
        </div>
      </div>

      <div className="glass-card p-6">
        {loading ? (
          <div className="space-y-5">
            <div className="h-6 w-1/3 rounded-full bg-slate-700/70 shimmer" />
            <div className="grid gap-4">
              {[...Array(3)].map((_, idx) => (
                <div key={idx} className="h-16 rounded-[24px] bg-slate-950/70 shimmer" />
              ))}
            </div>
          </div>
        ) : error ? (
          <div className="rounded-[24px] border border-red-500/20 bg-red-500/10 p-6 text-sm text-red-200 flex items-center gap-3">
             <svg className="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
             {error}
          </div>
        ) : profiles.length === 0 ? (
          <div className="rounded-[24px] border border-slate-500/20 bg-slate-500/10 p-6 text-center text-slate-300">
            <p>{relationship.emptyLabel}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300 border-separate border-spacing-y-2">
              <thead className="text-[10px] sm:text-xs uppercase tracking-[0.2em] text-slate-500">
                <tr>
                  <th scope="col" className="px-6 py-3 font-medium">User</th>
                  <th scope="col" className="px-6 py-3 font-medium">Status</th>
                  <th scope="col" className="px-6 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {profiles.map((profile) => {
                  const pImage = profile?.profile_image || profile?.profile_picture || profile?.photoURL || profile?.avatar || profile?.image_url;
                  return (
                  <tr key={profile.id} className="group bg-white/5 shadow-sm transition-all hover:bg-white/10">
                    <td className="whitespace-nowrap px-6 py-4 first:rounded-l-2xl">
                      <div className="flex items-center gap-4">
                        <div className="h-10 w-10 sm:h-12 sm:w-12 shrink-0 rounded-full bg-slate-800 flex items-center justify-center text-slate-300 font-bold overflow-hidden border border-white/10 relative">
                           {pImage ? <img src={pImage} className="h-full w-full object-cover absolute inset-0" /> : (profile.first_name ? profile.first_name[0] : 'U')}
                        </div>
                        <div>
                          <p className="text-sm sm:text-base font-bold text-white group-hover:text-pink-300 transition-colors">{profile.first_name || profile.name || 'Unnamed'} {profile.last_name || ''}</p>
                          <p className="text-[10px] sm:text-xs text-slate-400 font-mono mt-0.5 tracking-wider">ID: {profile.id.slice(0, 8)}</p>
                        </div>
                      </div>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4">
                      <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-[0.1em] ${
                        profile.account_status === 'banned' 
                          ? 'bg-red-500/10 text-red-400 border border-red-500/20' 
                          : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                      }`}>
                        {profile.account_status || 'active'}
                      </span>
                    </td>
                    <td className="whitespace-nowrap px-6 py-4 text-right last:rounded-r-2xl">
                      <div className="flex justify-end gap-2">
                        <Link
                          className="inline-flex items-center justify-center rounded-full border border-white/10 bg-white/5 px-4 py-1.5 text-[10px] sm:text-xs font-semibold text-slate-300 transition hover:border-primary/40 hover:bg-white/15 hover:text-white shadow-sm"
                          to={`/profiles/${profile.id}`}
                        >
                          Profile
                        </Link>
                        <Link
                          className="inline-flex items-center justify-center rounded-full bg-primary/20 border border-primary/30 px-4 py-1.5 text-[10px] sm:text-xs font-semibold text-pink-300 transition hover:bg-primary hover:text-white shadow-sm hover:shadow-[0_8px_20px_rgba(255,77,141,0.25)]"
                          to={`/conversation/${profile.id}`}
                        >
                          Chat
                        </Link>
                      </div>
                    </td>
                  </tr>
                )})}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default RelationshipPage;
