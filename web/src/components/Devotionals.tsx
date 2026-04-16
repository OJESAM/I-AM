import { useEffect, useMemo, useState } from "react";
import { collection, deleteDoc, doc, onSnapshot, orderBy, query, setDoc, Unsubscribe } from "firebase/firestore";
import { firestore } from "../firebase";
import type { Devotional, Fellowship, UserProfile } from "../types";

const categories = ["All", "Faith", "Prayer", "Fasting", "Wisdom", "Grace"];
const searchTypes = ["Devotional", "Fellowship", "User"] as const;

type SearchType = (typeof searchTypes)[number];
interface SearchResult {
  id: string;
  type: SearchType;
  title: string;
  subtitle: string;
  description: string;
}

interface Props {
  currentUser: UserProfile;
  isAdmin: boolean;
}

export default function Devotionals({ currentUser, isAdmin }: Props) {
  const [devotionals, setDevotionals] = useState<Devotional[]>([]);
  const [fellowships, setFellowships] = useState<Fellowship[]>([]);
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("All");
  const [selectedDevotional, setSelectedDevotional] = useState<Devotional | null>(null);
  const [selectedSearchResult, setSelectedSearchResult] = useState<SearchResult | null>(null);
  const [title, setTitle] = useState("");
  const [content, setContent] = useState("");
  const [scripture, setScripture] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [category, setCategory] = useState("Faith");
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    const q = query(collection(firestore, "devotionals"), orderBy("timestamp", "desc"));
    const unsubscribe: Unsubscribe = onSnapshot(q, (snapshot) => {
      setDevotionals(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as Devotional)));
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    const q = query(collection(firestore, "fellowships"), orderBy("name", "asc"));
    const unsubscribe: Unsubscribe = onSnapshot(q, (snapshot) => {
      setFellowships(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as Fellowship)));
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    const q = query(collection(firestore, "users"), orderBy("username", "asc"));
    const unsubscribe: Unsubscribe = onSnapshot(q, (snapshot) => {
      setUsers(snapshot.docs.map((doc) => ({ id: doc.id, ...(doc.data() as any) } as UserProfile)));
    });
    return unsubscribe;
  }, []);

  const normalizedQuery = searchQuery.trim().toLowerCase();

  const filtered = useMemo(() => {
    if (normalizedQuery) return devotionals;
    if (selectedCategory === "All") return devotionals;
    return devotionals.filter((item) => item.category === selectedCategory);
  }, [normalizedQuery, selectedCategory, devotionals]);

  const searchResults = useMemo(() => {
    if (!normalizedQuery) return [];

    const devotionalResults: SearchResult[] = devotionals
      .filter((item) => {
        const combined = `${item.title} ${item.scripture} ${item.content} ${item.category}`.toLowerCase();
        return combined.includes(normalizedQuery);
      })
      .map((item) => ({
        id: item.id,
        type: "Devotional",
        title: item.title,
        subtitle: `${item.category} • ${item.date}`,
        description: item.content.slice(0, 140)
      }));

    const fellowshipResults: SearchResult[] = fellowships
      .filter((item) => {
        const combined = `${item.name} ${item.description} ${item.inviteCode || ""}`.toLowerCase();
        return combined.includes(normalizedQuery);
      })
      .map((item) => ({
        id: item.id,
        type: "Fellowship",
        title: item.name,
        subtitle: item.description || "",
        description: `Category: ${item.category || "Uncategorized"}`
      }));

    const userResults: SearchResult[] = users
      .filter((item) => {
        const combined = `${item.username} ${item.contact} ${item.role}`.toLowerCase();
        return combined.includes(normalizedQuery);
      })
      .map((item) => ({
        id: item.id,
        type: "User",
        title: item.username,
        subtitle: item.contact,
        description: `Role: ${item.role}`
      }));

    return [...devotionalResults, ...fellowshipResults, ...userResults];
  }, [normalizedQuery, devotionals, fellowships, users]);

  const countWords = (text: string) => {
    return text.trim().split(/\s+/).filter(Boolean).length;
  };

  const handleSave = async () => {
    setSaving(true);
    setMessage(null);

    const wordCount = countWords(content);
    if (wordCount > 1000) {
      setMessage(`Content cannot exceed 1,000 words. Current count: ${wordCount}.`);
      setSaving(false);
      return;
    }

    if (!title.trim() || !scripture.trim() || !content.trim()) {
      setMessage("Title, scripture, and content are required.");
      setSaving(false);
      return;
    }

    try {
      const devotionalRef = doc(collection(firestore, "devotionals"));
      await setDoc(devotionalRef, {
        id: devotionalRef.id,
        title: title.trim(),
        content: content.trim(),
        scripture: scripture.trim(),
        category,
        date: new Date().toLocaleDateString(),
        imageUrl: photoUrl.trim() || null,
        createdBy: currentUser.id,
        likesCount: 0,
        commentsEnabled: true,
        timestamp: Date.now()
      });

      setTitle("");
      setContent("");
      setScripture("");
      setPhotoUrl("");
      setCategory("Faith");
      setMessage("Devotional saved successfully.");
    } catch (error) {
      setMessage("Unable to save devotional. Check your permissions.");
    } finally {
      setSaving(false);
    }
  };

  const handleSelectSearchResult = (result: SearchResult) => {
    if (result.type === "Devotional") {
      const devotional = devotionals.find((item) => item.id === result.id);
      if (devotional) {
        setSelectedDevotional(devotional);
        setSelectedSearchResult(null);
      }
    } else {
      setSelectedSearchResult(result);
      setSelectedDevotional(null);
    }
  };

  const handleDelete = async (id: string) => {
    setMessage(null);
    try {
      await deleteDoc(doc(firestore, "devotionals", id));
      setMessage("Devotional deleted.");
    } catch (error) {
      setMessage("Unable to delete devotional.");
    }
  };

  return (
    <div>
      <div className="card-heading">
        <div>
          <h2>Search</h2>
          <p className="secondary-text">Search devotionals, fellowships, and users from one global search bar.</p>
        </div>
      </div>

      <div className="form-card" style={{ marginBottom: 16 }}>
        <label>Search keyword</label>
        <input
          value={searchQuery}
          onChange={(event) => setSearchQuery(event.target.value)}
          placeholder="Search devotionals, fellowships, users..."
        />
      </div>

      <div className="toolbar">
        {categories.map((option) => (
          <button key={option} onClick={() => setSelectedCategory(option)} className={selectedCategory === option ? "active" : ""}>
            {option}
          </button>
        ))}
      </div>

      {normalizedQuery ? (
        <>
          <div className="devotional-list">
            {searchResults.length === 0 ? (
              <div className="card">No results found for "{searchQuery}".</div>
            ) : (
              searchResults.map((result) => (
                <div
                  key={`${result.type}-${result.id}`}
                  className="devotional-card"
                  style={{ cursor: "pointer" }}
                  onClick={() => handleSelectSearchResult(result)}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "center" }}>
                    <div>
                      <div className="small-badge">{result.type}</div>
                      <h3>{result.title}</h3>
                      <div className="secondary-text">{result.subtitle}</div>
                    </div>
                  </div>
                  <p className="secondary-text">{result.description}</p>
                </div>
              ))
            )}
          </div>

          {selectedSearchResult && (
            <div className="card" style={{ marginTop: 20 }}>
              <div className="card-heading">
                <div>
                  <h3>{selectedSearchResult.title}</h3>
                  <p className="secondary-text">{selectedSearchResult.subtitle}</p>
                </div>
                <button onClick={() => setSelectedSearchResult(null)}>Close</button>
              </div>
              <p>{selectedSearchResult.description}</p>
            </div>
          )}
        </>
      ) : (
        <div className="devotional-list">
          {filtered.length === 0 ? (
            <div className="card">No devotionals found for this category.</div>
          ) : (
            filtered.map((item) => (
              <div key={item.id} className="devotional-card">
                <div style={{ display: "flex", justifyContent: "space-between", gap: 16, alignItems: "center" }}>
                  <div>
                    <div className="small-badge">{item.category}</div>
                    <h3>{item.title}</h3>
                    <div className="secondary-text">{item.date} • {item.scripture}</div>
                  </div>
                  <div style={{ display: "flex", gap: 8 }}>
                    <button className="link-button" onClick={() => setSelectedDevotional(item)}>
                      View
                    </button>
                    {isAdmin && (
                      <button className="link-button" onClick={() => handleDelete(item.id)}>
                        Delete
                      </button>
                    )}
                  </div>
                </div>
                <p className="secondary-text">{item.content.slice(0, 140)}{item.content.length > 140 ? "..." : ""}</p>
              </div>
            ))
          )}
        </div>
      )}

      {selectedDevotional && (
        <div className="card" style={{ marginTop: 20 }}>
          <div className="card-heading">
            <div>
              <h3>{selectedDevotional.title}</h3>
              <p className="secondary-text">{selectedDevotional.category} • {selectedDevotional.date}</p>
            </div>
            <button onClick={() => setSelectedDevotional(null)}>Close</button>
          </div>
          {selectedDevotional.imageUrl ? (
            <img
              src={selectedDevotional.imageUrl}
              alt={selectedDevotional.title}
              style={{ width: "100%", maxHeight: 360, objectFit: "cover", borderRadius: 8, marginBottom: 16 }}
            />
          ) : null}
          <p><strong>Scripture:</strong> {selectedDevotional.scripture}</p>
          <p>{selectedDevotional.content}</p>
        </div>
      )}

      {isAdmin && (
        <div className="form-card">
          <div className="card-heading">
            <div>
              <h3>Create a new devotional</h3>
              <p className="secondary-text">Only admins can add devotionals through the web app.</p>
            </div>
          </div>
          <div className="field-row">
            <div>
              <label>Title</label>
              <input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Devotional title" />
            </div>
            <div>
              <label>Scripture</label>
              <input value={scripture} onChange={(event) => setScripture(event.target.value)} placeholder="John 3:16" />
            </div>
            <div>
              <label>Photo URL</label>
              <input value={photoUrl} onChange={(event) => setPhotoUrl(event.target.value)} placeholder="https://example.com/image.jpg" />
            </div>
          </div>
          <div style={{ marginTop: 16 }}>
            <label>Category</label>
            <select value={category} onChange={(event) => setCategory(event.target.value)}>
              {categories.filter((item) => item !== "All").map((option) => (
                <option key={option} value={option}>{option}</option>
              ))}
            </select>
          </div>
          <div style={{ marginTop: 16 }}>
            <label>Content</label>
            <textarea value={content} onChange={(event) => setContent(event.target.value)} rows={6} placeholder="Write the devotional content here." />
            <div className="secondary-text" style={{ marginTop: 8 }}>
              Word count: {countWords(content)} / 1000
            </div>
          </div>
          <div style={{ marginTop: 16, display: "flex", gap: 10 }}>
            <button onClick={handleSave} disabled={saving}>Save Devotional</button>
          </div>
          {message && <div className="notification">{message}</div>}
        </div>
      )}
    </div>
  );
}
