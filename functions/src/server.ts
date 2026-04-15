import express from 'express';
import * as admin from 'firebase-admin';
import cors from 'cors';
import * as dotenv from 'dotenv';

dotenv.config();

const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT
    ? JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)
    : undefined;

if (serviceAccount) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount)
    });
} else {
    admin.initializeApp();
}

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

// Helper to check for Admin role
const isAdmin = async (uid: string) => {
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    return userDoc.data()?.role === "ADMIN";
};

/**
 * Authentication Middleware
 */
const authenticate = async (req: any, res: any, next: any) => {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).send('Unauthorized');
    }
    const idToken = authHeader.split('Bearer ')[1];
    try {
        const decodedToken = await admin.auth().verifyIdToken(idToken);
        req.user = decodedToken;
        next();
    } catch (error) {
        res.status(401).send('Unauthorized');
    }
};

/**
 * Routes
 */

app.post('/createFellowship', authenticate, async (req: any, res: any) => {
    if (!(await isAdmin(req.user.uid))) {
        return res.status(403).send('Only admins can create fellowships.');
    }

    const { name, description, leaderId } = req.body;
    const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const fellowshipRef = admin.firestore().collection("fellowships").doc();

    const fellowship = {
        id: fellowshipRef.id,
        name,
        description,
        leaderId,
        inviteCode,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    };

    await fellowshipRef.set(fellowship);
    await admin.firestore().collection("fellowship_members").add({
        fellowshipId: fellowshipRef.id,
        userId: leaderId,
        role: "LEADER",
        joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    res.send({ success: true, id: fellowshipRef.id });
});

app.post('/saveDevotional', authenticate, async (req: any, res: any) => {
    if (!(await isAdmin(req.user.uid))) {
        return res.status(403).send('Unauthorized');
    }

    const { id, ...devotionalData } = req.body;
    const docId = id || admin.firestore().collection("devotionals").doc().id;

    await admin.firestore().collection("devotionals").doc(docId).set({
        ...devotionalData,
        id: docId,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    res.send({ success: true });
});

app.post('/deleteDevotional', authenticate, async (req: any, res: any) => {
    if (!(await isAdmin(req.user.uid))) {
        return res.status(403).send('Unauthorized');
    }

    const { id } = req.body;
    await admin.firestore().collection("devotionals").doc(id).delete();

    res.send({ success: true });
});

app.post('/updateUserRole', authenticate, async (req: any, res: any) => {
    if (!(await isAdmin(req.user.uid))) {
        return res.status(403).send('Only admins can update user roles.');
    }

    const { userId, role } = req.body;
    await admin.firestore().collection("users").doc(userId).update({ role });

    res.send({ success: true });
});

app.post('/updateLivestreamSettings', authenticate, async (req: any, res: any) => {
    if (!(await isAdmin(req.user.uid))) {
        return res.status(403).send('Unauthorized');
    }

    await admin.firestore().collection("settings").doc("livestream").set(req.body, { merge: true });

    res.send({ success: true });
});

// Real-time Push Notification Listener
// Note: In Express, we start this listener when the server starts.
admin.firestore().collection("direct_messages").onSnapshot(snapshot => {
    snapshot.docChanges().forEach(async (change) => {
        if (change.type === 'added') {
            const message = change.doc.data();
            const senderId = message.senderId;
            const receiverId = message.receiverId;
            const content = message.content;

            const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
            const senderName = senderDoc.data()?.username || "Someone";

            const receiverDoc = await admin.firestore().collection("users").doc(receiverId).get();
            const fcmToken = receiverDoc.data()?.fcmToken;

            if (fcmToken) {
                const payload = {
                    notification: {
                        title: `New message from ${senderName}`,
                        body: content,
                    },
                    data: {
                        type: "direct_message",
                        senderId: senderId,
                    }
                };
                try {
                    await admin.messaging().send({
                        token: fcmToken,
                        notification: payload.notification,
                        data: payload.data
                    });
                } catch (error) {
                    console.error("Error sending notification:", error);
                }
            }
        }
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
