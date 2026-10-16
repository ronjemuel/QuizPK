package com.zeke.quizpk.ui.components;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.zeke.quizpk.R;

/** Reusable native mode card, with no invented game data. */
public final class ModeCard {
    private ModeCard() { }

    public static void bind(View card, int icon, int title, int body, int status,
                            View.OnClickListener onClick) {
        ((ImageView) card.findViewById(R.id.mode_icon)).setImageResource(icon);
        ((TextView) card.findViewById(R.id.mode_title)).setText(title);
        ((TextView) card.findViewById(R.id.mode_body)).setText(body);
        ((TextView) card.findViewById(R.id.mode_status)).setText(status);
        card.setContentDescription(card.getContext().getString(title) + ". "
                + card.getContext().getString(body) + " " + card.getContext().getString(status));
        card.setOnClickListener(onClick);
    }
}
