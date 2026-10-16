package com.zeke.quizpk.ui.screens;

import android.content.res.ColorStateList;
import android.os.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.TextView;
import androidx.recyclerview.widget.*;
import com.google.android.material.button.MaterialButton;
import com.zeke.quizpk.R;
import com.zeke.quizpk.multiplayer.*;
import java.util.*;

/** Incremental view of server authority. Local time is display-only. */
final class ClassicMatchView {
  private final View root;
  private final LobbyViewModel model;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final int[] choices = {
      R.id.match_option_a, R.id.match_option_b, R.id.match_option_c, R.id.match_option_d};
  private final PlayerAdapter adapter = new PlayerAdapter();
  private LobbyViewModel.State state;
  private String selectedRound;
  private int selected = -1;
  private boolean active;
  private final Runnable tick = new Runnable() {
    public void run() {
      if (active) {
        updateClockAndActions();
        main.postDelayed(this, 250);
      }
    }
  };
  ClassicMatchView(View root, LobbyViewModel model, Bundle saved) {
    this.root = root;
    this.model = model;
    if (saved != null) {
      selectedRound = saved.getString("classic_round");
      selected = saved.getInt("classic_choice", -1);
    }
    RecyclerView list = root.findViewById(R.id.match_roster);
    list.setLayoutManager(new LinearLayoutManager(root.getContext()));
    list.setAdapter(adapter);
    for (int i = 0; i < choices.length; i++) {
      final int option = i;
      root.findViewById(choices[i]).setOnClickListener(v -> {
        selected = option;
        updateClockAndActions();
      });
    }
    root.findViewById(R.id.match_submit).setOnClickListener(v -> {
      if (selected >= 0)
        model.answer(selected);
      updateClockAndActions();
    });
    root.findViewById(R.id.match_done).setOnClickListener(v -> model.leave());
  }
  void save(Bundle out) {
    out.putString("classic_round", selectedRound);
    out.putInt("classic_choice", selected);
  }
  void resume() {
    if (!active) {
      active = true;
      main.post(tick);
    }
  }
  void pause() {
    active = false;
    main.removeCallbacks(tick);
  }
  private String s(int id, Object... args) {
    return root.getContext().getString(id, args);
  }
  private void text(int id, String value) {
    TextView v = root.findViewById(id);
    if (!TextUtils.equals(v.getText(), value))
      v.setText(value);
  }
  private void visible(int id, boolean shown) {
    View v = root.findViewById(id);
    int value = shown ? View.VISIBLE : View.GONE;
    if (v.getVisibility() != value)
      v.setVisibility(value);
  }
  private void enabled(int id, boolean value) {
    View v = root.findViewById(id);
    if (v.isEnabled() != value)
      v.setEnabled(value);
  }
  private String display(String id) {
    if (id == null)
      return s(R.string.match_none);
    for (RoomProtocol.Member p : state.snapshot.members)
      if (p.participantId.equals(id))
        return p.displayName;
    return s(R.string.match_none);
  }
  void render(LobbyViewModel.State next) {
    state = next;
    if (next == null || next.snapshot == null || !next.snapshot.inMatch())
      return;
    RoomProtocol.Snapshot room = next.snapshot;
    MatchProtocol.Match m = room.match;
    MatchProtocol.Round r = m.round;
    if (!Objects.equals(selectedRound, r.id)) {
      selectedRound = r.id;
      selected = -1;
      ((androidx.core.widget.NestedScrollView) root).scrollTo(0, 0);
    }
    if (r.ownOption != null)
      selected = r.ownOption;
    text(R.id.match_progress, s(R.string.match_round, r.number, m.questionCount));
    text(R.id.match_question,
        r.text != null ? r.text
                       : s(r.state.equals("CANCELLED") ? R.string.match_interrupted_question
                                                       : R.string.match_hidden_question));
    for (int i = 0; i < 4; i++) {
      visible(choices[i], !r.options.isEmpty());
      if (!r.options.isEmpty())
        text(choices[i], ((char) ('A' + i)) + " · " + r.options.get(i));
    }
    visible(R.id.match_submit, !m.finished() && r.state.equals("ROUND_LIVE"));
    visible(R.id.match_reveal, !m.finished() && r.reveal != null);
    visible(R.id.match_records, !m.finished() && r.reveal != null);
    if (r.reveal != null) {
      text(R.id.match_reveal,
          s(R.string.match_reveal_text, r.options.get(r.reveal.correctIndex),
              r.reveal.explanation));
      text(R.id.match_records,
          s(R.string.match_records_text, display(r.reveal.firstSubmission),
              display(r.reveal.fastestCorrect)));
    }
    visible(R.id.match_done, m.finished());
    visible(R.id.match_question_card, !m.finished());
    List<Row> rows = new ArrayList<>();
    for (MatchProtocol.Standing p : m.standings) {
      String detail;
      if (m.finished())
        detail = s(R.string.match_final_detail, p.correctCount, m.questionCount, p.accuracy,
            p.fastestWins);
      else if (r.reveal != null) {
        MatchProtocol.Answer a = null;
        for (MatchProtocol.Answer answer : r.reveal.answers)
          if (answer.participantId.equals(p.participantId))
            a = answer;
        detail = a == null || a.option == null
            ? s(R.string.match_unanswered)
            : s(R.string.match_answer_detail, String.valueOf((char) ('A' + a.option)),
                  s(a.correct ? R.string.match_correct : R.string.match_wrong),
                  a.elapsedMs / 1000.0, a.points);
      } else {
        boolean online = false;
        for (RoomProtocol.Member member : room.members)
          if (member.participantId.equals(p.participantId))
            online = member.online;
        detail = s(r.submitted.contains(p.participantId) ? R.string.match_received
                : !online                                ? R.string.match_offline
                                                         : R.string.match_answering);
      }
      rows.add(new Row(p.participantId,
          s(R.string.match_player_points, p.rank, p.displayName, p.score), detail));
    }
    adapter.replace(rows);
    updateClockAndActions();
  }
  private void updateClockAndActions() {
    if (state == null || state.snapshot == null || !state.snapshot.inMatch())
      return;
    MatchProtocol.Match m = state.snapshot.match;
    MatchProtocol.Round r = m.round;
    boolean fresh = model.freshMatchConnection(),
            live = r.state.equals("ROUND_LIVE") && !m.finished();
    long end = r.state.equals("ROUND_PREPARED") ? r.startedAtMs
        : r.state.equals("ROUND_REVEALED")      ? r.revealUntilMs
                                                : r.deadlineMs;
    boolean timeLeft = model.serverNow() < end;
    text(R.id.match_clock,
        m.finished()
            ? s(R.string.match_duration, Math.max(0, (m.finishedAtMs - m.startedAtMs) / 1000))
            : fresh ? s(R.string.match_seconds, Math.max(0, (end - model.serverNow() + 999) / 1000))
                    : s(R.string.match_sync));
    String phase;
    if (m.finished()) {
      List<String> winners = new ArrayList<>();
      for (MatchProtocol.Standing p : m.standings)
        if (p.rank == 1)
          winners.add(p.displayName);
      phase = m.finishReason.equals("COMPLETED")
          ? s(winners.size() == 1 ? R.string.match_winner : R.string.match_tie,
                TextUtils.join(", ", winners))
          : s(R.string.match_interrupted);
    } else if (!fresh)
      phase = s(R.string.match_sync);
    else if (state.pending || state.actionBusy)
      phase = s(R.string.match_pending);
    else if (r.state.equals("ROUND_PREPARED"))
      phase = s(R.string.match_prepare);
    else if (r.reveal != null)
      phase = s(R.string.match_next);
    else if (r.ownOption != null)
      phase = s(R.string.match_locked);
    else if (!timeLeft)
      phase = s(R.string.match_wait_deadline);
    else
      phase = s(R.string.match_choose);
    text(R.id.match_phase, phase);
    boolean answerable =
        live && fresh && timeLeft && r.ownOption == null && !state.pending && !state.actionBusy;
    enabled(R.id.match_submit, answerable && selected >= 0);
    enabled(R.id.match_done, state.canWrite());
    for (int i = 0; i < 4; i++) {
      MaterialButton b = root.findViewById(choices[i]);
      enabled(choices[i], answerable);
      int color = r.reveal != null && i == r.reveal.correctIndex ? R.color.success
          : r.reveal != null && selected == i                    ? R.color.error
          : selected == i                                        ? R.color.primary
                                                                 : R.color.outline;
      ColorStateList tint = ColorStateList.valueOf(root.getContext().getColor(color));
      if (!tint.equals(b.getStrokeColor()))
        b.setStrokeColor(tint);
      if (b.isSelected() != (selected == i))
        b.setSelected(selected == i);
    }
  }
  private static final class Row {
    final String id, title, detail;
    Row(String id, String title, String detail) {
      this.id = id;
      this.title = title;
      this.detail = detail;
    }
  }
  private final class PlayerAdapter extends RecyclerView.Adapter<PlayerHolder> {
    List<Row> items = Collections.emptyList();
    void replace(List<Row> next) {
      final List<Row> old = items;
      DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
        public int getOldListSize() {
          return old.size();
        }
        public int getNewListSize() {
          return next.size();
        }
        public boolean areItemsTheSame(int a, int b) {
          return old.get(a).id.equals(next.get(b).id);
        }
        public boolean areContentsTheSame(int a, int b) {
          return old.get(a).title.equals(next.get(b).title)
              && old.get(a).detail.equals(next.get(b).detail);
        }
      });
      items = next;
      diff.dispatchUpdatesTo(this);
    }
    public PlayerHolder onCreateViewHolder(ViewGroup parent, int type) {
      return new PlayerHolder(LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_match_player, parent, false));
    }
    public void onBindViewHolder(PlayerHolder h, int position) {
      Row r = items.get(position);
      h.title.setText(r.title);
      h.detail.setText(r.detail);
    }
    public int getItemCount() {
      return items.size();
    }
  }
  private static final class PlayerHolder extends RecyclerView.ViewHolder {
    final TextView title, detail;
    PlayerHolder(View v) {
      super(v);
      title = v.findViewById(R.id.match_player_name);
      detail = v.findViewById(R.id.match_player_detail);
    }
  }
}
