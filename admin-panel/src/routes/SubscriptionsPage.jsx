import { useEffect, useState } from 'react';
import { collection, onSnapshot, doc, setDoc, query, orderBy, deleteDoc, getDoc } from 'firebase/firestore';
import { db } from '../../firebase-config';

function SubscriptionsPage() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [generatedOtps, setGeneratedOtps] = useState({}); // { userId: otp }
  const [userDetails, setUserDetails] = useState({}); // { userId: { name, email } }

  useEffect(() => {
    const q = query(collection(db, 'subscription_requests'), orderBy('timestamp', 'desc'));
    const unsubscribe = onSnapshot(q, async (snapshot) => {
      const requestList = snapshot.docs.map((docSnap) => ({ id: docSnap.id, ...docSnap.data() }));
      setRequests(requestList);

      // Fetch user details for any new requests
      for (const req of requestList) {
        if (!userDetails[req.userId]) {
          const userDoc = await getDoc(doc(db, 'users', req.userId));
          if (userDoc.exists()) {
            const data = userDoc.data();
            setUserDetails(prev => ({
              ...prev,
              [req.userId]: {
                name: `${data.first_name} ${data.last_name}`,
                email: data.email
              }
            }));
          }
        }
      }
      setLoading(false);
    }, (error) => {
      console.error('Failed to load subscription requests', error);
      setLoading(false);
    });

    return () => unsubscribe();
  }, [userDetails]);

  const generateOtp = async (userId, durationDays) => {
    const otp = Math.floor(100000 + Math.random() * 900000).toString();
    const otpData = {
      userId: userId,
      durationDays: durationDays,
      isUsed: false,
      expiryTimestamp: Date.now() + (24 * 60 * 60 * 1000), // 24 hours
      createdAt: Date.now()
    };

    try {
      await setDoc(doc(db, 'subscription_otps', otp), otpData);

      // Notify the user by adding a notification to their document (for a cloud function trigger)
      await setDoc(doc(db, 'user_notifications', `${userId}_premium`), {
        userId: userId,
        title: 'Premium Request Approved!',
        message: `Your premium activation code is: ${otp}. Go to Subscription Settings to activate.`,
        timestamp: Date.now(),
        type: 'premium_otp'
      });

      setGeneratedOtps(prev => ({ ...prev, [userId]: otp }));
      alert(`Generated OTP: ${otp}. A notification entry has been created.`);
    } catch (error) {
      console.error('Error generating OTP:', error);
      alert('Failed to generate OTP.');
    }
  };

  const deleteRequest = async (userId) => {
    if (window.confirm('Are you sure you want to delete this request?')) {
      try {
        await deleteDoc(doc(db, 'subscription_requests', userId));
      } catch (error) {
        console.error('Error deleting request:', error);
      }
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-3 rounded-[32px] border border-white/10 bg-white/5 p-6 shadow-glow backdrop-blur-xl">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-pink-300/75">Subscriptions</p>
          <h2 className="mt-2 text-3xl font-semibold text-white">Premium Requests</h2>
          <p className="mt-2 text-sm text-slate-400">Manage user requests for premium subscriptions and generate activation codes.</p>
        </div>
      </div>

      <div className="glass-card p-6">
        {loading ? (
          <p className="text-slate-300">Loading requests...</p>
        ) : requests.length === 0 ? (
          <p className="text-slate-300">No pending subscription requests.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-300 border-separate border-spacing-y-2">
              <thead className="text-xs uppercase tracking-[0.2em] text-slate-500">
                <tr>
                  <th className="px-6 py-3 font-medium">User</th>
                  <th className="px-6 py-3 font-medium">Plan</th>
                  <th className="px-6 py-3 font-medium">Price</th>
                  <th className="px-6 py-3 font-medium">Time</th>
                  <th className="px-6 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((req) => (
                  <tr key={req.id} className="group bg-white/5 transition-all hover:bg-white/10">
                    <td className="px-6 py-4 first:rounded-l-2xl">
                      <div className="font-semibold text-white">
                        {userDetails[req.userId]?.name || 'Loading...'}
                      </div>
                      <div className="text-[10px] text-slate-400 font-mono">
                        {userDetails[req.userId]?.email || req.userId.slice(0, 12)}
                      </div>
                      {generatedOtps[req.userId] && (
                        <div className="mt-2 inline-block rounded bg-emerald-500/20 px-2 py-0.5 text-xs font-bold text-emerald-400 border border-emerald-500/30">
                          CODE: {generatedOtps[req.userId]}
                        </div>
                      )}
                    </td>
                    <td className="px-6 py-4 font-semibold text-white">{req.planName}</td>
                    <td className="px-6 py-4">{req.price}</td>
                    <td className="px-6 py-4 text-xs text-slate-400">
                      {new Date(req.timestamp).toLocaleString()}
                    </td>
                    <td className="px-6 py-4 text-right last:rounded-r-2xl">
                      <div className="flex justify-end gap-2">
                        <button
                          onClick={() => generateOtp(req.userId, 30)}
                          className="rounded-full bg-emerald-500/10 border border-emerald-500/20 px-3 py-1.5 text-[10px] font-bold text-emerald-400 hover:bg-emerald-500/20"
                        >
                          1M OTP
                        </button>
                        <button
                          onClick={() => generateOtp(req.userId, 180)}
                          className="rounded-full bg-blue-500/10 border border-blue-500/20 px-3 py-1.5 text-[10px] font-bold text-blue-400 hover:bg-blue-500/20"
                        >
                          6M OTP
                        </button>
                        <button
                          onClick={() => deleteRequest(req.id)}
                          className="rounded-full bg-red-500/10 border border-red-500/20 px-3 py-1.5 text-[10px] font-bold text-red-400 hover:bg-red-500/20"
                        >
                          Dismiss
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export default SubscriptionsPage;
