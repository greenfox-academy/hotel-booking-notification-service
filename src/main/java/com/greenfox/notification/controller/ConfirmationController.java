package com.greenfox.notification.controller;

import com.greenfox.notification.model.Data;
import com.greenfox.notification.service.EmailSenderService;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfirmationController {
  private final EmailSenderService emailSenderService;

  @Autowired
  public ConfirmationController(EmailSenderService emailSenderService) {
    this.emailSenderService = emailSenderService;
  }

  @PostMapping("/email/registration")
  public Data registration(@RequestBody Data data, HttpServletRequest httpServletRequest) throws IOException {
    emailSenderService.sendConfirmationEmail(httpServletRequest, data);
    return data;
  }
}
