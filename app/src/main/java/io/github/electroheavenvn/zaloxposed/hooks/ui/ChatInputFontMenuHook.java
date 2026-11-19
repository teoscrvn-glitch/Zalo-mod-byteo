package io.github.electroheavenvn.zaloxposed.hooks.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import io.github.electroheavenvn.zaloxposed.hooks.BaseHook;
import io.github.electroheavenvn.zaloxposed.utilities.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ChatInputFontMenuHook extends BaseHook
{
    private static final int ID_FONT_MENU_BUTTON = 0x7F0A9876;
    private static final int TAG_LISTENER_ATTACHED = 0x7F0A9877;
    private Field actionEditTextField = null;
    private Field sendButtonField = null;

    private static int dp(Context ctx, int val)
    {
        return (int) (val * ctx.getResources().getDisplayMetrics().density);
    }

    @Override
    public void hook() throws Throwable
    {
        try
        {
            Class<?> chatInputBarClass = Class.forName("com.zing.zalo.ui.chat.widget.inputbar.ChatInputBar", false, classLoader);
            Class<?> actionEditTextClass = Class.forName("com.zing.zalo.uicontrol.ActionEditText", false, classLoader);

            // 1. Locate fields
            for (Field f : chatInputBarClass.getDeclaredFields())
            {
                if (actionEditTextClass.isAssignableFrom(f.getType()))
                {
                    actionEditTextField = f;
                    actionEditTextField.setAccessible(true);
                }
                if (f.getName().equals("q") && View.class.isAssignableFrom(f.getType()))
                {
                    sendButtonField = f;
                    sendButtonField.setAccessible(true);
                }
            }

            // 2. Hook setupEditTextState(int) to attach Font button & gestures
            Method setupEditTextState = chatInputBarClass.getDeclaredMethod("setupEditTextState", int.class);
            Logger.i("ChatInputFontMenuHook: Hooking setupEditTextState -> " + setupEditTextState);

            module.hook(setupEditTextState).setPriority(12).intercept(chain ->
            {
                Object result = chain.proceed();
                try
                {
                    Object chatInputBar = chain.getThisObject();
                    if (!(chatInputBar instanceof ViewGroup)) return result;

                    ViewGroup inputBarGroup = (ViewGroup) chatInputBar;
                    Context context = inputBarGroup.getContext();

                    // Retrieve ActionEditText
                    EditText actionEditText = null;
                    if (actionEditTextField != null)
                    {
                        Object et = actionEditTextField.get(chatInputBar);
                        if (et instanceof EditText)
                        {
                            actionEditText = (EditText) et;
                        }
                    }

                    if (actionEditText == null) return result;
                    final EditText finalEditText = actionEditText;

                    // Ensure button is added
                    TextView fontBtn = inputBarGroup.findViewById(ID_FONT_MENU_BUTTON);
                    if (fontBtn == null)
                    {
                        fontBtn = new TextView(context);
                        fontBtn.setId(ID_FONT_MENU_BUTTON);
                        fontBtn.setText("Aa");
                        fontBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                        fontBtn.setTypeface(Typeface.DEFAULT_BOLD);
                        fontBtn.setTextColor(Color.WHITE);
                        fontBtn.setGravity(Gravity.CENTER);

                        GradientDrawable bg = new GradientDrawable();
                        bg.setCornerRadius(dp(context, 15));
                        bg.setColor(0xEE1C1C1E); // Frosted dark glass
                        bg.setStroke(dp(context, 1), 0x55FFFFFF);
                        fontBtn.setBackground(bg);

                        int btnSize = dp(context, 30);
                        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(btnSize, btnSize);
                        lp.gravity = Gravity.CENTER_VERTICAL | Gravity.END;
                        lp.rightMargin = dp(context, 48); // Sits neatly next to Send button
                        fontBtn.setLayoutParams(lp);

                        fontBtn.setOnClickListener(v -> FontMenuDialog.show(v.getContext(), finalEditText));
                        inputBarGroup.addView(fontBtn);
                    }

                    // CRITICAL FIX: Only show button when typing!
                    // When empty, voice button is displayed by Zalo, so fontBtn MUST BE GONE to never block voice.
                    final TextView finalFontBtn = fontBtn;
                    boolean hasText = finalEditText.getText().length() > 0;
                    finalFontBtn.setVisibility(hasText ? View.VISIBLE : View.GONE);

                    if (finalEditText.getTag(TAG_LISTENER_ATTACHED) == null)
                    {
                        finalEditText.setTag(TAG_LISTENER_ATTACHED, Boolean.TRUE);

                        // Dynamic visibility on typing
                        finalEditText.addTextChangedListener(new TextWatcher()
                        {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count)
                            {
                                View btn = inputBarGroup.findViewById(ID_FONT_MENU_BUTTON);
                                if (btn != null)
                                {
                                    btn.setVisibility((s != null && s.length() > 0) ? View.VISIBLE : View.GONE);
                                }
                            }

                            @Override
                            public void afterTextChanged(Editable s) {}
                        });

                        // Double tap on input box triggers menu anytime
                        GestureDetector doubleTapDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener()
                        {
                            @Override
                            public boolean onDoubleTap(MotionEvent e)
                            {
                                FontMenuDialog.show(context, finalEditText);
                                return true;
                            }
                        });

                        finalEditText.setOnTouchListener((v, event) -> {
                            doubleTapDetector.onTouchEvent(event);
                            return false; // let normal text touches proceed
                        });
                    }

                    // Attach long click listener to send button
                    if (sendButtonField != null)
                    {
                        Object sendBtn = sendButtonField.get(chatInputBar);
                        if (sendBtn instanceof View)
                        {
                            ((View) sendBtn).setOnLongClickListener(v ->
                            {
                                FontMenuDialog.show(v.getContext(), finalEditText);
                                return true;
                            });
                        }
                    }
                }
                catch (Exception e)
                {
                    Logger.e(e);
                }
                return result;
            });

            // 3. Hook onLongClick on ChatInputBar as backup
            Method onLongClickMethod = chatInputBarClass.getDeclaredMethod("onLongClick", View.class);
            Logger.i("ChatInputFontMenuHook: Hooking onLongClick -> " + onLongClickMethod);

            module.hook(onLongClickMethod).intercept(chain ->
            {
                View clickedView = (View) chain.getArg(0);
                if (clickedView != null)
                {
                    Context context = clickedView.getContext();
                    int sendId = context.getResources().getIdentifier("new_chat_input_btn_chat_send", "id", context.getPackageName());
                    if (sendId != 0 && clickedView.getId() == sendId)
                    {
                        Object chatInputBar = chain.getThisObject();
                        if (actionEditTextField != null)
                        {
                            Object et = actionEditTextField.get(chatInputBar);
                            if (et instanceof EditText)
                            {
                                FontMenuDialog.show(context, (EditText) et);
                                return true;
                            }
                        }
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
}
