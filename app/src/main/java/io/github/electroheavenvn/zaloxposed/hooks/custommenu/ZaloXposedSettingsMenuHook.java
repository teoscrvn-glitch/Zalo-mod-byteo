package io.github.electroheavenvn.zaloxposed.hooks.custommenu;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import io.github.electroheavenvn.zaloxposed.MorpheConstants;
import io.github.electroheavenvn.zaloxposed.hooks.BaseHook;
import io.github.electroheavenvn.zaloxposed.hooks.ResourceHook;
import io.github.electroheavenvn.zaloxposed.hooks.ui.CustomBackgroundHook;
import io.github.electroheavenvn.zaloxposed.utilities.Config;
import io.github.electroheavenvn.zaloxposed.utilities.FontHelper;
import io.github.electroheavenvn.zaloxposed.utilities.Logger;
import io.github.electroheavenvn.zaloxposed.utilities.Utils;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressLint("SetTextI18n")
public class ZaloXposedSettingsMenuHook extends BaseHook
{
    private static final String CUSTOM_ITEM_MARKER = "zalo_xposed_settings";
    private static final int CUSTOM_DRAWABLE_ID = -6767;
    private static final int REQUEST_CODE_IMPORT_CONFIG = 3636;
    private static final int REQUEST_CODE_PICK_BACKGROUND = 4282;
    private static boolean isOpenZaloXposedSettings1 = false;
    private static boolean isOpenZaloXposedSettings2 = false;
    @SuppressLint("StaticFieldLeak")
    private static LinearLayout rootLayout = null;
    private static Class<?> headerTextViewClass = null;
    private TextView templateHeader = null;
    private View templateSeparator = null;

    private void createCustomMenu() throws Exception
    {
        Context context = rootLayout.getContext();
        View separator;
        TextView headerTitle;
        RelativeLayout listItemSetting;


        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "App Custom Wallpaper" : "Hình nền ứng dụng (Toàn App)");
        rootLayout.addView(headerTitle);

