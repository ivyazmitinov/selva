package org.ivanvyazmitinov;

import com.google.common.io.Resources;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy;
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope;
import jakarta.inject.Inject;
import org.jooq.DSLContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.FILE;
import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.USER;

@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName.class)
@MicronautTest
class SelvaUserAccountManagementTest {

    @Inject
    EmbeddedServer server;
    @Inject
    DSLContext dsl;
    @Inject
    PlaywrightBrowserLauncher browserLauncher;

    /**
     * Test of user account management
     * <ol>
     *     <li>Registration of a new user</li>
     *     <li>Fill the base profile</li>
     *     <li>Delete account</li>
     * </ol>
     *
     */
    @Test
    void testUserAccountManagement() throws URISyntaxException {
        try (var playwright = Playwright.create()) {
            var context = browserLauncher.launch(playwright);
            var baseUrl = "http://127.0.0.1:%s/".formatted(server.getPort());
            Page page = context.newPage();
            page.navigate(baseUrl);

            // Register as a new user
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("create an account")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Repeat password:")).fill("test");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            // Fill the base profile
            PlaywrightAssertions.assertThat(page).hasURL(baseUrl + "app/base-profile/1");
            var addNewFieldControl = page.locator("#add-new-field");
            for (int i = 0; i < 3; i++) {
                addNewFieldControl.click();
            }
            page.getByTestId("new-field-0-value").fill("Test Name");
            page.getByTestId("new-field-1-value").fill("Test Surname");
            page.getByTestId("new-field-2-label").fill("Description");
            page.getByTestId("new-field-2-value").fill("Test Bio");
            page.getByTestId("new-field-3-type").selectOption("date");
            page.getByTestId("new-field-3-label").fill("Birthday");
            page.getByTestId("new-field-3-value").fill("2025-09-10");
            page.getByTestId("new-field-4-type").selectOption("file");
            page.getByTestId("new-field-4-label").fill("Some File");
            page.getByTestId("new-field-4-value").setInputFiles(
                    Paths.get(Resources.getResource("assets/AWD-CW2-v2.pdf").toURI()));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Let's go!")).click();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Account")).click();

            // Verify state after creating
            var fileUploaded = dsl.fetchExists(FILE, FILE.FILE_NAME.eq("AWD-CW2-v2.pdf"));
            Assertions.assertTrue(fileUploaded);

            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-label")).hasValue("Name:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-value")).hasValue("Test Name");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-label")).hasValue("Surname:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-value")).hasValue("Test Surname");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-label")).hasValue("Description:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-value")).hasValue("Test Bio");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-3-label")).hasValue("Birthday:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-3-value")).hasValue("2025-09-10");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-4-label")).hasValue("Some File:");

            // Modify the profile
            page.getByTestId("new-field-0-value").fill("Test Name New");
            page.getByTestId("new-field-1-value").fill("Test Surname New");
            page.getByTestId("new-field-3-remove-button").click();
            page.getByTestId("new-field-4-remove-button").click();
            PlaywrightAssertions.assertThat(page.locator("#editable-fields > .fields-container")).hasCount(3);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            // Verify changes are applied
            PlaywrightAssertions.assertThat(page.locator("#editable-fields > .fields-container")).hasCount(3);
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-label")).hasValue("Name:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-value")).hasValue("Test Name New");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-label")).hasValue("Surname:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-value")).hasValue("Test Surname New");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-label")).hasValue("Description:");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-value")).hasValue("Test Bio");

            // Delete the account
            var confirmationWasShown = new AtomicBoolean(false);
            page.onceDialog(dialog -> {
                confirmationWasShown.set(true);
                Assertions.assertEquals("confirm", dialog.type());
                dialog.accept();
            });
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete account")).click();

            // Verify user deleted
            Assertions.assertTrue(confirmationWasShown.get());
            var userDeleted = !dsl.fetchExists(USER, USER.USERNAME.eq("test"));
            Assertions.assertTrue(userDeleted);

            // Verify can't log in
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:")).fill("test");
            var loginResponse = page.waitForResponse("**/auth/login", () ->
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click());
            Assertions.assertEquals(303, loginResponse.status());
        }
    }
}
