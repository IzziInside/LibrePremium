package xyz.kyngs.librepremium.api.premium;

public class PremiumException extends Exception {

    private final Issue issue;

    public PremiumException(Issue issue, Exception exception) {
        super(exception);
        this.issue = issue;
    }

    public PremiumException(Issue issue, String message) {
        super(message);
        this.issue = issue;
    }

    public Issue getIssue() {
        return issue;
    }

    public enum Issue {
        THROTTLED, SERVER_EXCEPTION, UNDEFINED;
    }

}
