import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

admin.initializeApp();

// Helper to check for Admin role
const isAdmin = async (uid: string) => {
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    return userDoc.data()?.role === "ADMIN";
};

/**
 * Creates a fellowship with a unique invite code.
 * Accessible only by Admins.
 */
export const createFellowship = functions.https.onCall(async (data, context) => {
    if (!context.auth || !(await isAdmin(context.auth.uid))) {
        throw new functions.https.HttpsError("permission-denied", "Only admins can create fellowships.");
    }

    const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const fellowshipRef = admin.firestore().collection("fellowships").doc();

    const fellowship = {
        id: fellowshipRef.id,
        name: data.name,
        description: data.description,
        leaderId: data.leaderId,
        inviteCode: inviteCode,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    };

    await fellowshipRef.set(fellowship);

    // Automatically add leader to members
    await admin.firestore().collection("fellowship_members").add({
        fellowshipId: fellowshipRef.id,
        userId: data.leaderId,
        role: "LEADER",
        joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, id: fellowshipRef.id };
});

/**
 * Saves or Updates a Devotional.
 * Accessible only by Admins.
 */
export const saveDevotional = functions.https.onCall(async (data, context) => {
    if (!context.auth || !(await isAdmin(context.auth.uid))) {
        throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    const { id, ...devotionalData } = data;
    const docId = id || admin.firestore().collection("devotionals").doc().id;

    await admin.firestore().collection("devotionals").doc(docId).set({
        ...devotionalData,
        id: docId,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    return { success: true };
});

/**
 * Join a fellowship via invite code.
 */
export const joinFellowship = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { userId, inviteCode } = data;

    // Find fellowship by invite code
    const snapshot = await admin.firestore().collection("fellowships")
        .where("inviteCode", "==", inviteCode.toUpperCase())
        .limit(1)
        .get();

    if (snapshot.empty) {
        return { success: false, message: "Invalid invite code" };
    }

    const fellowshipDoc = snapshot.docs[0];
    const fellowshipId = fellowshipDoc.id;

    // Check if already a member
    const memberSnapshot = await admin.firestore().collection("fellowship_members")
        .where("fellowshipId", "==", fellowshipId)
        .where("userId", "==", userId)
        .limit(1)
        .get();

    if (!memberSnapshot.empty) {
        return { success: true, message: "Already a member" };
    }

    // Add member
    await admin.firestore().collection("fellowship_members").add({
        fellowshipId: fellowshipId,
        userId: userId,
        role: "USER",
        joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true };
});

/**
 * Remove a member from a fellowship.
 * Accessible by Admins or Fellowship Leaders.
 */
export const removeMember = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { fellowshipId, userId } = data;
    const adminUser = await isAdmin(context.auth.uid);

    // Check if leader
    const leaderDoc = await admin.firestore().collection("fellowships").doc(fellowshipId).get();
    const isLeader = leaderDoc.data()?.leaderId === context.auth.uid;

    if (!adminUser && !isLeader) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to remove members.");
    }

    const memberSnapshot = await admin.firestore().collection("fellowship_members")
        .where("fellowshipId", "==", fellowshipId)
        .where("userId", "==", userId)
        .get();

    const batch = admin.firestore().batch();
    memberSnapshot.docs.forEach((doc) => {
        batch.delete(doc.ref);
    });
    await batch.commit();

    return { success: true };
});

/**
 * Delete a devotional.
 */
export const deleteDevotional = functions.https.onCall(async (data, context) => {
    if (!context.auth || !(await isAdmin(context.auth.uid))) {
        throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }

    await admin.firestore().collection("devotionals").doc(data.id).delete();
    return { success: true };
});

/**
 * Sends a push notification when a new direct message is created.
 */
export const onNewDirectMessage = functions.firestore
    .document("direct_messages/{messageId}")
    .onCreate(async (snapshot, context) => {
        const message = snapshot.data();
        if (!message) return;

        const senderId = message.senderId;
        const receiverId = message.receiverId;
        const content = message.content;

        // Get sender's name
        const senderDoc = await admin.firestore().collection("users").doc(senderId).get();
        const senderName = senderDoc.data()?.username || "Someone";

        // Get receiver's FCM token
        const receiverDoc = await admin.firestore().collection("users").doc(receiverId).get();
        const fcmToken = receiverDoc.data()?.fcmToken;

        if (fcmToken) {
            const payload = {
                notification: {
                    title: `New message from ${senderName}`,
                    body: content,
                    clickAction: "FLUTTER_NOTIFICATION_CLICK", // Or your Android intent filter
                },
                data: {
                    type: "direct_message",
                    senderId: senderId,
                }
            };

            try {
                await admin.messaging().sendToDevice(fcmToken, payload);
                console.log(`Notification sent to ${receiverId}`);
            } catch (error) {
                console.error("Error sending notification:", error);
            }
        }
    });
