# I AM Web Version

This is a Vite + React + Firebase web version of the I AM app.

## Setup

1. Copy `.env.example` to `.env`.
2. Fill in your Firebase Web app values.
3. Install dependencies:
   - `npm install`
4. Run locally:
   - `npm run dev`

## Production Build

1. Build the app in `web`:
   - `npm run build`
2. Deploy to Firebase Hosting from the repo root:
   - `npm run deploy`

## Features

- Firebase Authentication (email/password)
- Global search across devotionals, fellowships, and users
- Devotional creation with photo URL, scripture, category, and 1,000-word limit
- Fellowship browsing, join by invite code, post creation, and member management
- Admin-only devotional management and fellowship leader controls

## Implementation Notes

- The web app writes directly to Firestore for devotionals, fellowships, members, and posts.
- Cloud Functions are no longer required for devotional save/delete.
- `web/.env.production.example` is provided for production Firebase config.
- `web/dist` is the Firebase Hosting output folder.
