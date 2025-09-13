package org.ivanvyazmitinov;

import com.google.common.io.Resources;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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

import static org.ivanvyazmitinov.selva.repository.jooq.generated.selva.Tables.EXTERNAL_INTEGRATION;

@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName.class)
@MicronautTest
class SelvaIntegrationManagementTest {

    @Inject
    EmbeddedServer server;
    @Inject
    DSLContext dsl;
    @Inject
    PlaywrightBrowserLauncher browserLauncher;

    /**
     * Test case that checks
     *
     * <ol>
     *     <li>Create an integration</li>
     *     <li>Modify the integration</li>
     *     <li>Generate new token for the integration</li>
     *     <li>Delete integration</li>
     * </ol>
     */
    @Test
    void testIntergrationManagement() throws URISyntaxException, InterruptedException {
        try (var playwright = Playwright.create()) {
            var context = browserLauncher.launch(playwright);
            var baseUrl = "http://127.0.0.1:%s/".formatted(server.getPort());
            Page page = context.newPage();
            page.navigate(baseUrl);

            // Fill login form with admin credentials
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("admin");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:")).fill("admin");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            // Fill integration data
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add new")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Integration Name:")).fill("LinkedIn");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Integration Logo:"))
                    .setInputFiles(Paths.get(Resources.getResource("assets/LinkedIn_logo.webp").toURI()));
            var addNewFieldControl = page.locator("#add-new-field");
            addNewFieldControl.click();
            page.getByTestId("new-field-0-label").fill("Name");
            addNewFieldControl.click();
            page.getByTestId("new-field-1-label").fill("Birthday");
            page.getByTestId("new-field-1-type").selectOption("date");
            addNewFieldControl.click();
            page.getByTestId("new-field-2-label").fill("Some File");
            page.getByTestId("new-field-2-type").selectOption("file");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            // Assert result of the integration creation
            PlaywrightAssertions.assertThat(page.locator("#fieldset")).containsText("Your API key.");
            var tokenInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your API key."));
            PlaywrightAssertions.assertThat(tokenInput).isVisible();
            var generateNewTokenButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Generate new token"));
            PlaywrightAssertions.assertThat(generateNewTokenButton).isVisible();
            var deleteIntegrationButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete integration"));
            PlaywrightAssertions.assertThat(deleteIntegrationButton).isVisible();

            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-label")).hasValue("Name");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-type")).hasValue("text");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-label")).hasValue("Birthday");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-type")).hasValue("date");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-label")).hasValue("Some File");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-2-type")).hasValue("file");

            // Test new token generation
            var currentToken = tokenInput.inputValue();
            // Verify token format
            var tokenSplit = currentToken.split("__");
            Integer.parseInt(tokenSplit[0]);
            generateNewTokenButton.click();
            tokenInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your API key."));
            var newToken = tokenInput.inputValue();
            tokenSplit = newToken.split("__");
            Integer.parseInt(tokenSplit[0]);
            Assertions.assertNotEquals(currentToken, newToken);

            // Modify integration data
            page.getByTestId("new-field-0-type").selectOption("date");
            page.getByTestId("new-field-0-label").fill("Name Now Date");
            page.getByTestId("new-field-1-type").selectOption("text");
            page.getByTestId("new-field-1-label").fill("Birthday Now Text");
            page.getByTestId("new-field-2-remove-button").click();
            PlaywrightAssertions.assertThat(page.locator("#editable-fields > .fields-container")).hasCount(2);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            // Verify that changes have applied correctly
            PlaywrightAssertions.assertThat(page.locator("#editable-fields > .fields-container")).hasCount(2);
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-type")).hasValue("date");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-0-label")).hasValue("Name Now Date");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-type")).hasValue("text");
            PlaywrightAssertions.assertThat(page.getByTestId("new-field-1-label")).hasValue("Birthday Now Text");

            // Delete integration
            var confirmationWasShown = new AtomicBoolean(false);
            page.onceDialog(dialog -> {
                confirmationWasShown.set(true);
                Assertions.assertEquals("confirm", dialog.type());
                dialog.accept();
            });
            deleteIntegrationButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete integration"));
            deleteIntegrationButton.click();

            // Verify integration deletion
            Assertions.assertTrue(confirmationWasShown.get());
            var integrationsCount = dsl.selectCount()
                    .from(EXTERNAL_INTEGRATION)
                    .fetchOneInto(Integer.class);
            Assertions.assertEquals(0, integrationsCount);
        }
    }
}
