package com.greenfox.notification.service;

import org.springframework.stereotype.Service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@Service
public class TimeStampService {

  public String getISO8601CurrentDate() {
    Date now = new Date();
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+2"));
    return dateFormat.format(now);
  }
}
