package weaponsprocurement.ui

import com.fs.starfarer.api.campaign.CoreInteractionListener

class WimGuiNoopCoreInteractionListener private constructor() : CoreInteractionListener {
    override fun coreUIDismissed() {
    }

    companion object {
        @JvmField
        val INSTANCE: WimGuiNoopCoreInteractionListener = WimGuiNoopCoreInteractionListener()
    }
}
