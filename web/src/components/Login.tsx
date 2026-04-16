import { useState } from "react";
import { auth, firestore } from "../firebase";
import {
  createUserWithEmailAndPassword,
  signInWithEmailAndPassword
} from "firebase/auth";
import { doc, setDoc } from "firebase/firestore";

export default function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleLogin = async () => {
    setLoading(true);
    setError(null);
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err) {
      setError("Unable to sign in. Check your credentials.");
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await createUserWithEmailAndPassword(auth, email, password);
      const uid = result.user.uid;
      const username = email.split("@")[0];
      await setDoc(doc(firestore, "users", uid), {
        id: uid,
        username,
        contact: email,
        isVerified: result.user.emailVerified,
        role: "USER"
      });
    } catch (err) {
      setError("Unable to register. Please verify your email and password.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-shell">
      <div className="form-card" style={{ maxWidth: 420, margin: "0 auto" }}>
        <div className="card-heading">
          <div>
            <h1>Sign in to I AM</h1>
            <p className="secondary-text">Use your Firebase email account to continue.</p>
          </div>
        </div>
        <div className="field-row">
          <div>
            <label>Email</label>
            <input value={email} onChange={(event) => setEmail(event.target.value)} type="email" placeholder="you@example.com" />
          </div>
          <div>
            <label>Password</label>
            <input value={password} onChange={(event) => setPassword(event.target.value)} type="password" placeholder="Password" />
          </div>
        </div>

        {error && <div className="error-text">{error}</div>}

        <div style={{ display: "flex", gap: 10, marginTop: 20, flexWrap: "wrap" }}>
          <button onClick={handleLogin} disabled={loading}>Sign In</button>
          <button onClick={handleRegister} disabled={loading}>Create Account</button>
        </div>
      </div>
    </div>
  );
}
