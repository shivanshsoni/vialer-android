package com.voipgrid.vialer.t9;

import java.io.ObjectStreamException;
import java.util.Objects;

/**
 * Created by marcov on 5-2-16.
 */
public class T9Result {

    public static final String NUMBER_TYPE = "NUMBER";
    public static final String NAME_TYPE = "NAME";

    private long contactId;
    private String displayName;
    private String number;

    public T9Result(long contactId, String displayName, String number) {
        setContactId(contactId);
        setDisplayName(displayName);
        setNumber(number);
    }

    public long getContactId() {
        return contactId;
    }

    public void setContactId(long contactId) {
        this.contactId = contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
