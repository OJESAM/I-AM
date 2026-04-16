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
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const inviteCode = Math.random().toString(36).substring(2, 8).toUpperCase();
    const fellowshipRef = admin.firestore().collection("fellowships").doc();

    const fellowship = {
        id: fellowshipRef.id,
        name: data.name,
        description: data.description || "",
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

const getFellowship = async (fellowshipId: string) => {
    const fellowshipDoc = await admin.firestore().collection("fellowships").doc(fellowshipId).get();
    return fellowshipDoc.exists ? fellowshipDoc.data() : null;
};

const isFellowshipManager = async (userId: string, fellowshipId: string) => {
    const fellowship = await getFellowship(fellowshipId);
    if (!fellowship) {
        return false;
    }
    if (fellowship.leaderId === userId) {
        return true;
    }
    return await isAdmin(userId);
};

export const saveFellowship = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { fellowshipId, name, description } = data;
    if (!fellowshipId || !name) {
        throw new functions.https.HttpsError("invalid-argument", "Fellowship ID and name are required.");
    }

    if (!(await isFellowshipManager(context.auth.uid, fellowshipId))) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to edit this fellowship.");
    }

    await admin.firestore().collection("fellowships").doc(fellowshipId).set({
        name,
        description: description || ""
    }, { merge: true });

    return { success: true };
});

export const deleteFellowship = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { fellowshipId } = data;
    if (!fellowshipId) {
        throw new functions.https.HttpsError("invalid-argument", "Fellowship ID is required.");
    }

    if (!(await isFellowshipManager(context.auth.uid, fellowshipId))) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to delete this fellowship.");
    }

    const fellowshipRef = admin.firestore().collection("fellowships").doc(fellowshipId);
    const postsSnapshot = await admin.firestore().collection("fellowship_posts").where("fellowshipId", "==", fellowshipId).get();
    const membersSnapshot = await admin.firestore().collection("fellowship_members").where("fellowshipId", "==", fellowshipId).get();

    const batch = admin.firestore().batch();
    batch.delete(fellowshipRef);
    postsSnapshot.docs.forEach((doc) => batch.delete(doc.ref));
    membersSnapshot.docs.forEach((doc) => batch.delete(doc.ref));
    await batch.commit();

    return { success: true };
});

export const inviteMember = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { fellowshipId, email } = data;
    if (!fellowshipId || !email) {
        throw new functions.https.HttpsError("invalid-argument", "Fellowship ID and email are required.");
    }

    if (!(await isFellowshipManager(context.auth.uid, fellowshipId))) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to invite members.");
    }

    const userSnapshot = await admin.firestore().collection("users").where("contact", "==", email).limit(1).get();
    if (userSnapshot.empty) {
        throw new functions.https.HttpsError("not-found", "No user found with that email.");
    }

    const userId = userSnapshot.docs[0].id;
    const memberSnapshot = await admin.firestore().collection("fellowship_members")
        .where("fellowshipId", "==", fellowshipId)
        .where("userId", "==", userId)
        .limit(1)
        .get();

    if (!memberSnapshot.empty) {
        return { success: true, message: "User is already a member." };
    }

    await admin.firestore().collection("fellowship_members").add({
        fellowshipId,
        userId,
        role: "USER",
        joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, message: "Member invited successfully." };
});

export const deletePost = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { postId } = data;
    if (!postId) {
        throw new functions.https.HttpsError("invalid-argument", "Post ID is required.");
    }

    const postDoc = await admin.firestore().collection("fellowship_posts").doc(postId).get();
    if (!postDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Post not found.");
    }

    const postData = postDoc.data();
    const fellowshipId = postData?.fellowshipId;
    const isOwner = postData?.userId === context.auth.uid;
    const isManager = fellowshipId ? await isFellowshipManager(context.auth.uid, fellowshipId) : false;

    if (!isOwner && !isManager) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to delete this post.");
    }

    await admin.firestore().collection("fellowship_posts").doc(postId).delete();
    return { success: true };
});

export const savePost = functions.https.onCall(async (data, context) => {
    if (!context.auth) {
        throw new functions.https.HttpsError("unauthenticated", "User must be logged in.");
    }

    const { postId, content } = data;
    if (!postId || !content?.trim()) {
        throw new functions.https.HttpsError("invalid-argument", "Post ID and content are required.");
    }

    const postDoc = await admin.firestore().collection("fellowship_posts").doc(postId).get();
    if (!postDoc.exists) {
        throw new functions.https.HttpsError("not-found", "Post not found.");
    }

    const postData = postDoc.data();
    const fellowshipId = postData?.fellowshipId;
    const isOwner = postData?.userId === context.auth.uid;
    const isManager = fellowshipId ? await isFellowshipManager(context.auth.uid, fellowshipId) : false;

    if (!isOwner && !isManager) {
        throw new functions.https.HttpsError("permission-denied", "Not authorized to edit this post.");
    }

    await admin.firestore().collection("fellowship_posts").doc(postId).set({
        content: content.trim()
    }, { merge: true });

    return { success: true };
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
