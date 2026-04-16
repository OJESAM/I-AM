export interface Devotional {
  id: string;
  title: string;
  date: string;
  content: string;
  scripture: string;
  category: string;
  imageUrl?: string;
  createdBy: string;
  likesCount: number;
  commentsEnabled: boolean;
  timestamp: number;
}

export interface Fellowship {
  id: string;
  name: string;
  description: string;
  category?: string;
  locationName: string;
  latitude: number;
  longitude: number;
  inviteCode: string;
  leaderId: string;
}

export interface FellowshipPost {
  id: string;
  fellowshipId: string;
  userId: string;
  userName: string;
  content: string;
  mediaUrl?: string;
  mediaType?: string;
  timestamp: number;
}

export interface FellowshipMember {
  id: string;
  fellowshipId: string;
  userId: string;
  role: string;
}

export interface UserProfile {
  id: string;
  username: string;
  contact: string;
  isVerified: boolean;
  role: string;
}
