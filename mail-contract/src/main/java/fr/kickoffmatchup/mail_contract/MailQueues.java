package fr.kickoffmatchup.mail_contract;

public final class MailQueues {

    private MailQueues() {}

    public static final String EXCHANGE = "mail.exchange";

    public static final String AUTH_ROUTING_KEY = "mail.auth";
    public static final String AUTH_QUEUE = "mail.auth.queue";
    public static final String AUTH_DLQ = "mail.auth.queue.dlq";
}
