import { useEffect, useState } from 'react';
import { collection, doc, onSnapshot, query, where } from 'firebase/firestore';
import { Link, useParams } from 'react-router-dom';
import { db } from '../../firebase-config';

function ProfilePage() {
    const { userId } = useParams();
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!userId) {
            setLoading(false);
            return;
        }

        const userDoc = doc(db, 'users', userId);
        const unsubscribe = onSnapshot(userDoc, (snapshot) => {
            if (!snapshot.exists()) {
                setUser(null);
                setError('User not found');
            } else {
                setUser({ id: snapshot.id, ...snapshot.data() });
                setError(null);
            }
            setLoading(false);
        }, (listenError) => {
            console.error('Failed to load user profile', listenError);
            setError(listenError.message || 'Failed to load user profile');
            setLoading(false);
        });

        return () => unsubscribe();
    }, [userId]);

    const relationshipCollections = {
        likes: 'liked_profiles',
        dislikes: 'disliked_profiles',
        blocked: 'blocked_users',
    };

    const relationshipButtons = [
        { label: 'Liked profiles', route: 'likes', fields: ['likes', 'likedUsers', 'liked', 'liked_user_ids', 'liked_profiles', 'liked_people'] },
        { label: 'Disliked profiles', route: 'dislikes', fields: ['dislikes', 'dislikedUsers', 'disliked', 'disliked_user_ids', 'disliked_profiles', 'disliked_people'] },
        { label: 'Blocked users', route: 'blocked', fields: ['blocked', 'blockedUsers', 'blocked_user_ids', 'blocked_profiles', 'blocked_people'] },
    ];

    const [relationshipCounts, setRelationshipCounts] = useState({ likes: 0, dislikes: 0, blocked: 0 });

    useEffect(() => {
        if (!userId) return;

        const unsubscribes = Object.entries(relationshipCollections).map(([kind, collectionName]) => {
            const q = query(collection(db, collectionName), where('fromUserId', '==', userId));
            return onSnapshot(q, (snapshot) => {
                setRelationshipCounts((prev) => ({ ...prev, [kind]: snapshot.size }));
            }, (countError) => {
                console.error('Failed to load relationship count', kind, countError);
            });
        });

        return () => unsubscribes.forEach((unsubscribe) => unsubscribe && unsubscribe());
    }, [userId]);

    const getRelationshipCount = (route, fields) => {
        const count = relationshipCounts[route];
        if (count > 0) return count;
        const fieldName = fields.find((name) => Array.isArray(user?.[name]));
        return fieldName ? user[fieldName].length : 0;
    };

    const formatValue = (value) => {
        if (value === undefined || value === null || value === '') {
            return null;
        }

        if (Array.isArray(value)) {
            if (value.length === 0) return <span className="text-slate-500 italic text-sm">Empty</span>;
            return (
                <div className="flex flex-wrap gap-2 mt-1">
                    {value.map((item, index) => {
                        if (typeof item === 'string' || typeof item === 'number') {
                            return (
                                <span key={index} className="inline-flex items-center rounded-full bg-white/5 border border-white/10 px-2.5 py-1 text-xs text-slate-300 hover:bg-white/10 transition-colors">
                                    {item}
                                </span>
                            );
                        }
                        const label = item?.name || item?.displayName || item?.id || item?.userId || JSON.stringify(item);
                        return (
                            <span key={index} className="inline-flex items-center rounded-full bg-white/5 border border-white/10 px-2.5 py-1 text-xs text-slate-300 hover:bg-white/10 transition-colors">
                                {label}
                            </span>
                        );
                    })}
                </div>
            );
        }

        if (typeof value === 'object') {
            return <pre className="whitespace-pre-wrap rounded-xl bg-black/20 p-3 text-[10px] sm:text-xs text-slate-300 overflow-x-auto border border-white/5 mt-1">{JSON.stringify(value, null, 2)}</pre>;
        }

        return <span className="text-sm font-medium text-slate-200">{String(value)}</span>;
    };

    const renderField = (key, value) => {
        const rendered = formatValue(value);
        if (!rendered) return null;

        const label = key.replace(/_/g, ' ').replace(/\b\w/g, (c) => c.toUpperCase());
        return (
            <div key={key} className="py-4 sm:grid sm:grid-cols-3 sm:gap-4 border-b border-white/5 last:border-0 hover:bg-white/[0.02] transition-colors px-4 rounded-xl group">
                <dt className="text-sm font-medium text-slate-400 flex items-center group-hover:text-pink-300/80 transition-colors">
                    {label}
                </dt>
                <dd className="mt-2 text-sm text-slate-200 sm:col-span-2 sm:mt-0 flex items-center break-words">
                    {typeof value === 'object' && !Array.isArray(value) ? rendered : <div className="w-full break-words">{rendered}</div>}
                </dd>
            </div>
        );
    };

    const userImage = user?.profile_image || user?.profile_picture || user?.photoURL || user?.avatar || user?.image_url || user?.profileImageUrl;
    const userCover = user?.cover_image || user?.cover_photo || user?.coverImage || user?.banner_picture || userImage;

    return (
        <div className="space-y-8 animate-in fade-in duration-500">
            {/* Header Banner & Profile Info */}
            <div className="relative rounded-[32px] bg-slate-950/40 border border-white/10 shadow-xl overflow-hidden backdrop-blur-xl pb-6 sm:pb-10">
                {/* Banner Image */}
                <div className="relative h-48 sm:h-56 w-full bg-slate-900">
                    {userCover ? (
                        <>
                            <img src={userCover} alt="Cover" className="w-full h-full object-cover" />
                            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/90 via-slate-950/20 to-transparent" />
                        </>
                    ) : (
                        <div className="absolute inset-0 bg-gradient-to-r from-pink-500/20 via-purple-500/20 to-indigo-500/20" />
                    )}
                </div>
                
                <div className="relative z-10 flex flex-col sm:flex-row gap-6 sm:gap-8 items-start sm:items-end px-6 sm:px-10 -mt-16 sm:-mt-20">
                    <div className="flex h-32 w-32 shrink-0 items-center justify-center rounded-[32px] bg-gradient-to-br from-pink-500 to-purple-600 text-5xl font-bold text-white shadow-[0_8px_30px_rgba(0,0,0,0.6)] border border-white/20 ring-4 ring-slate-950/80 overflow-hidden relative backdrop-blur-sm">
                        {userImage ? (
                            <img src={userImage} alt={`${user?.first_name || 'User'}'s profile`} className="h-full w-full object-cover absolute inset-0" />
                        ) : (
                            <span>{user ? (user.first_name ? user.first_name[0] : user.name ? user.name[0] : 'U') : 'U'}</span>
                        )}
                    </div>
                    <div className="space-y-3 flex-1 pt-2 sm:pt-0">
                        <div>
                            <p className="text-xs uppercase tracking-[0.3em] text-pink-300/80 font-medium">User Profile</p>
                            <h2 className="mt-1 text-4xl sm:text-5xl font-bold text-white tracking-tight">{user ? `${user.first_name || user.name || 'User'} ${user.last_name || ''}`.trim() : 'Profile details'}</h2>
                        </div>
                        {user?.bio && <p className="max-w-2xl text-base leading-relaxed text-slate-300">{user.bio}</p>}
                        <div className="flex flex-wrap items-center gap-3 pt-2">
                            <span className={`rounded-full px-4 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] border ${user?.account_status === 'banned' ? 'bg-red-500/10 text-red-400 border-red-500/20' : 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'}`}>
                                {user?.account_status || 'Active'}
                            </span>
                            <span className={`rounded-full px-4 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] border ${user?.verified ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' : 'bg-slate-500/10 text-slate-400 border-slate-500/20'}`}>
                                {user?.verified ? 'Verified' : 'Unverified'}
                            </span>
                            {user?.id && (
                                <span className="rounded-full bg-white/5 border border-white/10 px-4 py-1.5 text-xs uppercase tracking-[0.2em] text-slate-400 font-mono">
                                    ID: {user.id.slice(0, 8)}
                                </span>
                            )}
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Stats / Actions */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                {relationshipButtons.map((button, i) => {
                    const count = getRelationshipCount(button.route, button.fields);
                    const colors = [
                        'from-pink-500/10 to-rose-500/10 border-pink-500/20 hover:border-pink-500/40 text-pink-400',
                        'from-purple-500/10 to-indigo-500/10 border-purple-500/20 hover:border-purple-500/40 text-purple-400',
                        'from-slate-500/10 to-slate-600/10 border-slate-500/20 hover:border-slate-500/40 text-slate-400'
                    ];
                    return (
                        <Link
                            key={button.route}
                            to={`/profiles/${userId}/${button.route}`}
                            className={`group relative overflow-hidden rounded-[24px] border bg-gradient-to-br p-6 transition-all duration-300 hover:-translate-y-1 ${colors[i]}`}
                        >
                            <div className="relative z-10 flex flex-col gap-1">
                                <span className="text-xs font-semibold uppercase tracking-[0.2em] opacity-80">{button.label}</span>
                                <span className="text-4xl font-bold text-white mt-2 group-hover:scale-105 transition-transform origin-left">{count}</span>
                            </div>
                            <div className="absolute -right-4 -bottom-4 h-24 w-24 rounded-full bg-white/5 blur-2xl group-hover:bg-white/10 transition-colors" />
                        </Link>
                    );
                })}
            </div>

            <div className="grid lg:grid-cols-2 gap-6">
                {/* Contact Info */}
                <section className="rounded-[32px] border border-white/10 bg-slate-950/40 p-6 sm:p-8 backdrop-blur-md transition-colors hover:bg-slate-950/50">
                    <div className="flex items-center gap-3 mb-6">
                        <div className="h-10 w-10 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400 shadow-[0_0_15px_rgba(59,130,246,0.3)]">
                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
                        </div>
                        <h3 className="text-lg font-bold text-white">Contact & Location</h3>
                    </div>
                    <div className="grid sm:grid-cols-2 gap-4">
                        <div className="rounded-[20px] bg-white/5 p-4 border border-white/5 hover:bg-white/10 transition-colors group">
                            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-1 group-hover:text-blue-400/80 transition-colors">Email</p>
                            <p className="text-sm text-slate-200 font-medium truncate" title={user?.email}>{user?.email || 'Not available'}</p>
                        </div>
                        <div className="rounded-[20px] bg-white/5 p-4 border border-white/5 hover:bg-white/10 transition-colors group">
                            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-1 group-hover:text-blue-400/80 transition-colors">Location</p>
                            <p className="text-sm text-slate-200 font-medium truncate">{user?.location || 'Unknown'}</p>
                        </div>
                    </div>
                </section>

                {/* Profile Summary */}
                <section className="rounded-[32px] border border-white/10 bg-slate-950/40 p-6 sm:p-8 backdrop-blur-md transition-colors hover:bg-slate-950/50">
                    <div className="flex items-center gap-3 mb-6">
                        <div className="h-10 w-10 rounded-full bg-emerald-500/20 flex items-center justify-center text-emerald-400 shadow-[0_0_15px_rgba(16,185,129,0.3)]">
                            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 13.255A23.931 23.931 0 0112 15c-3.183 0-6.22-.62-9-1.745M16 6V4a2 2 0 00-2-2h-4a2 2 0 00-2 2v2m4 6h.01M5 20h14a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
                        </div>
                        <h3 className="text-lg font-bold text-white">Work & Education</h3>
                    </div>
                    <div className="grid sm:grid-cols-2 gap-4">
                        <div className="rounded-[20px] bg-white/5 p-4 border border-white/5 hover:bg-white/10 transition-colors group">
                            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-1 group-hover:text-emerald-400/80 transition-colors">Work</p>
                            <p className="text-sm text-slate-200 font-medium truncate">{user?.work || 'Not specified'}</p>
                        </div>
                        <div className="rounded-[20px] bg-white/5 p-4 border border-white/5 hover:bg-white/10 transition-colors group">
                            <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-1 group-hover:text-emerald-400/80 transition-colors">Education</p>
                            <p className="text-sm text-slate-200 font-medium truncate">{user?.education || 'Not specified'}</p>
                        </div>
                    </div>
                </section>
            </div>

            {loading ? (
                <div className="space-y-4">
                    <div className="h-6 rounded-full bg-slate-700/70 shimmer" />
                    <div className="grid gap-4">
                        {[...Array(3)].map((_, idx) => (
                            <div key={idx} className="h-24 rounded-[32px] bg-slate-950/70 shimmer" />
                        ))}
                    </div>
                </div>
            ) : error ? (
                <div className="rounded-[24px] border border-red-500/20 bg-red-500/10 p-6 text-sm text-red-200 flex items-center gap-3">
                    <svg className="w-5 h-5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                    {error}
                </div>
            ) : !user ? (
                <div className="rounded-[24px] border border-slate-500/20 bg-slate-500/10 p-6 text-center text-slate-300">
                    <p>No profile found for this user.</p>
                </div>
            ) : (
                <section className="rounded-[32px] border border-white/10 bg-slate-950/40 backdrop-blur-md overflow-hidden">
                    <div className="px-6 py-6 sm:px-10 border-b border-white/10 flex flex-wrap items-center justify-between gap-4 bg-white/[0.02]">
                        <div>
                            <p className="text-xs uppercase tracking-[0.3em] text-pink-300/80 font-medium mb-1">Deep Dive</p>
                            <h3 className="text-2xl font-bold text-white tracking-tight">Additional Details</h3>
                        </div>
                        <div className="rounded-full bg-white/5 border border-white/10 px-5 py-2 text-xs uppercase tracking-[0.2em] text-slate-300 font-semibold shadow-inner">
                            {Object.keys(user).length} attributes
                        </div>
                    </div>

                    <div className="px-2 py-4 sm:px-6">
                        <dl className="grid grid-cols-1 lg:grid-cols-2 gap-x-12">
                            {Object.entries(user)
                                .filter(([key]) => ![
                                    'id', 'first_name', 'last_name', 'name', 'bio', 'account_status', 'verified', 'location', 'work', 'education', 'email', 'createdAt',
                                    'profile_image', 'cover_image', 'profile_picture', 'photoURL', 'avatar', 'image_url', 'profileImageUrl', 'cover_photo', 'coverImage', 'banner_picture',
                                    'likes', 'likedUsers', 'liked', 'liked_user_ids', 'liked_profiles', 'liked_people',
                                    'dislikes', 'dislikedUsers', 'disliked', 'disliked_user_ids', 'disliked_profiles', 'disliked_people',
                                    'blocked', 'blockedUsers', 'blocked_user_ids', 'blocked_profiles', 'blocked_people',
                                ].includes(key))
                                .map(([key, value]) => renderField(key, value))}
                        </dl>
                    </div>
                </section>
            )}
        </div>
    );
}

export default ProfilePage;
