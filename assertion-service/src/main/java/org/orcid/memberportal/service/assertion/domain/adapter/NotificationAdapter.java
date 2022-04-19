package org.orcid.memberportal.service.assertion.domain.adapter;

import java.util.Arrays;

import org.orcid.jaxb.model.v3.release.notification.NotificationType;
import org.orcid.jaxb.model.v3.release.notification.permission.Item;
import org.orcid.jaxb.model.v3.release.notification.permission.ItemType;
import org.orcid.jaxb.model.v3.release.notification.permission.Items;
import org.orcid.jaxb.model.v3.release.notification.permission.NotificationPermission;
import org.orcid.memberportal.service.assertion.domain.Notification;

public class NotificationAdapter {
    
    public static NotificationPermission toNotificationPermission(Notification notification) {
        NotificationPermission notificationPermission = new NotificationPermission();
        notificationPermission.setNotificationIntro(notification.getIntro());
        notificationPermission.setNotificationSubject(notification.getSubject());
        notificationPermission.setNotificationType(NotificationType.PERMISSION);
        
        Item item = new Item();
        item.setItemName(notification.getName());
        
        switch (notification.getType()) {
        case DISTINCTION:
            item.setItemType(ItemType.DISTINCTION);
            break;
        case EDUCATION:
            item.setItemType(ItemType.EDUCATION);
            break;
        case EMPLOYMENT:
            item.setItemType(ItemType.EMPLOYMENT);
            break;
        case INVITED_POSITION:
            item.setItemType(ItemType.INVITED_POSITION);
            break;
        case MEMBERSHIP:
            item.setItemType(ItemType.MEMBERSHIP);
            break;
        case QUALIFICATION:
            item.setItemType(ItemType.QUALIFICATION);
            break;
        case SERVICE:
            item.setItemType(ItemType.SERVICE);
            break;
        default:
            throw new IllegalArgumentException("Invalid item type found");
        }

        notificationPermission.setItems(new Items(Arrays.asList(item)));
        return notificationPermission;
    }

}
