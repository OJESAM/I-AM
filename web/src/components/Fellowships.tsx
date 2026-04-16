import { useEffect, useMemo, useState } from "react";
import {
  collection,
  doc,
  getDocs,
  onSnapshot,
  orderBy,
  query,
  Unsubscribe,
  serverTimestamp,
  setDoc,
  updateDoc,
  deleteDoc,
  writeBatch,
  where,
  addDoc
} from "firebase/firestore";
import { firestore, getUserProfile } from "../firebase";
import type { Fellowship, FellowshipMember, FellowshipPost, UserProfile } from "../types";

interface Props {
  currentUser: UserProfile;
}

export default function Fellowships({ currentUser }: Props) {
  const [fellowships, setFellowships] = useState<Fellowship[]>([]);
  const [selected, setSelected] = useState<Fellowship | null>(null);
  const [posts, setPosts] = useState<FellowshipPost[]>([]);
  const [members, setMembers] = useState<FellowshipMember[]>([]);
  const [joinCode, setJoinCode] = useState("");
  const [createName, setCreateName] = useState("");
  const [createDescription, setCreateDescription] = useState("");
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [memberNames, setMemberNames] = useState<Record<string, string>>({});
  const [postText, setPostText] = useState("");
  const [editingPostId, setEditingPostId] = useState<string | null>(null);
  const [editingPostText, setEditingPostText] = useState("");
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const q = query(collection(firestore, "fellowships"), orderBy("name", "asc"));
    const unsubscribe: Unsubscribe = onSnapshot(q, (snapshot) => {
      setFellowships(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as Fellowship)));
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (!selected) {
      setPosts([]);
      setMembers([]);
      setMemberNames({});
      setEditName("");
      setEditDescription("");
      setInviteEmail("");
      setEditingPostId(null);
      setEditingPostText("");
      return;
    }

    setEditName(selected.name);
    setEditDescription(selected.description || "");

    const postsQuery = query(collection(firestore, "fellowship_posts"), where("fellowshipId", "==", selected.id), orderBy("timestamp", "desc"));
    const membersQuery = query(collection(firestore, "fellowship_members"), where("fellowshipId", "==", selected.id));

    const unsubscribePosts: Unsubscribe = onSnapshot(postsQuery, (snapshot) => {
      setPosts(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as FellowshipPost)));
    });

    const unsubscribeMembers: Unsubscribe = onSnapshot(membersQuery, (snapshot) => {
      setMembers(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as FellowshipMember)));
    });

    return () => {
      unsubscribePosts();
      unsubscribeMembers();
    };
  }, [selected]);

  useEffect(() => {
    if (members.length === 0) {
      setMemberNames({});
      return;
    }

    const loadMemberNames = async () => {
      const names: Record<string, string> = {};
      await Promise.all(
        members.map(async (member) => {
          const profile = await getUserProfile(member.userId);
          names[member.userId] = profile?.username || member.userId;
        })
      );
      setMemberNames(names);
    };

    loadMemberNames();
  }, [members]);

  const currentMember = useMemo(
    () => members.find((member) => member.userId === currentUser.id),
    [members, currentUser.id]
  );

  const isLeader = useMemo(() => selected?.leaderId === currentUser.id, [selected, currentUser.id]);
  const canManage = useMemo(
    () =>
      isLeader ||
      currentUser.role === "ADMIN" ||
      currentMember?.role === "ADMIN" ||
      currentMember?.role === "LEADER",
    [currentMember?.role, currentUser.role, isLeader]
  );

  const handleJoin = async () => {
    setMessage(null);
    if (!joinCode.trim()) {
      setMessage("Invite code is required.");
      return;
    }
    try {
      const fellowshipQuery = query(
        collection(firestore, "fellowships"),
        where("inviteCode", "==", joinCode.trim().toUpperCase()),
        orderBy("inviteCode")
      );
      const fellowshipSnapshot = await getDocs(fellowshipQuery);
      if (fellowshipSnapshot.empty) {
        setMessage("Invalid invite code.");
        return;
      }

      const fellowshipId = fellowshipSnapshot.docs[0].id;
      const memberQuery = query(
        collection(firestore, "fellowship_members"),
        where("fellowshipId", "==", fellowshipId),
        where("userId", "==", currentUser.id)
      );
      const memberSnapshot = await getDocs(memberQuery);
      if (!memberSnapshot.empty) {
        setMessage("Already a member.");
        return;
      }

      await addDoc(collection(firestore, "fellowship_members"), {
        fellowshipId,
        userId: currentUser.id,
        role: "USER",
        joinedAt: serverTimestamp()
      });
      setMessage("Joined fellowship successfully.");
    } catch (error) {
      setMessage("Unable to join fellowship. Verify the code and try again.");
    }
  };

  const handleCreateFellowship = async () => {
    setMessage(null);
    if (!createName.trim()) {
      setMessage("Name is required for a fellowship.");
      return;
    }
    try {
      const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
      const fellowshipRef = doc(collection(firestore, "fellowships"));
      await setDoc(fellowshipRef, {
        id: fellowshipRef.id,
        name: createName.trim(),
        description: createDescription.trim(),
        leaderId: currentUser.id,
        inviteCode,
        timestamp: serverTimestamp()
      });

      await addDoc(collection(firestore, "fellowship_members"), {
        fellowshipId: fellowshipRef.id,
        userId: currentUser.id,
        role: "LEADER",
        joinedAt: serverTimestamp()
      });

      setCreateName("");
      setCreateDescription("");
      setMessage("Fellowship created successfully.");
    } catch (error) {
      setMessage("Unable to create fellowship. Check your permission.");
    }
  };

  const handleSaveFellowship = async () => {
    if (!selected) {
      return;
    }

    setMessage(null);
    try {
      await updateDoc(doc(firestore, "fellowships", selected.id), {
        name: editName.trim(),
        description: editDescription.trim()
      });
      setSelected({ ...selected, name: editName.trim(), description: editDescription.trim() });
      setMessage("Fellowship saved.");
    } catch (error) {
      setMessage("Unable to save fellowship. Verify your permissions.");
    }
  };

  const handleDeleteFellowship = async () => {
    if (!selected) {
      return;
    }

    if (!window.confirm("Delete this fellowship and all related posts/members?")) {
      return;
    }

    setMessage(null);
    try {
      const fellowshipRef = doc(firestore, "fellowships", selected.id);
      const postsSnapshot = await getDocs(query(collection(firestore, "fellowship_posts"), where("fellowshipId", "==", selected.id)));
      const membersSnapshot = await getDocs(query(collection(firestore, "fellowship_members"), where("fellowshipId", "==", selected.id)));

      const batch = writeBatch(firestore);
      batch.delete(fellowshipRef);
      postsSnapshot.docs.forEach((docItem) => batch.delete(docItem.ref));
      membersSnapshot.docs.forEach((docItem) => batch.delete(docItem.ref));
      await batch.commit();

      setSelected(null);
      setMessage("Fellowship deleted.");
    } catch (error) {
      setMessage("Unable to delete fellowship.");
    }
  };

  const handleInviteMember = async () => {
    if (!selected || !inviteEmail.trim()) {
      setMessage("Email is required to invite a member.");
      return;
    }

    setMessage(null);
    try {
      const userQuery = query(collection(firestore, "users"), where("contact", "==", inviteEmail.trim()));
      const userSnapshot = await getDocs(userQuery);
      if (userSnapshot.empty) {
        setMessage("No user found with that email.");
        return;
      }

      const userId = userSnapshot.docs[0].id;
      const memberQuery = query(
        collection(firestore, "fellowship_members"),
        where("fellowshipId", "==", selected.id),
        where("userId", "==", userId)
      );
      const memberSnapshot = await getDocs(memberQuery);
      if (!memberSnapshot.empty) {
        setMessage("User is already a member.");
        return;
      }

      await addDoc(collection(firestore, "fellowship_members"), {
        fellowshipId: selected.id,
        userId,
        role: "USER",
        joinedAt: serverTimestamp()
      });
      setInviteEmail("");
      setMessage("Member invited successfully.");
    } catch (error) {
      setMessage("Unable to invite member. Make sure the email exists in users.");
    }
  };

  const handleCreatePost = async () => {
    if (!selected || !postText.trim()) {
      return;
    }
    try {
      await addDoc(collection(firestore, "fellowship_posts"), {
        fellowshipId: selected.id,
        userId: currentUser.id,
        userName: currentUser.username,
        content: postText.trim(),
        timestamp: Date.now()
      });
      setPostText("");
      setMessage("Post added.");
    } catch (error) {
      setMessage("Unable to add post.");
    }
  };

  const handleEditPost = (post: FellowshipPost) => {
    setEditingPostId(post.id);
    setEditingPostText(post.content);
  };

  const handleCancelEdit = () => {
    setEditingPostId(null);
    setEditingPostText("");
  };

  const handleSavePost = async () => {
    if (!editingPostId || !editingPostText.trim()) {
      return;
    }

    setMessage(null);
    try {
      await updateDoc(doc(firestore, "fellowship_posts", editingPostId), {
        content: editingPostText.trim()
      });
      setEditingPostId(null);
      setEditingPostText("");
      setMessage("Post updated.");
    } catch (error) {
      setMessage("Unable to save post.");
    }
  };

  const handleDeletePost = async (postId: string) => {
    setMessage(null);
    try {
      await deleteDoc(doc(firestore, "fellowship_posts", postId));
      setMessage("Post deleted.");
    } catch (error) {
      setMessage("Unable to delete post.");
    }
  };

  const handleRemoveMember = async (member: FellowshipMember) => {
    setMessage(null);
    try {
      const memberQuery = query(
        collection(firestore, "fellowship_members"),
        where("fellowshipId", "==", member.fellowshipId),
        where("userId", "==", member.userId)
      );
      const memberSnapshot = await getDocs(memberQuery);
      await Promise.all(memberSnapshot.docs.map((docItem) => deleteDoc(docItem.ref)));
      setMessage("Member removed.");
    } catch (error) {
      setMessage("Unable to remove member.");
    }
  };

  return (
    <div>
      <div className="card-heading">
        <div>
          <h2>Fellowships</h2>
          <p className="secondary-text">Browse group communities, join with an invite code, and manage members if you're a leader or admin.</p>
        </div>
      </div>

      <div className="details-grid">
        <div>
          <div className="form-card">
            <div className="card-heading">
              <div>
                <h3>Available Fellowships</h3>
              </div>
            </div>
            {fellowships.length === 0 ? (
              <p className="secondary-text">No fellowships are available yet.</p>
            ) : (
              <ul>
                {fellowships.map((item) => (
                  <li key={item.id} className="fellowship-card">
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "center" }}>
                      <div>
                        <h3>{item.name}</h3>
                        <p className="secondary-text">{item.description}</p>
                      </div>
                      <button onClick={() => setSelected(item)}>Open</button>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="form-card">
            <div className="card-heading">
              <div>
                <h3>Join by invite code</h3>
              </div>
            </div>
            <label>Invite Code</label>
            <input value={joinCode} onChange={(event) => setJoinCode(event.target.value)} placeholder="Enter invite code" />
            <button style={{ marginTop: 12 }} onClick={handleJoin}>Join</button>
          </div>

          <div className="form-card">
            <div className="card-heading">
              <div>
                <h3>Create Fellowship</h3>
              </div>
            </div>
            <label>Name</label>
            <input value={createName} onChange={(event) => setCreateName(event.target.value)} placeholder="Fellowship name" />
            <label>Description</label>
            <textarea value={createDescription} onChange={(event) => setCreateDescription(event.target.value)} rows={4} placeholder="Short description" />
            <button style={{ marginTop: 12 }} onClick={handleCreateFellowship}>Create</button>
          </div>
        </div>

        <div>
          {selected ? (
            <div className="form-card">
              <div className="card-heading">
                <div>
                  <h3>{selected.name}</h3>
                  <p className="secondary-text">{selected.description}</p>
                </div>
                <button onClick={() => setSelected(null)}>Close</button>
              </div>
              <p className="small-badge">Invite code: {selected.inviteCode || "N/A"}</p>
              <p className="secondary-text">Leader ID: {selected.leaderId}</p>

              {canManage && (
                <div style={{ marginTop: 16, borderTop: "1px solid #ddd", paddingTop: 16 }}>
                  <h4>Edit fellowship</h4>
                  <label>Name</label>
                  <input value={editName} onChange={(event) => setEditName(event.target.value)} />
                  <label>Description</label>
                  <textarea value={editDescription} onChange={(event) => setEditDescription(event.target.value)} rows={3} />
                  <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 12 }}>
                    <button onClick={handleSaveFellowship}>Save</button>
                    <button className="signout" onClick={handleDeleteFellowship}>Delete Fellowship</button>
                  </div>

                  <div style={{ marginTop: 20 }}>
                    <h4>Invite member</h4>
                    <label>Email</label>
                    <input value={inviteEmail} onChange={(event) => setInviteEmail(event.target.value)} placeholder="member@example.com" />
                    <button style={{ marginTop: 12 }} onClick={handleInviteMember}>Invite</button>
                  </div>
                </div>
              )}

              <div style={{ marginTop: 16 }}>
                <label>New post</label>
                <textarea value={postText} onChange={(event) => setPostText(event.target.value)} rows={4} placeholder="Write a fellowship update." />
                <button style={{ marginTop: 12 }} onClick={handleCreatePost}>Post</button>
              </div>

              <div className="toolbar" style={{ marginTop: 24 }}>
                <div className="small-badge">Members {members.length}</div>
                <div className="small-badge">Posts {posts.length}</div>
                {canManage && <div className="small-badge">You can manage this fellowship</div>}
              </div>

              <div>
                <h4>Posts</h4>
                {posts.length === 0 && <p className="secondary-text">No posts yet.</p>}
                {posts.map((post) => (
                  <div key={post.id} className="post-card">
                    <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
                      <div>
                        <h3>{post.userName}</h3>
                        <p className="secondary-text">{new Date(post.timestamp).toLocaleString()}</p>
                      </div>
                      {(canManage || post.userId === currentUser.id) && (
                        <div style={{ display: "flex", gap: 8 }}>
                          {editingPostId !== post.id ? (
                            <>
                              <button className="link-button" onClick={() => handleEditPost(post)}>Edit</button>
                              <button className="link-button" onClick={() => handleDeletePost(post.id)}>Delete</button>
                            </>
                          ) : null}
                        </div>
                      )}
                    </div>
                    {editingPostId === post.id ? (
                      <div style={{ marginTop: 12 }}>
                        <textarea
                          value={editingPostText}
                          onChange={(event) => setEditingPostText(event.target.value)}
                          rows={4}
                        />
                        <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
                          <button onClick={handleSavePost}>Save</button>
                          <button className="link-button" onClick={handleCancelEdit}>Cancel</button>
                        </div>
                      </div>
                    ) : (
                      <p>{post.content}</p>
                    )}
                  </div>
                ))}
              </div>

              <div style={{ marginTop: 24 }}>
                <h4>Members</h4>
                {members.length === 0 && <p className="secondary-text">No member records for this fellowship yet.</p>}
                {members.map((member) => (
                  <div key={member.id} className="member-card" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <strong>{memberNames[member.userId] || member.userId}</strong>
                      <p className="secondary-text">{member.role}</p>
                    </div>
                    {canManage && member.userId !== selected.leaderId && (
                      <button className="link-button" onClick={() => handleRemoveMember(member)}>
                        Remove
                      </button>
                    )}
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="form-card">
              <h3>Select a fellowship to see posts and members.</h3>
            </div>
          )}
        </div>
      </div>

      {message && <div className="notification">{message}</div>}
    </div>
  );
}
