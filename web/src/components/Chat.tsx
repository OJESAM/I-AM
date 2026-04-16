import { useEffect, useRef, useState } from "react";
import { collection, addDoc, onSnapshot, orderBy, query, serverTimestamp } from "firebase/firestore";
import { firestore } from "../firebase";
import type { UserProfile } from "../types";
import type { ChatMessage } from "../types.chat";

interface Props {
  currentUser: UserProfile;
  fellowshipId?: string; // If present, group chat; else, direct chat
  receiverId?: string;   // For direct chat
}

export default function Chat({ currentUser, fellowshipId, receiverId }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let q;
    if (fellowshipId) {
      q = query(collection(firestore, "chatMessages"), orderBy("timestamp"));
    } else if (receiverId) {
      q = query(collection(firestore, "chatMessages"), orderBy("timestamp"));
    } else {
      return;
    }
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setMessages(
        snapshot.docs
          .map((doc) => ({ id: doc.id, ...(doc.data() as any) } as ChatMessage))
          .filter((msg) =>
            fellowshipId
              ? msg.fellowshipId === fellowshipId
              : (msg.senderId === currentUser.id && msg.receiverId === receiverId) ||
                (msg.senderId === receiverId && msg.receiverId === currentUser.id)
          )
      );
    });
    return unsubscribe;
  }, [fellowshipId, receiverId, currentUser.id]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const sendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim()) return;
    await addDoc(collection(firestore, "chatMessages"), {
      senderId: currentUser.id,
      senderName: currentUser.username,
      content: input,
      timestamp: Date.now(),
      ...(fellowshipId ? { fellowshipId } : {}),
      ...(receiverId ? { receiverId } : {}),
    });
    setInput("");
  };

  return (
    <div className="chat-box">
      <div className="chat-messages">
        {messages.map((msg) => (
          <div key={msg.id} className={msg.senderId === currentUser.id ? "my-message" : "other-message"}>
            <b>{msg.senderName}:</b> {msg.content}
          </div>
        ))}
        <div ref={messagesEndRef} />
      </div>
      <form onSubmit={sendMessage} className="chat-input-form">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Type a message..."
        />
        <button type="submit">Send</button>
      </form>
    </div>
  );
}
