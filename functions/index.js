const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();
const db = admin.firestore();

exports.joinContest = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login required");
  }
  const contestId = data.contestId;
  const matchId = data.matchId;
  const teamId = data.teamId;
  const userId = context.auth.uid;
  const contestRef = db.collection("contests").doc(contestId);
  const userRef = db.collection("users").doc(userId);
  return db.runTransaction(async (transaction) => {
    const contest = await transaction.get(contestRef);
    const user = await transaction.get(userRef);
    if (!contest.exists) {
      throw new functions.https.HttpsError("not-found", "Contest not found");
    }
    if (!user.exists) {
      throw new functions.https.HttpsError("not-found", "User not found");
    }
    const contestData = contest.data();
    const userData = user.data();
    const entryFee = contestData.entryFee || 0;
    const balance = userData.balance || 0;
    const joinedCount = contestData.joinedCount || 0;
    const totalSpots = contestData.totalSpots || 0;
    const spotsLeft = totalSpots - joinedCount;
    if (spotsLeft <= 0) {
      throw new functions.https.HttpsError("resource-exhausted", "Contest full!");
    }
    if (balance < entryFee) {
      throw new functions.https.HttpsError("failed-precondition", "Insufficient balance!");
    }
    const joinedRef = db.collection("joined_contests").doc(userId + "_" + contestId);
    const joined = await transaction.get(joinedRef);
    if (joined.exists) {
      throw new functions.https.HttpsError("already-exists", "Already joined!");
    }
    transaction.update(userRef, { balance: balance - entryFee });
    transaction.update(contestRef, {
      joinedCount: admin.firestore.FieldValue.increment(1)
    });
    const txnRef = db.collection("transactions").doc();
    transaction.set(txnRef, {
      userId: userId,
      type: "debit",
      amount: entryFee,
      description: "Joined " + contestData.name,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });
    transaction.set(joinedRef, {
      userId: userId,
      contestId: contestId,
      matchId: matchId,
      teamId: teamId,
      entryFee: entryFee,
      points: 0,
      rank: 0,
      joinedAt: admin.firestore.FieldValue.serverTimestamp()
    });
    return { success: true, message: "Contest joined!" };
  });
});

exports.calculatePoints = functions.https.onCall(async (data, context) => {
  const matchId = data.matchId;
  const joinedSnap = await db.collection("joined_contests")
    .where("matchId", "==", matchId).get();
  const batch = db.batch();
  joinedSnap.docs.forEach(function(doc) {
    const totalPoints = Math.floor(Math.random() * 500) + 400;
    batch.update(doc.ref, { points: totalPoints });
  });
  await batch.commit();
  const updatedSnap = await db.collection("joined_contests")
    .where("matchId", "==", matchId)
    .orderBy("points", "desc").get();
  const rankBatch = db.batch();
  updatedSnap.docs.forEach(function(doc, index) {
    rankBatch.update(doc.ref, { rank: index + 1 });
  });
  await rankBatch.commit();
  return { success: true, message: "Points calculated!" };
});

exports.distributePrizes = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Login required");
  }
  const contestId = data.contestId;
  const contestSnap = await db.collection("contests").doc(contestId).get();
  if (!contestSnap.exists) {
    throw new functions.https.HttpsError("not-found", "Contest not found");
  }
  const contest = contestSnap.data();
  const prizeBreakdown = contest.prizeBreakdown || [];
  const joinedSnap = await db.collection("joined_contests")
    .where("contestId", "==", contestId)
    .orderBy("rank").get();
  const batch = db.batch();
  joinedSnap.docs.forEach(function(doc, index) {
    const rank = index + 1;
    const prize = prizeBreakdown.find(function(p) {
      return rank >= p.rankFrom && rank <= p.rankTo;
    });
    if (prize) {
      const userRef = db.collection("users").doc(doc.data().userId);
      batch.update(userRef, {
        balance: admin.firestore.FieldValue.increment(prize.amount),
        winnings: admin.firestore.FieldValue.increment(prize.amount)
      });
      const txnRef = db.collection("transactions").doc();
      batch.set(txnRef, {
        userId: doc.data().userId,
        type: "credit",
        amount: prize.amount,
        description: "Won Rank #" + rank + " in " + contest.name,
        timestamp: admin.firestore.FieldValue.serverTimestamp()
      });
    }
  });
  batch.update(db.collection("contests").doc(contestId), { status: "completed" });
  await batch.commit();
  return { success: true, message: "Prizes distributed!" };
});

exports.createSampleContests = functions.https.onCall(async (data, context) => {
  const matchId = data.matchId;
  const contests = [
    {
      name: "Mega Contest",
      entryFee: 49,
      totalSpots: 120000,
      joinedCount: 0,
      prizePool: "Rs.50 Crores",
      firstPrize: "Rs.1 Crore",
      matchId: matchId,
      status: "open",
      isGuaranteed: true,
      isHot: true,
      prizeBreakdown: [
        { rankFrom: 1, rankTo: 1, amount: 10000000 },
        { rankFrom: 2, rankTo: 2, amount: 5000000 },
        { rankFrom: 3, rankTo: 10, amount: 1000000 }
      ]
    },
    {
      name: "Head to Head",
      entryFee: 49,
      totalSpots: 2,
      joinedCount: 0,
      prizePool: "Rs.90",
      firstPrize: "Rs.90",
      matchId: matchId,
      status: "open",
      isGuaranteed: false,
      isHot: false,
      prizeBreakdown: [{ rankFrom: 1, rankTo: 1, amount: 90 }]
    },
    {
      name: "Free Contest",
      entryFee: 0,
      totalSpots: 50000,
      joinedCount: 0,
      prizePool: "Rs.1 Lakh",
      firstPrize: "Rs.10000",
      matchId: matchId,
      status: "open",
      isGuaranteed: false,
      isHot: false,
      isFree: true,
      prizeBreakdown: [{ rankFrom: 1, rankTo: 1, amount: 10000 }]
    }
  ];
  const batch = db.batch();
  contests.forEach(function(contest) {
    const ref = db.collection("contests").doc();
    batch.set(ref, contest);
  });
  await batch.commit();
  return { success: true, message: "Contests created!" };
});

exports.getLeaderboard = functions.https.onCall(async (data, context) => {
  const contestId = data.contestId;
  const snap = await db.collection("joined_contests")
    .where("contestId", "==", contestId)
    .orderBy("points", "desc")
    .limit(100).get();
  const leaderboard = [];
  for (let i = 0; i < snap.docs.length; i++) {
    const d = snap.docs[i].data();
    const userSnap = await db.collection("users").doc(d.userId).get();
    const user = userSnap.data();
    leaderboard.push({
      rank: i + 1,
      userId: d.userId,
      name: user ? user.name : "Player",
      points: d.points || 0,
      teamId: d.teamId
    });
  }
  return { success: true, leaderboard: leaderboard };
});
