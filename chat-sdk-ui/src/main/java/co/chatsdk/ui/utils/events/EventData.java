package co.chatsdk.ui.utils.events;

/**
 * Created by Eraño Payawal on 01/04/2020.
 * hunterxer31@gmail.com
 */
public class EventData {

    public static class CreateButtonClickEvent {

        public CreateButtonClickEvent() {
        }

    }

    public static class UnreadMessageCountEvent {

        public final int unreadMessageCount;

        public UnreadMessageCountEvent(int unreadMessageCount) {
            this.unreadMessageCount = unreadMessageCount;
        }

    }

}
