package gg.itzkatze.katze.bingo;

import gg.itzkatze.katze.bingo.pack.BingoPackAdmission;
import org.bukkit.event.player.PlayerResourcePackStatusEvent.Status;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BingoPackAdmissionTest {
    @Test void acceptingAndDownloadingCannotUnlockPlayBeforeSuccessfulLoad() {
        BingoPackAdmission admission = new BingoPackAdmission();
        UUID player = UUID.randomUUID();
        admission.begin(player, 0);
        admission.update(player, Status.ACCEPTED);
        assertFalse(admission.ready(player));
        admission.update(player, Status.DOWNLOADED);
        assertFalse(admission.ready(player));
        assertEquals(BingoPackAdmission.Outcome.READY, admission.update(player, Status.SUCCESSFULLY_LOADED));
        assertTrue(admission.ready(player));
        assertEquals(List.of(), admission.expired(10000, 120));
    }

    @Test void everyFailureRejectsAndReconnectionRequiresAnotherSuccessfulLoad() {
        for (Status failure : List.of(Status.DECLINED, Status.FAILED_DOWNLOAD, Status.INVALID_URL, Status.FAILED_RELOAD, Status.DISCARDED)) {
            BingoPackAdmission admission = new BingoPackAdmission();
            UUID player = UUID.randomUUID();
            admission.begin(player, 0);
            admission.update(player, Status.SUCCESSFULLY_LOADED);
            assertEquals(BingoPackAdmission.Outcome.REJECT, admission.update(player, failure));
            assertFalse(admission.ready(player));
            assertEquals(BingoPackAdmission.Outcome.IGNORE, admission.update(player, Status.SUCCESSFULLY_LOADED));
            admission.begin(player, 100);
            assertFalse(admission.ready(player));
            admission.update(player, Status.SUCCESSFULLY_LOADED);
            assertTrue(admission.ready(player));
            admission.forget(player);
            assertFalse(admission.ready(player));
        }
    }

    @Test void timeoutDoesNotRestartOnAcceptedOrDownloaded() {
        BingoPackAdmission admission = new BingoPackAdmission();
        UUID player = UUID.randomUUID();
        admission.begin(player, 10);
        admission.update(player, Status.ACCEPTED);
        assertEquals(List.of(), admission.expired(109, 100));
        admission.update(player, Status.DOWNLOADED);
        assertEquals(List.of(player), admission.expired(110, 100));
        assertFalse(admission.ready(player));
        assertEquals(BingoPackAdmission.Outcome.IGNORE, admission.update(player, Status.SUCCESSFULLY_LOADED));
    }
}
