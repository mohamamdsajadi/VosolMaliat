package com.mparsict.vosolmaliat.domain.model;

import jakarta.persistence.PreRemove;

/** ORM immutability covers updates; deleting a business identity must also be prohibited. */
public final class ImmutableEntityListener {
    @PreRemove
    public void preventDelete(Object entity) {
        throw new IllegalStateException("حذف این سابقه یا شناسه کسب‌وکار مجاز نیست.");
    }
}
