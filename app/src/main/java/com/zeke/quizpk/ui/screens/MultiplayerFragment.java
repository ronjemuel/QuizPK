package com.zeke.quizpk.ui.screens;

import android.content.*;
import android.os.*;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.zeke.quizpk.MainActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.data.remote.BackendCredentials;
import com.zeke.quizpk.multiplayer.RoomProtocol;
import java.text.DateFormat;
import java.util.*;

/**
 * Native Online entry/forms/lobbies. No match control or local server in M4.2.
 */
public final class MultiplayerFragment extends Fragment {
  private enum Page { NETWORK, ONLINE, CREATE, JOIN, LOBBY, TERMINAL, MATCH }
  private Page page = Page.NETWORK;
  private LobbyViewModel model;
  private RosterAdapter roster;
  private ClassicMatchView classic;
  private Bundle matchDraft;
  private OnBackPressedCallback back;
  private androidx.appcompat.app.AlertDialog dialog;
  private Bundle draft;
  private boolean viewRestored;
  private long handledConfirmation = -1, viewEpoch;
  public MultiplayerFragment() {
    super(R.layout.fragment_multiplayer);
  }
  @Override
  public void onCreate(@Nullable Bundle state) {
    super.onCreate(state);
    if (state != null)
      try {
        page = Page.valueOf(state.getString("lobby_page", "NETWORK"));
        draft = state.getBundle("lobby_draft");
        matchDraft = state.getBundle("classic_draft");
      } catch (IllegalArgumentException ignored) {
      }
  }
  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle saved) {
    viewRestored = false;
    viewEpoch++;
    model = new ViewModelProvider(requireActivity()).get(LobbyViewModel.class);
    classic = new ClassicMatchView(view, model, matchDraft);
    view.findViewById(R.id.mp_start_match).setOnClickListener(v -> model.startMatch());
    fillSettings(view, new RoomProtocol.Settings("All Categories", "Normal", 10, 15));
    RecyclerView list = view.findViewById(R.id.mp_roster);
    roster = new RosterAdapter();
    list.setLayoutManager(new LinearLayoutManager(requireContext()));
    list.setAdapter(roster);
    view.findViewById(R.id.mp_online).setOnClickListener(v -> {
      page(Page.ONLINE);
      model.activate();
    });
    view.findViewById(R.id.mp_choose_connection).setOnClickListener(v -> {
      page = Page.NETWORK;
      model.deactivate();
      render(model.state.getValue());
    });
    view.findViewById(R.id.create_room).setOnClickListener(v -> page(Page.CREATE));
    view.findViewById(R.id.join_room).setOnClickListener(v -> page(Page.JOIN));
    view.findViewById(R.id.mp_create_back).setOnClickListener(v -> page(Page.ONLINE));
    view.findViewById(R.id.mp_join_back).setOnClickListener(v -> page(Page.ONLINE));
    view.findViewById(R.id.mp_create_submit).setOnClickListener(v -> create());
    view.findViewById(R.id.mp_join_submit).setOnClickListener(v -> join());
    ((RadioGroup) view.findViewById(R.id.mp_mode))
        .setOnCheckedChangeListener(
            (g, id)
                -> text(R.id.mp_role_help,
                    getString(id == R.id.mp_moderator ? R.string.mp_moderator_help
                                                      : R.string.mp_classic_help)));
    view.findViewById(R.id.mp_retry).setOnClickListener(v -> model.retry());
    view.findViewById(R.id.mp_connect).setOnClickListener(v -> connect());
    view.findViewById(R.id.mp_ready).setOnClickListener(v -> {
      RoomProtocol.Snapshot s = model.state.getValue().snapshot;
      if (s != null && s.self() != null)
        model.ready(!s.self().ready);
    });
    view.findViewById(R.id.mp_edit_settings).setOnClickListener(v -> editSettings());
    view.findViewById(R.id.mp_leave).setOnClickListener(v -> leave());
    view.findViewById(R.id.mp_copy_code).setOnClickListener(v -> copy());
    view.findViewById(R.id.mp_share_code).setOnClickListener(v -> share());
    view.findViewById(R.id.mp_terminal_back).setOnClickListener(v -> {
      if (model.dismissTerminal())
        page(Page.ONLINE);
    });
    back = new OnBackPressedCallback(page != Page.NETWORK) {
      @Override
      public void handleOnBackPressed() {
        goBack();
      }
    };
    requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), back);
    render(model.state.getValue());
    model.state.observe(getViewLifecycleOwner(), this::onState);
  }
  @Override
  public void onViewStateRestored(@Nullable Bundle state) {
    super.onViewStateRestored(state);
    viewRestored = true;
    restoreDraft();
    onState(model.state.getValue());
  }
  @Override
  public void onResume() {
    super.onResume();
    if (page != Page.NETWORK)
      model.activate();
    classic.resume();
  }
  @Override
  public void onPause() {
    if (classic != null) {
      matchDraft = new Bundle();
      classic.save(matchDraft);
      classic.pause();
    }
    captureDraft();
    super.onPause();
  }
  @Override
  public void onStop() {
    if (model != null)
      model.deactivate();
    super.onStop();
  }
  @Override
  public void onDestroyView() {
    if (classic != null) {
      matchDraft = new Bundle();
      classic.save(matchDraft);
      classic.pause();
      classic = null;
    }
    captureDraft();
    viewRestored = false;
    viewEpoch++;
    if (dialog != null) {
      dialog.dismiss();
      dialog = null;
    }
    roster = null;
    back = null;
    super.onDestroyView();
  }
  @Override
  public void onSaveInstanceState(@NonNull Bundle state) {
    captureDraft();
    state.putString("lobby_page", page.name());
    if (draft != null)
      state.putBundle("lobby_draft", new Bundle(draft));
    if (classic != null) {
      matchDraft = new Bundle();
      classic.save(matchDraft);
    }
    if (matchDraft != null)
      state.putBundle("classic_draft", new Bundle(matchDraft));
    super.onSaveInstanceState(state);
  }
  private void page(Page next) {
    if (page == next)
      return;
    captureDraft();
    hideKeyboard();
    page = next;
    render(model.state.getValue());
    ((androidx.core.widget.NestedScrollView) requireView()).scrollTo(0, 0);
  }
  private void onState(LobbyViewModel.State state) {
    if (!viewRestored || getView() == null)
      return;
    // Connection/busy publications carry cached projections, not navigation.
    if (state.connected && !state.busy && state.snapshot != null
        && state.confirmation != handledConfirmation) {
      handledConfirmation = state.confirmation;
      if (page != Page.NETWORK) {
        RoomProtocol.Snapshot room = state.snapshot;
        if (room.hasRoom())
          page(room.inMatch() ? Page.MATCH : room.lobby() ? Page.LOBBY : Page.TERMINAL);
        else if (page == Page.LOBBY || page == Page.TERMINAL || page == Page.MATCH)
          page(Page.ONLINE);
      }
    } else if ((page == Page.LOBBY || page == Page.TERMINAL || page == Page.MATCH)
        && (state.code.equals("AUTH_REQUIRED") || state.code.equals("SETUP_REQUIRED"))) {
      page(Page.ONLINE);
    }
    render(state);
  }
  private static final int[] EDITORS = {R.id.mp_create_name, R.id.mp_join_name, R.id.mp_join_code};
  private static final int[] SPINNERS = {
      R.id.mp_category, R.id.mp_difficulty, R.id.mp_count, R.id.mp_timer};
  private void captureDraft() {
    if (!viewRestored || getView() == null)
      return;
    Bundle saved = new Bundle();
    for (int id : EDITORS) {
      EditText field = requireView().findViewById(id);
      saved.putString("text" + id, field.getText().toString());
      saved.putInt("start" + id, field.getSelectionStart());
      saved.putInt("end" + id, field.getSelectionEnd());
    }
    for (int id : SPINNERS)
      saved.putInt(
          "spinner" + id, ((Spinner) requireView().findViewById(id)).getSelectedItemPosition());
    saved.putInt(
        "mode", ((RadioGroup) requireView().findViewById(R.id.mp_mode)).getCheckedRadioButtonId());
    View focus = requireView().findFocus();
    int focusId = focus == null ? View.NO_ID : focus.getId();
    if ((focusId == View.NO_ID || focusId == R.id.mp_scroll) && draft != null
        && page.name().equals(draft.getString("page")))
      focusId = draft.getInt("focus", View.NO_ID);
    saved.putInt("focus", focusId);
    saved.putString("page", page.name());
    saved.putInt("scroll", requireView().getScrollY());
    draft = saved;
  }
  private void restoreDraft() {
    if (draft == null)
      return;
    final Bundle saved = draft;
    for (int id : EDITORS) {
      EditText field = requireView().findViewById(id);
      String value = saved.getString("text" + id, "");
      if (!TextUtils.equals(field.getText(), value))
        field.setText(value);
      int start = saved.getInt("start" + id, value.length()), end = saved.getInt("end" + id, start);
      if (start >= 0 && end >= 0)
        field.setSelection(Math.min(start, field.length()), Math.min(end, field.length()));
    }
    for (int id : SPINNERS) {
      Spinner spinner = requireView().findViewById(id);
      int selected = saved.getInt("spinner" + id, spinner.getSelectedItemPosition());
      if (selected >= 0 && selected < spinner.getCount()
          && selected != spinner.getSelectedItemPosition())
        spinner.setSelection(selected);
    }
    ((RadioGroup) requireView().findViewById(R.id.mp_mode))
        .check(saved.getInt("mode", R.id.mp_classic));
    restoreFocus(saved);
    final androidx.core.widget.NestedScrollView scroll =
        (androidx.core.widget.NestedScrollView) requireView();
    final long epoch = viewEpoch;
    final Page restoredPage = page;
    final boolean[] completed = {false};
    View.OnLayoutChangeListener restore = new View.OnLayoutChangeListener() {
      @Override
      public void onLayoutChange(
          View v, int l, int t, int r, int b, int ol, int ot, int or, int ob) {
        scroll.removeOnLayoutChangeListener(this);
        if (completed[0])
          return;
        completed[0] = true;
        if (getView() == scroll && epoch == viewEpoch && page == restoredPage
            && draftUnchanged(saved)) {
          // The first layout can run TextView's focus/selection initialization
          // after onViewStateRestored. Finish restoration after that pass.
          restoreFocus(saved);
          scroll.scrollTo(0, saved.getInt("scroll", 0));
        }
      }
    };
    scroll.addOnLayoutChangeListener(restore);
    scroll.post(() -> {
      if (scroll.getHeight() > 0) {
        scroll.removeOnLayoutChangeListener(restore);
        if (completed[0])
          return;
        completed[0] = true;
        if (getView() == scroll && epoch == viewEpoch && page == restoredPage
            && draftUnchanged(saved)) {
          restoreFocus(saved);
          scroll.scrollTo(0, saved.getInt("scroll", 0));
        }
      }
    });
  }
  private boolean draftUnchanged(Bundle saved) {
    for (int id : EDITORS)
      if (!TextUtils.equals(((EditText) requireView().findViewById(id)).getText(),
              saved.getString("text" + id, "")))
        return false;
    return true;
  }
  private boolean restoreFocus(Bundle saved) {
    View focus = requireView().findViewById(saved.getInt("focus", View.NO_ID));
    if (focus == null)
      return true;
    if (!focus.isShown() || !focus.isEnabled())
      return false;
    if (focus instanceof EditText) {
      EditText editor = (EditText) focus;
      if (!TextUtils.equals(editor.getText(), saved.getString("text" + focus.getId(), "")))
        return true; // A new edit wins over a deferred restoration callback.
      focus.requestFocus();
      int start = saved.getInt("start" + focus.getId(), editor.length());
      int end = saved.getInt("end" + focus.getId(), start);
      if (start >= 0 && end >= 0)
        editor.setSelection(Math.min(start, editor.length()), Math.min(end, editor.length()));
    } else {
      focus.requestFocus();
    }
    return focus.hasFocus();
  }
  private void text(int id, CharSequence value) {
    TextView v = requireView().findViewById(id);
    if (!TextUtils.equals(v.getText(), value))
      v.setText(value);
  }
  private void visible(int id, boolean value) {
    View v = requireView().findViewById(id);
    int visibility = value ? View.VISIBLE : View.GONE;
    if (v.getVisibility() != visibility)
      v.setVisibility(visibility);
  }
  private void enabled(int id, boolean value) {
    View v = requireView().findViewById(id);
    if (v.isEnabled() != value)
      v.setEnabled(value);
  }
  private void render(LobbyViewModel.State state) {
    if (getView() == null)
      return;
    RoomProtocol.Snapshot room = state.snapshot;
    visible(R.id.mp_network_panel, page == Page.NETWORK);
    visible(R.id.mp_content, page != Page.NETWORK);
    visible(R.id.mp_entry_panel, page == Page.ONLINE);
    visible(R.id.mp_create_panel, page == Page.CREATE);
    visible(R.id.mp_join_panel, page == Page.JOIN);
    visible(R.id.mp_lobby_panel, page == Page.LOBBY);
    visible(R.id.mp_terminal_panel, page == Page.TERMINAL);
    visible(R.id.mp_match_panel, page == Page.MATCH);
    classic.render(page == Page.MATCH ? state : null);
    back.setEnabled(page != Page.NETWORK);
    int heading = page == Page.MATCH ? R.string.match_title
        : page == Page.CREATE        ? R.string.mp_create_title
        : page == Page.JOIN          ? R.string.mp_join_title
        : page == Page.TERMINAL      ? R.string.mp_terminal_title
        : page == Page.LOBBY && room != null
        ? (room.mode.equals("MODERATOR") ? R.string.mp_lobby_moderator : R.string.mp_lobby_classic)
        : R.string.mp_entry_title;
    text(R.id.mp_heading, getString(heading));
    text(R.id.mp_status, getString(status(state.code)));
    View progress = requireView().findViewById(R.id.mp_progress);
    int progressVisibility =
        state.busy && (state.actionBusy || !state.connected) ? View.VISIBLE : View.INVISIBLE;
    if (progress.getVisibility() != progressVisibility)
      progress.setVisibility(progressVisibility);
    visible(R.id.mp_retry,
        (!state.connected || state.pending)
            && !Arrays.asList("SETUP_REQUIRED", "AUTH_REQUIRED", "CHOOSE_NETWORK", "PAUSED")
                .contains(state.code));
    text(R.id.mp_retry, getString(state.pending ? R.string.mp_retry_action : R.string.mp_retry));
    enabled(R.id.mp_retry, !state.busy && SystemClock.elapsedRealtime() >= state.retryAtElapsedMs);
    enabled(R.id.mp_connect, !state.busy);
    enabled(R.id.mp_choose_connection, !state.busy);
    boolean write = state.canWrite() && page != Page.NETWORK;
    enabled(R.id.create_room, write && (room == null || !room.hasRoom()));
    enabled(R.id.join_room, write && (room == null || !room.hasRoom()));
    enabled(R.id.mp_create_submit, write);
    enabled(R.id.mp_join_submit, write);
    visible(R.id.mp_start_help, page == Page.LOBBY && room != null && room.mode.equals("CLASSIC"));
    visible(
        R.id.mp_start_match, page == Page.LOBBY && room != null && room.owner && room.matchEnabled);
    enabled(R.id.mp_start_match,
        write && room != null && room.members.size() >= 2
            && room.members.stream().allMatch(m -> m.online && m.ready));
    for (int id :
        new int[] {R.id.mp_create_name, R.id.mp_join_name, R.id.mp_join_code, R.id.mp_classic,
            R.id.mp_moderator, R.id.mp_category, R.id.mp_difficulty, R.id.mp_count, R.id.mp_timer})
      enabled(id, !state.actionBusy && !state.pending);
    if (page == Page.LOBBY && room != null && room.lobby()) {
      text(R.id.mp_invite_code, room.inviteCode);
      text(R.id.mp_expiry,
          getString(R.string.mp_expiry,
              DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                  .format(new Date(room.expiresAtMs))));
      List<RoomProtocol.Member> contestants = new ArrayList<>();
      RoomProtocol.Member moderator = null;
      int ready = 0;
      for (RoomProtocol.Member member : room.members)
        if (member.role.equals("MODERATOR"))
          moderator = member;
        else {
          contestants.add(member);
          if (member.ready)
            ready++;
        }
      visible(R.id.mp_moderator_card, moderator != null);
      if (moderator != null) {
        text(R.id.mp_moderator_name,
            moderator.participantId.equals(room.selfId)
                ? getString(R.string.mp_you, moderator.displayName)
                : moderator.displayName);
        text(R.id.mp_moderator_presence,
            getString(
                moderator.online ? R.string.mp_moderator_online : R.string.mp_moderator_offline));
      }
      text(R.id.mp_capacity, getString(R.string.mp_contestants, contestants.size()));
      text(R.id.mp_ready_summary,
          getResources().getQuantityString(R.plurals.mp_ready_count, ready, ready));
      visible(R.id.mp_empty_roster, contestants.isEmpty());
      roster.replace(contestants, room.selfId, room.owner);
      visible(R.id.mp_host_status, !room.hostOnline);
      text(R.id.mp_room_settings,
          getString(R.string.mp_settings_summary, room.settings.category, room.settings.difficulty,
              room.settings.questionCount, room.settings.timerSeconds));
      visible(R.id.mp_edit_settings, room.owner);
      enabled(R.id.mp_edit_settings, write && room.hostOnline);
      visible(R.id.mp_ready, room.selfRole.equals("CONTESTANT"));
      enabled(R.id.mp_ready, write && room.self() != null && room.self().online);
      text(R.id.mp_ready,
          getString(room.self() != null && room.self().ready ? R.string.mp_not_ready
                                                             : R.string.mp_ready));
      text(R.id.mp_leave, getString(room.owner ? R.string.mp_close : R.string.mp_leave));
      enabled(R.id.mp_leave, write);
      enabled(R.id.mp_copy_code, state.connected && !state.busy);
      enabled(R.id.mp_share_code, state.connected && !state.busy);
    } else {
      text(R.id.mp_invite_code, "");
      text(R.id.mp_moderator_name, "");
      text(R.id.mp_room_settings, "");
      roster.replace(Collections.emptyList(), null, false);
    }
  }
  private int status(String code) {
    switch (code) {
      case "CHOOSE_NETWORK":
        return R.string.mp_choose_status;
      case "TOO_FEW_CONTESTANTS":
        return R.string.match_need_players;
      case "PLAYERS_NOT_READY":
        return R.string.match_need_ready;
      case "INSUFFICIENT_QUESTIONS":
        return R.string.match_insufficient;
      case "CLASSIC_UNAVAILABLE":
      case "UPDATE_REQUIRED":
        return R.string.match_unavailable;
      case "ROUND_NOT_LIVE":
      case "ANSWER_TOO_LATE":
      case "STALE_ROUND":
        return R.string.match_stale_round;
      case "ANSWER_ALREADY_ACCEPTED":
        return R.string.match_locked;
      case "MATCH_IN_PROGRESS":
        return R.string.match_back_body;
      case "SETUP_REQUIRED":
        return R.string.mp_setup_status;
      case "CONNECTING":
        return R.string.mp_connecting_status;
      case "CONNECTED":
        return R.string.mp_connected_status;
      case "NO_ROOM":
        return R.string.mp_no_room_status;
      case "SENDING":
        return R.string.mp_sending_status;
      case "COMMAND_PENDING":
        return R.string.mp_pending_status;
      case "AUTH_REQUIRED":
        return R.string.mp_auth_status;
      case "ROOM_FULL":
        return R.string.mp_full_status;
      case "ROOM_NOT_FOUND":
      case "ROOM_UNAVAILABLE":
        return R.string.mp_missing_status;
      case "ROOM_RATE_LIMITED":
      case "ROOM_LIMIT":
        return R.string.mp_limit_status;
      case "REVISION_CONFLICT":
      case "STALE_SNAPSHOT":
        return R.string.mp_conflict_status;
      case "ROOMS_UNAVAILABLE":
      case "PROTOCOL_UNSUPPORTED":
        return R.string.mp_unsupported_status;
      case "ALREADY_IN_ROOM":
        return R.string.mp_already_status;
      case "HOST_UNAVAILABLE":
        return R.string.mp_host_missing;
      case "HEARTBEAT_REQUIRED":
      case "INTERRUPTED":
      case "PAUSED":
      case "NO_CONNECTION":
      case "TIMEOUT":
        return R.string.mp_network_status;
      case "SECURE_CONNECTION_FAILED":
      case "HTTPS_REQUIRED":
        return R.string.mp_secure_status;
      case "FORBIDDEN":
        return R.string.mp_forbidden_status;
      case "INVALID_COMMAND":
      case "INVALID_MODE":
      case "INVALID_SETTINGS":
      case "INVALID_REVISION":
        return R.string.mp_invalid_status;
      case "COMMAND_EXPIRED":
      case "IDEMPOTENCY_CONFLICT":
        return R.string.mp_expired_command_status;
      case "LEFT_ROOM":
        return R.string.mp_left_status;
      case "ROOM_CLOSED":
        return R.string.mp_closed_status;
      case "ROOM_EXPIRED":
        return R.string.mp_expired_status;
      default:
        return R.string.mp_server_status;
    }
  }
  private String name(int id, int layout) {
    TextInputLayout input = requireView().findViewById(layout);
    try {
      String name =
          RoomProtocol.name(((EditText) requireView().findViewById(id)).getText().toString());
      input.setError(null);
      return name;
    } catch (IllegalArgumentException e) {
      input.setError(getString(R.string.mp_name_error));
      return null;
    }
  }
  private void create() {
    String name = name(R.id.mp_create_name, R.id.mp_create_name_layout);
    if (name == null)
      return;
    if (model.create(
            ((RadioGroup) requireView().findViewById(R.id.mp_mode)).getCheckedRadioButtonId()
                    == R.id.mp_moderator
                ? "MODERATOR"
                : "CLASSIC",
            name, settings(requireView())))
      hideKeyboard();
  }
  private void join() {
    String name = name(R.id.mp_join_name, R.id.mp_join_name_layout);
    if (name == null)
      return;
    TextInputLayout input = requireView().findViewById(R.id.mp_code_layout);
    try {
      String code = RoomProtocol.code(
          ((EditText) requireView().findViewById(R.id.mp_join_code)).getText().toString());
      input.setError(null);
      if (model.join(code, name))
        hideKeyboard();
    } catch (IllegalArgumentException e) {
      input.setError(getString(R.string.mp_code_error));
    }
  }
  private void spinner(View root, int id, String[] values, String selected) {
    Spinner s = root.findViewById(id);
    ArrayAdapter<String> a =
        new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, values);
    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(a);
    s.setSelection(Arrays.asList(values).indexOf(selected));
  }
  private void fillSettings(View root, RoomProtocol.Settings s) {
    spinner(root, R.id.mp_category, RoomProtocol.CATEGORIES, s.category);
    spinner(root, R.id.mp_difficulty, RoomProtocol.DIFFICULTIES, s.difficulty);
    spinner(
        root, R.id.mp_count, new String[] {"5", "10", "15", "20"}, String.valueOf(s.questionCount));
    spinner(
        root, R.id.mp_timer, new String[] {"10", "15", "20", "30"}, String.valueOf(s.timerSeconds));
  }
  private RoomProtocol.Settings settings(View root) {
    return new RoomProtocol.Settings(
        ((Spinner) root.findViewById(R.id.mp_category)).getSelectedItem().toString(),
        ((Spinner) root.findViewById(R.id.mp_difficulty)).getSelectedItem().toString(),
        Integer.parseInt(((Spinner) root.findViewById(R.id.mp_count)).getSelectedItem().toString()),
        Integer.parseInt(
            ((Spinner) root.findViewById(R.id.mp_timer)).getSelectedItem().toString()));
  }
  private void editSettings() {
    RoomProtocol.Snapshot s = model.state.getValue().snapshot;
    if (s == null || !s.lobby() || !s.owner || !model.state.getValue().canWrite())
      return;
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout form = new LinearLayout(requireContext());
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(24), dp(16), dp(24), 0);
    scroll.addView(form);
    TextView help = new TextView(requireContext());
    help.setText(R.string.mp_settings_notice);
    form.addView(help);
    View fields = getLayoutInflater().inflate(R.layout.lobby_settings_fields, form, false);
    form.addView(fields);
    fillSettings(fields, s.settings);
    dialog = new MaterialAlertDialogBuilder(requireContext())
                 .setTitle(R.string.mp_edit_settings)
                 .setView(scroll)
                 .setPositiveButton(R.string.mp_save_settings, null)
                 .setNegativeButton(android.R.string.cancel, null)
                 .show();
    dialog.getButton(-1).setOnClickListener(v -> {
      if (model.settings(settings(fields), s.revision))
        dialog.dismiss();
      else
        help.setText(
            model.state.getValue().busy ? R.string.mp_sending_status : R.string.mp_network_status);
    });
  }
  private void leave() {
    RoomProtocol.Snapshot s = model.state.getValue().snapshot;
    if (s == null || !s.lobby())
      return;
    dialog = new MaterialAlertDialogBuilder(requireContext())
                 .setTitle(s.owner ? R.string.mp_close : R.string.mp_leave)
                 .setMessage(s.owner ? R.string.mp_close_confirm : R.string.mp_leave_confirm)
                 .setPositiveButton(s.owner ? R.string.mp_close : R.string.mp_leave, null)
                 .setNegativeButton(android.R.string.cancel, null)
                 .show();
    dialog.getButton(-1).setOnClickListener(v -> {
      if (model.leave())
        dialog.dismiss();
      else
        Toast
            .makeText(requireContext(),
                model.state.getValue().busy ? R.string.mp_sending_status
                                            : R.string.mp_network_status,
                Toast.LENGTH_SHORT)
            .show();
    });
  }
  private void goBack() {
    if (page == Page.MATCH) {
      if (model.state.getValue().snapshot != null && model.state.getValue().snapshot.inMatch()
          && model.state.getValue().snapshot.match.finished()) {
        model.leave();
        return;
      }
      dialog = new MaterialAlertDialogBuilder(requireContext())
                   .setTitle(R.string.match_title)
                   .setMessage(R.string.match_back_body)
                   .setPositiveButton(R.string.mp_keep_seat,
                       (d, w) -> ((MainActivity) requireActivity()).navigateTo(R.id.nav_home))
                   .setNegativeButton(android.R.string.cancel, null)
                   .show();
    } else if (page == Page.LOBBY) {
      RoomProtocol.Snapshot s = model.state.getValue().snapshot;
      dialog =
          new MaterialAlertDialogBuilder(requireContext())
              .setTitle(R.string.mp_back_title)
              .setMessage(
                  (s != null && s.owner ? getString(R.string.mp_close_confirm) + "\n\n" : "")
                  + getString(R.string.mp_back_body))
              .setPositiveButton(s != null && s.owner ? R.string.mp_close : R.string.mp_leave, null)
              .setNeutralButton(R.string.mp_keep_seat,
                  (d, w) -> ((MainActivity) requireActivity()).navigateTo(R.id.nav_home))
              .setNegativeButton(android.R.string.cancel, null)
              .show();
      dialog.getButton(-1).setEnabled(model.state.getValue().canWrite());
      dialog.getButton(-1).setOnClickListener(v -> {
        if (model.leave())
          dialog.dismiss();
      });
    } else if (page == Page.TERMINAL) {
      if (model.dismissTerminal())
        page(Page.ONLINE);
    } else if (page == Page.CREATE || page == Page.JOIN)
      page(Page.ONLINE);
    else {
      page = Page.NETWORK;
      model.deactivate();
      render(model.state.getValue());
    }
  }
  private RoomProtocol.Snapshot confirmedRoom() {
    LobbyViewModel.State s = model.state.getValue();
    return s.connected && !s.busy && s.snapshot != null && s.snapshot.lobby() ? s.snapshot : null;
  }
  private void copy() {
    RoomProtocol.Snapshot s = confirmedRoom();
    if (s == null)
      return;
    ClipboardManager clipboard =
        (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setPrimaryClip(
        ClipData.newPlainText(getString(R.string.mp_invite_label), s.inviteCode));
    Toast.makeText(requireContext(), R.string.mp_copied, Toast.LENGTH_SHORT).show();
  }
  private void share() {
    RoomProtocol.Snapshot s = confirmedRoom();
    if (s == null)
      return;
    String endpoint = model.endpoint();
    Intent intent =
        new Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT,
                getString(R.string.mp_invitation,
                    s.mode.equals("MODERATOR") ? "Moderator" : "Classic", s.inviteCode, endpoint));
    try {
      startActivity(Intent.createChooser(intent, getString(R.string.mp_share_title)));
    } catch (ActivityNotFoundException e) {
      Toast.makeText(requireContext(), R.string.mp_share_unavailable, Toast.LENGTH_SHORT).show();
    }
  }
  private EditText loginField(String hint, boolean password) {
    EditText field = new EditText(requireContext());
    field.setHint(hint);
    field.setMinHeight(dp(56));
    field.setTextSize(16);
    field.setSingleLine(true);
    field.setInputType(InputType.TYPE_CLASS_TEXT
        | (password ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_VARIATION_URI));
    field.setSaveEnabled(false);
    field.setFilters(new InputFilter[] {new InputFilter.LengthFilter(password ? 128 : 240)});
    if (Build.VERSION.SDK_INT >= 26)
      field.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
    return field;
  }
  private void connect() {
    BackendCredentials login = new BackendCredentials(requireContext());
    ScrollView scroll = new ScrollView(requireContext());
    LinearLayout form = new LinearLayout(requireContext());
    form.setOrientation(LinearLayout.VERTICAL);
    form.setPadding(dp(24), dp(12), dp(24), 0);
    scroll.addView(form);
    TextView help = new TextView(requireContext());
    help.setText(R.string.mp_connection_help);
    form.addView(help);
    EditText endpoint = loginField(getString(R.string.ai_server_address), false),
             token = loginField(getString(R.string.ai_access_token), true);
    endpoint.setText(login.endpoint());
    form.addView(endpoint);
    form.addView(token);
    TextView error = new TextView(requireContext());
    error.setTextColor(requireContext().getColor(R.color.error));
    error.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    form.addView(error);
    dialog = new MaterialAlertDialogBuilder(requireContext())
                 .setTitle(R.string.mp_connect)
                 .setView(scroll)
                 .setPositiveButton(R.string.ai_connect_save, null)
                 .setNegativeButton(android.R.string.cancel, null)
                 .create();
    dialog.setOnShowListener(d -> dialog.getButton(-1).setOnClickListener(v -> {
      try {
        login.save(endpoint.getText().toString(), token.getText().toString().trim());
        token.setText("");
        dialog.dismiss();
        model.reload();
      } catch (RuntimeException e) {
        error.setText(R.string.ai_login_invalid);
      }
    }));
    dialog.show();
    if (dialog.getWindow() != null)
      dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  }
  private void hideKeyboard() {
    if (getView() == null)
      return;
    ((InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE))
        .hideSoftInputFromWindow(requireView().getWindowToken(), 0);
    requireView().clearFocus();
  }
  private int dp(int value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }
  private final class RosterAdapter extends RecyclerView.Adapter<MemberHolder> {
    private List<RoomProtocol.Member> items = Collections.emptyList();
    private String self;
    private boolean owner;
    void replace(List<RoomProtocol.Member> next, String nextSelf, boolean nextOwner) {
      final List<RoomProtocol.Member> previous = items;
      final boolean sameIdentity = Objects.equals(self, nextSelf) && owner == nextOwner;
      DiffUtil.DiffResult changes = DiffUtil.calculateDiff(new DiffUtil.Callback() {
        @Override
        public int getOldListSize() {
          return previous.size();
        }
        @Override
        public int getNewListSize() {
          return next.size();
        }
        @Override
        public boolean areItemsTheSame(int oldPosition, int newPosition) {
          return previous.get(oldPosition)
              .participantId.equals(next.get(newPosition).participantId);
        }
        @Override
        public boolean areContentsTheSame(int oldPosition, int newPosition) {
          RoomProtocol.Member a = previous.get(oldPosition), b = next.get(newPosition);
          return sameIdentity && a.displayName.equals(b.displayName) && a.role.equals(b.role)
              && a.ready == b.ready && a.online == b.online;
        }
      });
      items = new ArrayList<>(next);
      self = nextSelf;
      owner = nextOwner;
      changes.dispatchUpdatesTo(this);
    }
    @NonNull
    @Override
    public MemberHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
      return new MemberHolder(LayoutInflater.from(parent.getContext())
              .inflate(R.layout.item_lobby_member, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull MemberHolder holder, int position) {
      RoomProtocol.Member m = items.get(position);
      holder.name.setText(m.participantId.equals(self)
              ? getString(owner ? R.string.mp_you_host : R.string.mp_you, m.displayName)
              : m.displayName);
      holder.status.setText(m.online
              ? (m.ready ? R.string.mp_member_ready : R.string.mp_member_waiting)
              : R.string.mp_member_offline);
      holder.status.setTextColor(requireContext().getColor(
          m.online && m.ready ? R.color.success : R.color.text_secondary));
    }
    @Override
    public int getItemCount() {
      return items.size();
    }
  }
  private static final class MemberHolder extends RecyclerView.ViewHolder {
    final TextView name, status;
    MemberHolder(View view) {
      super(view);
      name = view.findViewById(R.id.mp_member_name);
      status = view.findViewById(R.id.mp_member_state);
    }
  }
}
