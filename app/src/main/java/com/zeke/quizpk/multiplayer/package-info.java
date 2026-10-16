/**
 * Future private rooms: classic has up to eight contestants including a playing
 * host; moderator mode has a separate nonplaying moderator plus eight contestants.
 * Trusted server code must enforce roles, capacity, answer secrecy and timing.
 * Match-start leaves moderator round one READY until explicitly started.
 * See PLAN.md; no multiplayer transport/state machine is implemented in M1.
 */
package com.zeke.quizpk.multiplayer;
