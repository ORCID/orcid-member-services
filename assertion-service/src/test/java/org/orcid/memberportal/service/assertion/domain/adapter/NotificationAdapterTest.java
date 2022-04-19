package org.orcid.memberportal.service.assertion.domain.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.memberportal.service.assertion.domain.Notification;
import org.orcid.memberportal.service.assertion.domain.enumeration.AffiliationSection;

public class NotificationAdapterTest {

    @Test
    void testToNotificationPermission() {
        Notification notification = getNotification();
        NotificationPermission notificationPermission = NotificationAdapter.toNotificationPermission(notification);
        
        assertThat(notificationPermission.getNotificationSubject()).isEqualTo("subject");
        assertThat(notificationPermission.getNotificationType()).isEqualTo(NotificationType.PERMISSION);
        assertThat(notificationPermission.getNotificationIntro()).isEqualTo("intro");
        assertThat(notificationPermission.getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems()).isNotNull();
        assertThat(notificationPermission.getItems().getItems().size()).isEqualTo(1);
        assertThat(notificationPermission.getItems().getItems().get(0).getItemName()).isEqualTo("name");
        assertThat(notificationPermission.getItems().getItems().get(0).getItemType()).isEqualTo(ItemType.EDUCATION);
    }

    private Notification getNotification() {
        Notification notification = new Notification();
        notification.setIntro("intro");
        notification.setName("name");
        notification.setOrcidId("orcid");
        notification.setSubject("subject");
        notification.setType(AffiliationSection.EDUCATION);
        return notification;
    }

}
