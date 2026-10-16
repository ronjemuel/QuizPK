package com.zeke.quizpk.ui.screens;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.card.MaterialCardView;
import com.zeke.quizpk.OfflineQuizActivity;
import com.zeke.quizpk.R;
import com.zeke.quizpk.domain.*;

public final class ReviewFragment extends Fragment {
    public ReviewFragment() { super(R.layout.fragment_review); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        QuizSession s = ((OfflineQuizActivity)requireActivity()).session;
        LinearLayout list = view.findViewById(R.id.review_list);
        for (int i = 0; i < s.questions.size(); i++) {
            Question q = s.questions.get(i);
            String answer = s.outcome(i) == QuizSession.Outcome.CORRECT || s.outcome(i) == QuizSession.Outcome.INCORRECT
                    ? q.options.get(s.selected(i)) : getString(R.string.no_answer);
            String body = getString(R.string.review_item, i+1, q.text,
                    getString(GameplayFragment.outcomeText(s.outcome(i))), answer,
                    q.options.get(q.correct), q.explanation, "AI_GENERATED".equals(q.source) ? getString(R.string.ai_source_label) : q.source);
            MaterialCardView card = new MaterialCardView(requireContext());
            TextView text = new TextView(requireContext());
            text.setTextAppearance(R.style.Text_QuizPK_Body); text.setText(body); text.setTextIsSelectable(true);
            int pad = Math.round(16 * getResources().getDisplayMetrics().density);
            text.setPadding(pad,pad,pad,pad);
            LinearLayout bodyLayout=new LinearLayout(requireContext());bodyLayout.setOrientation(LinearLayout.VERTICAL);card.addView(bodyLayout);bodyLayout.addView(text);
            if("AI_GENERATED".equals(q.source)) {
                com.google.android.material.button.MaterialButton report=new com.google.android.material.button.MaterialButton(requireContext());
                OfflineQuizActivity host=(OfflineQuizActivity)requireActivity();
                report.setText(host.database.reported(q.id)?R.string.ai_report_saved:R.string.ai_report);
                report.setOnClickListener(v->{
                    android.widget.EditText reason=new android.widget.EditText(requireContext());reason.setHint(R.string.ai_report_reason);
                    reason.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(500)});
                    androidx.appcompat.app.AlertDialog dialog=new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.ai_report).setMessage(R.string.ai_report_body).setView(reason)
                            .setPositiveButton(R.string.ai_report,null).setNegativeButton(android.R.string.cancel,null).create();
                    dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(b->{
                        try {host.database.report(q,reason.getText().toString());report.setText(R.string.ai_report_saved);dialog.dismiss();}
                        catch(IllegalArgumentException e){reason.setError(getString(R.string.ai_report_invalid));}
                        catch(RuntimeException e){reason.setError(getString(R.string.storage_error_body));}
                    }));dialog.show();
                });bodyLayout.addView(report);
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2); p.bottomMargin = pad;
            list.addView(card,p);
        }
    }
}
