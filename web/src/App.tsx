import { useEffect, useMemo, useState } from "react";
import { auth, firestore, getUserProfile, signOutUser } from "./firebase";
import type { UserProfile } from "./types";
import Devotionals from "./components/Devotionals";
import Fellowships from "./components/Fellowships";
import Login from "./components/Login";
import { onAuthStateChanged } from "firebase/auth";
import { doc, getDoc, setDoc } from "firebase/firestore";

function App() {
  const [authLoading, setAuthLoading] = useState(true);
  const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
  const [activeView, setActiveView] = useState<"devotionals" | "fellowships">("devotionals");

  useEffect(() => {
    return onAuthStateChanged(auth, async (user) => {
      if (!user) {
        setUserProfile(null);
        setAuthLoading(false);
        return;
      }

      setAuthLoading(true);
      const profile = await getUserProfile(user.uid);
      if (profile) {
        setUserProfile(profile);
      } else {
        const document = doc(firestore, "users", user.uid);
        const displayName = user.email?.split("@")[0] || "Guest";
        const newProfile: UserProfile = {
          id: user.uid,
          username: displayName,
          contact: user.email || "",
          isVerified: user.emailVerified,
          role: "USER"
        };
        await setDoc(document, newProfile, { merge: true });
        setUserProfile(newProfile);
      }
      setAuthLoading(false);
    });
  }, []);

  const isAdmin = useMemo(() => userProfile?.role === "ADMIN", [userProfile]);

  if (authLoading) {
    return <div className="page-shell">Loading authentication...</div>;
  }

  if (!userProfile) {
    return <Login />;
  }

  return (
    <div className="page-shell">
      <header className="header">
        <div>
          <div className="brand">I AM Web</div>
          <div className="subtitle">Welcome, {userProfile.username}</div>
        </div>
        <div className="header-actions">
          <button onClick={() => setActiveView("devotionals")} className={activeView === "devotionals" ? "active" : ""}>
            Devotionals
          </button>
          <button onClick={() => setActiveView("fellowships")} className={activeView === "fellowships" ? "active" : ""}>
            Fellowships
          </button>
          <button className="signout" onClick={signOutUser}>
            Sign Out
          </button>
        </div>
      </header>

      <main className="content">
        {activeView === "devotionals" ? (
          <Devotionals currentUser={userProfile} isAdmin={isAdmin} />
        ) : (
          <Fellowships currentUser={userProfile} />
        )}
      </main>
    </div>
  );
}

export default App;
