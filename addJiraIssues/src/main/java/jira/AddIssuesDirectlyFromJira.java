package jira;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.SearchRestClient;
import com.atlassian.jira.rest.client.domain.*;
import com.atlassian.jira.rest.client.domain.input.IssueInput;
import com.atlassian.jira.rest.client.domain.input.IssueInputBuilder;
import com.atlassian.util.concurrent.Promise;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entry-point invoked when the jar is executed.
 */
public class AddIssuesDirectlyFromJira {
    private static JiraRestClient client;
    private static Utils utils;

    public static void main(String[] args) throws Exception {
        //First go to config.properties file and put your Jira credentitials
        try {
            Utils utils =  new Utils();
            client = utils.getJiraRestClient();

            //get issues types
            ArrayList<IssueType> issueTypes = utils.getIssueTypes(client);

            //  Search Test Case issues
            //String jql = "project = \"TOOL Vision Team Test \" AND issuetype=\"Test Case\"  ORDER BY  Rank ASC";
            String jql = "project =Test1  and issuetype =\"Test Case\" ORDER BY Rank ASC";
            String jqlTestRun = "project =Test1  and issuetype =\"Test Run\" ORDER BY Rank ASC";

            int maxPerQuery = 200;
            int startIndex = 0;

            SearchRestClient searchRestClient = client.getSearchClient();

            BasicIssueType testRunType = issueTypes.stream().filter(issueType -> "Test Run".equals(issueType.getName())).findAny().orElse(null);
            System.out.println();
            System.out.println("----------Test Case Issues: ----------------");
            while (true) {
                Promise<SearchResult> searchResult = searchRestClient.searchJql(jql, maxPerQuery, startIndex);
                SearchResult results = searchResult.claim();

                List<BasicIssue> resulIssuesList = Lists.newArrayList(results.getIssues());

                // System.out.println(resulIssuesList.size() + " Test Cases found");
                resulIssuesList.stream().map(iss -> {
                    Issue issue = client.getIssueClient().getIssue(iss.getKey()).claim();
                    System.out.println(issue.getKey() + ", " + issue.getSummary());
                    if (!utils.isTestRunWithSameSummary(issue.getSummary(), jqlTestRun, client)) {
                        adddTestRunFromTestCase(issue, testRunType);
                        System.out.println("Test Run created for " + issue.getSummary());
                    }else{
                        System.out.println("A Test Run was already created for " + issue.getSummary());
                    }
                    return iss;
                }).collect(Collectors.toList());
                if (startIndex >= results.getTotal()) {
                    break;
                }
                startIndex += maxPerQuery;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void adddTestRunFromTestCase(Issue testCaseIssue, BasicIssueType testRunType) {
        IssueInputBuilder iib = new IssueInputBuilder(testCaseIssue.getProject().getKey(), testRunType.getId());
        iib.setProjectKey(testCaseIssue.getProject().getKey());
        iib.setSummary(testCaseIssue.getSummary());
        iib.setIssueType(testRunType);
        iib.setDescription(testCaseIssue.getDescription());
        iib.setPriorityId(testCaseIssue.getPriority().getId());

        IssueInput issueInput = iib.build();
        client.getIssueClient().createIssue(issueInput).claim();
    }
}
