# I AM Web + Firebase

This repository contains the web client for the `I AM` app, built with Vite, React, and Firebase.

## Repository Setup

From the repository root:

1. Install root dependencies if needed:
   - `npm install`
2. Install web app dependencies:
   - `cd web && npm install`

## Local Development

From the `web` directory:

```bash
npm run dev
```

Then open the local Vite URL shown in the terminal.

## Production Build

From the repository root:

```bash
npm run build
```

This runs `cd web && npm ci && npm run build` and produces `web/dist`.

## Deployment

From the repository root:

```bash
npm run deploy
```

This builds the web app and deploys Firebase Hosting to the configured Firebase project.

## Firebase Hosting

- Hosting output: `web/dist`
- SPA rewrite: all routes point to `/index.html`

## Environment files

- `web/.env.example`
- `web/.env.production.example`

Copy the example and fill in your Firebase web credentials.

## Notes

- The web app stores data in Firestore collections such as `users`, `devotionals`, `fellowships`, `fellowship_members`, and `fellowship_posts`.
- Firestore security rules have been updated to protect user-created data.
