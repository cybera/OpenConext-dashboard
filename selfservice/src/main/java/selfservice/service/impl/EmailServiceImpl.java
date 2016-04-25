package selfservice.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.springframework.mail.SimpleMailMessage;

import selfservice.service.EmailService;
import selfservice.util.mail.Emailer;

public class EmailServiceImpl implements EmailService {

  private final String[] administrativeEmails;

  private final Emailer emailer;

  public EmailServiceImpl(String administrativeEmails, Emailer emailer) {
    this.administrativeEmails = Iterables.toArray(Splitter.on(",").split(administrativeEmails), String.class);
    this.emailer = emailer;
  }

  @Override
  public void sendMail(String from, String subject, String body) {
    SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
    simpleMailMessage.setFrom(from);
    simpleMailMessage.setTo(administrativeEmails);
    simpleMailMessage.setSubject(subject);
    simpleMailMessage.setText(body);

    emailer.sendAsync(simpleMailMessage);
  }

}
