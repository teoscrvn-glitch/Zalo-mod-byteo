package io.github.electroheavenvn.zaloxposed.hooks.functional;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.github.electroheavenvn.zaloxposed.hooks.BaseHook;
import io.github.electroheavenvn.zaloxposed.utilities.Config;
import io.github.electroheavenvn.zaloxposed.utilities.FontHelper;
import io.github.electroheavenvn.zaloxposed.utilities.Logger;

import org.luckypray.dexkit.query.FindClass;
import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.ClassMatcher;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.ClassData;
import org.luckypray.dexkit.result.MethodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

public class TrollReplyAndFontHook extends BaseHook
{
    private Class<?> messageBuilderClass = null; // j20.n
    private Class<?> quoteClass = null;          // j20.l2
    private Field textMsgField = null;           // j20.n.g
    private Field quoteField = null;             // j20.n.l
    private Field quoteMsgField = null;          // j20.l2.f

    @Override
    public void hook() throws Throwable
    {
        hookMessageBuilder();
        hookQuotePreviewBar();
    }

    private void hookMessageBuilder()
    {
        try
        {
            // 1. Find quoteClass (j20.l2)
            try
            {
                quoteClass = Class.forName("j20.l2", false, classLoader);
            }
            catch (ClassNotFoundException e)
            {
                List<ClassData> quoteClasses = bridge.findClass(FindClass.create()
                    .matcher(ClassMatcher.create()
                        .addUsingString("replySenderUid=", StringMatchType.Contains)
                        .addUsingString("replySenderGlobalUid=", StringMatchType.Contains)
                        .addUsingString("replyMsg=", StringMatchType.Contains)
                    ));
                if (!quoteClasses.isEmpty())
                {
                    quoteClass = quoteClasses.get(0).getInstance(classLoader);
                }
            }

            if (quoteClass != null)
            {
                try
                {
                    quoteMsgField = quoteClass.getDeclaredField("f");
                    quoteMsgField.setAccessible(true);
                }
                catch (NoSuchFieldException e)
                {
                    // Fallback to searching field of type String
                    for (Field f : quoteClass.getDeclaredFields())
                    {
                        if (f.getType().equals(String.class) && !Modifier.isStatic(f.getModifiers()))
                        {
                            quoteMsgField = f;
                            quoteMsgField.setAccessible(true);
                            break;
                        }
                    }
                }
            }

            // 2. Find messageBuilderClass (j20.n)
            Method buildMethod = null;
            try
            {
                messageBuilderClass = Class.forName("j20.n", false, classLoader);
                buildMethod = messageBuilderClass.getDeclaredMethod("a");
            }
            catch (Throwable ignored)
            {
                List<MethodData> methods = bridge.findMethod(FindMethod.create()
                    .matcher(MethodMatcher.create()
                        .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                        .paramCount(0)
                        .returnType("j20.q")
                    ));
                for (MethodData md : methods)
                {
                    Method m = md.getMethodInstance(classLoader);
                    if ("a".equals(m.getName()))
                    {
                        messageBuilderClass = m.getDeclaringClass();
                        buildMethod = m;
                        break;
                    }
                }
            }

            if (messageBuilderClass == null || buildMethod == null)
            {
                Logger.e("TrollReplyAndFontHook: Target messageBuilderClass or buildMethod not found");
                return;
            }

            // Locate fields in messageBuilderClass
            try
            {
                textMsgField = messageBuilderClass.getDeclaredField("g");
                textMsgField.setAccessible(true);
            }
            catch (NoSuchFieldException ignored) {}

            try
            {
                quoteField = messageBuilderClass.getDeclaredField("l");
                quoteField.setAccessible(true);
            }
            catch (NoSuchFieldException ignored) {}

            if (quoteField == null && quoteClass != null)
            {
                for (Field f : messageBuilderClass.getDeclaredFields())
                {
                    if (f.getType().equals(quoteClass))
                    {
                        quoteField = f;
                        quoteField.setAccessible(true);
                        break;
                    }
                }
            }

            Logger.i("TrollReplyAndFontHook: Hooking message builder -> " + buildMethod);
            module.hook(buildMethod).setPriority(10).intercept(chain ->
            {
                Object builder = chain.getThisObject();
                if (builder != null && textMsgField != null)
                {
                    try
                    {
                        Object textObj = textMsgField.get(builder);
                        if (textObj instanceof String)
                        {
                            String originalText = (String) textObj;
                            String text = originalText.trim();
                            String lower = text.toLowerCase();

                            // 1. Process Troll Quote Reply syntax
                            if (Config.getEnableTrollReply() && (lower.startsWith("/troll ") || lower.startsWith("/t ") || lower.startsWith("/q ") || lower.startsWith("/fake ")))
                            {
                                int firstSpace = text.indexOf(' ');
                                String content = text.substring(firstSpace + 1).trim();

                                String fakeQuote;
                                String realReply;

                                int barIdx = content.indexOf('|');
                                if (barIdx != -1)
                                {
                                    fakeQuote = content.substring(0, barIdx).trim();
                                    realReply = content.substring(barIdx + 1).trim();
                                }
                                else
                                {
                                    fakeQuote = content;
                                    realReply = ":))";
                                }

                                Object quoteObj = (quoteField != null) ? quoteField.get(builder) : null;
                                if (quoteObj != null && quoteMsgField != null)
                                {
                                    quoteMsgField.set(quoteObj, fakeQuote);
                                    Logger.i("Troll quote modified to: " + fakeQuote);
                                }

                                textMsgField.set(builder, realReply);
                                text = realReply;
                            }

                            // 2. Process Unicode Fancy Font conversion
                            Object currentTextObj = textMsgField.get(builder);
                            if (currentTextObj instanceof String)
                            {
                                String currentText = (String) currentTextObj;
                                String transformed = FontHelper.processText(currentText, Config.getDefaultFontStyle());
                                if (!currentText.equals(transformed))
                                {
                                    textMsgField.set(builder, transformed);
                                    Logger.i("Font transformed outgoing message text");
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        Logger.e(e);
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

    private void hookQuotePreviewBar()
    {
        try
        {
            Class<?> containerClass = Class.forName("com.zing.zalo.ui.chat.widget.attachment.ChatAttachmentContainer", false, classLoader);
            Class<?> itemClass = Class.forName("ai1.i", false, classLoader);

            Method createReplyViewMethod = null;
            try
            {
                createReplyViewMethod = containerClass.getDeclaredMethod("f", itemClass);
            }
            catch (NoSuchMethodException e)
            {
                for (Method m : containerClass.getDeclaredMethods())
                {
                    if (m.getParameterCount() == 1 && m.getParameterTypes()[0].equals(itemClass) && View.class.isAssignableFrom(m.getReturnType()))
                    {
                        createReplyViewMethod = m;
                        break;
                    }
                }
            }

            if (createReplyViewMethod == null)
            {
                Logger.e("TrollReplyAndFontHook: createReplyViewMethod not found in ChatAttachmentContainer");
                return;
            }

            Field itemQuoteField = null;
            try
            {
                itemQuoteField = itemClass.getDeclaredField("d");
                itemQuoteField.setAccessible(true);
            }
            catch (NoSuchFieldException ignored) {}

            final Field finalItemQuoteField = itemQuoteField;

            Logger.i("TrollReplyAndFontHook: Hooking quote preview view -> " + createReplyViewMethod);
            module.hook(createReplyViewMethod).intercept(chain ->
            {
                Object resultView = chain.proceed();
                if (!Config.getEnableTrollReply())
                {
                    return resultView;
                }

                if (resultView instanceof View)
                {
                    View previewView = (View) resultView;
                    Object item = chain.getArg(0);
                    if (item != null && finalItemQuoteField != null)
                    {
                        Object quoteObj = finalItemQuoteField.get(item);
                        if (quoteObj != null && quoteMsgField != null)
                        {
                            // Enable click or long click on the quote preview bar to edit quoted text in-place!
                            previewView.setOnLongClickListener(v ->
                            {
                                showEditQuoteDialog(v.getContext(), quoteObj, previewView);
                                return true;
                            });

                            previewView.setOnClickListener(v ->
                            {
                                showEditQuoteDialog(v.getContext(), quoteObj, previewView);
                            });
                        }
                    }
                }

                return resultView;
            });
        }
        catch (Throwable t)
        {
            Logger.e(t);
        }
    }

    private void showEditQuoteDialog(Context context, Object quoteObj, View previewView)
    {
        try
        {
            String currentQuote = (String) quoteMsgField.get(quoteObj);
            if (currentQuote == null) currentQuote = "";

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("✏️ Sửa trích dẫn (Troll Reply)");

            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.VERTICAL);
            int pad = (int) (18 * context.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad / 2, pad, pad / 2);

            TextView hintTv = new TextView(context);
            hintTv.setText("Sửa nội dung người này đã nói để trích dẫn troll:");
            hintTv.setTextSize(14f);
            hintTv.setPadding(0, 0, 0, pad / 3);
            layout.addView(hintTv);

            final EditText input = new EditText(context);
            input.setText(currentQuote);
            input.setSelection(input.getText().length());
            layout.addView(input);

            builder.setView(layout);

            final String initialQuote = currentQuote;
            builder.setPositiveButton("Lưu & Troll", (dialog, which) ->
            {
                try
                {
                    String newQuote = input.getText().toString();
                    quoteMsgField.set(quoteObj, newQuote);

                    // Update UI text in preview view
                    int replyTextId = context.getResources().getIdentifier("reply_text", "id", context.getPackageName());
                    if (replyTextId != 0)
                    {
                        View tv = previewView.findViewById(replyTextId);
                        if (tv instanceof TextView)
                        {
                            ((TextView) tv).setText(newQuote);
                        }
                    }

                    Toast.makeText(context, "✅ Đã sửa trích dẫn! Giờ hãy gõ câu trả lời của bạn.", Toast.LENGTH_SHORT).show();
                }
                catch (Exception e)
                {
                    Logger.e(e);
                    Toast.makeText(context, "Lỗi khi sửa quote: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            builder.setNegativeButton("Huỷ", (dialog, which) -> dialog.dismiss());
            builder.show();
        }
        catch (Exception e)
        {
            Logger.e(e);
        }
    }
}
