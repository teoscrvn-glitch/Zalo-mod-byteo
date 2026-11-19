package io.github.electroheavenvn.zaloxposed.hooks.ui;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import io.github.electroheavenvn.zaloxposed.hooks.BaseHook;
import io.github.electroheavenvn.zaloxposed.utilities.Config;
import io.github.electroheavenvn.zaloxposed.utilities.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

public class ClientMessageEditorHook extends BaseHook
{
    private static class TapTracker
    {
        long lastTapTime = 0;
        int count = 0;
    }

    private final Map<View, TapTracker> trackers = new WeakHashMap<>();

    private Method getMessageMethod = null;
    private Field msgTextField = null;
    private Field msgQuoteField = null;
    private Field quoteMsgField = null;
    private Method q2Method = null;
    private Method y1Method = null;
    private Method i2Method = null;

    private static int dp(Context ctx, int val)
    {
        return (int) (val * ctx.getResources().getDisplayMetrics().density);
    }

    @Override
    public void hook() throws Throwable
    {
        try
        {
            Class<?> chatRowClass = Class.forName("com.zing.zalo.ui.chat.chatrow.ChatRow", false, classLoader);

            // Locate getMessage()
            for (Method m : chatRowClass.getMethods())
            {
                if (m.getParameterCount() == 0 && m.getReturnType().getName().equals("j20.q"))
                {
                    getMessageMethod = m;
                    break;
                }
            }

            if (getMessageMethod == null)
            {
                for (Method m : chatRowClass.getDeclaredMethods())
                {
                    if (m.getParameterCount() == 0 && m.getReturnType().getName().equals("j20.q"))
                    {
                        getMessageMethod = m;
                        getMessageMethod.setAccessible(true);
                        break;
                    }
                }
            }

            // Locate q2(j20.q, boolean)
            for (Method m : chatRowClass.getDeclaredMethods())
            {
                if (m.getName().equals("q2") && m.getParameterCount() == 2 && m.getParameterTypes()[1].equals(boolean.class))
                {
                    q2Method = m;
                    break;
                }
            }

            // Locate dispatchTouchEvent
            Method dispatchTouchMethod = chatRowClass.getDeclaredMethod("dispatchTouchEvent", MotionEvent.class);
            Logger.i("ClientMessageEditorHook: Hooking dispatchTouchEvent -> " + dispatchTouchMethod);

            module.hook(dispatchTouchMethod).intercept(chain ->
            {
                if (!Config.getEnableClientMessageEditor())
                {
                    return chain.proceed();
                }

                Object thisObj = chain.getThisObject();
                if (!(thisObj instanceof View))
                {
                    return chain.proceed();
                }

                View chatRowView = (View) thisObj;
                MotionEvent ev = (MotionEvent) chain.getArg(0);

                if (ev != null && ev.getActionMasked() == MotionEvent.ACTION_UP)
                {
                    long now = System.currentTimeMillis();
                    TapTracker tracker;
                    synchronized (trackers)
                    {
                        tracker = trackers.get(chatRowView);
                        if (tracker == null)
                        {
                            tracker = new TapTracker();
                            trackers.put(chatRowView, tracker);
                        }
                    }

                    if (now - tracker.lastTapTime < 450)
                    {
                        tracker.count++;
                    }
                    else
                    {
                        tracker.count = 1;
                    }
                    tracker.lastTapTime = now;

                    // 3 taps triggers client-side editor
                    if (tracker.count >= 3)
                    {
                        tracker.count = 0;
                        handleEditMessage(chatRowView);
                    }
                }

                return chain.proceed();
            });
        }
        catch (Throwable t)
        {
            Logger.e(t);
        }
    }

