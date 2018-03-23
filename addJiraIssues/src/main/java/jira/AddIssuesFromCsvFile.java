package jira;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.BasicIssueType;
import com.atlassian.jira.rest.client.domain.IssueType;
import com.atlassian.jira.rest.client.domain.input.IssueInput;
import com.atlassian.jira.rest.client.domain.input.IssueInputBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;


public class AddIssuesFromCsvFile {
    private static JiraRestClient client;
    private static Utils utils;

    public static void main(String[] args) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader("C:/jira/JIRA 2018-03-23T10_59_58+0200.csv"));
        String splitBy = ",";
        String line = br.readLine();

        try {
             utils = new Utils();
            //First go to config.properties file and put your Jira credentitials
            client = utils.getJiraRestClient();
            ArrayList<IssueType> issueTypes = utils.getIssueTypes(client);
            BasicIssueType testRunType = issueTypes.stream().filter(issueType -> "Test Run".equals(issueType.getName())).findAny().orElse(null);
            String jqlTestRun = "project =Test1  and issuetype =\"Test Run\" ORDER BY Rank ASC";

            try {
                System.out.println();
                System.out.println("----------Test Case Issues: ----------------");
                while ((line = br.readLine()) != null) {
                    String[] l = line.split(splitBy);
                    String summary = l[0];
                    String projectKey = l[5];
                    String description = l[22];
                    if (!utils.isTestRunWithSameSummary(summary, jqlTestRun, client)) {
                        adddTestRunFromTestCase(summary, projectKey, description, testRunType);
                        System.out.println("Created new Test Run for " + summary);
                    } else {
                        System.out.println("A Test Run was already created for " + summary);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void adddTestRunFromTestCase(String summary, String projectKey, String description, BasicIssueType testRunType) {

        IssueInputBuilder iib = new IssueInputBuilder(projectKey, testRunType.getId());
        iib.setProjectKey(projectKey);
        iib.setSummary(summary);
        iib.setIssueType(testRunType);
        iib.setDescription(description);

        IssueInput issueInput = iib.build();
        client.getIssueClient().createIssue(issueInput).claim();
    }
}
