// Copyright 2019 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.browser.examples.twawebviewfallback;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import com.google.androidbrowserhelper.trusted.LauncherActivityMetadata;

public class WebViewFallbackActivity extends AppCompatActivity {
    private static final String KEY_PREFIX =
            "com.google.browser.examples.twawebviewfallback.WebViewFallbackActivity.";
    private static final String KEY_LAUNCH_URI = KEY_PREFIX + "LAUNCH_URL";
    private static final String KEY_NAVIGATION_BAR_COLOR = KEY_PREFIX + "KEY_NAVIGATION_BAR_COLOR";
    private static final String KEY_STATUS_BAR_COLOR = KEY_PREFIX + "KEY_STATUS_BAR_COLOR";

    private Uri mLaunchUrl;
    private int mStatusBarColor;

    public static Intent createLaunchIntent(
            Context context,
            Uri launchUrl,
            LauncherActivityMetadata launcherActivityMetadata) {
        Intent intent = new Intent(context, WebViewFallbackActivity.class);
        intent.putExtra(WebViewFallbackActivity.KEY_LAUNCH_URI, launchUrl);

        intent.putExtra(WebViewFallbackActivity.KEY_STATUS_BAR_COLOR,
                ContextCompat.getColor(context, launcherActivityMetadata.statusBarColorId));
        intent.putExtra(WebViewFallbackActivity.KEY_NAVIGATION_BAR_COLOR,
                ContextCompat.getColor(context, launcherActivityMetadata.navigationBarColorId));
        return intent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.mLaunchUrl = this.getIntent().getParcelableExtra(KEY_LAUNCH_URI);

        if (getIntent().hasExtra(KEY_NAVIGATION_BAR_COLOR)) {
            int navigationBarColor = this.getIntent().getIntExtra(
                    KEY_NAVIGATION_BAR_COLOR, 0);
            getWindow().setNavigationBarColor(navigationBarColor);
        }

        if (getIntent().hasExtra(KEY_STATUS_BAR_COLOR)) {
            mStatusBarColor = this.getIntent().getIntExtra(KEY_STATUS_BAR_COLOR, 0);
            getWindow().setStatusBarColor(mStatusBarColor);
        } else {
            mStatusBarColor = getWindow().getStatusBarColor();
        }

        WebView webView = new WebView(this);
        webView.setWebViewClient(createWebViewClient());

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);


        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        setContentView(webView,layoutParams);
        webView.loadUrl(mLaunchUrl.toString());
    }

    private WebViewClient createWebViewClient() {
        return new WebViewClient() {
            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public boolean onRenderProcessGone(
                    WebView view, RenderProcessGoneDetail detail) {
                ViewGroup vg = (ViewGroup)view.getParent();

                // Remove crashed WebView from the hierarchy
                // and ensure it is destroyed.
                vg.removeView(view);
                view.destroy();

                // Create a new instance, and ensure it also
                // handles crashes - in this case, re-using
                // the current WebViewClient
                WebView webView = new WebView(view.getContext());
                webView.setWebViewClient(this);
                WebSettings webSettings = webView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                vg.addView(webView);

                // With the crash recovered, decide what to do next.
                // We are sending a toast and loading the origin
                // URL, in this example.
                Toast.makeText(view.getContext(), "Recovering from crash",
                        Toast.LENGTH_LONG).show();
                webView.loadUrl(mLaunchUrl.toString());
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri launchUrl = WebViewFallbackActivity.this.mLaunchUrl;
                Uri navigationUrl = request.getUrl();

                // If the user is navigation to a different origin, use CCT to handle the navigation
                if (!launchUrl.getScheme().equals(navigationUrl.getScheme())
                    || !launchUrl.getHost().equals(navigationUrl.getHost())) {
                    CustomTabsIntent intent  = new CustomTabsIntent.Builder()
                            .setToolbarColor(mStatusBarColor)
                            .build();
                    intent.launchUrl(WebViewFallbackActivity.this, navigationUrl);
                    return true;
                }

                return super.shouldOverrideUrlLoading(view, request);
            }
        };
    }
}
