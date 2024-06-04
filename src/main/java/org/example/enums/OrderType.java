package org.example.enums;

import lombok.Getter;

@Getter
public enum OrderType {
  EXIT_LONG(null),
  EXIT_SHORT(null),
  LONG(EXIT_LONG),
  SHORT(EXIT_SHORT);

  private final OrderType next;

  OrderType(OrderType next) {
    this.next = next;
  }
}
