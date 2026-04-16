import { UserProfile } from "../types";

export interface ChatMessage {
  id: string;
  senderId: string;
  senderName: string;
  content: string;
  timestamp: number;
  fellowshipId?: string; // If present, it's a group message
  receiverId?: string;   // If present, it's a direct message
}

export interface ChatRoom {
  id: string;
  name: string;
  isGroup: boolean;
  fellowshipId?: string;
  userIds: string[];
}

export interface Comment {
  id: string;
  devotionalId: string;
  userId: string;
  userName: string;
  content: string;
  timestamp: number;
}
