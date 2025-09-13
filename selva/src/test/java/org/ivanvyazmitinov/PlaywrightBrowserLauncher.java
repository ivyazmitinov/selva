package org.ivanvyazmitinov;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.inject.Singleton;

@Singleton
public class PlaywrightBrowserLauncher {

    public BrowserContext launch(Playwright playwright) {
        return playwright.chromium().launch(new BrowserType.LaunchOptions()
//                        .setHeadless(false)
//                        .setSlowMo(200)
                )
                .newContext(new Browser.NewContextOptions().setViewportSize(1280, 720));
    }
}
