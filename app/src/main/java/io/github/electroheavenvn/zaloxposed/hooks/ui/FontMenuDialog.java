package io.github.electroheavenvn.zaloxposed.hooks.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import io.github.electroheavenvn.zaloxposed.utilities.FontHelper;

@SuppressLint("SetTextI18n")
public final class FontMenuDialog
{
    private FontMenuDialog() {}

    private static int dp(Context ctx, int val)
    {
        return (int) (val * ctx.getResources().getDisplayMetrics().density);
    }

    public static void show(Context context, EditText targetEditText)
    {
        if (context == null || targetEditText == null) return;

        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int screenWidth = dm.widthPixels;
        int screenHeight = dm.heightPixels;

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // --- ROOT CONTAINER (Full-Width Bottom Sheet) ---
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(context, 16);
        int padV = dp(context, 10);
        root.setPadding(padH, padV, padH, dp(context, 16));
        root.setMinimumWidth(screenWidth);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(0xFF16161A);
        float rTop = dp(context, 24);
        rootBg.setCornerRadii(new float[]{rTop, rTop, rTop, rTop, 0, 0, 0, 0});
        rootBg.setStroke(dp(context, 1), 0xFF2A2A34);
        root.setBackground(rootBg);

        // 1. Drag Handle
        View handle = new View(context);
        int handleW = dp(context, 40);
        int handleH = dp(context, 4);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(handleW, handleH);
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.topMargin = dp(context, 2);
        handleLp.bottomMargin = dp(context, 10);
        handle.setLayoutParams(handleLp);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setCornerRadius(dp(context, 10));
        handleBg.setColor(0xFF3E3E48);
        handle.setBackground(handleBg);
        root.addView(handle);

        // 2. Header (Title + Close Button)
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        header.setPadding(0, 0, 0, dp(context, 10));

        TextView titleTv = new TextView(context);
        titleTv.setText("✨ Kiểu Chữ & Màu Sắc");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleTv.setLayoutParams(titleLp);
        header.addView(titleTv);

        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setTextColor(0xFF8E8E93);
        closeBtn.setGravity(Gravity.CENTER);
        int closeSize = dp(context, 28);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
        closeBtn.setLayoutParams(closeLp);
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(0xFF24242C);
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn);

        root.addView(header);

        // 3. Live Preview Card
        String currentText = targetEditText.getText().toString();
        final String rawInitial = (currentText.isEmpty()) ? "Xin chào Zalo 2026" : currentText;

        LinearLayout previewCard = new LinearLayout(context);
        previewCard.setOrientation(LinearLayout.VERTICAL);
        previewCard.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        previewCard.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setCornerRadius(dp(context, 14));
        previewBg.setColor(0xFF202026);
        previewBg.setStroke(dp(context, 1), 0xFF2E2E38);
        previewCard.setBackground(previewBg);

