package mozilla.lockbox.screenshots;


import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import kotlin.jvm.JvmField;
import mozilla.lockbox.view.RootActivity;


import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import tools.fastlane.screengrab.locale.LocaleTestRule;


@RunWith(AndroidJUnit4.class)
public class ScreengrabTest {
    @ClassRule public static final LocaleTestRule localeTestRule = new LocaleTestRule();

    @Rule @JvmField
    public ActivityTestRule<RootActivity> activityRule = new ActivityTestRule<>(RootActivity.class);

    @Test
    public void testTakeScreenshot() {
        //Workaround to so that the screenshot test in ScreenshotsTest suite works
    }
}
