package com.heroku.api.request;

import com.heroku.api.*;
import com.heroku.api.connection.Connection;
import com.heroku.api.connection.HttpClientConnection;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;
import com.heroku.api.http.HttpUtil;
import com.heroku.api.request.addon.AddonInstall;
import com.heroku.api.request.addon.AddonList;
import com.heroku.api.request.addon.AppAddonsList;
import com.heroku.api.request.app.AppCreate;
import com.heroku.api.request.app.AppDestroy;
import com.heroku.api.request.app.AppInfo;
import com.heroku.api.request.app.AppList;
import com.heroku.api.request.config.ConfigList;
import com.heroku.api.request.config.ConfigRemove;
import com.heroku.api.request.log.Log;
import com.heroku.api.request.log.LogStreamResponse;
import com.heroku.api.request.ps.ProcessList;
import com.heroku.api.request.ps.Restart;
import com.heroku.api.request.ps.Scale;
import com.heroku.api.request.releases.ListReleases;
import com.heroku.api.request.releases.ReleaseInfo;
import com.heroku.api.request.run.Run;
import com.heroku.api.request.run.RunResponse;
import com.heroku.api.request.sharing.CollabList;
import com.heroku.api.request.sharing.SharingAdd;
import com.heroku.api.request.sharing.SharingRemove;
import com.heroku.api.request.user.UserInfo;
import com.heroku.api.response.Unit;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.util.RetryAnalyzerCount;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.heroku.api.Heroku.Stack.Cedar;
import static com.heroku.api.IntegrationTestConfig.CONFIG;
import static org.testng.Assert.*;


/**
 * TODO: Javadoc
 *
 * @author Naaman Newbold
 */
public class RequestIntegrationTest extends BaseRequestIntegrationTest {

    static String apiKey = IntegrationTestConfig.CONFIG.getDefaultUser().getApiKey();


    @Test(successPercentage = 80)
    public void testCreateAppCommand() throws IOException {
        AppCreate cmd = new AppCreate(new App().on(Cedar));
        App response = connection.execute(cmd, apiKey);

        assertNotNull(response.getId());
        assertEquals(Heroku.Stack.fromString(response.getStack()), Cedar);
        assertTrue(response.getCreateStatus().equals("complete")); //todo: move "complete" to a static final?
        deleteApp(response.getName());
    }

    @DataProvider
    public Object[][] logParameters() {
        final String appName = getApp().getName();
        return new Object[][]{
                {new Log(appName)},
                {new Log(appName, true)},
                {Log.logFor(appName).tail(false).num(1).getRequest()}
        };
    }

    @Test(dataProvider = "logParameters", retryAnalyzer = LogRetryAnalyzer.class, successPercentage = 10)
    public void testLogCommand(Log log) throws Exception {
        LogStreamResponse logsResponse = connection.execute(log, apiKey);
        assertLogIsReadable(logsResponse);
    }

    @Test(dataProvider = "app")
    public void testAppCommand(App app) throws IOException {
        AppInfo cmd = new AppInfo(app.getName());
        App response = connection.execute(cmd, apiKey);
        assertEquals(response.getName(), app.getName());
    }

    @Test(dataProvider = "app")
    public void testListAppsCommand(App app) throws IOException {
        AppList cmd = new AppList();
        List<App> response = connection.execute(cmd, apiKey);
        assertNotNull(response);
        assertTrue(response.size() > 0, "At least one app should be present, but there are none.");
    }

    // don't use the app dataprovider because it'll try to delete an already deleted app
    @Test
    public void testDestroyAppCommand() throws IOException {
        AppDestroy cmd = new AppDestroy(new HerokuAPI(connection, apiKey).createApp(new App().on(Cedar)).getName());
        Unit response = connection.execute(cmd, apiKey);
        assertNotNull(response);
    }

    @Test(dataProvider = "app")
    public void testSharingAddCommand(App app) throws IOException {
        SharingAdd cmd = new SharingAdd(app.getName(), sharingUser.getUsername());
        Unit response = connection.execute(cmd, apiKey);
        assertNotNull(response);
    }

