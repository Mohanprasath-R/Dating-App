import { useEffect, useMemo, useState } from 'react';
import { collection, doc, onSnapshot, query, updateDoc, where } from 'firebase/firestore';
import { db } from '../../firebase-config';

function DashboardPage() {
  const [reports, setReports] = useState([]);
  const [users, setUsers] = useState({});
  const [subRequests, setSubRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedReport, setSelectedReport] = useState(null);
  const [chatPreview, setChatPreview] = useState([]);

  useEffect(() => {
    const pendingReportsQuery = query(collection(db, 'reported_users'), where('status', '==', 'pending'));
    const unsubscribeReports = onSnapshot(
      pendingReportsQuery,
      (snapshot) => {
        const reportList = snapshot.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));
        setReports(reportList);
        setSelectedReport((current) => current && reportList.some((report) => report.id === current.id) ? current : reportList[0] || null);
        setLoading(false);
      },
      (error) => {
        console.error('Failed to load admin data', error);
        setLoading(false);
      }
    );

    const unsubscribeUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      const userMap = {};
      snapshot.docs.forEach((userDoc) => {
        userMap[userDoc.id] = userDoc.data();
      });
      setUsers(userMap);
    });

    const unsubscribeSubs = onSnapshot(collection(db, 'subscription_requests'), (snapshot) => {
      setSubRequests(snapshot.docs.map(doc => doc.data()));
    });

    return () => {
      unsubscribeReports();
      unsubscribeUsers();
      unsubscribeSubs();
    };
  }, []);

  useEffect(() => {
    if (!selectedReport?.reporter_id || !selectedReport?.reported_user) {
      setChatPreview([]);
      return;
    }

    const unsubscribeMessages = onSnapshot(collection(db, 'messages'), (snapshot) => {
      const messages = snapshot.docs
        .map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }))
        .filter((message) => {
          const match1 = message.senderId === selectedReport.reporter_id && message.receiverId === selectedReport.reported_user;
          const match2 = message.senderId === selectedReport.reported_user && message.receiverId === selectedReport.reporter_id;
          return match1 || match2;
        })
        .slice(-8)
        .reverse();

      setChatPreview(messages);
    }, (error) => {
      console.error('Failed to load chat messages', error);
    });

    return () => unsubscribeMessages();
  }, [selectedReport]);

  const pendingCount = useMemo(() => reports.length, [reports]);

  const handleAction = async (action) => {
    if (!selectedReport) return;

    try {
      if (action === 'ban') {
        await updateDoc(doc(db, 'users', selectedReport.reported_user), { account_status: 'banned' });
      }

      await updateDoc(doc(db, 'reported_users', selectedReport.id), {
        status: 'resolved',
        action_taken: action,
        resolved_at: Date.now(),
      });

      setReports((prev) => prev.filter((report) => report.id !== selectedReport.id));
      setSelectedReport(null);
    } catch (error) {
      console.error('Failed to update report', error);
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-500">
      {/* Header */}
      

      {/* Stats Grid */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <article className="relative overflow-hidden rounded-[24px] border border-emerald-500/20 bg-gradient-to-br from-emerald-500/10 to-teal-500/5 p-6 transition-all duration-300 hover:-translate-y-1 group">
          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-emerald-400 opacity-80">Active Users</p>
            <h3 className="text-4xl font-bold text-white mt-2 group-hover:scale-105 transition-transform origin-left">{Object.keys(users).length}</h3>
            <span className="text-xs text-slate-400 mt-2 block">Live in community</span>
          </div>
          <div className="absolute -right-4 -bottom-4 h-24 w-24 rounded-full bg-emerald-500/20 blur-2xl group-hover:bg-emerald-500/30 transition-colors" />
        </article>
        
        <article className="relative overflow-hidden rounded-[24px] border border-amber-500/20 bg-gradient-to-br from-amber-500/10 to-orange-500/5 p-6 transition-all duration-300 hover:-translate-y-1 group">
          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-amber-400 opacity-80">Pending Reports</p>
            <h3 className="text-4xl font-bold text-white mt-2 group-hover:scale-105 transition-transform origin-left">{pendingCount}</h3>
            <span className="text-xs text-slate-400 mt-2 block">Awaiting review</span>
          </div>
          <div className="absolute -right-4 -bottom-4 h-24 w-24 rounded-full bg-amber-500/20 blur-2xl group-hover:bg-amber-500/30 transition-colors" />
        </article>

        <article className="relative overflow-hidden rounded-[24px] border border-blue-500/20 bg-gradient-to-br from-blue-500/10 to-indigo-500/5 p-6 transition-all duration-300 hover:-translate-y-1 group">
          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-400 opacity-80">Premium Requests</p>
            <h3 className="text-4xl font-bold text-white mt-2 group-hover:scale-105 transition-transform origin-left">{subRequests.length}</h3>
            <span className="text-xs text-slate-400 mt-2 block">Awaiting OTP</span>
          </div>
          <div className="absolute -right-4 -bottom-4 h-24 w-24 rounded-full bg-blue-500/20 blur-2xl group-hover:bg-blue-500/30 transition-colors" />
        </article>
        
        <article className="relative overflow-hidden rounded-[24px] border border-rose-500/20 bg-gradient-to-br from-rose-500/10 to-pink-500/5 p-6 transition-all duration-300 hover:-translate-y-1 group">
          <div className="relative z-10">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-rose-400 opacity-80">Banned Accounts</p>
            <h3 className="text-4xl font-bold text-white mt-2 group-hover:scale-105 transition-transform origin-left">{Object.values(users).filter((u) => u.account_status === 'banned').length}</h3>
            <span className="text-xs text-slate-400 mt-2 block">Removed from platform</span>
          </div>
          <div className="absolute -right-4 -bottom-4 h-24 w-24 rounded-full bg-rose-500/20 blur-2xl group-hover:bg-rose-500/30 transition-colors" />
        </article>
      </section>

      {/* Main Content */}
      <div className="grid lg:grid-cols-12 gap-6">
        {/* Reports Queue */}
        <section className="lg:col-span-5 flex flex-col gap-4">
          <div className="rounded-[32px] border border-white/10 bg-slate-950/40 backdrop-blur-md overflow-hidden flex flex-col h-full min-h-[500px]">
            <div className="px-6 py-6 border-b border-white/10 bg-white/[0.02]">
              <h3 className="text-lg font-semibold text-white tracking-tight">Review Queue</h3>
              <p className="text-sm text-slate-400 mt-1">Select a case to view details.</p>
            </div>
            
            <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
              {loading ? (
                <div className="space-y-4">
                    {[...Array(4)].map((_, i) => <div key={i} className="h-20 w-full bg-white/5 rounded-2xl shimmer" />)}
                </div>
              ) : reports.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-48 text-center text-slate-400">
                    <svg className="w-12 h-12 mb-3 opacity-20" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                    <p>All caught up!</p>
                </div>
              ) : (
                <ul className="space-y-3">
                  {reports.map((report) => (
                    <li
                      key={report.id}
                      onClick={() => setSelectedReport(report)}
                      className={`group cursor-pointer rounded-[20px] border p-4 transition-all duration-200 ${
                          selectedReport?.id === report.id 
                            ? 'bg-primary/20 border-primary/50 shadow-[0_0_20px_rgba(236,72,153,0.15)]' 
                            : 'bg-white/5 border-white/10 hover:bg-white/10 hover:border-white/20'
                      }`}
                    >
                      <div className="flex justify-between items-start mb-2">
                        <span className={`text-[10px] uppercase tracking-[0.2em] font-semibold px-2.5 py-1 rounded-full ${
                            report.severity === 'high' ? 'bg-red-500/20 text-red-400' : 'bg-amber-500/20 text-amber-400'
                        }`}>
                          {report.reason || 'General'}
                        </span>
                        <span className="text-[10px] text-slate-500 font-mono">New</span>
                      </div>
                      <div className="flex items-center gap-3 mt-3">
                        <div className="h-10 w-10 rounded-full bg-slate-800 border border-white/10 shrink-0 overflow-hidden relative flex items-center justify-center">
                           {users[report.reported_user]?.profile_image || users[report.reported_user]?.profile_picture ? (
                               <img src={users[report.reported_user].profile_image || users[report.reported_user].profile_picture} className="w-full h-full object-cover absolute inset-0" />
                           ) : (
                               <span className="text-slate-400 text-sm font-bold">U</span>
                           )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-semibold text-slate-200 group-hover:text-white truncate">{users[report.reported_user]?.first_name || 'Unknown'} {users[report.reported_user]?.last_name || ''}</p>
                          <p className="text-xs text-slate-400 font-mono truncate w-full mt-0.5" title={report.reported_user}>ID: {report.reported_user.slice(0, 8)}</p>
                        </div>
                      </div>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </section>

        {/* Detail Panel */}
        <section className="lg:col-span-7 flex flex-col gap-4">
          <div className="rounded-[32px] border border-white/10 bg-slate-950/40 backdrop-blur-md overflow-hidden flex flex-col h-full min-h-[600px]">
            {selectedReport ? (
              <>
                <div className="px-6 py-6 border-b border-white/10 bg-gradient-to-r from-slate-900 to-slate-950 flex items-center justify-between">
                  <div>
                    <h3 className="text-xl font-bold text-white tracking-tight">Case #{selectedReport.id.slice(0,6)}</h3>
                    <p className="text-sm text-slate-400 mt-1">Status: <span className="text-amber-400 uppercase tracking-wider text-xs font-semibold">{selectedReport.status || 'Pending'}</span></p>
                  </div>
                </div>

                <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
                  {/* Subject Details */}
                  <div className="grid sm:grid-cols-2 gap-4">
                      <div className="rounded-[24px] bg-white/5 p-5 border border-white/5 hover:bg-white/10 transition-colors">
                          <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-4">Reported User</p>
                          <div className="flex items-center gap-4">
                              <div className="h-12 w-12 rounded-full bg-slate-800 border border-white/10 overflow-hidden relative flex items-center justify-center">
                                  {users[selectedReport.reported_user]?.profile_image || users[selectedReport.reported_user]?.profile_picture ? (
                                      <img src={users[selectedReport.reported_user].profile_image || users[selectedReport.reported_user].profile_picture} className="w-full h-full object-cover absolute inset-0" />
                                  ) : (
                                      <span className="text-slate-400 font-bold">U</span>
                                  )}
                              </div>
                              <div>
                                  <p className="font-bold text-white text-base">{users[selectedReport.reported_user]?.first_name || 'Unknown User'}</p>
                                  <span className={`inline-block mt-1 text-[10px] px-2.5 py-0.5 rounded-full font-bold uppercase tracking-wider ${
                                      users[selectedReport.reported_user]?.account_status === 'banned' ? 'bg-red-500/20 text-red-400' : 'bg-emerald-500/20 text-emerald-400'
                                  }`}>
                                      {users[selectedReport.reported_user]?.account_status || 'active'}
                                  </span>
                              </div>
                          </div>
                      </div>
                      <div className="rounded-[24px] bg-white/5 p-5 border border-white/5 hover:bg-white/10 transition-colors">
                          <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-semibold mb-4">Reporter</p>
                          <div className="flex items-center gap-4">
                              <div className="h-12 w-12 rounded-full bg-slate-800 border border-white/10 overflow-hidden relative flex items-center justify-center">
                                  {users[selectedReport.reporter_id]?.profile_image || users[selectedReport.reporter_id]?.profile_picture ? (
                                      <img src={users[selectedReport.reporter_id].profile_image || users[selectedReport.reporter_id].profile_picture} className="w-full h-full object-cover absolute inset-0" />
                                  ) : (
                                      <span className="text-slate-400 font-bold">R</span>
                                  )}
                              </div>
                              <div className="min-w-0">
                                  <p className="font-bold text-white text-base truncate">{users[selectedReport.reporter_id]?.first_name || 'Unknown Reporter'}</p>
                                  <p className="mt-1 text-xs text-slate-400 font-mono truncate">ID: {selectedReport.reporter_id.slice(0,8)}</p>
                              </div>
                          </div>
                      </div>
                  </div>

                  {/* Complaint */}
                  <div className="rounded-[24px] bg-red-500/5 p-6 border border-red-500/10">
                      <p className="text-[10px] uppercase tracking-[0.2em] text-red-400 font-bold mb-3 flex items-center gap-2">
                          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                          Complaint Details
                      </p>
                      <p className="text-slate-200 text-sm leading-relaxed">{selectedReport.reason}</p>
                      {selectedReport.description && <p className="text-slate-400 text-sm leading-relaxed mt-2 italic">"{selectedReport.description}"</p>}
                  </div>

                  {/* Chat Logs */}
                  <div className="rounded-[24px] border border-white/5 bg-black/20 p-6">
                      <p className="text-[10px] uppercase tracking-[0.2em] text-slate-500 font-bold mb-5 flex items-center gap-2">
                          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" /></svg>
                          Chat Evidence Log
                      </p>
                      <div className="space-y-4 max-h-64 overflow-y-auto pr-2 custom-scrollbar">
                          {chatPreview.length === 0 ? (
                              <p className="text-sm text-slate-400 italic text-center py-4">No recent messages found between these users.</p>
                          ) : (
                              chatPreview.map((msg) => {
                                  const isReporter = msg.senderId === selectedReport.reporter_id;
                                  return (
                                      <div key={msg.id} className={`flex flex-col ${isReporter ? 'items-end' : 'items-start'}`}>
                                          <span className="text-[10px] text-slate-500 mb-1 px-1 font-semibold">{isReporter ? 'Reporter' : 'Reported User'}</span>
                                          <div className={`px-4 py-2.5 max-w-[85%] text-sm ${
                                              isReporter 
                                                ? 'bg-blue-600/30 text-blue-100 rounded-[20px] rounded-tr-sm border border-blue-500/20' 
                                                : 'bg-rose-500/30 text-rose-100 rounded-[20px] rounded-tl-sm border border-rose-500/20'
                                          }`}>
                                              {msg.messageText || msg.mediaUrl || '[Media Attachment]'}
                                          </div>
                                      </div>
                                  )
                              })
                          )}
                      </div>
                  </div>
                </div>

                {/* Actions */}
                <div className="p-6 border-t border-white/10 bg-white/[0.02] flex items-center justify-end gap-4 mt-auto">
                    <button onClick={() => handleAction('dismiss')} className="px-6 py-2.5 rounded-full border border-white/10 bg-white/5 text-sm font-bold text-slate-300 hover:bg-white/10 hover:text-white transition-colors">
                        Dismiss Report
                    </button>
                    <button onClick={() => handleAction('ban')} className="px-6 py-2.5 rounded-full bg-red-500 text-sm font-bold text-white hover:bg-red-600 transition-colors shadow-[0_4px_14px_rgba(239,68,68,0.4)]">
                        Ban User
                    </button>
                </div>
              </>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-center p-8">
                    <div className="h-24 w-24 rounded-full bg-white/5 border border-white/10 flex items-center justify-center mb-6 shadow-inner">
                        <svg className="w-10 h-10 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
                    </div>
                    <h3 className="text-2xl font-bold text-white mb-2">No Case Selected</h3>
                    <p className="text-sm text-slate-400 max-w-sm leading-relaxed">Select a report from the review queue on the left to inspect the case details and chat evidence.</p>
                </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

export default DashboardPage;
