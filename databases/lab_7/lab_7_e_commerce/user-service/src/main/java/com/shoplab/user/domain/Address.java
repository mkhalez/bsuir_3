package com.shoplab.user.domain;

import java.io.Serializable;

public record Address(Long id, Long profileId, String city, String street, String zip, boolean isDefault) implements Serializable {
}
