import { useEffect, useMemo, useState } from 'react';
import { collection, onSnapshot, query } from 'firebase/firestore';
import { Link, useParams } from 'react-router-dom';
import { db } from '../../firebase-config';

function ConversationPage() {
  const { userId } = useParams();
  const [messages, setMessages] = useState([]);
  const [users, setUsers] = useState({});
  const [loading, setLoading] = useState(true);

  const formatName = (user) => {
    if (!user) return 'Unknown';
    return `${user.first_name || user.name || ''} ${user.last_name || ''}`.trim() || user.displayName || 'Unknown';
  };

  const getMessageField = (message, primary, fallback) => message?.[primary] ?? message?.[fallback];

  const threads = useMemo(() => {
    const threadMap = {};

    messages.forEach((message) => {
      const senderId = getMessageField(message, 'senderId', 'fromUserId');
      const receiverId = getMessageField(message, 'receiverId', 'toUserId');
      const partnerId = senderId === userId ? receiverId : senderId;
      if (!partnerId) return;

      if (!threadMap[partnerId]) {
        threadMap[partnerId] = { partnerId, messages: [] };
      }
      threadMap[partnerId].messages.push(message);
    });

    return Object.values(threadMap)
      .map((thread) => ({
        ...thread,
        partner: users[thread.partnerId] || null,
        messages: thread.messages.sort((a, b) => {
          const aTs = getMessageField(a, 'timestamp', 'timestamp');
          const bTs = getMessageField(b, 'timestamp', 'timestamp');
          return (aTs?.toDate ? aTs.toDate() : new Date(aTs)) - (bTs?.toDate ? bTs.toDate() : new Date(bTs));
        }),
      }))
      .sort((a, b) => {
        const aLast = a.messages[a.messages.length - 1];
        const bLast = b.messages[b.messages.length - 1];
        const aTs = getMessageField(aLast, 'timestamp', 'timestamp');
        const bTs = getMessageField(bLast, 'timestamp', 'timestamp');
        return (bTs?.toDate ? bTs.toDate() : new Date(bTs)) - (aTs?.toDate ? aTs.toDate() : new Date(aTs));
      });
  }, [messages, users, userId]);

  useEffect(() => {
    const unsubscribeUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      const userMap = {};
      snapshot.docs.forEach((userDoc) => {
        userMap[userDoc.id] = userDoc.data();
      });
      setUsers(userMap);
    });

    return () => unsubscribeUsers();
  }, []);

  useEffect(() => {
    if (!userId) {
      setLoading(false);
      return;
    }

    const q = query(collection(db, 'messages'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const allMessages = snapshot.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));
      const filtered = allMessages.filter((message) => {
        const match1 = message.senderId === userId || message.fromUserId === userId;
        const match2 = message.receiverId === userId || message.toUserId === userId;
        return match1 || match2;
      });
      setMessages(filtered.slice(-40).reverse());
      setLoading(false);
    }, (error) => {
      console.error('Failed to load conversation', error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [userId]);

  const selectedUser = useMemo(() => users[userId] || null, [users, userId]);

  return (
    <div className="space-y-6 animate-in fade-in duration-500 flex flex-col h-[calc(100vh-80px)]">
      {/* Header */}
      <div className="flex flex-col gap-4 rounded-[32px] border border-white/10 bg-slate-950/40 p-6 shadow-xl backdrop-blur-xl sm:flex-row sm:items-center sm:justify-between relative overflow-hidden shrink-0">
        <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-r from-pink-500/10 via-purple-500/10 to-indigo-500/10 pointer-events-none" />
        <div className="relative z-10 flex items-center gap-6">
          <div className="h-16 w-16 rounded-full bg-slate-800 border-2 border-white/10 overflow-hidden relative flex items-center justify-center shrink-0 shadow-lg">
            {selectedUser?.profile_image || selectedUser?.profile_picture ? (
                <img src={selectedUser.profile_image || selectedUser.profile_picture} className="w-full h-full object-cover absolute inset-0" />
            ) : (
                <span className="text-slate-400 font-bold text-xl">{selectedUser ? selectedUser.first_name?.[0] : 'U'}</span>
            )}
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.3em] text-pink-300/80 font-medium drop-shadow-sm">Chat Transcripts</p>
            <h2 className="mt-1 text-3xl font-bold text-white tracking-tight drop-shadow-md">{selectedUser ? `${selectedUser.first_name || ''} ${selectedUser.last_name || ''}`.trim() : 'Loading User...'}</h2>
            <p className="mt-1 text-sm text-slate-400">Viewing all active conversation threads.</p>
          </div>
        </div>
        <div className="relative z-10 flex flex-wrap gap-3">
          <Link
            className="inline-flex w-max items-center justify-center rounded-full border border-white/10 bg-white/5 px-6 py-2.5 text-sm font-semibold text-slate-300 transition hover:bg-white/10 hover:text-white shadow-sm"
            to={`/profiles/${userId}`}
          >
            View Profile
          </Link>
          <Link
            className="inline-flex w-max items-center justify-center rounded-full bg-primary/20 border border-primary/30 px-6 py-2.5 text-sm font-bold text-pink-300 transition hover:bg-primary hover:text-white shadow-[0_4px_14px_rgba(236,72,153,0.2)]"
            to="/users"
          >
            Back to Directory
          </Link>
        </div>
      </div>

      <div className="flex-1 min-h-[500px] glass-card overflow-hidden flex flex-col p-0">
        {loading ? (
          <div className="flex-1 flex items-center justify-center p-8">
              <div className="flex flex-col items-center">
                  <div className="w-8 h-8 border-4 border-pink-500/30 border-t-pink-500 rounded-full animate-spin mb-4" />
                  <p className="text-slate-400 font-medium">Decrypting message logs...</p>
              </div>
          </div>
        ) : threads.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-12">
            <div className="h-24 w-24 rounded-full bg-white/5 border border-white/10 flex items-center justify-center mb-6 shadow-inner">
                <svg className="w-10 h-10 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
            </div>
            <h3 className="text-2xl font-bold text-white mb-2">No conversations</h3>
            <p className="text-sm text-slate-400 max-w-sm leading-relaxed">This user hasn't sent or received any messages yet.</p>
          </div>
        ) : (
          <div className="flex-1 overflow-y-auto p-6 space-y-8 custom-scrollbar">
            {threads.map((thread) => {
              const partnerImg = thread.partner?.profile_image || thread.partner?.profile_picture;
              return (
              <section key={thread.partnerId} className="rounded-[32px] border border-white/10 bg-slate-950/60 overflow-hidden shadow-2xl relative">
                {/* Thread Header */}
                <div className="px-6 py-4 border-b border-white/10 bg-gradient-to-r from-slate-900 to-slate-950 flex flex-wrap items-center justify-between gap-4 sticky top-0 z-20 backdrop-blur-md">
                  <div className="flex items-center gap-4">
                    <div className="h-12 w-12 rounded-full bg-slate-800 border border-white/10 overflow-hidden relative flex items-center justify-center shadow-md">
                        {partnerImg ? (
                            <img src={partnerImg} className="w-full h-full object-cover absolute inset-0" />
                        ) : (
                            <span className="text-slate-400 font-bold">{thread.partner ? thread.partner.first_name?.[0] : 'U'}</span>
                        )}
                    </div>
                    <div>
                      <h3 className="text-lg font-bold text-white tracking-tight">{formatName(thread.partner)}</h3>
                      <p className="text-[10px] text-slate-400 font-mono tracking-wider">ID: {thread.partnerId.slice(0,8)}</p>
                    </div>
                  </div>
                  <span className="rounded-full bg-blue-500/10 border border-blue-500/20 px-3 py-1 text-[10px] font-bold uppercase tracking-[0.2em] text-blue-400 shadow-inner">
                      {thread.messages.length} messages
                  </span>
                </div>
                
                {/* Thread Messages */}
                <div className="p-6 space-y-4 bg-slate-950/80 relative">
                  
                  {thread.messages.map((message) => {
                    const senderId = message.senderId || message.fromUserId;
                    const isMainUser = senderId === userId;
                    
                    return (
                      <div
                        key={message.id}
                        className={`flex w-full relative z-10 ${isMainUser ? 'justify-end' : 'justify-start'}`}
                      >
                        <div className={`flex max-w-[85%] sm:max-w-[70%] items-end gap-2 ${isMainUser ? 'flex-row-reverse' : 'flex-row'}`}>
                            {/* Avatar beside bubble */}
                            {!isMainUser && (
                                <div className="h-6 w-6 rounded-full bg-slate-800 shrink-0 overflow-hidden relative border border-white/5 mb-1 hidden sm:block">
                                    {partnerImg ? <img src={partnerImg} className="w-full h-full object-cover absolute inset-0" /> : null}
                                </div>
                            )}

                            {/* Chat Bubble */}
                            <div className="flex flex-col group w-full">
                                <span className={`text-[9px] text-slate-500 font-semibold px-2 mb-1 ${isMainUser ? 'text-right' : 'text-left'}`}>
                                    {isMainUser ? 'Main User' : formatName(thread.partner).split(' ')[0]}
                                </span>
                                <div className={`px-4 py-2.5 text-[15px] leading-relaxed shadow-md ${
                                    isMainUser 
                                        ? 'bg-blue-600 text-white rounded-[20px] rounded-br-[4px]' 
                                        : 'bg-slate-800 text-slate-100 rounded-[20px] rounded-bl-[4px] border border-white/5'
                                }`}>
                                    {message.messageText || message.text || message.body || message.mediaUrl || '[Media Message]'}
                                </div>
                                <span className={`text-[9px] text-slate-600 px-2 mt-1 ${isMainUser ? 'text-right' : 'text-left'} opacity-0 group-hover:opacity-100 transition-opacity`}>
                                    {message.timestamp ? (message.timestamp.toDate ? message.timestamp.toDate().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : new Date(message.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })) : 'Unknown time'}
                                </span>
                            </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </section>
            )})}
          </div>
        )}
      </div>
    </div>
  );
}

export default ConversationPage;
