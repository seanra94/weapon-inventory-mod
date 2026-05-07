package weaponinventorymod.gui;

import com.fs.starfarer.api.campaign.CoreInteractionListener;

final class WimGuiNoopCoreInteractionListener implements CoreInteractionListener {
    static final WimGuiNoopCoreInteractionListener INSTANCE = new WimGuiNoopCoreInteractionListener();

    private WimGuiNoopCoreInteractionListener() {
    }

    @Override
    public void coreUIDismissed() {
    }
}