    @Test(timeOut = 30000L)
    public void testSharingTransferCommand() throws IOException {
        assertNotSame(IntegrationTestConfig.CONFIG.getDefaultUser().getUsername(), sharingUser.getUsername());
        HerokuAPI api = new HerokuAPI(IntegrationTestConfig.CONFIG.getDefaultUser().getApiKey());
        App app = api.createApp(new App().on(Cedar));
        api.addCollaborator(app.getName(), sharingUser.getUsername());
        api.transferApp(app.getName(), sharingUser.getUsername());

        HerokuAPI sharedUserAPI = new HerokuAPI(sharingUser.getApiKey());
        App transferredApp = sharedUserAPI.getApp(app.getName());
        assertEquals(transferredApp.getOwnerEmail(), sharingUser.getUsername());
        sharedUserAPI.destroyApp(transferredApp.getName());
    }

    @Test(dataProvider = "newApp", invocationCount = 5, successPercentage = 20)
    public void testSharingRemoveCommand(App app) throws IOException {
        SharingAdd sharingAddCommand = new SharingAdd(app.getName(), sharingUser.getUsername());
        Unit sharingAddResp = connection.execute(sharingAddCommand, apiKey);
        assertNotNull(sharingAddResp);

        SharingRemove cmd = new SharingRemove(app.getName(), sharingUser.getUsername());
        Unit response = connection.execute(cmd, apiKey);
        assertNotNull(response);

        CollabList collabList = new CollabList(app.getName());
        assertCollaboratorNotPresent(sharingUser.getUsername(), collabList);
    }

    @Test
    public void testConfigAddCommand() throws IOException {
        HerokuAPI api = new HerokuAPI(IntegrationTestConfig.CONFIG.getDefaultUser().getApiKey());
        App app = api.createApp();
        Map<String, String> config = new HashMap<String, String>();
        config.put("FOO", "bar");
        config.put("BAR", "foo");
        api.addConfig(app.getName(), config);
        Map<String, String> retrievedConfig = api.listConfig(app.getName());
        assertEquals(retrievedConfig.get("FOO"), "bar");
        assertEquals(retrievedConfig.get("BAR"), "foo");
    }

    @Test(dataProvider = "app", invocationCount = 4, successPercentage = 25)
    public void testConfigCommand(App app) {
        addConfig(app, "FOO", "BAR");
        Request<Map<String, String>> req = new ConfigList(app.getName());
        Map<String, String> response = connection.execute(req, apiKey);
        assertNotNull(response.get("FOO"));
        assertEquals(response.get("FOO"), "BAR");
    }

    @Test(dataProvider = "newApp")
    public void testConfigRemoveCommand(App app) {
        addConfig(app, "FOO", "BAR", "JOHN", "DOE");
        Request<Map<String, String>> removeRequest = new ConfigRemove(app.getName(), "FOO");
        Map<String, String> response = connection.execute(removeRequest, apiKey);
        assertNotNull(response.get("JOHN"), "Config var 'JOHN' should still exist, but it's not there.");
        assertNull(response.get("FOO"));
    }

    @Test(dataProvider = "app")
    public void testProcessCommand(App app) {
        Request<List<Proc>> req = new ProcessList(app.getName());
        List<Proc> response = connection.execute(req, apiKey);
        assertNotNull(response, "Expected a non-null response for a new app, but the data was null.");
        assertEquals(response.size(), 1);
    }

    @Test(dataProvider = "app")
    public void testScaleCommand(App app) {
        Request<Unit> req = new Scale(app.getName(), "web", 1);
        Unit response = connection.execute(req, apiKey);
        assertNotNull(response);
    }

    @Test(dataProvider = "app")
    public void testRestartCommand(App app) {
        Request<Unit> req = new Restart(app.getName());
        Unit response = connection.execute(req, apiKey);
        assertNotNull(response);
    }

