import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';

const firebaseConfig = {
  apiKey: 'AIzaSyAVq77taIhtUYQiPSo_1kCKMHA965UEtbc',
  authDomain: 'dating-application-45fb8.firebaseapp.com',
  projectId: 'dating-application-45fb8',
  storageBucket: 'dating-application-45fb8.firebasestorage.app',
  messagingSenderId: '285463623790',
  appId: '1:285463623790:android:39a192fbebc00ec4580e7f',
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);

export { app, auth, db };