    private void handleEditMessage(View chatRowView)
    {
        try
        {
            if (getMessageMethod == null) return;
            Object msg = getMessageMethod.invoke(chatRowView);
            if (msg == null) return;

            Class<?> msgClass = msg.getClass();

            // Locate msg.f
            if (msgTextField == null)
            {
                try
                {
                    msgTextField = msgClass.getDeclaredField("f");
                    msgTextField.setAccessible(true);
                }
                catch (NoSuchFieldException e)
                {
                    for (Field f : msgClass.getDeclaredFields())
                    {
                        if (f.getType().equals(String.class))
                        {
                            msgTextField = f;
                            msgTextField.setAccessible(true);
                            break;
                        }
                    }
                }
            }

            if (msgTextField == null) return;

            String currentText = (String) msgTextField.get(msg);
            if (currentText == null) currentText = "";

            // Locate quote
            if (msgQuoteField == null)
            {
                for (Field f : msgClass.getDeclaredFields())
                {
                    if (f.getType().getName().contains("Quote") || f.getType().getName().equals("c01.k"))
                    {
                        msgQuoteField = f;
                        msgQuoteField.setAccessible(true);
                        break;
                    }
                }
            }

            Object quoteObj = msgQuoteField != null ? msgQuoteField.get(msg) : null;
            String currentQuoteText = null;

            if (quoteObj != null)
            {
                if (quoteMsgField == null)
                {
                    try
                    {
                        quoteMsgField = quoteObj.getClass().getDeclaredField("f");
                        quoteMsgField.setAccessible(true);
                    }
                    catch (NoSuchFieldException ignored) {}
                }
                if (quoteMsgField != null)
                {
                    Object qVal = quoteMsgField.get(quoteObj);
                    if (qVal instanceof String)
                    {
                        currentQuoteText = (String) qVal;
                    }
                }
            }

            showEditDialog(chatRowView, msg, currentText, quoteObj, currentQuoteText);
        }
        catch (Exception e)
        {
            Logger.e(e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void showEditDialog(View chatRowView, Object msg, String currentText, Object quoteObj, String currentQuoteText)
    {
        Context context = chatRowView.getContext();

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null)
        {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.55f);
            window.setWindowAnimations(android.R.style.Animation_InputMethod);
        }

        // --- ROOT CONTAINER (Dark Frosted Bottom Sheet) ---
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dp(context, 16);
        int padV = dp(context, 10);
        root.setPadding(padH, padV, padH, dp(context, 16));
        root.setMinimumWidth(context.getResources().getDisplayMetrics().widthPixels);

        GradientDrawable rootBg = new GradientDrawable();
        rootBg.setColor(0xFF16161A);
        float rTop = dp(context, 24);
        rootBg.setCornerRadii(new float[]{rTop, rTop, rTop, rTop, 0, 0, 0, 0});
        rootBg.setStroke(dp(context, 1), 0xFF2A2A34);
        root.setBackground(rootBg);

        // 1. Drag Handle
        View handle = new View(context);
        LinearLayout.LayoutParams handleLp = new LinearLayout.LayoutParams(dp(context, 38), dp(context, 4));
        handleLp.gravity = Gravity.CENTER_HORIZONTAL;
        handleLp.topMargin = dp(context, 2);
        handleLp.bottomMargin = dp(context, 10);
        handle.setLayoutParams(handleLp);
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setCornerRadius(dp(context, 10));
        handleBg.setColor(0xFF3E3E48);
        handle.setBackground(handleBg);
        root.addView(handle);

        // 2. Header
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(context, 10));

        TextView titleTv = new TextView(context);
        titleTv.setText("✏️ Sửa Tin Nhắn (Fake Ảnh)");
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        titleTv.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleTv.setLayoutParams(titleLp);
        header.addView(titleTv);

        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        closeBtn.setTypeface(Typeface.DEFAULT_BOLD);
        closeBtn.setTextColor(0xFF8E8E93);
        closeBtn.setGravity(Gravity.CENTER);
        int closeSize = dp(context, 30);
        closeBtn.setLayoutParams(new LinearLayout.LayoutParams(closeSize, closeSize));
        GradientDrawable closeBg = new GradientDrawable();
        closeBg.setShape(GradientDrawable.OVAL);
        closeBg.setColor(0xFF24242C);
        closeBtn.setBackground(closeBg);
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        header.addView(closeBtn);

        root.addView(header);

        // 3. Scrollable Content
        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        LinearLayout contentLayout = new LinearLayout(context);
        contentLayout.setOrientation(LinearLayout.VERTICAL);

        // Input 1: Main message text
        TextView labelMsg = new TextView(context);
        labelMsg.setText("NỘI DUNG TIN NHẮN");
        labelMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        labelMsg.setTypeface(Typeface.DEFAULT_BOLD);
        labelMsg.setTextColor(0xFF8E8E93);
        labelMsg.setPadding(0, 0, 0, dp(context, 4));
        contentLayout.addView(labelMsg);

        final EditText inputMsg = new EditText(context);
        inputMsg.setText(currentText);
        inputMsg.setTextColor(0xFFFFFFFF);
        inputMsg.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
        inputMsg.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));
        GradientDrawable etBg = new GradientDrawable();
        etBg.setCornerRadius(dp(context, 12));
        etBg.setColor(0xFF202026);
        etBg.setStroke(dp(context, 1), 0xFF2E2E38);
        inputMsg.setBackground(etBg);
        inputMsg.setSelection(inputMsg.getText().length());
        contentLayout.addView(inputMsg);

        // Color Palette Selector
        TextView labelColor = new TextView(context);
        labelColor.setText("🎨 CHỌN MÀU CHỮ HIỂN THỊ");
        labelColor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
        labelColor.setTypeface(Typeface.DEFAULT_BOLD);
        labelColor.setTextColor(0xFF8E8E93);
        labelColor.setPadding(0, dp(context, 12), 0, dp(context, 6));
        contentLayout.addView(labelColor);

        final int[] chosenColor = {0};
        final int[] colors = {
            0, 0xFFFF3B30, 0xFF007AFF, 0xFF34C759, 0xFFFFCC00, 0xFFAF52DE,
            0xFFFF9500, 0xFFFF2D55, 0xFF00C7BE, 0xFFFFFFFF, 0xFF000000
        };
        final String[] colorNames = {
            "Mặc định", "Đỏ", "Xanh biển", "Xanh lá", "Vàng", "Tím",
            "Cam", "Hồng", "Cyan", "Trắng", "Đen"
        };

        HorizontalScrollView colorScroll = new HorizontalScrollView(context);
        colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colorRow = new LinearLayout(context);
        colorRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < colors.length; i++)
        {
            final int c = colors[i];
            final String cName = colorNames[i];

            TextView colorPill = new TextView(context);
            colorPill.setText(cName);
            colorPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            colorPill.setGravity(Gravity.CENTER);
            colorPill.setPadding(dp(context, 12), dp(context, 6), dp(context, 12), dp(context, 6));

            GradientDrawable cBg = new GradientDrawable();
            cBg.setCornerRadius(dp(context, 16));
            if (c != 0)
            {
                cBg.setColor(c);
                colorPill.setTextColor((c == 0xFFFFFFFF || c == 0xFFFFCC00) ? Color.BLACK : Color.WHITE);
                cBg.setStroke(dp(context, 1), 0x33FFFFFF);
            }
            else
            {
                cBg.setColor(0xFF2C2C34);
                colorPill.setTextColor(0xFFE5E5EA);
                cBg.setStroke(dp(context, 1), 0xFF3E3E48);
            }
            colorPill.setBackground(cBg);

            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            clp.rightMargin = dp(context, 6);
            colorPill.setLayoutParams(clp);

            colorPill.setOnClickListener(v -> {
                chosenColor[0] = c;
                if (c != 0)
                {
                    inputMsg.setTextColor(c);
                }
                else
                {
                    inputMsg.setTextColor(0xFFFFFFFF);
                }
            });

            colorRow.addView(colorPill);
        }
        colorScroll.addView(colorRow);
        contentLayout.addView(colorScroll);

        // Input 2: Quote text if present
        final EditText inputQuote;
        if (quoteObj != null && currentQuoteText != null)
        {
            TextView labelQuote = new TextView(context);
            labelQuote.setText("NỘI DUNG TRÍCH DẪN (QUOTE)");
            labelQuote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            labelQuote.setTypeface(Typeface.DEFAULT_BOLD);
            labelQuote.setTextColor(0xFF8E8E93);
            labelQuote.setPadding(0, dp(context, 12), 0, dp(context, 4));
            contentLayout.addView(labelQuote);

            inputQuote = new EditText(context);
            inputQuote.setText(currentQuoteText);
            inputQuote.setTextColor(0xFFFFFFFF);
            inputQuote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            inputQuote.setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10));

            GradientDrawable qBg = new GradientDrawable();
            qBg.setCornerRadius(dp(context, 12));
            qBg.setColor(0xFF202026);
            qBg.setStroke(dp(context, 1), 0xFF2E2E38);
            inputQuote.setBackground(qBg);

            contentLayout.addView(inputQuote);
        }
        else
        {
            inputQuote = null;
        }

        scrollView.addView(contentLayout);
        root.addView(scrollView);

        // 4. Footer Actions
        LinearLayout footer = new LinearLayout(context);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setPadding(0, dp(context, 14), 0, 0);

        TextView cancelBtn = createActionButton(context, "Huỷ", 0xFF24242C, 0xFF8E8E93);
        cancelBtn.setOnClickListener(v -> dialog.dismiss());
        footer.addView(cancelBtn);

        TextView saveBtn = createActionButton(context, "✓ Lưu & Chụp Ảnh", 0xFF007AFF, 0xFFFFFFFF);
        saveBtn.setTypeface(Typeface.DEFAULT_BOLD);
        saveBtn.setOnClickListener(v -> {
            try
            {
                String newText = inputMsg.getText().toString();
                if (msgTextField != null)
                {
                    msgTextField.set(msg, newText);
                }

                if (quoteObj != null && inputQuote != null && quoteMsgField != null)
                {
                    String newQuote = inputQuote.getText().toString();
                    quoteMsgField.set(quoteObj, newQuote);
                }

                // Apply text color if chosen
                if (chosenColor[0] != 0)
                {
                    try
                    {
                        if (y1Method == null)
                        {
                            for (Method m : msg.getClass().getMethods())
                            {
                                if (m.getName().equals("Y1") && m.getParameterCount() == 0)
                                {
                                    y1Method = m;
                                    break;
                                }
                            }
                        }
                        if (y1Method != null)
                        {
                            Object textPaint = y1Method.invoke(msg);
                            if (textPaint instanceof android.graphics.Paint)
                            {
                                ((android.graphics.Paint) textPaint).setColor(chosenColor[0]);
                            }
                        }
                    }
                    catch (Exception ignored) {}
                }

                // Call I2() if available
                try
                {
                    if (i2Method == null)
                    {
                        Method[] methods = chatRowView.getClass().getMethods();
                        for (Method m : methods)
                        {
                            if (m.getName().equals("I2") && m.getParameterCount() == 0)
                            {
                                i2Method = m;
                                break;
                            }
                        }
                    }
                    if (i2Method != null)
                    {
                        i2Method.invoke(chatRowView);
                    }
                }
                catch (Exception ignored) {}

                // Call q2(msg, true) to re-bind
                if (q2Method != null)
                {
                    q2Method.invoke(chatRowView, msg, true);
                }

                chatRowView.requestLayout();
                chatRowView.invalidate();

                if (chatRowView.getParent() instanceof ViewGroup)
                {
                    ViewGroup parent = (ViewGroup) chatRowView.getParent();
                    parent.requestLayout();
                    parent.invalidate();
                }

                dialog.dismiss();
                Toast.makeText(context, "✅ Đã sửa tin nhắn & màu sắc!", Toast.LENGTH_SHORT).show();
            }
            catch (Exception e)
            {
                Logger.e(e);
                Toast.makeText(context, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        footer.addView(saveBtn);

        root.addView(footer);

        dialog.setContentView(root);
        dialog.show();

        Window win = dialog.getWindow();
        if (win != null)
        {
            win.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            win.setGravity(Gravity.BOTTOM);
            win.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            win.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            win.setDimAmount(0.55f);
            win.setWindowAnimations(android.R.style.Animation_InputMethod);

            WindowManager.LayoutParams wlp = win.getAttributes();
            wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            win.setAttributes(wlp);
        }
    }

    private static TextView createActionButton(Context context, String text, int bgColor, int textColor)
    {
        TextView btn = new TextView(context);
        btn.setText(text);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
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

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(context, 42), 1f);
        lp.leftMargin = dp(context, 4);
        lp.rightMargin = dp(context, 4);
        btn.setLayoutParams(lp);

        return btn;
    }
}
