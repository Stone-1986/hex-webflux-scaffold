package com.co.lab.hex.exceptions;

public interface DomainException {
    String getCode();
    String getTitle();
    Reason getReason();
}
