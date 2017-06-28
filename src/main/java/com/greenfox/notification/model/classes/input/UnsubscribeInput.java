package com.greenfox.notification.model.classes.input;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnsubscribeInput {
  private UnsubscribeData data;

  public UnsubscribeInput(UnsubscribeData data) {
    this.data = data;
  }

  public UnsubscribeInput(){
  }
}
