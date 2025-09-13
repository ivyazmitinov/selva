package org.ivanvyazmitinov;

import com.google.common.io.Resources;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.extensions.junit5.annotation.ScopeNamingStrategy;
import io.micronaut.test.extensions.junit5.annotation.TestResourcesScope;
import jakarta.inject.Inject;
import org.ivanvyazmitinov.selva.model.ExternalApiUserProfiles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;

@TestResourcesScope(namingStrategy = ScopeNamingStrategy.TestClassName.class)
@MicronautTest
class SelvaMainUseCaseScenarioTest {

    @Inject
    EmbeddedServer server;
    @Inject
    @Client("/")
    HttpClient httpClient;
    @Inject
    PlaywrightBrowserLauncher browserLauncher;

    /**
     * Test that covers the basic scenario of system usage
     *
     * <ol>
     *     <li>Admin creates two integrations, with all field types and some logically similar fields</li>
     *     <li>Two users are registering on the platform, filling the base profile</li>
     *     <li>One user fills external profiles for both integrations, both with some fields linked to the base profile</li>
     *     <li>An integration try to fetch user profile</li>
     *     <li>Users makes changes in the base and external profiles, including changes in visibility</li>
     *     <li>The integration tries to fetch extended user profile</li>
     *     <li>The second user tries to access objects of another user, by direct links, and fails to do so</li>
     * </ol>
     */
    @Test
    public void testMainUseScenario() throws URISyntaxException, InterruptedException, IOException {
        try (var playwright = Playwright.create()) {
            var context = browserLauncher.launch(playwright);
            var baseUrl = "http://127.0.0.1:%s/".formatted(server.getPort());
            Page page = context.newPage();
            page.navigate(baseUrl);

            // Fill login form with admin credentials
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("admin");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:")).fill("admin");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            // Fill LinkedIn data
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add new")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Integration Name:")).fill("LinkedIn");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Integration Logo:"))
                    .setInputFiles(Paths.get(Resources.getResource("assets/LinkedIn_logo.webp").toURI()));
            var addNewFieldControl = page.locator("#add-new-field");
            for (int i = 0; i < 6; i++) {
                addNewFieldControl.click();
            }
            page.getByTestId("new-field-0-label").fill("Name");
            page.getByTestId("new-field-1-label").fill("Surname");
            page.getByTestId("new-field-2-label").fill("Birthday");
            page.getByTestId("new-field-3-label").fill("Company name");
            page.getByTestId("new-field-4-label").fill("Company work start date");
            page.getByTestId("new-field-5-label").fill("Additional documents");
            page.getByTestId("new-field-2-type").selectOption("date");
            page.getByTestId("new-field-4-type").selectOption("date");
            page.getByTestId("new-field-5-type").selectOption("file");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            var tokenInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your API key."));
            var linkedinToken = tokenInput.inputValue();

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")).click();

