package jira;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.SearchRestClient;
import com.atlassian.jira.rest.client.domain.*;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.atlassian.util.concurrent.Promise;
import com.google.common.collect.Lists;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Utils {
    private static String jiraUrl;
    private static String jiraUsername;
    private static String jiraPassword;

    public JiraRestClient getJiraRestClient() throws URISyntaxException {
        readCredentials();
        // Construct the JRJC client
        URI uri = new URI(jiraUrl);
        System.out.println(String.format("Logging in to %s with username '%s'", uri, jiraUsername));
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();

        JiraRestClient client = factory.createWithBasicHttpAuthentication(uri, jiraUsername, jiraPassword);

        // Invoke the JRJC Client
        Promise<User> promise = client.getUserClient().getUser(jiraUsername);
        com.atlassian.jira.rest.client.domain.User user = promise.claim();

        // Print the result
        System.out.println(String.format("Your admin user's email address is: %s\r\n", user.getEmailAddress()));
        return client;
    }

    private void readCredentials() {
        Properties prop = new Properties();
        InputStream input = null;

        try {
            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            jiraUrl = prop.getProperty("JIRA_URL");
            jiraUsername = prop.getProperty("JIRA_ADMIN_USERNAME");
            jiraPassword = prop.getProperty("JIRA_ADMIN_PASSWORD");

        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ArrayList<IssueType> getIssueTypes(JiraRestClient client) {
        System.out.println("----------Issues types: ----------------");
        ArrayList<IssueType> issueTypes = Lists.newArrayList(client.getMetadataClient().getIssueTypes().claim());
        List<String> collect = issueTypes.stream().map(issueType -> issueType.getName()).collect(Collectors.toList());
        System.out.println(collect);
        return issueTypes;
    }

    public boolean isTestRunWithSameSummary(String summary, String jql, JiraRestClient client) {
        SearchRestClient searchRestClient = client.getSearchClient();
        int maxPerQuery = 200;
        int startIndex = 0;
        AtomicBoolean exists = new AtomicBoolean(false);
        while (true) {
            Promise<SearchResult> searchResult = searchRestClient.searchJql(jql, maxPerQuery, startIndex);
            SearchResult results = searchResult.claim();

            List<BasicIssue> resulIssuesList = Lists.newArrayList(results.getIssues());
            resulIssuesList.stream().map(iss -> {
                Issue issue = client.getIssueClient().getIssue(iss.getKey()).claim();
                if (issue.getSummary().equals(summary)) {
                    exists.set(true);
                }
                return iss;
            }).collect(Collectors.toList());
            if (startIndex >= results.getTotal()) {
                break;
            }
            startIndex += maxPerQuery;
        }
        return exists.get();
    }
}
