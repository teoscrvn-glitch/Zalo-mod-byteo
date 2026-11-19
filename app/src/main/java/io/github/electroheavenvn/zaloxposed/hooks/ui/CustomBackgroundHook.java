package io.github.electroheavenvn.zaloxposed.hooks.ui;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.github.electroheavenvn.zaloxposed.hooks.BaseHook;
import io.github.electroheavenvn.zaloxposed.utilities.Config;
import io.github.electroheavenvn.zaloxposed.utilities.Logger;

import org.luckypray.dexkit.query.FindMethod;
import org.luckypray.dexkit.query.enums.StringMatchType;
import org.luckypray.dexkit.query.matchers.MethodMatcher;
import org.luckypray.dexkit.result.MethodData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Random;

public class CustomBackgroundHook extends BaseHook
{
    private static final String BG_TAG = "zalo_bg_msg_root";
    private static final Random random = new Random();
    private static AssetManager sAssetManager;
    public static WeakReference<ImageView> sBgView = new WeakReference<>(null);

    public static boolean hasCustomBackground(Context context)
    {
        if (context == null) return false;
        File file = new File(context.getFilesDir(), "custom_app_bg.png");
        return file.exists() && file.length() > 0;
    }

    public static boolean saveCustomBackground(Context context, Uri uri)
    {
        if (context == null || uri == null) return false;
        try
        {
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            int maxW = dm.widthPixels;
            int maxH = dm.heightPixels;

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(uri))
            {
                if (is == null) return false;
                BitmapFactory.decodeStream(is, null, options);
            }

            int sampleSize = 1;
            while (options.outWidth / (sampleSize * 2) >= maxW && options.outHeight / (sampleSize * 2) >= maxH)
            {
                sampleSize *= 2;
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            Bitmap bitmap;
            try (InputStream is = context.getContentResolver().openInputStream(uri))
            {
                if (is == null) return false;
                bitmap = BitmapFactory.decodeStream(is, null, options);
            }

            if (bitmap == null) return false;

            File bgFile = new File(context.getFilesDir(), "custom_app_bg.png");
            try (FileOutputStream fos = new FileOutputStream(bgFile))
            {
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
            }
            return true;
        }
        catch (Throwable t)
        {
            Logger.e("saveCustomBackground failed", t);
            return false;
        }
    }

    public static Bitmap loadBg(Context context)
    {
        // 1. Try custom user image first
        if (context != null)
        {
            File customFile = new File(context.getFilesDir(), "custom_app_bg.png");
            if (customFile.exists() && customFile.length() > 0)
            {
                try
                {
                    return BitmapFactory.decodeFile(customFile.getAbsolutePath());
                }
                catch (Throwable t)
                {
                    Logger.e("Error loading custom background file", t);
                }
            }
        }

        // 2. Fallback to bundled wallpapers if any
        if (sAssetManager != null)
        {
            try
            {
                String[] bgImgs = sAssetManager.list("bg");
                if (bgImgs != null && bgImgs.length > 0)
                {
                    String bgImg = bgImgs[random.nextInt(bgImgs.length)];
                    try (InputStream stream = sAssetManager.open("bg/" + bgImg))
                    {
                        return BitmapFactory.decodeStream(stream);
                    }
                }
            }
            catch (Throwable t)
            {
                Logger.e("Error loading bundled background", t);
            }
        }
        return null;
    }

    public static void reloadBackground(Context context)
    {
        new Handler(Looper.getMainLooper()).post(() ->
        {
            ImageView bgView = sBgView.get();
            if (bgView == null) return;
            if (!Config.getEnableBackground())
            {
                bgView.setVisibility(View.GONE);
                return;
            }
            Bitmap bg = loadBg(context != null ? context : bgView.getContext());
            if (bg != null)
            {
                bgView.setImageDrawable(new BitmapDrawable(bgView.getContext().getResources(), bg));
                bgView.setAlpha(Config.getBackgroundOpacity());
                bgView.setVisibility(View.VISIBLE);
            }
            else
            {
                bgView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void hook() throws Throwable
    {
        sAssetManager = this.assetManager;

        Class<?> msgViewCls = Class.forName("com.zing.zalo.ui.maintab.MainTabView", false, classLoader);
        List<MethodData> methods = bridge.findMethod(FindMethod.create()
            .matcher(MethodMatcher.create()
                .declaredClass(msgViewCls)
                .modifiers(Modifier.PUBLIC | Modifier.FINAL)
                .returnType("android.view.View")
                .paramCount(3)
                .paramTypes("android.view.LayoutInflater", "android.view.ViewGroup", "android.os.Bundle")
                .addUsingString("onCreateView", StringMatchType.Equals)
            ));
        if (methods.isEmpty())
        {
            Logger.e("Target method not found");
            return;
        }
        for (MethodData method : methods)
        {
            Method m = method.getMethodInstance(classLoader);
            Logger.i("Hooking: " + m);
            module.hook(m).intercept(chain ->
            {
                Object result = chain.proceed();
                if (!(result instanceof FrameLayout root))
                {
                    Logger.e("Not a FrameLayout: " + result);
                    return result;
                }

                View existing = root.findViewWithTag(BG_TAG);
                if (existing instanceof ImageView existingBg)
                {
                    sBgView = new WeakReference<>(existingBg);
                    reloadBackground(root.getContext());
                    return result;
                }

                if (!Config.getEnableBackground())
                    return result;

                Bitmap bg = loadBg(root.getContext());
                if (bg == null)
                {
                    Logger.e("Failed to load background");
                    return result;
                }

                ImageView bgView = new ImageView(root.getContext());
                bgView.setTag(BG_TAG);
                bgView.setImageDrawable(new BitmapDrawable(root.getContext().getResources(), bg));
                bgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                bgView.setAlpha(Config.getBackgroundOpacity());
                bgView.setClickable(false);
                bgView.setFocusable(false);
                sBgView = new WeakReference<>(bgView);
                root.addView(bgView, 1, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return result;
            });
        }
    }
}