            // Fill Facebook data
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Add new")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Integration Name:")).fill("Facebook");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Integration Logo:"))
                    .setInputFiles(Paths.get(Resources.getResource("assets/Facebook_logo.png").toURI()));
            addNewFieldControl = page.locator("#add-new-field");
            for (int i = 0; i < 4; i++) {
                addNewFieldControl.click();
            }
            page.getByTestId("new-field-0-label").fill("Name");
            page.getByTestId("new-field-1-label").fill("Surname");
            page.getByTestId("new-field-2-label").fill("Birthday");
            page.getByTestId("new-field-3-label").fill("Avatar");
            page.getByTestId("new-field-2-type").selectOption("date");
            page.getByTestId("new-field-3-type").selectOption("file");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            tokenInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Your API key."));
            var facebookToken = tokenInput.inputValue();

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

            // Register first user
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("create an account")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test1");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test1");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Repeat password:")).fill("test1");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test1");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test1");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            // Fill the base profile
            addNewFieldControl = page.locator("#add-new-field");
            for (int i = 0; i < 2; i++) {
                addNewFieldControl.click();
            }
            page.getByTestId("new-field-0-value").fill("Test 1 Name");
            page.getByTestId("new-field-1-value").fill("Test 1 Surname");
            page.getByTestId("new-field-2-label").fill("Birthday");
            page.getByTestId("new-field-2-value").fill("1980-01-01");
            page.getByTestId("new-field-2-type").selectOption("date");
            page.getByTestId("new-field-3-type").selectOption("file");
            page.getByTestId("new-field-3-label").fill("Avatar");
            var avatarPath = Resources.getResource("assets/avatar.jpg");
            page.getByTestId("new-field-3-value").setInputFiles(
                    Paths.get(avatarPath.toURI()));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Let's go!")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

            // Register second user
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("create an account")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test2");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test2");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Repeat password:")).fill("test2");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test2");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test2");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();


            page.getByTestId("new-field-0-value").fill("Test 2 Name");
            page.getByTestId("new-field-1-value").fill("Test 2 Surname");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Let's go!")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

            // Fill external profiles by the first user
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test1");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test1");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).first().click();
            page.getByTestId("field-0-switch-to-reference").click();
            page.getByTestId("field-0-reference").selectOption("\uD83D\uDD17 to \"Name\" (Test 1 Name)");
            page.getByTestId("field-1-switch-to-reference").click();
            page.getByTestId("field-1-reference").selectOption("\uD83D\uDD17 to \"Surname\" (Test 1 Surname)");
            page.getByTestId("field-2-switch-to-reference").click();
            page.getByTestId("field-2-reference").selectOption("\uD83D\uDD17 to \"Birthday\" (1980-01-01)");
            page.getByTestId("field-3-value").fill("Google");
            page.getByTestId("field-4-value").fill("2025-09-10");
            var documentPath = Resources.getResource("assets/AWD-CW2-v2.pdf");
            page.getByTestId("field-5-value").setInputFiles(Paths.get(documentPath.toURI()));
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create")).first().click();
            page.getByTestId("field-0-value").fill("MeGaTestPowerUser2025");
            page.getByTestId("field-1-switch-to-reference").click();
            page.getByTestId("field-1-reference").selectOption("\uD83D\uDD17 to \"Surname\" (Test 1 Surname)");
            page.getByTestId("field-2-switch-to-reference").click();
            page.getByTestId("field-2-reference").selectOption("\uD83D\uDD17 to \"Birthday\" (1980-01-01)");
            page.getByTestId("field-3-switch-to-reference").click();
            page.getByTestId("field-3-reference").selectOption("\uD83D\uDD17 to \"Avatar\" (avatar.jpg)");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            // Verify data available for integrations
            var firstUserProfilesForLinkedin = httpClient.toBlocking().retrieve(HttpRequest.GET("/api/user-profile/" + 2)
                            .bearerAuth(linkedinToken)
                    , ExternalApiUserProfiles.class);
            Assertions.assertNotNull(firstUserProfilesForLinkedin.current());
            Assertions.assertEquals(0, firstUserProfilesForLinkedin.other().size());
            Assertions.assertEquals("LinkedIn", firstUserProfilesForLinkedin.current().integrationName());
            Assertions.assertEquals(6, firstUserProfilesForLinkedin.current().fields().size());
            Assertions.assertEquals("Test 1 Name", firstUserProfilesForLinkedin.current().fields().get("Name").value());
            Assertions.assertEquals("Test 1 Surname", firstUserProfilesForLinkedin.current().fields().get("Surname").value());
            Assertions.assertEquals("Google", firstUserProfilesForLinkedin.current().fields().get("Company name").value());
            Assertions.assertEquals("2025-09-10", firstUserProfilesForLinkedin.current().fields().get("Company work start date").value());
            Assertions.assertEquals("1980-01-01", firstUserProfilesForLinkedin.current().fields().get("Birthday").value());
            var documentsUrl = String.valueOf(firstUserProfilesForLinkedin.current().fields()
                    .get("Additional documents").value());
            var additionalDocumentsResponse = httpClient.toBlocking().exchange(HttpRequest.GET(documentsUrl)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .bearerAuth(linkedinToken), byte[].class);
            Assertions.assertEquals("attachment; filename=\"AWD-CW2-v2.pdf\"; filename*=utf-8''AWD-CW2-v2.pdf",
                    additionalDocumentsResponse.header(HttpHeaders.CONTENT_DISPOSITION));
            Assertions.assertArrayEquals(Resources.toByteArray(documentPath), additionalDocumentsResponse.body());

            var firstUserProfilesForFacebook = httpClient.toBlocking().retrieve(HttpRequest.GET("/api/user-profile/" + 2)
                            .bearerAuth(facebookToken)
                    , ExternalApiUserProfiles.class);
            Assertions.assertNotNull(firstUserProfilesForFacebook.current());
            Assertions.assertEquals(0, firstUserProfilesForFacebook.other().size());
            Assertions.assertEquals("Facebook", firstUserProfilesForFacebook.current().integrationName());
            Assertions.assertEquals(4, firstUserProfilesForFacebook.current().fields().size());
            Assertions.assertEquals("MeGaTestPowerUser2025", firstUserProfilesForFacebook.current().fields().get("Name").value());
            Assertions.assertEquals("Test 1 Surname", firstUserProfilesForFacebook.current().fields().get("Surname").value());
            Assertions.assertEquals("1980-01-01", firstUserProfilesForFacebook.current().fields().get("Birthday").value());
            var avatarUrl = String.valueOf(firstUserProfilesForFacebook.current().fields()
                    .get("Avatar").value());
            var avatarResponse = httpClient.toBlocking().exchange(HttpRequest.GET(avatarUrl)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .bearerAuth(facebookToken), byte[].class);
            Assertions.assertEquals("attachment; filename=\"avatar.jpg\"; filename*=utf-8''avatar.jpg",
                    avatarResponse.header(HttpHeaders.CONTENT_DISPOSITION));
            Assertions.assertArrayEquals(Resources.toByteArray(avatarPath), avatarResponse.body());

            // Add changes to profiles, including visibility
            page.getByTestId("field-0-value").fill("MeGaTestPowerUser2026");
            page.getByTestId("field-3-switch-to-raw").click();
            var newAvatarUrl = Resources.getResource("assets/ava-new.jpg");
            page.getByTestId("field-3-value").setInputFiles(Paths.get(newAvatarUrl.toURI()));
            page.getByTestId("is-public").check();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Home")).click();

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Edit")).first().click();

            page.getByTestId("is-public").check();
            page.getByTestId("field-3-value").fill("Meta");
            page.getByTestId("field-4-value").fill("2026-09-10");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Account")).click();

            page.getByTestId("new-field-0-value").fill("Test 1 Name New");
            page.getByTestId("new-field-1-value").fill("Test 1 Surname New");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit")).click();

            // Verify that changes are available for the integrations
            firstUserProfilesForLinkedin = httpClient.toBlocking().retrieve(HttpRequest.GET("/api/user-profile/" + 2)
                            .bearerAuth(linkedinToken)
                    , ExternalApiUserProfiles.class);
            Assertions.assertNotNull(firstUserProfilesForLinkedin.current());
            Assertions.assertEquals(1, firstUserProfilesForLinkedin.other().size());
            Assertions.assertEquals("LinkedIn", firstUserProfilesForLinkedin.current().integrationName());
            Assertions.assertEquals(6, firstUserProfilesForLinkedin.current().fields().size());
            Assertions.assertEquals("Test 1 Name New", firstUserProfilesForLinkedin.current().fields().get("Name").value());
            Assertions.assertEquals("Test 1 Surname New", firstUserProfilesForLinkedin.current().fields().get("Surname").value());
            Assertions.assertEquals("Meta", firstUserProfilesForLinkedin.current().fields().get("Company name").value());
            Assertions.assertEquals("2026-09-10", firstUserProfilesForLinkedin.current().fields().get("Company work start date").value());
            Assertions.assertEquals("1980-01-01", firstUserProfilesForLinkedin.current().fields().get("Birthday").value());
            documentsUrl = String.valueOf(firstUserProfilesForLinkedin.current().fields()
                    .get("Additional documents").value());
            additionalDocumentsResponse = httpClient.toBlocking().exchange(HttpRequest.GET(documentsUrl)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .bearerAuth(linkedinToken), byte[].class);
            Assertions.assertEquals("attachment; filename=\"AWD-CW2-v2.pdf\"; filename*=utf-8''AWD-CW2-v2.pdf",
                    additionalDocumentsResponse.header(HttpHeaders.CONTENT_DISPOSITION));
            Assertions.assertArrayEquals(Resources.toByteArray(documentPath), additionalDocumentsResponse.body());
            var otherIntegrationData = firstUserProfilesForLinkedin.other().get(0);
            Assertions.assertEquals("Facebook", otherIntegrationData.integrationName());
            Assertions.assertEquals(4, otherIntegrationData.fields().size());
            Assertions.assertEquals("MeGaTestPowerUser2026", otherIntegrationData.fields().get("Name").value());
            Assertions.assertEquals("Test 1 Surname New", otherIntegrationData.fields().get("Surname").value());
            Assertions.assertEquals("1980-01-01", otherIntegrationData.fields().get("Birthday").value());
            avatarUrl = String.valueOf(otherIntegrationData.fields()
                    .get("Avatar").value());
            avatarResponse = httpClient.toBlocking().exchange(HttpRequest.GET(avatarUrl)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM_TYPE)
                    .bearerAuth(linkedinToken), byte[].class);
            Assertions.assertEquals("attachment; filename=\"ava-new.jpg\"; filename*=utf-8''ava-new.jpg",
                    avatarResponse.header(HttpHeaders.CONTENT_DISPOSITION));
            Assertions.assertArrayEquals(Resources.toByteArray(newAvatarUrl), avatarResponse.body());

            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout")).click();

            // Try to access LinkedIn profile of test1 using test2 account
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username:")).fill("test2");
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Password:").setExact(true)).fill("test2");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login")).click();

            var response = page.waitForResponse("**/external-profile/1", () -> {
                page.navigate(baseUrl + "app/external-profile/1");
            });
            Assertions.assertEquals(500, response.status());
        }
    }
}