    @Test
    public void testListAddons() {
        AddonList req = new AddonList();
        List<Addon> response = connection.execute(req, apiKey);
        assertNotNull(response, "Expected a response from listing addons, but the result is null.");
    }

    @Test(dataProvider = "newApp")
    public void testListAppAddons(App app) {
        connection.execute(new AddonInstall(app.getName(), "shared-database:5mb"), apiKey);
        Request<List<Addon>> req = new AppAddonsList(app.getName());
        List<Addon> response = connection.execute(req, apiKey);
        assertNotNull(response);
        assertTrue(response.size() > 0, "Expected at least one addon to be present.");
        assertNotNull(response.get(0).getName());
    }

    @Test(dataProvider = "app")
    public void testAddAddonToApp(App app) {
        AddonInstall req = new AddonInstall(app.getName(), "shared-database:5mb");
        AddonChange response = connection.execute(req, apiKey);
        assertEquals(response.getStatus(), "Installed");
    }

    @Test(dataProvider = "newApp")
    public void testCollaboratorList(App app) {
        Request<List<Collaborator>> req = new CollabList(app.getName());
        List<Collaborator> xmlArrayResponse = connection.execute(req, apiKey);
        assertEquals(xmlArrayResponse.size(), 1);
        assertNotNull(xmlArrayResponse.get(0).getEmail());
    }

    @Test(dataProvider = "app")
    public void testRunCommand(App app) throws IOException {
        Run run = new Run(app.getName(), "echo helloworld");
        Run runAttached = new Run(app.getName(), "echo helloworld", true);
        RunResponse response = connection.execute(run, apiKey);
        try {
            response.attach();
            fail("Should throw an illegal state exception");
        } catch (IllegalStateException ex) {
            //ok
        }
        RunResponse responseAttach = connection.execute(runAttached, apiKey);
        String output = HttpUtil.getUTF8String(HttpUtil.getBytes(responseAttach.attach()));
        System.out.println("RUN OUTPUT:" + output);
        assertTrue(output.contains("helloworld"));
    }

    @Test
    public void testUserInfo() {
        IntegrationTestConfig.TestUser testUser = CONFIG.getDefaultUser();
        Connection userInfoConnection = new HttpClientConnection();
        UserInfo userInfo = new UserInfo();
        User user = userInfoConnection.execute(userInfo, testUser.getApiKey());
        assertEquals(user.getEmail(), testUser.getUsername());
    }

    @Test(dataProvider = "app")
    public void testListReleases(App app) {
        List<Release> releases = connection.execute(new ListReleases(app.getName()), apiKey);
        addConfig(app, "releaseTest", "releaseTest");
        List<Release> newReleases = connection.execute(new ListReleases(app.getName()), apiKey);
        assertEquals(newReleases.size(), releases.size() + 1);
    }

    @Test(dataProvider = "app")
    public void testReleaseInfo(App app) {
        addConfig(app, "releaseTest", "releaseTest"); //ensure a release exists
        List<Release> releases = connection.execute(new ListReleases(app.getName()), apiKey);
        Release releaseInfo = connection.execute(new ReleaseInfo(app.getName(), releases.get(0).getName()), apiKey);
        assertEquals(releaseInfo.getName(), releases.get(0).getName());
    }

    public static class LogRetryAnalyzer extends RetryAnalyzerCount {
        public LogRetryAnalyzer() {
            setCount(10);
        }

        @Override
        public boolean retryMethod(ITestResult result) {
            System.out.println("Retry? " + Boolean.valueOf(result.getThrowable() instanceof RequestFailedException && ((RequestFailedException)result.getThrowable()).getStatusCode() == Http.Status.UNPROCESSABLE_ENTITY.statusCode));
            result.setStatus(ITestResult.SKIP);
            return result.getThrowable() instanceof RequestFailedException && ((RequestFailedException)result.getThrowable()).getStatusCode() == Http.Status.UNPROCESSABLE_ENTITY.statusCode;
        }
    }
}