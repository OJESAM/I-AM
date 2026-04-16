"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.deleteDevotional = exports.removeMember = exports.joinFellowship = exports.saveDevotional = exports.savePost = exports.deletePost = exports.inviteMember = exports.deleteFellowship = exports.saveFellowship = exports.createFellowship = void 0;
const functions = __importStar(require("firebase-functions"));
const admin = __importStar(require("firebase-admin"));
admin.initializeApp();
// Helper to check for Admin role
const isAdmin = async (uid) => {
    const userDoc = await admin.firestore().collection("users").doc(uid).get();
    return userDoc.data()?.role === "ADMIN";
};
/**
 * Creates a fellowship with a unique invite code.
 * Accessible only by Admins.
 */
exports.createFellowship = functions.https.onCall(async (data, context) => {
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
const getFellowship = async (fellowshipId) => {
    const fellowshipDoc = await admin.firestore().collection("fellowships").doc(fellowshipId).get();
    return fellowshipDoc.exists ? fellowshipDoc.data() : null;
};
const isFellowshipManager = async (userId, fellowshipId) => {
    const fellowship = await getFellowship(fellowshipId);
    if (!fellowship) {
        return false;
    }
    if (fellowship.leaderId === userId) {
        return true;
    }
    return await isAdmin(userId);
};
exports.saveFellowship = functions.https.onCall(async (data, context) => {
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
exports.deleteFellowship = functions.https.onCall(async (data, context) => {
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
exports.inviteMember = functions.https.onCall(async (data, context) => {
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
exports.deletePost = functions.https.onCall(async (data, context) => {
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
exports.savePost = functions.https.onCall(async (data, context) => {
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
exports.saveDevotional = functions.https.onCall(async (data, context) => {
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
exports.joinFellowship = functions.https.onCall(async (data, context) => {
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
exports.removeMember = functions.https.onCall(async (data, context) => {
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
exports.deleteDevotional = functions.https.onCall(async (data, context) => {
    if (!context.auth || !(await isAdmin(context.auth.uid))) {
        throw new functions.https.HttpsError("permission-denied", "Unauthorized.");
    }
    await admin.firestore().collection("devotionals").doc(data.id).delete();
    return { success: true };
});
