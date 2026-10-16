package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.content.Context;
import android.text.InputType;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.zeke.quizpk.*;
import com.zeke.quizpk.domain.*;
import com.zeke.quizpk.data.local.QuizDatabase;
import com.zeke.quizpk.data.remote.BackendCredentials;
import java.text.DateFormat;
import java.util.*;

public final class AiQuizFragment extends Fragment {
    private AiQuizViewModel model;
    private boolean savedPanel;
    public AiQuizFragment(){super(R.layout.fragment_ai_quiz);}
    @Override public void onCreate(@Nullable Bundle state){super.onCreate(state);if(state!=null)savedPanel=state.getBoolean("saved_panel");}
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        model=new ViewModelProvider(requireActivity()).get(AiQuizViewModel.class);
        spinner(view,R.id.ai_difficulty,QuizConfig.DIFFICULTIES,1);
        spinner(view,R.id.ai_count,new String[]{"5","10","15","20"},1);
        spinner(view,R.id.ai_timer,new String[]{"10","15","20","30"},1);
        if(state==null && model.pending()!=null){
            AiQuizRequest p=model.pending();((EditText)view.findViewById(R.id.ai_topic)).setText(p.topic);
            ((EditText)view.findViewById(R.id.ai_instructions)).setText(p.instructions);
            ((Spinner)view.findViewById(R.id.ai_difficulty)).setSelection(Arrays.asList(QuizConfig.DIFFICULTIES).indexOf(p.difficulty));
            ((Spinner)view.findViewById(R.id.ai_count)).setSelection(Arrays.asList(5,10,15,20).indexOf(p.count));
            ((Spinner)view.findViewById(R.id.ai_timer)).setSelection(Arrays.asList(10,15,20,30).indexOf(p.seconds));
        }
        view.findViewById(R.id.ai_create_tab).setOnClickListener(v->panel(false));
        view.findViewById(R.id.ai_saved_tab).setOnClickListener(v->panel(true));
        view.findViewById(R.id.generate_quiz).setOnClickListener(v->generate());
        view.findViewById(R.id.ai_cancel).setOnClickListener(v->model.cancel());
        view.findViewById(R.id.ai_connect).setOnClickListener(v->connect());
        view.findViewById(R.id.ai_saved_retry).setOnClickListener(v->loadSaved());
        model.state.observe(getViewLifecycleOwner(),s->render(s));
        panel(savedPanel);
    }
    private void spinner(View view,int id,String[] options,int selected){
        Spinner spinner=view.findViewById(id);ArrayAdapter<String> adapter=new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_item,options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);spinner.setAdapter(adapter);spinner.setSelection(selected);
    }
    private void panel(boolean saved){
        savedPanel=saved;View v=requireView();v.findViewById(R.id.ai_form_scroll).setVisibility(saved?View.GONE:View.VISIBLE);
        v.findViewById(R.id.ai_saved_panel).setVisibility(saved?View.VISIBLE:View.GONE);
        v.findViewById(R.id.ai_create_tab).setSelected(!saved);v.findViewById(R.id.ai_saved_tab).setSelected(saved);
        if(saved)loadSaved();
    }
    private void generate(){
        View v=requireView();AiQuizRequest request;
        try{request=new AiQuizRequest(((EditText)v.findViewById(R.id.ai_topic)).getText().toString(),
                ((Spinner)v.findViewById(R.id.ai_difficulty)).getSelectedItem().toString(),
                Integer.parseInt(((Spinner)v.findViewById(R.id.ai_count)).getSelectedItem().toString()),
                Integer.parseInt(((Spinner)v.findViewById(R.id.ai_timer)).getSelectedItem().toString()),
                ((EditText)v.findViewById(R.id.ai_instructions)).getText().toString());
            ((TextInputLayout)v.findViewById(R.id.ai_topic_layout)).setError(null);
        }catch(IllegalArgumentException e){((TextInputLayout)v.findViewById(R.id.ai_topic_layout)).setError(getString(R.string.ai_invalid));return;}
        try{
            BackendCredentials credentials=new BackendCredentials(requireContext());
            if(credentials.endpoint().isEmpty()||credentials.token().isEmpty()){status(R.string.ai_setup);connect();return;}
            model.generate(credentials.endpoint(),credentials.token(),request);
        }catch(RuntimeException e){status(R.string.ai_auth_error);}
    }
    private void status(int text){((TextView)requireView().findViewById(R.id.ai_status)).setText(text);}
    private void render(AiQuizViewModel.State s){
        View v=requireView();v.findViewById(R.id.generate_quiz).setEnabled(!s.busy);
        for(int id:new int[]{R.id.ai_topic,R.id.ai_instructions,R.id.ai_difficulty,R.id.ai_count,R.id.ai_timer,R.id.ai_connect})v.findViewById(id).setEnabled(!s.busy);
        v.findViewById(R.id.ai_progress).setVisibility(s.busy?View.VISIBLE:View.GONE);
        v.findViewById(R.id.ai_cancel).setVisibility(s.busy?View.VISIBLE:View.GONE);
        v.findViewById(R.id.ai_play_generated).setVisibility(s.collectionId!=null?View.VISIBLE:View.GONE);
        v.findViewById(R.id.ai_play_generated).setOnClickListener(b->OfflineQuizActivity.openAi(requireContext(),s.collectionId));
        int text;
        switch(s.code){
            case "GENERATING":text=R.string.ai_generating;break;
            case "SUCCESS":text=R.string.ai_success;break;
            case "SETUP_REQUIRED":text=R.string.ai_setup;break;
            case "NO_CONNECTION":text=R.string.ai_no_connection;break;
            case "TIMEOUT":text=R.string.ai_timeout;break;
            case "GENERATION_TIMEOUT":text=R.string.ai_generation_timeout;break;
            case "AUTH_REQUIRED":text=R.string.ai_auth_error;break;
            case "RATE_LIMITED":case "QUOTA_EXCEEDED":case "PROVIDER_LIMIT":case "BUSY":text=R.string.ai_throttled;break;
            case "MALFORMED_QUESTIONS":text=R.string.ai_malformed;break;
            case "IN_PROGRESS":text=R.string.ai_in_progress;break;
            case "INTERRUPTED":text=R.string.ai_interrupted;break;
            case "REQUEST_INTERRUPTED":case "IDEMPOTENCY_CONFLICT":text=R.string.ai_request_lost;break;
            case "CANCELED":text=R.string.ai_canceled;break;
            case "TOPIC_REJECTED":case "INVALID_TOPIC":text=R.string.ai_rejected;break;
            case "STORAGE_ERROR":text=R.string.storage_error_body;break;
            case "READY":text=R.string.ai_ready;break;
            default:text=R.string.ai_server_error;
        }
        status(text);
        boolean retry=Arrays.asList("NO_CONNECTION","TIMEOUT","IN_PROGRESS","INTERRUPTED","CANCELED").contains(s.code);
        ((MaterialButton)v.findViewById(R.id.generate_quiz)).setText(retry?R.string.ai_retry_label:"SUCCESS".equals(s.code)?R.string.ai_new_generation:R.string.generate_quiz);
        if(savedPanel&&!s.busy)loadSaved();
    }
    private EditText field(String hint,boolean password){
        EditText text=new EditText(requireContext());text.setHint(hint);text.setMinHeight(dp(56));text.setTextSize(16);
        text.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        if(android.os.Build.VERSION.SDK_INT>=26)text.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        text.setSaveEnabled(false);text.setFilters(new InputFilter[]{new InputFilter.LengthFilter(password?128:240)});return text;
    }
    private void connect(){
        BackendCredentials credentials=new BackendCredentials(requireContext());
        LinearLayout form=new LinearLayout(requireContext());form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(24),dp(8),dp(24),0);
        TextView help=new TextView(requireContext());help.setText(R.string.ai_connect_body);form.addView(help);
        EditText endpoint=field(getString(R.string.ai_server_address),false),token=field(getString(R.string.ai_access_token),true);
        endpoint.setText(credentials.endpoint());form.addView(endpoint);form.addView(token);
        TextView error=new TextView(requireContext());form.addView(error);
        androidx.appcompat.app.AlertDialog dialog=new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_connect_label).setView(form)
                .setPositiveButton(R.string.ai_connect_save,null).setNegativeButton(android.R.string.cancel,null)
                .setNeutralButton(R.string.ai_disconnect,(d,w)->{credentials.clear();status(R.string.ai_setup);}).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(b->{
            try{credentials.save(endpoint.getText().toString(),token.getText().toString().trim());dialog.dismiss();status(R.string.ai_ready);}
            catch(RuntimeException e){error.setText(R.string.ai_login_invalid);}
        }));dialog.show();
    }
    private void loadSaved(){
        View v=requireView();TextView status=v.findViewById(R.id.ai_saved_status);status.setText(R.string.ai_saved_loading);
        try(QuizDatabase db=new QuizDatabase(requireContext())){
            List<SavedAiQuiz> quizzes=db.savedAi();RecyclerView list=v.findViewById(R.id.ai_saved_list);
            list.setLayoutManager(new LinearLayoutManager(requireContext()));list.setAdapter(new SavedAdapter(quizzes));
            status.setText(quizzes.isEmpty()?R.string.ai_saved_empty:R.string.ai_saved_body);v.findViewById(R.id.ai_saved_retry).setVisibility(View.GONE);
        }catch(RuntimeException e){status.setText(R.string.storage_error_body);v.findViewById(R.id.ai_saved_retry).setVisibility(View.VISIBLE);}
    }
    private void rename(SavedAiQuiz quiz){
        EditText title=new EditText(requireContext());title.setText(quiz.title);title.setFilters(new InputFilter[]{new InputFilter.LengthFilter(120)});
        androidx.appcompat.app.AlertDialog dialog=new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_rename).setView(title)
                .setPositiveButton(android.R.string.ok,null).setNegativeButton(android.R.string.cancel,null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(b->{
            try(QuizDatabase db=new QuizDatabase(requireContext())){db.renameAi(quiz.id,title.getText().toString());dialog.dismiss();loadSaved();}
            catch(IllegalArgumentException e){title.setError(getString(R.string.ai_title_invalid));}
            catch(RuntimeException e){title.setError(getString(R.string.storage_error_body));}
        }));dialog.show();
    }
    private void delete(SavedAiQuiz quiz){
        new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.ai_delete).setMessage(R.string.ai_delete_body)
                .setPositiveButton(R.string.ai_delete,(d,w)->{
                    try(QuizDatabase db=new QuizDatabase(requireContext())){db.deleteAi(quiz.id);loadSaved();}
                    catch(RuntimeException e){((TextView)requireView().findViewById(R.id.ai_saved_status)).setText(R.string.storage_error_body);}
                }).setNegativeButton(android.R.string.cancel,null).show();
    }
    @Override public void onResume(){super.onResume();if(savedPanel)loadSaved();}
    @Override public void onSaveInstanceState(@NonNull Bundle out){out.putBoolean("saved_panel",savedPanel);super.onSaveInstanceState(out);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private final class SavedAdapter extends RecyclerView.Adapter<SavedHolder>{
        private final List<SavedAiQuiz> items;SavedAdapter(List<SavedAiQuiz> items){this.items=items;}
        @NonNull @Override public SavedHolder onCreateViewHolder(@NonNull ViewGroup parent,int type){
            MaterialCardView card=new MaterialCardView(parent.getContext());LinearLayout body=new LinearLayout(parent.getContext());
            body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(16),dp(16),dp(16),dp(16));card.addView(body);
            TextView title=new TextView(parent.getContext());title.setTextAppearance(R.style.Text_QuizPK_Section);body.addView(title);
            TextView meta=new TextView(parent.getContext());meta.setTextAppearance(R.style.Text_QuizPK_Body);body.addView(meta);
            MaterialButton play=new MaterialButton(parent.getContext());play.setText(R.string.ai_play_again);body.addView(play,new LinearLayout.LayoutParams(-1,-2));
            LinearLayout actions=new LinearLayout(parent.getContext());body.addView(actions);
            MaterialButton rename=new MaterialButton(parent.getContext(),null,com.google.android.material.R.attr.borderlessButtonStyle);rename.setText(R.string.ai_rename);
            MaterialButton delete=new MaterialButton(parent.getContext(),null,com.google.android.material.R.attr.borderlessButtonStyle);delete.setText(R.string.ai_delete);
            actions.addView(rename,new LinearLayout.LayoutParams(0,-2,1));actions.addView(delete,new LinearLayout.LayoutParams(0,-2,1));
            RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(-1,-2);p.bottomMargin=dp(16);card.setLayoutParams(p);
            return new SavedHolder(card,title,meta,play,rename,delete);
        }
        @Override public void onBindViewHolder(@NonNull SavedHolder h,int index){
            SavedAiQuiz quiz=items.get(index);h.title.setText(quiz.title);h.meta.setText(getString(R.string.ai_saved_meta,quiz.difficulty,quiz.count,quiz.seconds,
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(quiz.createdAt))));
            h.play.setOnClickListener(v->OfflineQuizActivity.openAi(requireContext(),quiz.id));h.rename.setOnClickListener(v->rename(quiz));h.delete.setOnClickListener(v->delete(quiz));
        }
        @Override public int getItemCount(){return items.size();}
    }
    private static final class SavedHolder extends RecyclerView.ViewHolder{
        final TextView title,meta;final MaterialButton play,rename,delete;
        SavedHolder(View card,TextView title,TextView meta,MaterialButton play,MaterialButton rename,MaterialButton delete){super(card);this.title=title;this.meta=meta;this.play=play;this.rename=rename;this.delete=delete;}
    }
}