        TextView previewLabel = new TextView(context);
        previewLabel.setText("XEM TRƯỚC (LIVE PREVIEW)");
        previewLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f);
        previewLabel.setTypeface(Typeface.DEFAULT_BOLD);
        previewLabel.setTextColor(0xFF0A84FF);
        previewCard.addView(previewLabel);

        TextView previewTv = new TextView(context);
        previewTv.setText(rawInitial);
        previewTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        previewTv.setTextColor(0xFFFFFFFF);
        previewTv.setPadding(0, dp(context, 4), 0, 0);
        previewTv.setMaxLines(3);
        previewCard.addView(previewTv);

        root.addView(previewCard);

        // 4. Scrollable Content Area (Colors + Fonts)
        ScrollView scrollView = new ScrollView(context);
        int maxScrollH = (int) (screenHeight * 0.44);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxScrollH));
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);
        contentLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentLayout.setPadding(0, dp(context, 12), 0, dp(context, 8));

        // ================= SECTION 1: THEMES & COLOR BADGES =================
        TextView colorHeader = new TextView(context);
        colorHeader.setText("🎨 BẢNG MÀU SẮC & THEME NỔI BẬT");
        colorHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        colorHeader.setTypeface(Typeface.DEFAULT_BOLD);
        colorHeader.setTextColor(0xFF8E8E93);
        colorHeader.setPadding(0, 0, 0, dp(context, 8));
        contentLayout.addView(colorHeader);

        HorizontalScrollView colorScroll = new HorizontalScrollView(context);
        colorScroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);

        String[] themes = {
            FontHelper.THEME_RED, FontHelper.THEME_BLUE, FontHelper.THEME_GREEN,
            FontHelper.THEME_YELLOW, FontHelper.THEME_PURPLE, FontHelper.THEME_ORANGE,
            FontHelper.THEME_PINK, FontHelper.THEME_RAINBOW, FontHelper.THEME_FIRE,
            FontHelper.THEME_STAR, FontHelper.THEME_DIAMOND, FontHelper.THEME_VIP
        };
        String[] themeLabels = {
            "🔴 Đỏ", "🔵 Xanh biển", "🟢 Xanh lá",
            "🟡 Vàng", "🟣 Tím", "🟠 Cam",
            "🌸 Hồng", "🌈 Cầu vồng", "🔥 Lửa",
            "⭐ Sao", "💎 Kim cương", "🚨 VIP"
        };

        for (int i = 0; i < themes.length; i++)
        {
            final String th = themes[i];
            final String lbl = themeLabels[i];
            TextView chip = createPillChip(context, lbl);
            chip.setOnClickListener(v -> {
                String base = FontHelper.stripThemes(targetEditText.getText().toString());
                if (base.isEmpty()) base = "Nội dung tin nhắn";
                String themed = FontHelper.applyTheme(base, th);
                targetEditText.setText(themed);
                targetEditText.setSelection(themed.length());
                previewTv.setText(themed);
            });
            colorRow.addView(chip);
        }
        colorScroll.addView(colorRow);
        contentLayout.addView(colorScroll);

        // ================= SECTION 2: UNICODE FONT STYLES =================
        TextView fontHeader = new TextView(context);
        fontHeader.setText("🔤 13 KIỂU CHỮ NGHỆ THUẬT (UNICODE)");
        fontHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        fontHeader.setTypeface(Typeface.DEFAULT_BOLD);
        fontHeader.setTextColor(0xFF8E8E93);
        fontHeader.setPadding(0, dp(context, 14), 0, dp(context, 8));
        contentLayout.addView(fontHeader);

        final String[][] fonts = {
            {FontHelper.STYLE_BOLD, "In Đậm (Bold)", "𝐁𝐨𝐥𝐝 𝐓𝐞𝐱𝐭"},
            {FontHelper.STYLE_ITALIC, "In Nghiêng (Italic)", "𝐼𝑡𝑎𝑙𝑖𝑐 𝑇𝑒𝑥𝑡"},
            {FontHelper.STYLE_BOLD_ITALIC, "Đậm & Nghiêng", "𝑩𝒐𝒍𝒅 𝑰𝒕𝒂𝒍𝒊𝒄"},
            {FontHelper.STYLE_SCRIPT, "Viết Tay (Script)", "𝓕𝓪𝓷𝓬𝔂 𝓢𝓬𝓻𝓲𝓹𝓽"},
            {FontHelper.STYLE_GOTHIC, "Chữ Gothic Cổ", "𝕲𝖔𝖙𝖍𝖎𝖈 𝕿𝖊𝖝𝖙"},
            {FontHelper.STYLE_MONO, "Máy Đánh Chữ", "𝙼𝚘𝚗𝚘𝚜𝚙𝚊𝚌𝚎"},
            {FontHelper.STYLE_BUBBLE, "Bong Bóng", "Ⓑⓤⓑⓑⓛⓔ"},
            {FontHelper.STYLE_STRIKE, "Gạch Ngang", "S̶t̶r̶i̶k̶e̶"},
            {FontHelper.STYLE_UNDERLINE, "Gạch Chân", "U̲n̲d̲e̲r̲l̲i̲n̲e̲"},
            {FontHelper.STYLE_FLIP, "Lộn Ngược 180°", "ʇxǝ⊥ dıןℲ"},
            {FontHelper.STYLE_REGIONAL, "Chữ Khối Vuông", "🔤 🇦 🇧 🇨"},
            {FontHelper.STYLE_UPPER, "IN HOA HẾT", "VIẾT HOA"},
            {FontHelper.STYLE_LOWER, "viết thường hết", "viết thường"}
        };

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(2);
        grid.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int itemWidth = (screenWidth - (padH * 2) - dp(context, 12)) / 2;

        for (String[] fInfo : fonts)
        {
            final String styleKey = fInfo[0];
            final String styleName = fInfo[1];
            final String sampleText = fInfo[2];

            View card = createFontCard(context, itemWidth, sampleText, styleName, v -> {
                String cur = targetEditText.getText().toString();
                if (cur.isEmpty()) cur = "Tin nhắn thử nghiệm";
                String converted = FontHelper.transform(cur, styleKey);
                targetEditText.setText(converted);
                targetEditText.setSelection(converted.length());
                previewTv.setText(converted);
            });
            grid.addView(card);
        }
        contentLayout.addView(grid);

        scrollView.addView(contentLayout);
        root.addView(scrollView);

        // 5. Bottom Actions Bar (Sleek Modern Footer)
        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        footer.setPadding(0, dp(context, 10), 0, 0);

        // Reset Button
        TextView resetBtn = createActionButton(context, "🔄 Xoá kiểu", 0xFF26262E, 0xFFFF453A);
        resetBtn.setOnClickListener(v -> {
            String cur = targetEditText.getText().toString();
            String cleaned = FontHelper.stripThemes(cur);
            targetEditText.setText(cleaned);
            previewTv.setText(cleaned);
            Toast.makeText(context, "Đã khôi phục chữ gốc!", Toast.LENGTH_SHORT).show();
        });
        footer.addView(resetBtn);

        // Troll Reply Button
        TextView trollBtn = createActionButton(context, "🎭 Troll Reply", 0xFF2D1F3C, 0xFFD084F8);
        trollBtn.setOnClickListener(v -> {
            String cur = targetEditText.getText().toString();
            String res = "/troll [Nội dung giả] | " + (cur.isEmpty() ? "[Câu trả lời]" : cur);
            targetEditText.setText(res);
            targetEditText.setSelection(res.indexOf('|'));
            dialog.dismiss();
            Toast.makeText(context, "✅ Đã chèn cú pháp Troll!", Toast.LENGTH_SHORT).show();
        });
        footer.addView(trollBtn);

        // Done Button
        TextView doneBtn = createActionButton(context, "✓ Xong", 0xFF007AFF, 0xFFFFFFFF);
        doneBtn.setTypeface(Typeface.DEFAULT_BOLD);
        doneBtn.setOnClickListener(v -> dialog.dismiss());
        footer.addView(doneBtn);

        root.addView(footer);

        // Set content view before show
        dialog.setContentView(root);

        // Show dialog FIRST
        dialog.show();

        // CRITICAL: Configure Window attributes AFTER dialog.show() so Android does not override with squished defaults!
        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setWindowAnimations(android.R.style.Animation_InputMethod);

            // Force layout params
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(wlp);
        }
    }

    private static TextView createPillChip(Context context, String text)
    {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tv.setTextColor(0xFFE5E5EA);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(context, 14), dp(context, 7), dp(context, 14), dp(context, 7));

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(context, 18));
        normalBg.setColor(0xFF24242A);
        normalBg.setStroke(dp(context, 1), 0xFF363640);

        GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(context, 18));
        pressedBg.setColor(0xFF007AFF);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        sld.addState(new int[]{}, normalBg);

        tv.setBackground(sld);
        tv.setClickable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(context, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    private static View createFontCard(Context context, int itemWidth, String sample, String label, View.OnClickListener onClickListener)
    {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(context, 8), dp(context, 10), dp(context, 8), dp(context, 10));

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(context, 12));
        normalBg.setColor(0xFF202026);
        normalBg.setStroke(dp(context, 1), 0xFF2C2C34);

        GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(context, 12));
        pressedBg.setColor(0xFF2A2A35);
        pressedBg.setStroke(dp(context, 1), 0xFF0A84FF);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        sld.addState(new int[]{}, normalBg);

        card.setBackground(sld);
        card.setClickable(true);
        card.setOnClickListener(onClickListener);

        TextView sampleTv = new TextView(context);
        sampleTv.setText(sample);
        sampleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f);
        sampleTv.setTypeface(Typeface.DEFAULT_BOLD);
        sampleTv.setTextColor(0xFFFFFFFF);
        sampleTv.setGravity(Gravity.CENTER);
        card.addView(sampleTv);

        TextView labelTv = new TextView(context);
        labelTv.setText(label);
        labelTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        labelTv.setTextColor(0xFF8E8E93);
        labelTv.setGravity(Gravity.CENTER);
        labelTv.setPadding(0, dp(context, 2), 0, 0);
        card.addView(labelTv);

        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = itemWidth;
        lp.height = dp(context, 54);
        lp.setMargins(dp(context, 3), dp(context, 3), dp(context, 3), dp(context, 3));
        card.setLayoutParams(lp);

        return card;
    }

    private static TextView createActionButton(Context context, String text, int bgColor, int textColor)
    {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
        btn.setTextColor(textColor);
        btn.setGravity(Gravity.CENTER);

        GradientDrawable normalBg = new GradientDrawable();
        normalBg.setCornerRadius(dp(context, 12));
        normalBg.setColor(bgColor);
        normalBg.setStroke(dp(context, 1), 0x33FFFFFF);

        GradientDrawable pressedBg = new GradientDrawable();
        pressedBg.setCornerRadius(dp(context, 12));
        pressedBg.setColor(0x88FFFFFF);

        StateListDrawable sld = new StateListDrawable();
        sld.addState(new int[]{android.R.attr.state_pressed}, pressedBg);
        sld.addState(new int[]{}, normalBg);

        btn.setBackground(sld);
        btn.setClickable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(context, 40), 1f);
        lp.leftMargin = dp(context, 3);
        lp.rightMargin = dp(context, 3);
        btn.setLayoutParams(lp);

        return btn;
    }
}