        // 1. Bật/Tắt hình nền
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Enable app wallpaper" : "Bật hình nền ứng dụng");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Apply wallpaper across main app screen" : "Hiển thị ảnh nền trên toàn bộ màn hình chính");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableBackground());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, enabled -> {
            Config.setEnableBackground(enabled);
            CustomBackgroundHook.reloadBackground(context);
        });

        // 2. Chọn ảnh từ thiết bị
        RelativeLayout pickBgItem = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(pickBgItem);
        ListItemSettingHelper.SetIDTracking(pickBgItem, "");
        ListItemSettingHelper.ShowDivider(pickBgItem);
        ListItemSettingHelper.SetTitle(pickBgItem, Utils.IsEnglish() ? "Select wallpaper from device" : "Chọn ảnh nền từ thiết bị");
        boolean hasCustom = CustomBackgroundHook.hasCustomBackground(context);
        ListItemSettingHelper.SetSubtitle(pickBgItem, hasCustom ?
            (Utils.IsEnglish() ? "Custom photo set (tap to change)" : "Đã cài ảnh từ máy (chạm để đổi ảnh khác)") :
            (Utils.IsEnglish() ? "Tap to choose image from gallery" : "Chạm để chọn ảnh từ thư viện máy"));
        ListItemSettingHelper.SetShowChevronRight(pickBgItem, true);
        ListItemSettingHelper.SetOnClickListener(pickBgItem, v -> launchImagePicker(context));

        // 3. Độ mờ / Độ đậm
        RelativeLayout opacityItem = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(opacityItem);
        ListItemSettingHelper.SetIDTracking(opacityItem, "");
        ListItemSettingHelper.ShowDivider(opacityItem);
        ListItemSettingHelper.SetTitle(opacityItem, Utils.IsEnglish() ? "Wallpaper opacity" : "Độ mờ / Độ đậm ảnh nền");
        int opacityPct = (int)(Config.getBackgroundOpacity() * 100);
        ListItemSettingHelper.SetSubtitle(opacityItem, opacityPct + "%" + (Utils.IsEnglish() ? " (tap to adjust)" : " (chạm để chỉnh)"));
        ListItemSettingHelper.SetShowChevronRight(opacityItem, true);
        ListItemSettingHelper.SetOnClickListener(opacityItem, v -> showOpacityDialog(context, opacityItem));

        // 4. Xoá ảnh nền tuỳ chọn
        RelativeLayout resetBgItem = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(resetBgItem);
        ListItemSettingHelper.SetIDTracking(resetBgItem, "");
        ListItemSettingHelper.HideDivider(resetBgItem);
        ListItemSettingHelper.SetTitle(resetBgItem, Utils.IsEnglish() ? "Remove custom wallpaper" : "Xoá ảnh nền tuỳ chỉnh");
        ListItemSettingHelper.SetSubtitle(resetBgItem, Utils.IsEnglish() ? "Remove chosen image and revert to default" : "Xoá ảnh đã cài, quay về mặc định");
        ListItemSettingHelper.SetShowChevronRight(resetBgItem, true);
        ListItemSettingHelper.SetOnClickListener(resetBgItem, v -> {
            resetCustomBackground(context);
            try {
                ListItemSettingHelper.SetSubtitle(pickBgItem, Utils.IsEnglish() ? "Tap to choose image from gallery" : "Chạm để chọn ảnh từ thư viện máy");
            } catch (Exception ignored) {}
        });

        // ==================== TÍNH NĂNG TROLL & FONT CHỮ ====================
        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Troll & Text Formatting" : "Troll & Tuỳ chỉnh tin nhắn");
        rootLayout.addView(headerTitle);

        // 1. Sửa tin nhắn tại chỗ (Chụp ảnh màn hình)
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Client-side Message Editor" : "Sửa tin nhắn tại chỗ (Chụp ảnh màn hình)");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Triple-tap any message to edit text locally for screenshots" : "Chạm 3 lần vào tin nhắn bất kỳ để sửa nội dung và chụp màn hình sống ảo");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableClientMessageEditor());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableClientMessageEditor);

        // 2. Troll tin nhắn Reply
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Fake Quote Reply" : "Troll tin nhắn Reply (Sửa câu trích dẫn)");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Tap quote bar or use /troll <fake> | <reply>" : "Chạm thanh quote hoặc gõ /troll <câu giả> | <câu thật> khi trả lời");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableTrollReply());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableTrollReply);

        // 3. Kiểu font chữ mặc định
        RelativeLayout fontItem = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(fontItem);
        ListItemSettingHelper.SetIDTracking(fontItem, "");
        ListItemSettingHelper.ShowDivider(fontItem);
        ListItemSettingHelper.SetTitle(fontItem, Utils.IsEnglish() ? "Default Font Style" : "Kiểu font chữ mặc định");
        ListItemSettingHelper.SetSubtitle(fontItem, getFontDisplayName(Config.getDefaultFontStyle()) + " (chạm để chọn)");
        ListItemSettingHelper.SetShowChevronRight(fontItem, true);
        ListItemSettingHelper.SetOnClickListener(fontItem, v -> showFontSelectionDialog(context, fontItem));

        // 4. Hướng dẫn cú pháp lệnh nhanh
        RelativeLayout syntaxItem = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(syntaxItem);
        ListItemSettingHelper.SetIDTracking(syntaxItem, "");
        ListItemSettingHelper.HideDivider(syntaxItem);
        ListItemSettingHelper.SetTitle(syntaxItem, Utils.IsEnglish() ? "Commands & Syntax Guide" : "Xem hướng dẫn lệnh Troll & Font chữ");
        ListItemSettingHelper.SetSubtitle(syntaxItem, Utils.IsEnglish() ? "View all available prefix commands (/b, /c, /g, /troll...)" : "Xem danh sách lệnh đổi font nhanh và troll reply");
        ListItemSettingHelper.SetShowChevronRight(syntaxItem, true);
        ListItemSettingHelper.SetOnClickListener(syntaxItem, v -> showSyntaxHelpDialog(context));


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText("Mini Chat (Chat Head)");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Replace Android Bubble conversations with Mini Chat" : "Thay thế bong bóng hội thoại Android bằng Mini Chat");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Requires Android 11 or higher" : "Yêu cầu Android 11 trở lên");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableChatHead());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableChatHead);
        listItemSetting.setEnabled(Build.VERSION.SDK_INT > Build.VERSION_CODES.Q);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Disappearing messages" : "Tin nhắn tự xoá");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Override disappearing messages config" : "Ghi đè cấu hình tin nhắn tự xoá");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableTTLOverride());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableTTLOverride);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Disappearing message time" : "Thời gian tin nhắn tự xoá");
        TextView titleView = findTitleTextView(headerTitle);
        if (titleView != null)
        {
            TextView listItemSettingTitle = findTitleTextView(listItemSetting);
            if (listItemSettingTitle != null)
                titleView.setTextColor(listItemSettingTitle.getTextColors());
        }
        rootLayout.addView(headerTitle);
        EditText input = new EditText(context);
        rootLayout.addView(input);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setText(Config.getTTL() + "");
        input.setHint(Utils.IsEnglish() ? "Enter value (milliseconds)" : "Nhập giá trị (mili giây)");
        styleEditText(input, context, listItemSetting);
        input.setOnEditorActionListener((textView, actionId, keyEvent) ->
        {
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                try
                {
                    long ttlValue = Long.parseLong(textView.getText().toString());
                    Config.setTTL(ttlValue);
                }
                catch (Exception e)
                {
                    Logger.e(e);
                }
                textView.clearFocus();
                return true;
            }
            return false;
        });
        input.setOnFocusChangeListener((view, hasFocus) ->
        {
            if (!hasFocus)
            {
                try
                {
                    EditText editText = (EditText)view;
                    long ttlValue = Long.parseLong(editText.getText().toString());
                    Config.setTTL(ttlValue);
                    hideKeyboard(editText);
                }
                catch (Exception e)
                {
                    Logger.e(e);
                }
            }
        });
        rootLayout.setOnClickListener(v -> input.clearFocus());


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Ads" : "Quảng cáo");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide Media Box" : "Ẩn Media Box");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideMediaBox());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideMediaBox);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide Business Box" : "Ẩn Business Box");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideBizBox());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideBizBox);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide ZInstant Ads" : "Ẩn quảng cáo ZInstant");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Hide ads in Messages tab" : "Ẩn quảng cáo trong tab Tin nhắn");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideZInstantAds());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideZInstantAds);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide Ads in Timeline" : "Ẩn quảng cáo trong Nhật Ký");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideFeedItemZInstantAds());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideFeedItemZInstantAds);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Block ads network endpoints" : "Chặn yêu cầu mạng quảng cáo");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getBlockAdsEndpoints());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setBlockAdsEndpoints);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Logging and tracking" : "Ghi nhật ký và theo dõi");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Disable Firebase logging" : "Tắt ghi nhật ký Firebase");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getDisableFirebaseLogging());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setDisableFirebaseLogging);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Disable Zalo tracking" : "Tắt tính năng theo dõi của Zalo");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getDisableZaloTracking());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setDisableZaloTracking);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Block QoS network endpoints" : "Chặn yêu cầu mạng QoS");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getBlockQoSEndpoints());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setBlockQoSEndpoints);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Recalled and deleted messages" : "Thu hồi và xoá tin nhắn");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Anti-Recall" : "Chống thu hồi tin nhắn");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableAntiRecall());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableAntiRecall);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Anti-Delete" : "Chống xoá tin nhắn");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableAntiDelete());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableAntiDelete);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Include my recalled messages" : "Bao gồm tin nhắn tôi đã thu hồi");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getAntiRecallIncludeMe());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setAntiRecallIncludeMe);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Include messages deleted by me" : "Bao gồm tin nhắn được tôi xoá");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getAntiDeleteIncludeMyDeletion());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setAntiDeleteIncludeMyDeletion);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Privacy" : "Quyền riêng tư");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide typing indicators" : "Ẩn trạng thái đang nhập");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableSilentTyping());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableSilentTyping);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide \"Seen\" status" : "Ẩn trạng thái Đã xem");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getBlockSendSeen());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setBlockSendSeen);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "User interface" : "Giao diện");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Enable extended grid menu" : "Kích hoạt chat menu mở rộng");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableExtendedGridMenu());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableExtendedGridMenu);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Enable extended group right menu" : "Kích hoạt menu nhóm mở rộng");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableExtendedGroupRightMenu());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableExtendedGroupRightMenu);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Enable extended group settings menu" : "Kích hoạt menu cài đặt nhóm mở rộng");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableExtendedGroupSettingMenu());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableExtendedGroupSettingMenu);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText("ZCloud");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Unlock ZCloud" : "Mở khoá ZCloud");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Does not increase My Documents capacity" : "Không tăng dung lượng bộ nhớ My Documents");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getUnlockZCloud());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setUnlockZCloud);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Backup messages" : "Sao lưu tin nhắn");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Restore Google Drive backup options" : "Phục hồi tuỳ chọn sao lưu ảnh bằng Google Drive");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Replace the ZCloud backup options" : "Thay thế tuỳ chọn sao lưu bằng ZCloud");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getRestoreGoogleDriveBackup());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setRestoreGoogleDriveBackup);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Profile" : "Hồ sơ");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Restore profile music options" : "Phục hồi tuỳ chọn nhạc nền hồ sơ");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Replace the ZStyle profile music options" : "Thay thế tuỳ chọn chọn nhạc nền hồ sơ bằng ZStyle");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getRestoreProfileMusic());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setRestoreProfileMusic);

    
        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Group" : "Nhóm");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Fake my role in groups" : "Giả mạo vai trò trong nhóm");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableFakeGroupRole());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableFakeGroupRole);
        RelativeLayout listItemSettingFakeRole = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSettingFakeRole);
        ListItemSettingHelper.SetIDTracking(listItemSettingFakeRole, "");
        ListItemSettingHelper.HideDivider(listItemSettingFakeRole);
        ListItemSettingHelper.SetTitle(listItemSettingFakeRole, Utils.IsEnglish() ? "Fake role" : "Vai trò giả mạo");
        String fakeRole = getFakeRoleName(Config.getFakeGroupRoleLevel());
        ListItemSettingHelper.SetStateSetting(listItemSettingFakeRole, fakeRole);
        ListItemSettingHelper.SetOnClickListener(listItemSettingFakeRole, v ->
        {
            int roleLevel = Config.getFakeGroupRoleLevel();
            roleLevel = (roleLevel + 1) % 2;
            Config.setFakeGroupRoleLevel(roleLevel);
            String newFakeRole = getFakeRoleName(roleLevel);
            try
            {
                ListItemSettingHelper.SetStateSetting(listItemSettingFakeRole, newFakeRole);
            }
            catch (Exception e)
            {
                Logger.e(e);
            }
        });

    
        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Sticker pack" : "Bộ sticker");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Allow sharing hidden sticker packs" : "Cho phép chia sẻ bộ sticker ẩn");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableShareHiddenStickerPack());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableShareHiddenStickerPack);

        
        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Bottom row" : "Thanh dưới");
        rootLayout.addView(headerTitle);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Enable bottom row customization" : "Kích hoạt tuỳ chỉnh thanh dưới");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getEnableCustomizeBottomRow());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setEnableCustomizeBottomRow);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide Discovery tab" : "Ẩn tab Khám Phá");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideDiscoveryTab());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideDiscoveryTab);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Hide Newsfeed tab" : "Ẩn tab Tường Nhà");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getHideNewsFeedTab());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setHideNewsFeedTab);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Show Groups tab" : "Hiện tab Nhóm");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getShowGroupsTab());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setShowGroupsTab);
        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Show More tab" : "Hiện tab Thêm");
        ListItemSettingHelper.SetSwitch(listItemSetting, Config.getShowMoreTab());
        ListItemSettingHelper.SetCheckedChangeListener(listItemSetting, Config::setShowMoreTab);


        separator = createSeparator(context);
        rootLayout.addView(separator);
        headerTitle = createHeaderTitle(context);
        headerTitle.setText(Utils.IsEnglish() ? "Configuration backup & restore" : "Sao lưu & Khôi phục cấu hình");
        rootLayout.addView(headerTitle);

        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.ShowDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Export configuration file" : "Xuất tệp cấu hình");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Save to Downloads/ZaloXposed" : "Lưu vào thư mục Downloads/ZaloXposed");
        ListItemSettingHelper.SetOnClickListener(listItemSetting, v -> exportConfigFile(context));

        listItemSetting = ListItemSettingHelper.CreateNew(context);
        rootLayout.addView(listItemSetting);
        ListItemSettingHelper.SetIDTracking(listItemSetting, "");
        ListItemSettingHelper.HideDivider(listItemSetting);
        ListItemSettingHelper.SetTitle(listItemSetting, Utils.IsEnglish() ? "Import configuration from file" : "Nhập cấu hình từ tệp");
        ListItemSettingHelper.SetSubtitle(listItemSetting, Utils.IsEnglish() ? "Choose a JSON file" : "Chọn tệp JSON từ bộ nhớ");
        ListItemSettingHelper.SetOnClickListener(listItemSetting, v -> openFilePickerForImport(context));


        Object restartButton = ZButtonHelper.CreateNew(context);
        rootLayout.addView((View)restartButton);
        ZButtonHelper.SetText(restartButton, Utils.IsEnglish() ? "Restart app" : "Khởi động lại ứng dụng");
        ZButtonHelper.SetOnClickListener(restartButton, v -> restartApp(context));
        LinearLayout.LayoutParams restartLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int pad16 = dp(context, 16);
        restartLp.setMargins(pad16, pad16, pad16, 0);
        ((View)restartButton).setLayoutParams(restartLp);


        for (int i = 0; i < 5; i++)
        {
            separator = createSeparator(context);
            rootLayout.addView(separator);
        }
    }

    @Override
    public void hook() throws Throwable
    {
        ResourceHook.map(CUSTOM_DRAWABLE_ID, "extension_24px", "zds_oic_premium_crown_color_24");
        ListItemSettingHelper.Init(classLoader);
        ZButtonHelper.Init(classLoader);
        TabMeSettingItemHelper.Init(bridge, classLoader);
        hookTabMeView();
        hookSettingPrivateView();
        hookActivityForFilePicker();
        List<MethodData> methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass("com.zing.zalo.ui.maintab.me.TabMeView")
                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                .returnType("boolean")
                .addUsingString("features@tabme@fullzins@enable", StringMatchType.Equals)
                .addUsingNumber(0)
                .addUsingNumber(1)
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found 5");
            return;
        }
        for (MethodData methodData : methods)
        {
            Method method = methodData.getMethodInstance(classLoader);
            Logger.i("Hooking: " + method);
            module.hook(method).setPriority(10).intercept(chain -> false);
        }
    }

    private void exportConfigFile(Context context)
    {
        try
        {
            String jsonString = Config.getConfigJsonString();
            boolean success = false;
            String exportedPath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            {
                ContentResolver resolver = context.getContentResolver();
                Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                String fileName = "zaloxposed_config.json";
                String relativePath = android.os.Environment.DIRECTORY_DOWNLOADS + "/ZaloXposed/";
                try
                {
                    String selection = MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
                    String[] selectionArgs = new String[]{fileName};
                    resolver.delete(collection, selection, selectionArgs);
                }
                catch (Exception ignored) { }
                android.content.ContentValues contentValues = new android.content.ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/json");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
                Uri uri = resolver.insert(collection, contentValues);
                if (uri != null)
                {
                    try (java.io.OutputStream os = resolver.openOutputStream(uri, "rwt"))
                    {
                        if (os != null)
                        {
                            os.write(jsonString.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                            os.flush();
                            success = true;
                            exportedPath = "Download/ZaloXposed/" + fileName;
                        }
                    }
                }
            }
            if (!success)
            {
                File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                File exportDir = new File(downloadDir, "ZaloXposed");
                if (!exportDir.exists())
                    exportDir.mkdirs();
                File targetFile = new File(exportDir, "zaloxposed_config.json");
                success = Config.exportToFile(targetFile);
                if (success)
                    exportedPath = targetFile.getAbsolutePath();
            }
            boolean finalSuccess = success;
            String finalPath = exportedPath;
            new Handler(Looper.getMainLooper()).post(() ->
            {
                if (finalSuccess)
                    Toast.makeText(context, Utils.IsEnglish() ? "Exported config file to: " + finalPath : "Đã xuất tệp cấu hình ra: " + finalPath, Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(context, Utils.IsEnglish() ? "Export failed (permission denied)!" : "Xuất cấu hình thất bại (không có quyền bộ nhớ)!", Toast.LENGTH_SHORT).show();
            });
        }
        catch (Exception e)
        {
            Logger.e(e);
        }
    }

    private void openFilePickerForImport(Context context)
    {
        try
        {
            Activity activity = null;
            Context ctx = context;
            while (ctx instanceof ContextWrapper)
            {
                if (ctx instanceof Activity)
                {
                    activity = (Activity)ctx;
                    break;
                }
                ctx = ((ContextWrapper)ctx).getBaseContext();
            }
            if (activity == null)
            {
                new Handler(Looper.getMainLooper()).post(() ->
                {
                    Toast.makeText(context, Utils.IsEnglish() ? "Cannot find activity to open file picker!" : "Không tìm thấy Activity để mở trình chọn file!", Toast.LENGTH_SHORT).show();
                });
                return;
            }
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] extraMimeTypes = new String[]{"application/json", "text/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, extraMimeTypes);
            activity.startActivityForResult(intent, REQUEST_CODE_IMPORT_CONFIG);
        }
        catch (Exception e)
        {
            Logger.e(e);
            try
            {
                // Fallback to direct ACTION_GET_CONTENT
                if (context instanceof Activity)
                {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("*/*");
                    ((Activity)context).startActivityForResult(intent, REQUEST_CODE_IMPORT_CONFIG);
                }
            }
            catch (Exception ex)
            {
                Logger.e(ex);
            }
        }
    }

    private void hookActivityForFilePicker()
    {
        try
        {
            Method onActivityResultMethod = Activity.class.getDeclaredMethod("onActivityResult", int.class, int.class, Intent.class);
            Logger.i("Hooking: " + onActivityResultMethod);
            module.hook(onActivityResultMethod).setPriority(10).intercept(chain ->
            {
                int requestCode = (int)chain.getArg(0);
                if (requestCode == REQUEST_CODE_IMPORT_CONFIG)
                {
                    int resultCode = (int)chain.getArg(1);
                    Intent data = (Intent)chain.getArg(2);
                    Activity activity = (Activity)chain.getThisObject();
                    if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null)
                        handleImportUri(activity, data.getData());
                    return null;
                }
                if (requestCode == REQUEST_CODE_PICK_BACKGROUND)
                {
                    int resultCode = (int)chain.getArg(1);
                    Intent data = (Intent)chain.getArg(2);
                    Activity activity = (Activity)chain.getThisObject();
                    if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null)
                        handleBackgroundUri(activity, data.getData());
                    return null;
                }
                return chain.proceed();
            });
        }
        catch (Exception e)
        {
            Logger.e(e);
        }
    }

    private void handleImportUri(Context context, Uri uri)
    {
        try
        {
            boolean success = false;
            try (java.io.InputStream inputStream = context.getContentResolver().openInputStream(uri))
            {
                if (inputStream != null)
                    success = Config.importFromStream(inputStream);
            }
            boolean finalSuccess = success;
            new Handler(Looper.getMainLooper()).post(() ->
            {
                if (finalSuccess)
                    Toast.makeText(context, Utils.IsEnglish() ? "Configuration imported successfully, restart app to apply." : "Đã nhập cấu hình, khởi động lại app để áp dụng.", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(context, Utils.IsEnglish() ? "Failed to read or parse configuration file!" : "Không thể đọc tệp hoặc định dạng JSON không hợp lệ!", Toast.LENGTH_SHORT).show();
            });
        }
        catch (Exception e)
        {
            Logger.e(e);
            new Handler(Looper.getMainLooper()).post(() ->
            {
                Toast.makeText(context, Utils.IsEnglish() ? "Error importing: " + e.getMessage() : "Lỗi khi nhập tệp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private Activity getActivity(Context context)
    {
        Context ctx = context;
        while (ctx instanceof ContextWrapper)
        {
            if (ctx instanceof Activity)
                return (Activity)ctx;
            ctx = ((ContextWrapper)ctx).getBaseContext();
        }
        return null;
    }

    private void launchImagePicker(Context context)
    {
        Activity activity = getActivity(context);
        if (activity == null)
        {
            new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, Utils.IsEnglish() ? "Cannot find activity to pick image!" : "Không tìm thấy Activity để chọn ảnh!", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        try
        {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivityForResult(intent, REQUEST_CODE_PICK_BACKGROUND);
        }
        catch (Throwable t)
        {
            Logger.e(t);
            try
            {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                activity.startActivityForResult(intent, REQUEST_CODE_PICK_BACKGROUND);
            }
            catch (Throwable ex)
            {
                Logger.e(ex);
            }
        }
    }

    private void handleBackgroundUri(Context context, Uri uri)
    {
        try
        {
            boolean success = CustomBackgroundHook.saveCustomBackground(context, uri);
            new Handler(Looper.getMainLooper()).post(() ->
            {
                if (success)
                {
                    Config.setEnableBackground(true);
                    CustomBackgroundHook.reloadBackground(context);
                    Toast.makeText(context, Utils.IsEnglish() ? "Custom wallpaper applied!" : "Đã cài đặt hình nền ứng dụng thành công!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(context, Utils.IsEnglish() ? "Failed to save wallpaper!" : "Không thể lưu hình nền!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (Throwable t)
        {
            Logger.e(t);
        }
    }

    private void showOpacityDialog(Context context, RelativeLayout settingItem)
    {
        Activity activity = getActivity(context);
        if (activity == null) return;

        final String[] options = new String[]{
            Utils.IsEnglish() ? "15% - Very subtle" : "15% - Mờ nhẹ",
            Utils.IsEnglish() ? "25% - Subtle (Default)" : "25% - Vừa phải (Mặc định)",
            Utils.IsEnglish() ? "40% - Moderate" : "40% - Khá rõ",
            Utils.IsEnglish() ? "60% - Vibrant" : "60% - Đậm nét",
            Utils.IsEnglish() ? "85% - Strong" : "85% - Rất đậm"
        };
        final float[] values = new float[]{0.15f, 0.25f, 0.40f, 0.60f, 0.85f};

        float current = Config.getBackgroundOpacity();
        int selected = 1;
        for (int i = 0; i < values.length; i++)
        {
            if (Math.abs(current - values[i]) < 0.05f)
            {
                selected = i;
                break;
            }
        }

        new AlertDialog.Builder(activity)
            .setTitle(Utils.IsEnglish() ? "Wallpaper Opacity" : "Chọn độ đậm / mờ ảnh nền")
            .setSingleChoiceItems(options, selected, (dialog, which) ->
            {
                Config.setBackgroundOpacity(values[which]);
                CustomBackgroundHook.reloadBackground(context);
                try
                {
                    ListItemSettingHelper.SetSubtitle(settingItem, (int)(values[which] * 100) + "%" + (Utils.IsEnglish() ? " (tap to adjust)" : " (chạm để chỉnh)"));
                }
                catch (Exception ignored) {}
                Toast.makeText(context, Utils.IsEnglish() ? "Opacity updated: " + (int)(values[which] * 100) + "%" : "Đã cập nhật độ mờ: " + (int)(values[which] * 100) + "%", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            })
            .setNegativeButton(Utils.IsEnglish() ? "Cancel" : "Huỷ", null)
            .show();
    }

    private void resetCustomBackground(Context context)
    {
        File customFile = new File(context.getFilesDir(), "custom_app_bg.png");
        if (customFile.exists())
        {
            customFile.delete();
            CustomBackgroundHook.reloadBackground(context);
            Toast.makeText(context, Utils.IsEnglish() ? "Custom wallpaper removed!" : "Đã xoá ảnh nền tuỳ chỉnh!", Toast.LENGTH_SHORT).show();
        }
        else
        {
            Toast.makeText(context, Utils.IsEnglish() ? "No custom wallpaper set" : "Chưa cài ảnh nền tuỳ chỉnh nào", Toast.LENGTH_SHORT).show();
        }
    }

    private void restartApp(Context context)
    {
        try
        {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            if (intent == null)
                return;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(intent);
            if (Activity.class.isAssignableFrom(context.getClass()))
                ((Activity)context).finish();
            new Handler(Looper.getMainLooper()).postDelayed(() -> Runtime.getRuntime().exit(0), 0);
        }
        catch (Exception e)
        {
            Logger.e(e);
        }
    }

    @SuppressLint("PrivateApi")
    @SuppressWarnings("unchecked")
    private void hookTabMeView() throws Exception
    {
        List<MethodData> methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass("com.zing.zalo.ui.maintab.me.TabMeView")
                .modifiers(Modifier.STATIC)
                .returnType("java.util.ArrayList")
                .addUsingString("tab_me_privacy", StringMatchType.Equals)
                .addUsingString("tab_me_tool_storage", StringMatchType.Equals)
                .addUsingString("tab_me_account_and_security", StringMatchType.Equals)
                .addUsingString("tab_me_business_tools", StringMatchType.Equals)
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found 1");
            return;
        }
        Method method = methods.get(0).getMethodInstance(classLoader);
        Logger.i("Hooking: " + method);
        module.hook(method).setPriority(10).intercept(chain ->
        {
            Object result = chain.proceed();
            if (!(result instanceof ArrayList))
                return result;
            try
            {
                String moduleName = MorpheConstants.getModuleName();
                Object separatorItem = null;
                ArrayList<Object> items = (ArrayList<Object>)result;
                for (int i = items.size() - 1; i >= 0; i--)
                {
                    Object item = items.get(i);
                    if (!item.toString().contains("SettingData(id="))
                    {
                        separatorItem = Utils.Clone(item);
                        break;
                    }
                }
                Object customItem = TabMeSettingItemHelper.CreateNew(SettingItemID.PRIVACY, CUSTOM_DRAWABLE_ID, moduleName);
                TabMeSettingItemHelper.SetDescription(customItem, Utils.IsEnglish() ? moduleName + " settings" : "Cài đặt " + moduleName);
                TabMeSettingItemHelper.SetTracking(customItem, CUSTOM_ITEM_MARKER);
                items.add(2, customItem);
                items.add(3, separatorItem);
                return items;
            }
            catch (Throwable t)
            {
                Logger.e(t);
            }
            return result;
        });

        methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .modifiers(Modifier.PUBLIC)
                .paramCount(1)
                .returnType("void")
                .addUsingString("settingData", StringMatchType.Equals)
                .addUsingString("STR_SOURCE_START_VIEW", StringMatchType.Equals)
                .addUsingString("EXTRA_FEATURE_ID", StringMatchType.Equals)
                .addUsingString("features@business_account@zinstant@business_tool@enable", StringMatchType.Equals)
                .addUsingString("EXTRA_SOURCE_OPEN_MA", StringMatchType.Equals)
                .addUsingString("features@business_account@item_promotion@open_action@action_name", StringMatchType.Equals)
                .addUsingString("tab_me_open_item", StringMatchType.Equals)
                // .addUsingString("https://h5.zdn.vn/zapps/220259427665569271/", StringMatchType.Equals)
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target onClick method not found");
            return;
        }
        method = methods.get(0).getMethodInstance(classLoader);
        Logger.i("Hooking: " + method);
        module.hook(method).setPriority(10).intercept(chain ->
        {
            Object tabMeItem = chain.getArg(0);
            if (tabMeItem == null)
                return chain.proceed();
            try
            { 
                isOpenZaloXposedSettings1 = isOpenZaloXposedSettings2 = CUSTOM_ITEM_MARKER.equals(TabMeSettingItemHelper.GetTracking(tabMeItem));
            }
            catch (Throwable t)
            {
                Logger.e(t);
            }
            return chain.proceed();
        });
    }

    private void hookSettingPrivateView() throws Exception
    {
        headerTextViewClass = Class.forName("com.zing.zalo.ui.widget.RobotoTextView", false, classLoader);
        List<MethodData> methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass("com.zing.zalo.ui.settings.SettingPrivateV2View")
                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                .returnType("android.view.View")
                .paramCount(3)
                .paramTypes("android.view.LayoutInflater", "android.widget.LinearLayout", "android.os.Bundle")
                .addUsingString("inflater", StringMatchType.Equals)
                .addUsingString("EXTRA_CURRENT_SEEN_SETTING", StringMatchType.Equals)));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found 2");
            return;
        }
        Method method = methods.get(0).getMethodInstance(classLoader);
        Logger.i("Hooking: " + method);
        module.hook(method).setPriority(10).intercept(chain ->
        {
            Object result = chain.proceed();
            if (isOpenZaloXposedSettings1 || isOpenZaloXposedSettings2)
                rootLayout = (LinearLayout)chain.getArg(1);
            else 
                rootLayout = null;
            return result;
        });
        methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass("com.zing.zalo.ui.settings.SettingPrivateV2View")
                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                .returnType("void")
                .paramCount(0)
                .addUsingString("view_birthday", StringMatchType.Equals)
                .addUsingString("recently_online_status", StringMatchType.Equals)
                .addUsingString("display_seen_status", StringMatchType.Equals)
                .addUsingString("accept_stranger_call", StringMatchType.Equals)
                .addUsingString("allow_auto_friend_click", StringMatchType.Equals)
                .addUsingString("SETTING_PRIVACY_APP", StringMatchType.Equals)
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found 3");
            return;
        }
        method = methods.get(0).getMethodInstance(classLoader);
        Logger.i("Hooking: " + method);
        module.hook(method).setPriority(10).intercept(chain ->
        {
            Object result = chain.proceed();
            if (!isOpenZaloXposedSettings1)
                return result;
            if (rootLayout == null)
                return result;
            isOpenZaloXposedSettings1 = false; 
            templateHeader = null;
            templateSeparator = null;
            try
            {
                loadTemplates();
                createCustomMenu();
            }
            catch (Exception e)
            {
                Logger.e(e);
            }
            return result;
        });
        methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass("com.zing.zalo.ui.settings.SettingPrivateV2View")
                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                .returnType("void")
                .paramCount(0)
                .addUsingString("getString(...)", StringMatchType.Equals)
                // .addUsingField(FieldMatcher.create().name("str_title_setting_private"))
                .addInvoke(MethodMatcher.create()
                    .declaredClass("com.zing.zalo.zdesign.component.header.ZdsActionBar")
                    .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                    .returnType("void")
                    .name("setMiddleTitle")
                )
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found 4");
            return;
        }
        method = methods.get(0).getMethodInstance(classLoader);
        // Field actionBarField = Utils.FindFieldByType(Class.forName("com.zing.zalo.ui.settings.SettingPrivateV2View", false, classLoader), "com.zing.zalo.zdesign.component.header.ZdsActionBar");
        Field actionBarField = Utils.FindFieldByType(Class.forName("com.zing.zalo.ui.zviews.ZaloViewNewActionBar", false, classLoader), "com.zing.zalo.zdesign.component.header.ZdsActionBar");
        Logger.i("Hooking: " + method);
        module.hook(method).setPriority(10).intercept(chain ->
        {
            Object result = chain.proceed();
            if (!isOpenZaloXposedSettings2)
                return result;
            if (actionBarField == null)
                return result;
            isOpenZaloXposedSettings2 = false;
            try
            {
                Object actionBar = actionBarField.get(chain.getThisObject());
                if (actionBar == null)
                    return result;
                Method setMiddleTitle = actionBar.getClass().getMethod("setMiddleTitle", String.class);
                String moduleName = MorpheConstants.getModuleName();
                setMiddleTitle.invoke(actionBar, Utils.IsEnglish() ? moduleName + " Settings" : "Cài đặt " + moduleName);
            }
            catch (Exception t)
            {
                Logger.e(t);
            }
            return result;
        });
    }

    private static String getFakeRoleName(int roleLevel)
    {
        return switch (roleLevel)
        {
            case 0 -> Utils.IsEnglish() ? "Admin" : "Phó nhóm";
            case 1 -> Utils.IsEnglish() ? "Owner" : "Trưởng nhóm";
            default -> "";
        };
    }

    private TextView createHeaderTitle(Context context) throws Exception
    {
        TextView headerTitle = (TextView)headerTextViewClass.getConstructor(Context.class).newInstance(context);
        headerTitle.setVisibility(View.VISIBLE);
        if (templateHeader != null)
        {
            headerTitle.setTextColor(templateHeader.getTextColors());
            headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, templateHeader.getTextSize());
            headerTitle.setTypeface(templateHeader.getTypeface());
            headerTitle.setPadding(templateHeader.getPaddingLeft(), templateHeader.getPaddingTop(), templateHeader.getPaddingRight(), templateHeader.getPaddingBottom());
            try
            {
                headerTitle.setBackground(Objects.requireNonNull(templateHeader.getBackground()
                    .getConstantState())
                    .newDrawable()
                    .mutate()
                );
            }
            catch (Exception ignored)
            {
                headerTitle.setBackgroundColor(Color.TRANSPARENT);
            }
            if (templateHeader.getLayoutParams() instanceof LinearLayout.LayoutParams)
            {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((LinearLayout.LayoutParams)templateHeader.getLayoutParams());
                headerTitle.setLayoutParams(lp);
            }
        }
        else
        {
            headerTitle.setTextColor(0xFF808080);
            headerTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            headerTitle.setBackgroundColor(Color.TRANSPARENT);
            int pad16 = dp(context, 16);
            int pad8 = dp(context, 8);
            headerTitle.setPadding(pad16, pad16, pad16, pad8);
        }
        return headerTitle;
    }

    private View createSeparator(Context context)
    {
        View separator = new View(context);
        if (templateSeparator != null)
        {
            try
            {
                separator.setBackground(Objects.requireNonNull(templateSeparator.getBackground()
                    .getConstantState())
                    .newDrawable()
                    .mutate()
                );
            }
            catch (Exception ignored) { }
            if (templateSeparator.getLayoutParams() instanceof LinearLayout.LayoutParams)
            {
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams((LinearLayout.LayoutParams)templateSeparator.getLayoutParams());
                separator.setLayoutParams(lp);
            }
        }
        else
        {
            separator.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 8));
            separator.setLayoutParams(lp);
        }
        return separator;
    }

    private void loadTemplates()
    {
        // Find templates, then hide all existing children
        int count = rootLayout.getChildCount();
        for (int i = 0; i < count; i++)
        {
            View child = rootLayout.getChildAt(i);
            if (child.getVisibility() != View.VISIBLE)
                continue;
            if (templateHeader == null && child.getClass() == headerTextViewClass)
                templateHeader = (TextView)child;
            else if (templateSeparator == null && child.getClass() == View.class)
                templateSeparator = child;
        }
        for (int i = 0; i < count; i++)
        {
            View child = rootLayout.getChildAt(i);
            child.setVisibility(View.GONE);
        }
    }

    private int dp(Context context, int value)
    {
        return (int)(value * context.getResources().getDisplayMetrics().density);
    }

    private void hideKeyboard(View view)
    {
        InputMethodManager imm = (InputMethodManager)view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null)
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void styleEditText(EditText editText, Context context, RelativeLayout templateItem)
    {
        int pad16 = dp(context, 16);
        int pad12 = dp(context, 12);
        editText.setPadding(pad16, pad12, pad16, pad12);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        try
        {
            TextView titleView = findTitleTextView(templateItem);
            if (titleView != null)
            {
                editText.setTextColor(titleView.getTextColors());
                editText.setHintTextColor(titleView.getTextColors());
            }
            editText.setBackground(Objects.requireNonNull(templateItem.getBackground().getConstantState())
                .newDrawable()
                .mutate());
        }
        catch (Exception ignored) { }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        editText.setLayoutParams(lp);
    }

    private TextView findTitleTextView(View view)
    {
        if (view instanceof TextView)
            return (TextView)view;
        if (view instanceof ViewGroup group)
        {
            for (int i = 0; i < group.getChildCount(); i++)
            {
                TextView found = findTitleTextView(group.getChildAt(i));
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    private static String getFontDisplayName(String style)
    {
        if (style == null) return "Tắt (Gửi bình thường)";
        switch (style)
        {
            case FontHelper.STYLE_BOLD: return "In đậm (Bold) - 𝐗𝐢𝐧 𝐜𝐡𝐚̀𝐨";
            case FontHelper.STYLE_ITALIC: return "In nghiêng (Italic) - 𝑋𝑖𝑛 𝑐ℎ𝑎̀𝑜";
            case FontHelper.STYLE_BOLD_ITALIC: return "Đậm & Nghiêng - 𝑿𝒊𝒏 𝒄𝒉𝒂̀𝒐";
            case FontHelper.STYLE_SCRIPT: return "Viết tay (Script) - 𝓧𝓲𝓷 𝓬𝓱𝓪̀𝓸";
            case FontHelper.STYLE_GOTHIC: return "Cổ điển (Gothic) - 𝖃𝖎𝖓 𝖈𝖍𝖆̀𝖔";
            case FontHelper.STYLE_MONO: return "Máy đánh chữ - 𝚇𝚒𝚗 𝚌𝚑𝚊̀𝚘";
            case FontHelper.STYLE_BUBBLE: return "Vòng tròn (Bubble) - Ⓧⓘⓝ ⓒⓗⓐ̀ⓞ";
            case FontHelper.STYLE_STRIKE: return "Gạch ngang - X̶i̶n̶ ̶c̶h̶à̶o̶";
            case FontHelper.STYLE_UNDERLINE: return "Gạch chân - X̲i̲n̲ ̲c̲h̲à̲o̲";
            default: return "Tắt (Gửi bình thường)";
        }
    }

    private void showFontSelectionDialog(Context context, RelativeLayout fontItem)
    {
        final String[] styles = {
            FontHelper.STYLE_NONE,
            FontHelper.STYLE_BOLD,
            FontHelper.STYLE_ITALIC,
            FontHelper.STYLE_BOLD_ITALIC,
            FontHelper.STYLE_SCRIPT,
            FontHelper.STYLE_GOTHIC,
            FontHelper.STYLE_MONO,
            FontHelper.STYLE_BUBBLE,
            FontHelper.STYLE_STRIKE,
            FontHelper.STYLE_UNDERLINE
        };

        final String[] names = {
            "Tắt (Mặc định - Gửi bình thường)",
            "In đậm (Bold) - 𝐗𝐢𝐧 𝐜𝐡𝐚̀𝐨",
            "In nghiêng (Italic) - 𝑋𝑖𝑛 𝑐ℎ𝑎̀𝑜",
            "Đậm & Nghiêng - 𝑿𝒊𝒏 𝒄𝒉𝒂̀𝒐",
            "Viết tay nghệ thuật (Script) - 𝓧𝓲𝓷 𝓬𝓱𝓪̀𝓸",
            "Chữ cổ điển (Gothic) - 𝖃𝖎𝖓 𝖈𝖍𝖆̀𝖔",
            "Máy đánh chữ (Monospace) - 𝚇𝚒𝚗 𝚌𝚑𝚊̀𝚘",
            "Vòng tròn (Bubble) - Ⓧⓘⓝ ⓒⓗⓐ̀ⓞ",
            "Gạch ngang (Strikethrough) - X̶i̶n̶ ̶c̶h̶à̶o̶",
            "Gạch chân (Underline) - X̲i̲n̲ ̲c̲h̲à̲o̲"
        };

        String currentStyle = Config.getDefaultFontStyle();
        int selectedIndex = 0;
        for (int i = 0; i < styles.length; i++)
        {
            if (styles[i].equalsIgnoreCase(currentStyle))
            {
                selectedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("🔤 Chọn kiểu font chữ mặc định");
        builder.setSingleChoiceItems(names, selectedIndex, (dialog, which) ->
        {
            String chosen = styles[which];
            Config.setDefaultFontStyle(chosen);
            try
            {
                ListItemSettingHelper.SetSubtitle(fontItem, getFontDisplayName(chosen) + " (chạm để chọn)");
            }
            catch (Exception ignored) {}
            Toast.makeText(context, "Đã chọn: " + names[which], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        builder.setNegativeButton("Đóng", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showSyntaxHelpDialog(Context context)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("✨ Tính năng Troll & Menu Kiểu Chữ");

        String msg = "🎨 MENU CHỮ & BẢNG MÀU (KHÔNG CẦN NHỚ LỆNH):\n" +
                     "• Khi gõ chữ, chạm vào nút [Aa] ngay cạnh nút Gửi.\n" +
                     "• Hoặc Chạm 2 lần (Double-tap) vào ô gõ chữ để mở Menu trực tiếp.\n" +
                     "• Hỗ trợ 12 theme màu rực rỡ và 13 kiểu font nghệ thuật.\n\n" +
                     "1. TROLL TIN NHẮN REPLY (SỬA TRÍCH DẪN):\n" +
                     "• Cách 1: Khi reply tin nhắn, chạm vào thanh trích dẫn ở trên ô gõ chữ để sửa trực tiếp câu của người khác.\n" +
                     "• Cách 2: Mở menu [Aa] -> Bấm 'Troll Reply'.\n\n" +
                     "2. SỬA TIN NHẮN TẠI CHỖ (CHỤP ẢNH MÀN HÌNH):\n" +
                     "• Chạm 3 lần liên tiếp (Triple-tap) vào bất kỳ tin nhắn nào trong cuộc trò chuyện.\n" +
                     "• Chọn 10 màu chữ khác nhau và sửa nội dung để chụp ảnh sống ảo.\n\n" +
                     "3. LỆNH NHANH (NẾU MUỐN DÙNG GÕ TẮT):\n" +
                     "• /b <chữ>: In đậm | /i <chữ>: In nghiêng | /c <chữ>: Viết tay\n" +
                     "• /g <chữ>: Gothic | /m <chữ>: Máy đánh chữ | /o <chữ>: Bóng";

        builder.setMessage(msg);
        builder.setPositiveButton("Đã hiểu", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}